package thaumcraft.common.blocks.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import thaumcraft.common.lib.MobFXBridge;

/**
 * BlockGrassAmbient: the grass of the magical forest. It behaves like (and spreads) plain grass, and at daylight it
 * makes wispy motes drift over the grass around it.
 */
public class GrassAmbientBlock extends GrassBlock {
   public GrassAmbientBlock(Properties properties) {
      super(properties);
   }

   /** SpreadingSnowyDirtBlock.canBeGrass (private there). */
   private static boolean canBeGrass(BlockState state, LevelReader level, BlockPos pos) {
      BlockPos above = pos.above();
      BlockState up = level.getBlockState(above);
      if (up.is(Blocks.SNOW) && up.getValue(SnowLayerBlock.LAYERS) == 1) return true;
      if (up.getFluidState().getAmount() == 8) return false;
      int i = LightEngine.getLightBlockInto(level, state, pos, up, above, Direction.UP, up.getLightBlock(level, above));
      return i < level.getMaxLightLevel();
   }

   private static boolean canPropagate(BlockState state, LevelReader level, BlockPos pos) {
      return canBeGrass(state, level, pos) && !level.getFluidState(pos.above()).is(FluidTags.WATER);
   }

   /** The 1.12 BlockGrass tick: dies when covered, and spreads plain grass (not itself) onto nearby dirt. */
   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!canBeGrass(state, level, pos)) {
         if (!level.isAreaLoaded(pos, 1)) return;
         level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
         return;
      }
      if (!level.isAreaLoaded(pos, 3)) return;
      if (level.getMaxLocalRawBrightness(pos.above()) >= 9) {
         BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
         for (int i = 0; i < 4; i++) {
            BlockPos p = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (level.getBlockState(p).is(Blocks.DIRT) && canPropagate(grass, level, p)) {
               level.setBlockAndUpdate(p, grass.setValue(SNOWY, level.getBlockState(p.above()).is(Blocks.SNOW)));
            }
         }
      }
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      int i = level.getBrightness(LightLayer.SKY, pos.above()) - level.getSkyDarken();
      float f = level.getSunAngle(1.0F);
      float f1 = f < (float) Math.PI ? 0.0F : (float) (Math.PI * 2);
      f += (f1 - f) * 0.2F;
      i = Math.round(i * Mth.cos(f));
      i = Mth.clamp(i, 0, 15);
      if (4 + i * 2 < 1 + rand.nextInt(13)) {
         int x = Mth.nextInt(rand, -8, 8);
         int z = Mth.nextInt(rand, -8, 8);
         BlockPos pp = pos.offset(x, 5, z);
         for (int q = 0; q < 10 && pp.getY() > 50 && !level.getBlockState(pp).is(Blocks.GRASS_BLOCK); q++) {
            pp = pp.below();
         }
         if (level.getBlockState(pp).is(Blocks.GRASS_BLOCK)) {
            MobFXBridge.INSTANCE.wispyMotesOnBlock(pp.above(), 400, -0.01F);
         }
      }
   }
}
