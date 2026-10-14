package thaumcraft.common.golems.seals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemPayloads;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.network.ModNetwork;

/**
 * The seal types and the placed seals of each dimension. The server keeps its seals in a SavedData per level (1.12
 * kept them in the chunk data); clients get a copy of each seal of their dimension.
 */
public class SealHandler {
   public static final LinkedHashMap<String, ISeal> types = new LinkedHashMap<>();
   private static final Map<ResourceKey<Level>, ConcurrentHashMap<SealPos, SealEntity>> SERVER = new ConcurrentHashMap<>();
   private static final Map<ResourceKey<Level>, ConcurrentHashMap<SealPos, SealEntity>> CLIENT = new ConcurrentHashMap<>();
   private static final Map<ResourceKey<Level>, Data> DATA = new ConcurrentHashMap<>();
   static int count = 0;

   public static void registerSeal(ISeal seal) {
      if (types.containsKey(seal.getKey())) {
         Thaumcraft.LOGGER.error("Attempting to register Seal [{}] twice. Ignoring.", seal.getKey());
      } else {
         types.put(seal.getKey(), seal);
      }
   }

   public static String[] getRegisteredSeals() {
      return types.keySet().toArray(new String[0]);
   }

   @Nullable
   public static ISeal getSeal(String key) {
      return types.get(key);
   }

   public static ConcurrentHashMap<SealPos, SealEntity> getSeals(Level level) {
      return (level.isClientSide ? CLIENT : SERVER).computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
   }

   public static CopyOnWriteArrayList<SealEntity> getSealsInRange(Level level, BlockPos source, int range) {
      CopyOnWriteArrayList<SealEntity> out = new CopyOnWriteArrayList<>();
      for (SealEntity se : getSeals(level).values()) {
         if (se.getSeal() != null && se.getSealPos() != null && se.sealPos.pos.distSqr(source) <= range * range) out.add(se);
      }
      return out;
   }

   public static CopyOnWriteArrayList<SealEntity> getSealsInChunk(Level level, ChunkPos chunk) {
      CopyOnWriteArrayList<SealEntity> out = new CopyOnWriteArrayList<>();
      for (SealEntity se : getSeals(level).values()) {
         if (se.getSeal() != null && se.getSealPos() != null && new ChunkPos(se.sealPos.pos).equals(chunk)) out.add(se);
      }
      return out;
   }

   public static void removeSealEntity(Level level, SealPos pos, boolean quiet) {
      SealEntity seal = getSeals(level).remove(pos);
      try {
         if (!level.isClientSide && seal != null && seal.seal != null) seal.seal.onRemoval(level, pos.pos, pos.face);
         if (!quiet && seal != null && !level.isClientSide && seal.getSeal() != null) {
            ItemStack drop = GolemHelper.getSealStack(seal.getSeal().getKey());
            if (!drop.isEmpty()) {
               level.addFreshEntity(new ItemEntity(level, pos.pos.getX() + 0.5 + pos.face.getStepX() / 1.7F,
                  pos.pos.getY() + 0.5 + pos.face.getStepY() / 1.7F, pos.pos.getZ() + 0.5 + pos.face.getStepZ() / 1.7F, drop));
            }
         }
      } catch (Exception e) {
         Thaumcraft.LOGGER.warn("Removing invalid seal at {}", pos.pos);
      }
      if (level instanceof ServerLevel sl) {
         for (Task task : TaskHandler.getTasks(level).values()) {
            if (task.getSealPos() != null && task.getSealPos().equals(pos)) task.setSuspended(true);
         }
         sendToDimension(sl, new SealEntity(pos, null));
         markDirty(sl);
      }
   }

   @Nullable
   public static ISealEntity getSealEntity(Level level, @Nullable SealPos pos) {
      if (pos == null) return null;
      return getSeals(level).get(pos);
   }

   public static boolean addSealEntity(Level level, BlockPos pos, Direction face, ISeal seal, Player player) {
      ConcurrentHashMap<SealPos, SealEntity> se = getSeals(level);
      SealPos sp = new SealPos(pos, face);
      if (se.containsKey(sp)) return false;
      SealEntity sealent = new SealEntity(sp, seal);
      sealent.setOwner(player.getUUID().toString());
      se.put(sp, sealent);
      if (level instanceof ServerLevel sl) {
         sealent.syncToClient(level);
         markDirty(sl);
      }
      return true;
   }

   public static boolean addSealEntity(Level level, SealEntity seal) {
      ConcurrentHashMap<SealPos, SealEntity> se = getSeals(level);
      if (se.containsKey(seal.getSealPos())) return false;
      se.put(seal.getSealPos(), seal);
      if (level instanceof ServerLevel sl) {
         seal.syncToClient(level);
         markDirty(sl);
      }
      return true;
   }

   public static void tickSealEntities(ServerLevel level) {
      count++;
      // Seals keep task state (replanting, filters) that changes while they work; save it now and then.
      if (count % 600 == 0 && !getSeals(level).isEmpty()) markDirty(level);
      for (SealEntity sealEntity : getSeals(level).values()) {
         if (!level.isLoaded(sealEntity.sealPos.pos)) continue;
         try {
            boolean tick = true;
            if (count % 20 == 0 && !sealEntity.seal.canPlaceAt(level, sealEntity.sealPos.pos, sealEntity.sealPos.face)) {
               removeSealEntity(level, sealEntity.sealPos, false);
               tick = false;
            }
            if (tick) sealEntity.tickSealEntity(level);
         } catch (Exception e) {
            Thaumcraft.LOGGER.warn("Seal at {} failed to tick", sealEntity.sealPos, e);
            removeSealEntity(level, sealEntity.sealPos, false);
         }
      }
   }

   // ---------------------------------------------------------------- sync

   public static void sendToDimension(ServerLevel level, SealEntity seal) {
      CompoundTag tag = seal.writeNBT(level.registryAccess());
      for (ServerPlayer p : level.players()) ModNetwork.sendTo(p, new GolemPayloads.SealSync(tag));
   }

   /** PacketSealToClient for all seals of the player's dimension (on login and dimension change). */
   public static void sendAll(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      ModNetwork.sendTo(player, new GolemPayloads.SealSync(clearTag()));
      for (SealEntity se : getSeals(level).values()) {
         if (se.getSeal() != null) ModNetwork.sendTo(player, new GolemPayloads.SealSync(se.writeNBT(level.registryAccess())));
      }
   }

   private static CompoundTag clearTag() {
      CompoundTag t = new CompoundTag();
      t.putString("type", "CLEAR");
      return t;
   }

   /** Client side of PacketSealToClient. */
   public static void acceptSync(Level clientLevel, CompoundTag tag) {
      String type = tag.getString("type");
      if (type.equals("CLEAR")) {
         getSeals(clientLevel).clear();
         return;
      }
      SealPos sp = new SealPos(BlockPos.of(tag.getLong("pos")), Direction.from3DDataValue(tag.getByte("face")));
      getSeals(clientLevel).remove(sp);
      if (type.equals("REMOVE") || getSeal(type) == null) return;
      SealEntity seal = new SealEntity();
      seal.readNBT(tag, clientLevel.registryAccess());
      getSeals(clientLevel).put(sp, seal);
   }

   public static void clearClient() {
      CLIENT.clear();
   }

   // ---------------------------------------------------------------- persistence

   public static void markDirty(ServerLevel level) {
      Data d = DATA.get(level.dimension());
      if (d != null) d.setDirty();
   }

   public static void loadLevel(ServerLevel level) {
      SERVER.remove(level.dimension());
      Data d = level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(() -> new Data(level),
         (tag, reg) -> Data.load(level, tag, reg), null), "thaumcraft_seals");
      DATA.put(level.dimension(), d);
   }

   public static void unloadLevel(ServerLevel level) {
      SERVER.remove(level.dimension());
      DATA.remove(level.dimension());
      TaskHandler.tasks.remove(level.dimension());
      GolemHelper.provisionRequests.remove(level.dimension());
   }

   /** The seals of a level, saved with the world. */
   public static class Data extends SavedData {
      private final ServerLevel level;

      Data(ServerLevel level) {
         this.level = level;
      }

      static Data load(ServerLevel level, CompoundTag tag, HolderLookup.Provider reg) {
         Data d = new Data(level);
         ListTag list = tag.getList("seals", Tag.TAG_COMPOUND);
         ConcurrentHashMap<SealPos, SealEntity> map = getSeals(level);
         for (int i = 0; i < list.size(); i++) {
            SealEntity seal = new SealEntity();
            seal.readNBT(list.getCompound(i), reg);
            if (seal.getSeal() != null) map.put(seal.getSealPos(), seal);
         }
         return d;
      }

      @Override
      public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
         ListTag list = new ListTag();
         for (SealEntity se : getSeals(level).values()) {
            if (se.getSeal() != null) list.add(se.writeNBT(reg));
         }
         tag.put("seals", list);
         return tag;
      }
   }

   public static List<String> keys() {
      return List.copyOf(types.keySet());
   }
}
