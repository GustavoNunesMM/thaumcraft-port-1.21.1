package thaumcraft.common.world.features;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;

/** Port of WorldGenCustomFlowers: 18 tries to put a plant on grass or sand around the origin. */
public class PlantPatchFeature extends Feature<SimpleBlockConfiguration> {
   public PlantPatchFeature() {
      super(SimpleBlockConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<SimpleBlockConfiguration> ctx) {
      BlockState state = ctx.config().toPlace().getState(ctx.random(), ctx.origin());
      return scatter(ctx.level(), ctx.random(), ctx.origin(), state) > 0;
   }

   /** Returns how many plants were placed. Unlike the original, plants are only put where they can survive. */
   static int scatter(WorldGenLevel level, RandomSource random, BlockPos origin, BlockState plant) {
      int placed = 0;
      for (int i = 0; i < 18; i++) {
         BlockPos p = origin.offset(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4), random.nextInt(8) - random.nextInt(8));
         BlockState below = level.getBlockState(p.below());
         if (level.isEmptyBlock(p) && (below.is(BlockTags.DIRT) || below.is(BlockTags.SAND)) && plant.canSurvive(level, p)) {
            level.setBlock(p, plant, 2);
            placed++;
         }
      }
      return placed;
   }
}
