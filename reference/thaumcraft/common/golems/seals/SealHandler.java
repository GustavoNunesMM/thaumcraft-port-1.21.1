package thaumcraft.common.golems.seals;

import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketSealToClient;
import thaumcraft.common.world.aura.AuraHandler;

public class SealHandler {
   public static LinkedHashMap<String, ISeal> types = new LinkedHashMap<>();
   private static int lastID = 0;
   public static ConcurrentHashMap<Integer, ConcurrentHashMap<SealPos, SealEntity>> sealEntities = new ConcurrentHashMap<>();
   static int count = 0;

   public static void registerSeal(ISeal seal) {
      if (types.containsKey(seal.getKey())) {
         Thaumcraft.log.error("Attempting to register Seal [" + seal.getKey() + "] twice. Ignoring.");
      } else {
         types.put(seal.getKey(), seal);
      }
   }

   public static String[] getRegisteredSeals() {
      return types.keySet().toArray(new String[0]);
   }

   public static ISeal getSeal(String key) {
      return types.get(key);
   }

   public static CopyOnWriteArrayList<SealEntity> getSealsInRange(World world, BlockPos source, int range) {
      CopyOnWriteArrayList<SealEntity> out = new CopyOnWriteArrayList<>();
      ConcurrentHashMap<SealPos, SealEntity> list = sealEntities.get(world.provider.getDimension());
      if (list != null && list.size() > 0) {
         for (SealEntity se : list.values()) {
            if (se.getSeal() != null && se.getSealPos() != null && se.sealPos.pos.distanceSq(source) <= range * range) {
               out.add(se);
            }
         }
      }

      return out;
   }

   public static CopyOnWriteArrayList<SealEntity> getSealsInChunk(World world, ChunkPos chunk) {
      CopyOnWriteArrayList<SealEntity> out = new CopyOnWriteArrayList<>();
      ConcurrentHashMap<SealPos, SealEntity> list = sealEntities.get(world.provider.getDimension());
      if (list != null && list.size() > 0) {
         for (SealEntity se : list.values()) {
            if (se.getSeal() != null && se.getSealPos() != null) {
               ChunkPos cc = new ChunkPos(se.sealPos.pos);
               if (cc.equals(chunk)) {
                  out.add(se);
               }
            }
         }
      }

      return out;
   }

   public static void removeSealEntity(World world, SealPos pos, boolean quiet) {
      if (!sealEntities.containsKey(world.provider.getDimension())) {
         sealEntities.put(world.provider.getDimension(), new ConcurrentHashMap<>());
      }

      ConcurrentHashMap<SealPos, SealEntity> se = sealEntities.get(world.provider.getDimension());
      if (se != null) {
         SealEntity seal = se.remove(pos);

         try {
            if (!world.isRemote && seal != null && seal.seal != null) {
               seal.seal.onRemoval(world, pos.pos, pos.face);
            }

            if (!quiet && seal != null && !world.isRemote) {
               String[] rs = getRegisteredSeals();
               int indx = 1;

               for (String s : rs) {
                  if (s.equals(seal.getSeal().getKey())) {
                     world.spawnEntity(
                        new EntityItem(
                           world,
                           pos.pos.getX() + 0.5 + pos.face.getXOffset() / 1.7F,
                           pos.pos.getY() + 0.5 + pos.face.getYOffset() / 1.7F,
                           pos.pos.getZ() + 0.5 + pos.face.getZOffset() / 1.7F,
                           new ItemStack(ItemsTC.seals, 1, indx)
                        )
                     );
                     break;
                  }

                  indx++;
               }
            }
         } catch (Exception var11) {
            Thaumcraft.log.warn("Removing invalid seal at " + pos.pos);
         }

         ConcurrentHashMap<Integer, Task> ts = TaskHandler.getTasks(world.provider.getDimension());

         for (Task task : ts.values()) {
            if (task.getSealPos() != null && task.getSealPos().equals(pos)) {
               task.setSuspended(true);
            }
         }

         if (!world.isRemote) {
            PacketHandler.INSTANCE.sendToDimension(new PacketSealToClient(new SealEntity(world, pos, null)), world.provider.getDimension());
         }

         if (!quiet) {
            markChunkAsDirty(world.provider.getDimension(), pos.pos);
         }
      }
   }

   public static ISealEntity getSealEntity(int dim, SealPos pos) {
      if (!sealEntities.containsKey(dim)) {
         sealEntities.put(dim, new ConcurrentHashMap<>());
      }

      if (pos == null) {
         return null;
      } else {
         ConcurrentHashMap<SealPos, SealEntity> se = sealEntities.get(dim);
         return se != null ? se.get(pos) : null;
      }
   }

   public static boolean addSealEntity(World world, BlockPos pos, EnumFacing face, ISeal seal, EntityPlayer player) {
      if (!sealEntities.containsKey(world.provider.getDimension())) {
         sealEntities.put(world.provider.getDimension(), new ConcurrentHashMap<>());
      }

      ConcurrentHashMap<SealPos, SealEntity> se = sealEntities.get(world.provider.getDimension());
      SealPos sp = new SealPos(pos, face);
      if (se.containsKey(sp)) {
         return false;
      } else {
         SealEntity sealent = new SealEntity(world, sp, seal);
         sealent.setOwner(player.getUniqueID().toString());
         se.put(sp, sealent);
         if (!world.isRemote) {
            sealent.syncToClient(world);
            markChunkAsDirty(world.provider.getDimension(), pos);
         }

         return true;
      }
   }

   public static boolean addSealEntity(World world, SealEntity seal) {
      if (world != null && sealEntities != null) {
         if (!sealEntities.containsKey(world.provider.getDimension())) {
            sealEntities.put(world.provider.getDimension(), new ConcurrentHashMap<>());
         }

         ConcurrentHashMap<SealPos, SealEntity> se = sealEntities.get(world.provider.getDimension());
         if (se.containsKey(seal.getSealPos())) {
            return false;
         } else {
            se.put(seal.getSealPos(), seal);
            if (!world.isRemote) {
               seal.syncToClient(world);
               markChunkAsDirty(world.provider.getDimension(), seal.getSealPos().pos);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static void tickSealEntities(World world) {
      if (!sealEntities.containsKey(world.provider.getDimension())) {
         sealEntities.put(world.provider.getDimension(), new ConcurrentHashMap<>());
      }

      ConcurrentHashMap<SealPos, SealEntity> se = sealEntities.get(world.provider.getDimension());
      count++;

      for (SealEntity sealEntity : se.values()) {
         if (world.isBlockLoaded(sealEntity.sealPos.pos)) {
            try {
               boolean tick = true;
               if (count % 20 == 0 && !sealEntity.seal.canPlaceAt(world, sealEntity.sealPos.pos, sealEntity.sealPos.face)) {
                  removeSealEntity(world, sealEntity.sealPos, false);
                  tick = false;
               }

               if (tick) {
                  sealEntity.tickSealEntity(world);
               }
            } catch (Exception var5) {
               removeSealEntity(world, sealEntity.sealPos, false);
            }
         }
      }
   }

   public static void markChunkAsDirty(int dim, BlockPos bp) {
      ChunkPos pos = new ChunkPos(bp);
      if (!AuraHandler.dirtyChunks.containsKey(dim)) {
         AuraHandler.dirtyChunks.put(dim, new CopyOnWriteArrayList<>());
      }

      CopyOnWriteArrayList<ChunkPos> dc = AuraHandler.dirtyChunks.get(dim);
      if (!dc.contains(pos)) {
         dc.add(pos);
      }
   }
}
