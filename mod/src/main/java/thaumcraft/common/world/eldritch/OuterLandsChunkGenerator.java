package thaumcraft.common.world.eldritch;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

/**
 * Chunk generator of the Outer Lands ({@code thaumcraft:outer_lands}): void, plus one maze cell per chunk built by
 * MazeGenerator during the decoration step (where the world seed and block entities are available).
 */
public class OuterLandsChunkGenerator extends ChunkGenerator {
   public static final MapCodec<OuterLandsChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
      BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)).apply(i, OuterLandsChunkGenerator::new));

   public OuterLandsChunkGenerator(BiomeSource biomeSource) {
      super(biomeSource);
   }

   @Override
   protected MapCodec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   @Override
   public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager,
      ChunkAccess chunk, GenerationStep.Carving step) {
   }

   @Override
   public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
   }

   @Override
   public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
      MazeGenerator.carve(level, chunk.getPos().x, chunk.getPos().z);
   }

   @Override
   public void spawnOriginalMobs(WorldGenRegion level) {
   }

   @Override
   public int getGenDepth() {
      return 256;
   }

   @Override
   public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
      return CompletableFuture.completedFuture(chunk);
   }

   @Override
   public int getSeaLevel() {
      return 0;
   }

   @Override
   public int getMinY() {
      return 0;
   }

   @Override
   public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
      return MazeGenerator.FLOOR + 9;
   }

   @Override
   public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
      BlockState[] states = new BlockState[height.getHeight()];
      java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
      return new NoiseColumn(height.getMinBuildHeight(), states);
   }

   @Override
   public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
      int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
      info.add("Maze region " + MazeGenerator.region(cx) + ", " + MazeGenerator.region(cz) + " cell " + MazeGenerator.local(cx) + ", " + MazeGenerator.local(cz));
   }
}
