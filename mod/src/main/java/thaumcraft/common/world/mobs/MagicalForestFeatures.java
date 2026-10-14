package thaumcraft.common.world.mobs;

import thaumcraft.common.world.MobWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.MiscOverworldFeatures;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModFeatures;
import thaumcraft.common.world.features.BigMagicTreeFeature;

/** The decoration of BiomeGenMagicalForest, as two features placed by the biome. */
public final class MagicalForestFeatures {
   private MagicalForestFeatures() {
   }

   /**
    * getRandomTreeFeature: 1 in 18 a silverwood, else 1 in 12 a greatwood, else a big magic oak. Placed twice per
    * chunk (decorator.treesPerChunk = 2) at the surface.
    */
   public static class Trees extends Feature<NoneFeatureConfiguration> {
      public Trees() {
         super(NoneFeatureConfiguration.CODEC);
      }

      @Override
      public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
         RandomSource r = ctx.random();
         if (r.nextInt(18) == 0) {
            return ModFeatures.SILVERWOOD_TREE.get().place(NoneFeatureConfiguration.INSTANCE, ctx.level(), ctx.chunkGenerator(), r, ctx.origin());
         }
         if (r.nextInt(12) == 0) {
            return ModFeatures.GREATWOOD_TREE.get().place(NoneFeatureConfiguration.INSTANCE, ctx.level(), ctx.chunkGenerator(), r, ctx.origin());
         }
         return BigMagicTreeFeature.generate(ctx.level(), r, ctx.origin());
      }
   }

   /**
    * BiomeGenMagicalForest.decorate (minus what the biome's vanilla features already do): one patch of ambient grass,
    * 0-2 mossy cobblestone blobs, the odd huge mushroom, waterlilies, extra ferns, and vishrooms growing next to wood.
    * The origin is the chunk's corner.
    */
   public static class Decoration extends Feature<NoneFeatureConfiguration> {
      public Decoration() {
         super(NoneFeatureConfiguration.CODEC);
      }

      private static boolean configured(FeaturePlaceContext<?> ctx, ResourceKey<ConfiguredFeature<?, ?>> key, BlockPos pos) {
         WorldGenLevel level = ctx.level();
         ChunkGenerator gen = ctx.chunkGenerator();
         return level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(key).map(Holder::value)
            .map(f -> f.place(level, gen, ctx.random(), pos)).orElse(false);
      }

      private static BlockPos top(WorldGenLevel level, BlockPos p) {
         return level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, p);
      }

      @Override
      public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
         WorldGenLevel world = ctx.level();
         RandomSource random = ctx.random();
         BlockPos pos = ctx.origin();
         // ambient grass: 3 tries at a grass block
         for (int a = 0; a < 3; a++) {
            BlockPos pp = top(world, pos.offset(4 + random.nextInt(8), 0, 4 + random.nextInt(8)));
            while (pp.getY() > 30 && !world.getBlockState(pp).is(Blocks.GRASS_BLOCK)) pp = pp.below();
            if (world.getBlockState(pp).is(Blocks.GRASS_BLOCK)) {
               world.setBlock(pp, MobWorldgen.GRASS_AMBIENT.get().defaultBlockState(), 2);
               break;
            }
         }
         // mossy blobs (WorldGenBlockBlob = the vanilla forest rock)
         int k = random.nextInt(3);
         for (int l = 0; l < k; l++) {
            configured(ctx, MiscOverworldFeatures.FOREST_ROCK, top(world, pos.offset(random.nextInt(16), 0, random.nextInt(16))));
         }
         // huge mushrooms: 16 cells, 1 in 40 each
         for (int x = 0; x < 4; x++) {
            for (int z = 0; z < 4; z++) {
               if (random.nextInt(40) == 0) {
                  BlockPos p2 = top(world, pos.offset(x * 4 + 1 + random.nextInt(3), 0, z * 4 + 1 + random.nextInt(3)));
                  configured(ctx, random.nextBoolean() ? TreeFeatures.HUGE_BROWN_MUSHROOM : TreeFeatures.HUGE_RED_MUSHROOM, p2);
               }
            }
         }
         // decorator.waterlilyPerChunk = 6
         for (int i = 0; i < 6; i++) {
            BlockPos p = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, pos.offset(random.nextInt(16), 0, random.nextInt(16)));
            configured(ctx, VegetationFeatures.PATCH_WATERLILY, p);
         }
         // getRandomWorldGenForGrass: one in four is a fern (the biome's vanilla grass patches give the rest)
         for (int i = 0; i < 3; i++) {
            configured(ctx, VegetationFeatures.PATCH_TAIGA_GRASS, top(world, pos.offset(random.nextInt(16), 0, random.nextInt(16))));
         }
         // vishrooms next to wood
         for (int a = 0; a < 8; a++) {
            BlockPos p2 = top(world, pos.offset(random.nextInt(16), 0, random.nextInt(16)));
            while (p2.getY() > 50 && !world.getBlockState(p2).is(Blocks.GRASS_BLOCK)) p2 = p2.below();
            if (world.getBlockState(p2).is(Blocks.GRASS_BLOCK) && world.getBlockState(p2.above()).canBeReplaced()
               && world.getBlockState(p2.above()).getFluidState().isEmpty() && adjacentToWood(world, p2.above())) {
               world.setBlock(p2.above(), ModBlocks.VISHROOM.get().defaultBlockState(), 2);
            }
         }
         return true;
      }

      private static boolean adjacentToWood(WorldGenLevel world, BlockPos pos) {
         for (int xx = -1; xx <= 1; xx++) {
            for (int yy = -1; yy <= 1; yy++) {
               for (int zz = -1; zz <= 1; zz++) {
                  if ((xx != 0 || yy != 0 || zz != 0) && world.getBlockState(pos.offset(xx, yy, zz)).is(BlockTags.LOGS)) return true;
               }
            }
         }
         return false;
      }
   }
}
