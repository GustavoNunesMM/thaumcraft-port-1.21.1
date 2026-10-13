package thaumcraft.common.world.features;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block placement for the ported 1.12 tree generators. Records what it places so leaf decay distances can be
 * set afterwards: 1.21 leaves decay unless they know their distance to a log, which 1.12 trees never had to care about.
 */
final class TreeHelper {
   final LevelAccessor level;
   private final Set<BlockPos> logs = new HashSet<>();
   private final Set<BlockPos> leaves = new HashSet<>();

   TreeHelper(LevelAccessor level) {
      this.level = level;
   }

   /** WorldGenAbstractTree.canGrowInto */
   static boolean canGrowInto(BlockState s) {
      return s.isAir() || s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS)
         || s.is(BlockTags.DIRT) || s.is(BlockTags.SAPLINGS) || s.is(Blocks.VINE);
   }

   static boolean isSoil(BlockState s) {
      return s.is(BlockTags.DIRT);
   }

   BlockState get(BlockPos pos) {
      return level.getBlockState(pos);
   }

   void log(BlockPos pos, Block log, Direction.Axis axis) {
      level.setBlock(pos, log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis), 19);
      logs.add(pos.immutable());
      leaves.remove(pos);
   }

   void leaf(BlockPos pos, Block leaf) {
      level.setBlock(pos, leaf.defaultBlockState(), 19);
      leaves.add(pos.immutable());
   }

   /** Gives every placed leaf block its real distance to the nearest placed log (7 = unreachable, will decay). */
   void finish() {
      Map<BlockPos, Integer> dist = new HashMap<>();
      ArrayDeque<BlockPos> queue = new ArrayDeque<>();
      for (BlockPos p : logs) {
         dist.put(p, 0);
         queue.add(p);
      }
      while (!queue.isEmpty()) {
         BlockPos p = queue.poll();
         int d = dist.get(p);
         if (d >= 6) continue;
         for (Direction dir : Direction.values()) {
            BlockPos n = p.relative(dir);
            if (leaves.contains(n) && !dist.containsKey(n)) {
               dist.put(n, d + 1);
               queue.add(n);
            }
         }
      }
      for (BlockPos p : leaves) {
         BlockState s = level.getBlockState(p);
         if (s.hasProperty(LeavesBlock.DISTANCE)) {
            level.setBlock(p, s.setValue(LeavesBlock.DISTANCE, dist.getOrDefault(p, LeavesBlock.DECAY_DISTANCE)), 19);
         }
      }
   }
}
