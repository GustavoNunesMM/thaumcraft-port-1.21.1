package thaumcraft.common.blocks.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

/** Port of BlockSaplingTC. Growth (light 9+, 1 in 7 per random tick, two stages) is vanilla's; bone meal is weaker. */
public class TCSaplingBlock extends SaplingBlock {
   public TCSaplingBlock(TreeGrower grower, Properties properties) {
      super(grower, properties);
   }

   @Override
   public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
      return level.random.nextFloat() < 0.25F;
   }
}
