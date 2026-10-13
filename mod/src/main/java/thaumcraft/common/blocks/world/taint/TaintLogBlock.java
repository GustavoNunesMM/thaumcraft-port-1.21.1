package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** BlockTaintLog: a tree trunk taken over by taint; decays into flux goo away from taint seeds. */
public class TaintLogBlock extends RotatedPillarBlock implements ITaintBlock {
   public TaintLogBlock(Properties properties) {
      super(properties);
   }

   @Override
   public void die(Level level, BlockPos pos, BlockState state) {
      level.setBlockAndUpdate(pos, thaumcraft.common.registers.ModFlux.FLUX_GOO.get().defaultBlockState());
   }

   @Override
   protected boolean isRandomlyTicking(BlockState state) {
      return true;
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!TaintHelper.isNearTaintSeed(level, pos)) die(level, pos, state);
      else TaintHelper.spreadFibres(level, pos);
   }
}
