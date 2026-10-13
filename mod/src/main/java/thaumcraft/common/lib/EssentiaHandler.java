package thaumcraft.common.lib;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectSource;
import thaumcraft.common.network.EssentiaSourcePayload;
import thaumcraft.common.network.ModNetwork;

/**
 * EssentiaHandler: lets machines (essentia transfusers, the infusion matrix, the thaumatorium...) take essentia from,
 * or put it into, the aspect sources (jars) within range, without tubes. The nearest sources are tried first and the
 * list is cached per machine; a machine that found nothing waits 10 seconds before scanning again.
 */
public final class EssentiaHandler {
   private static final long DELAY = 10000L;
   private static final Map<Key, List<BlockPos>> SOURCES = new HashMap<>();
   private static final Map<Key, Long> SOURCES_DELAY = new HashMap<>();
   // Pending drain of drainEssentiaWithConfirmation, done by confirmDrain().
   private static BlockEntity lat;
   private static BlockEntity las;
   private static Aspect lasp;
   private static int lext;

   private record Key(ResourceKey<Level> dim, BlockPos pos) {
   }

   private EssentiaHandler() {
   }

   private static Key key(BlockEntity tile) {
      return new Key(tile.getLevel().dimension(), tile.getBlockPos());
   }

   @Nullable
   private static List<BlockPos> sources(BlockEntity tile, @Nullable Direction direction, int range) {
      Key k = key(tile);
      if (!SOURCES.containsKey(k)) findSources(tile.getLevel(), k, direction, range);
      return SOURCES.get(k);
   }

   private static void forget(Key k) {
      SOURCES.remove(k);
      SOURCES_DELAY.put(k, System.currentTimeMillis() + DELAY);
   }

   public static boolean drainEssentia(BlockEntity tile, Aspect aspect, @Nullable Direction direction, int range, int ext) {
      return drainEssentia(tile, aspect, direction, range, false, ext);
   }

   public static boolean drainEssentia(BlockEntity tile, Aspect aspect, @Nullable Direction direction, int range, boolean ignoreMirror, int ext) {
      List<BlockPos> list = sources(tile, direction, range);
      if (list == null) return false;
      for (BlockPos source : list) {
         if (!(tile.getLevel().getBlockEntity(source) instanceof IAspectSource as)) break;
         if (!as.isBlocked() && as.takeFromContainer(aspect, 1)) {
            fx(tile.getLevel(), tile.getBlockPos(), source, aspect.getColor(), ext);
            return true;
         }
      }
      forget(key(tile));
      return false;
   }

   /** Like drainEssentia, but only remembers the source; call confirmDrain() once the essentia was actually used. */
   public static boolean drainEssentiaWithConfirmation(BlockEntity tile, Aspect aspect, @Nullable Direction direction, int range,
                                                       boolean ignoreMirror, int ext) {
      List<BlockPos> list = sources(tile, direction, range);
      if (list == null) return false;
      for (BlockPos source : list) {
         BlockEntity sourceTile = tile.getLevel().getBlockEntity(source);
         if (!(sourceTile instanceof IAspectSource as)) break;
         if (!as.isBlocked() && as.doesContainerContainAmount(aspect, 1)) {
            las = sourceTile;
            lasp = aspect;
            lat = tile;
            lext = ext;
            return true;
         }
      }
      forget(key(tile));
      return false;
   }

   public static void confirmDrain() {
      if (las instanceof IAspectSource as && lasp != null && lat != null && as.takeFromContainer(lasp, 1)) {
         fx(lat.getLevel(), lat.getBlockPos(), las.getBlockPos(), lasp.getColor(), lext);
      }
      las = null;
      lasp = null;
      lat = null;
   }

   /** Puts one essentia into a source in range: one already holding some first, then empty ones. */
   public static boolean addEssentia(BlockEntity tile, Aspect aspect, @Nullable Direction direction, int range, boolean ignoreMirror, int ext) {
      List<BlockPos> list = sources(tile, direction, range);
      if (list == null) return false;
      List<BlockPos> empties = new ArrayList<>();
      for (BlockPos source : list) {
         if (!(tile.getLevel().getBlockEntity(source) instanceof IAspectSource as)) break;
         if (as.isBlocked()) continue;
         if (as.doesContainerAccept(aspect) && (as.getAspects() == null || as.getAspects().visSize() == 0)) {
            empties.add(source);
         } else if (as.doesContainerAccept(aspect) && as.addToContainer(aspect, 1) <= 0) {
            fx(tile.getLevel(), source, tile.getBlockPos(), aspect.getColor(), ext);
            return true;
         }
      }
      for (BlockPos source : empties) {
         if (!(tile.getLevel().getBlockEntity(source) instanceof IAspectSource as)) break;
         if (as.doesContainerAccept(aspect) && as.addToContainer(aspect, 1) <= 0) {
            fx(tile.getLevel(), source, tile.getBlockPos(), aspect.getColor(), ext);
            return true;
         }
      }
      forget(key(tile));
      return false;
   }

   public static boolean findEssentia(BlockEntity tile, Aspect aspect, @Nullable Direction direction, int range, boolean ignoreMirror) {
      List<BlockPos> list = sources(tile, direction, range);
      if (list == null) return false;
      for (BlockPos source : list) {
         if (!(tile.getLevel().getBlockEntity(source) instanceof IAspectSource as)) break;
         if (!as.isBlocked() && as.doesContainerContainAmount(aspect, 1)) return true;
      }
      return false;
   }

   public static boolean canAcceptEssentia(BlockEntity tile, Aspect aspect, @Nullable Direction direction, int range, boolean ignoreMirror) {
      List<BlockPos> list = sources(tile, direction, range);
      if (list == null) return false;
      for (BlockPos source : list) {
         if (!(tile.getLevel().getBlockEntity(source) instanceof IAspectSource as)) break;
         if (!as.isBlocked() && as.doesContainerAccept(aspect)) return true;
      }
      return false;
   }

   public static void refreshSources(BlockEntity tile) {
      SOURCES.remove(key(tile));
   }

   /**
    * Scans the box in front of the machine (range blocks deep in that direction, +-range sideways), or the whole cube
    * around it when direction is null, and keeps the sources sorted by distance.
    */
   private static void findSources(Level level, Key k, @Nullable Direction direction, int range) {
      Long d = SOURCES_DELAY.get(k);
      if (d != null) {
         if (d > System.currentTimeMillis()) return;
         SOURCES_DELAY.remove(k);
      }
      int start = 0;
      if (direction == null) {
         start = -range;
         direction = Direction.UP;
      }
      List<BlockPos> found = new ArrayList<>();
      BlockPos origin = k.pos();
      for (int aa = -range; aa <= range; aa++) {
         for (int bb = -range; bb <= range; bb++) {
            for (int cc = start; cc < range; cc++) {
               if (aa == 0 && bb == 0 && cc == 0) continue;
               BlockPos p;
               if (direction.getStepY() != 0) {
                  p = origin.offset(aa, cc * direction.getStepY(), bb);
               } else if (direction.getStepX() == 0) {
                  p = origin.offset(aa, bb, cc * direction.getStepZ());
               } else {
                  p = origin.offset(cc * direction.getStepX(), aa, bb);
               }
               if (level.isLoaded(p) && level.getBlockEntity(p) instanceof IAspectSource) found.add(p.immutable());
            }
         }
      }
      if (found.isEmpty()) {
         SOURCES_DELAY.put(k, System.currentTimeMillis() + DELAY);
      } else {
         found.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
         SOURCES.put(k, found);
      }
   }

   /** PacketFXEssentiaSource: a stream of essentia from one block to the other. */
   private static void fx(Level level, BlockPos to, BlockPos from, int color, int ext) {
      if (level instanceof ServerLevel sl) {
         ModNetwork.sendNear(sl, to.getX(), to.getY(), to.getZ(), 32.0, new EssentiaSourcePayload(from, to, color, ext));
      }
   }
}
