package thaumcraft.common.blocks.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.world.aura.AuraHandler;

/** Natural (non-player-placed) silverwood leaves slowly refill a depleted aura, as BlockLeavesTC.updateTick did. */
public class SilverwoodLeavesBlock extends LeavesBlock {
   public SilverwoodLeavesBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected boolean isRandomlyTicking(BlockState state) {
      return !state.getValue(PERSISTENT);
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!state.getValue(PERSISTENT) && AuraHandler.getVis(level, pos) < AuraHandler.getAuraBase(level, pos)) {
         AuraHandler.addVis(level, pos, 0.01F);
      }
      super.randomTick(state, level, pos, random);
   }
}
