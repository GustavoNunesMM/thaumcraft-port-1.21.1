package thaumcraft.common.world.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.world.BiomeHandler;

/**
 * Port of WorldGenSilverwoodTrees (height 7-10, plus-shaped trunk with roots and side branches).
 * In world generation it also checks the biome like ThaumcraftWorldGenerator.generateSilverwood and scatters
 * shimmerleaf around the roots.
 */
public class SilverwoodTreeFeature extends Feature<NoneFeatureConfiguration> {
   private static final int MIN_HEIGHT = 7;
   private static final int RANDOM_HEIGHT = 4;

   public SilverwoodTreeFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
      WorldGenLevel level = ctx.level();
      BlockPos pos = ctx.origin();
      RandomSource random = ctx.random();
      boolean worldgen = level instanceof WorldGenRegion;
      if (worldgen) {
         var biome = level.getBiome(pos);
         // Half the greatwood chance, or always in (non-Thaumcraft) magical biomes.
         if (!(BiomeHandler.getBiomeSupportsGreatwood(biome) / 2.0F > random.nextFloat()) && !biome.is(Tags.Biomes.IS_MAGICAL)) return false;
      }
      return generate(level, random, pos, worldgen);
   }

   private static boolean replaceable(BlockState s) {
      return s.isAir() || s.is(BlockTags.LEAVES) || s.canBeReplaced();
   }

   static boolean generate(WorldGenLevel world, RandomSource random, BlockPos pos, boolean worldgen) {
      int height = random.nextInt(RANDOM_HEIGHT) + MIN_HEIGHT;
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      if (y < world.getMinBuildHeight() + 1 || y + height + 1 > world.getMaxBuildHeight()) return false;

      for (int yy = y; yy <= y + 1 + height; yy++) {
         int spread = yy == y ? 0 : 1;
         if (yy >= y + 1 + height - 2) spread = 3;
         for (int xx = x - spread; xx <= x + spread; xx++) {
            for (int zz = z - spread; zz <= z + spread; zz++) {
               if (yy > y && !replaceable(world.getBlockState(new BlockPos(xx, yy, zz)))) return false;
            }
         }
      }

      if (!TreeHelper.isSoil(world.getBlockState(pos.below())) || y >= world.getMaxBuildHeight() - height - 1) return false;

      TreeHelper t = new TreeHelper(world);
      Block leaves = ModBlocks.LEAVES_SILVERWOOD.get();
      int start = y + height - 5;
      int end = y + height + 3 + random.nextInt(3);
      for (int yy = start; yy <= end; yy++) {
         int cty = Mth.clamp(yy, y + height - 3, y + height);
         for (int xx = x - 5; xx <= x + 5; xx++) {
            for (int zz = z - 5; zz <= z + 5; zz++) {
               double dx = xx - x;
               double dy = yy - cty;
               double dz = zz - z;
               double dist = dx * dx + dy * dy + dz * dz;
               BlockPos p = new BlockPos(xx, yy, zz);
               if (dist < 10 + random.nextInt(8) && replaceable(world.getBlockState(p))) t.leaf(p, leaves);
            }
         }
      }

      Logs logs = new Logs(t);
      int k;
      for (k = 0; k < height; k++) {
         if (replaceable(world.getBlockState(new BlockPos(x, y + k, z)))) {
            logs.y(x, y + k, z);
            logs.y(x - 1, y + k, z);
            logs.y(x + 1, y + k, z);
            logs.y(x, y + k, z - 1);
            logs.y(x, y + k, z + 1);
         }
      }
      logs.y(x, y + k, z);
      logs.y(x - 1, y, z - 1);
      logs.y(x + 1, y, z + 1);
      logs.y(x - 1, y, z + 1);
      logs.y(x + 1, y, z - 1);
      if (random.nextInt(3) != 0) logs.y(x - 1, y + 1, z - 1);
      if (random.nextInt(3) != 0) logs.y(x + 1, y + 1, z + 1);
      if (random.nextInt(3) != 0) logs.y(x - 1, y + 1, z + 1);
      if (random.nextInt(3) != 0) logs.y(x + 1, y + 1, z - 1);
      logs.place(x - 2, y, z, Direction.Axis.X);
      logs.place(x + 2, y, z, Direction.Axis.X);
      logs.place(x, y, z - 2, Direction.Axis.Z);
      logs.place(x, y, z + 2, Direction.Axis.Z);
      logs.y(x - 2, y - 1, z);
      logs.y(x + 2, y - 1, z);
      logs.y(x, y - 1, z - 2);
      logs.y(x, y - 1, z + 2);
      int branch = y + (height - 4);
      logs.y(x - 1, branch, z - 1);
      logs.y(x + 1, branch, z + 1);
      logs.y(x - 1, branch, z + 1);
      logs.y(x + 1, branch, z - 1);
      if (random.nextInt(3) == 0) logs.y(x - 1, branch - 1, z - 1);
      if (random.nextInt(3) == 0) logs.y(x + 1, branch - 1, z + 1);
      if (random.nextInt(3) == 0) logs.y(x - 1, branch - 1, z + 1);
      if (random.nextInt(3) == 0) logs.y(x + 1, branch - 1, z - 1);
      logs.place(x - 2, branch, z, Direction.Axis.X);
      logs.place(x + 2, branch, z, Direction.Axis.X);
      logs.place(x, branch, z - 2, Direction.Axis.Z);
      logs.place(x, branch, z + 2, Direction.Axis.Z);
      t.finish();

      if (worldgen) PlantPatchFeature.scatter(world, random, pos, ModBlocks.SHIMMERLEAF.get().defaultBlockState());
      return true;
   }

   /** setBlockAndNotifyAdequately in the original only overwrote air, leaves and replaceable blocks. */
   private record Logs(TreeHelper t) {
      void y(int x, int y, int z) {
         place(x, y, z, Direction.Axis.Y);
      }

      void place(int x, int y, int z, Direction.Axis axis) {
         BlockPos p = new BlockPos(x, y, z);
         if (replaceable(t.get(p))) t.log(p, ModBlocks.LOG_SILVERWOOD.get(), axis);
      }
   }
}
