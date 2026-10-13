package thaumcraft.common.world.features;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.world.CrystalBlock;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.world.BiomeHandler;

/**
 * Port of the vis crystal part of ThaumcraftWorldGenerator.generateOres: per chunk, a few random spots underground;
 * wherever a spot is open and next to rock, a 3x3x3 scatter of crystal clusters of one primal aspect
 * (a third of the time the aspect of the biome). Runs once per chunk from the chunk's origin.
 *
 * <p>Port change: 16 tries instead of 8, since 1.21 worlds have twice as much rock below the surface.
 */
public class VisCrystalFeature extends Feature<NoneFeatureConfiguration> {
   private static final int TRIES = 16;
   private static final int MAX_CRYSTALS = 64;

   public VisCrystalFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   private static CrystalBlock crystalFor(Aspect a) {
      List<CrystalBlock> all = ModBlocks.crystals();
      for (CrystalBlock c : all) {
         if (c.aspect == a) return c;
      }
      return null;
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
      WorldGenLevel level = ctx.level();
      RandomSource random = ctx.random();
      BlockPos chunk = ctx.origin();
      List<CrystalBlock> primals = ModBlocks.crystals().subList(0, 6);
      int placed = 0;
      for (int i = 0; i < TRIES && placed <= MAX_CRYSTALS; i++) {
         int x = chunk.getX() + 8 + Mth.randomBetweenInclusive(random, -6, 6);
         int z = chunk.getZ() + 8 + Mth.randomBetweenInclusive(random, -6, 6);
         int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
         int minY = level.getMinBuildHeight();
         int y = minY + random.nextInt(Math.max(5, surface - 5 - minY));
         BlockPos center = new BlockPos(x, y, z);
         CrystalBlock crystal = primals.get(random.nextInt(6));
         if (random.nextInt(3) == 0) {
            Aspect tag = BiomeHandler.getRandomBiomeTag(level.getBiome(center), random);
            CrystalBlock byBiome = tag == null ? null : crystalFor(tag);
            crystal = byBiome != null ? byBiome : primals.get(random.nextInt(6));
         }
         for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
               for (int dz = -1; dz <= 1; dz++) {
                  if (random.nextInt(3) == 0) continue;
                  BlockPos p = center.offset(dx, dy, dz);
                  BlockState s = level.getBlockState(p);
                  if (s.getFluidState().isEmpty() && (s.isAir() || s.canBeReplaced()) && CrystalBlock.isTouchingRock(level, p)) {
                     int size = 1 + random.nextInt(3);
                     level.setBlock(p, crystal.worldgenState(level, p, size), 2);
                     placed += size;
                  }
               }
            }
         }
      }
      return placed > 0;
   }
}
