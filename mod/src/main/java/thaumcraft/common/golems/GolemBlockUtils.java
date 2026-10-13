package thaumcraft.common.golems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.items.casters.foci.BlockWorks;

/** The BlockUtils/Utils pieces the lumberjack and breaker seals use. */
public final class GolemBlockUtils {
   private GolemBlockUtils() {
   }

   public static boolean isWoodLog(Level level, BlockPos pos) {
      return level.getBlockState(pos).is(BlockTags.LOGS);
   }

   /** BlockUtils.findBlocks: walks the connected blocks of the same kind, always to a further one, and returns the last. */
   public static BlockPos findFurthest(Level level, BlockPos pos, BlockState block, int reach) {
      BlockPos last = pos;
      double lastDistance = 0.0;
      boolean moved = true;
      int guard = 0;
      while (moved && guard++ < 512) {
         moved = false;
         search:
         for (int xx = -reach; xx <= reach; xx++) {
            for (int yy = reach; yy >= -reach; yy--) {
               for (int zz = -reach; zz <= reach; zz++) {
                  BlockPos q = last.offset(xx, yy, zz);
                  if (Math.abs(q.getX() - pos.getX()) > 24 || Math.abs(q.getY() - pos.getY()) > 48 || Math.abs(q.getZ() - pos.getZ()) > 24) {
                     break search;
                  }
                  BlockState bs = level.getBlockState(q);
                  if (bs.getBlock() == block.getBlock() && bs.getDestroySpeed(level, q) >= 0.0F) {
                     double d = q.distSqr(pos);
                     if (d > lastDistance) {
                        lastDistance = d;
                        last = q;
                        moved = true;
                        break search;
                     }
                  }
               }
            }
         }
      }
      return last;
   }

   /** BlockUtils.breakFurthestBlock: harvests the furthest connected block of the same kind (the top of a tree). */
   public static boolean breakFurthestBlock(ServerLevel level, BlockPos pos, BlockState block, Player player) {
      BlockPos target = findFurthest(level, pos, block, isWoodLog(level, pos) ? 2 : 1);
      boolean worked = BlockWorks.harvestBlock(level, player, target, false, false, 0, true);
      if (worked && isWoodLog(level, pos)) {
         // Leaves nearby re-check whether they are still attached.
         for (BlockPos q : BlockPos.betweenClosed(target.offset(-3, -3, -3), target.offset(3, 3, 3))) {
            BlockState s = level.getBlockState(q);
            if (s.is(BlockTags.LEAVES)) level.scheduleTick(q.immutable(), s.getBlock(), 50 + level.random.nextInt(75));
         }
      }
      return worked;
   }

   /** BlockUtils.destroyBlockPartially: the crack overlay as the golem grinds a block. */
   public static void destroyBlockPartially(Level level, int entityId, BlockPos pos, int progress) {
      level.destroyBlockProgress(entityId, pos, progress);
   }
}
