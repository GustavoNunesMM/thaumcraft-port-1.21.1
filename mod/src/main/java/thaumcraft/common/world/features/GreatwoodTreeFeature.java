package thaumcraft.common.world.features;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.world.BiomeHandler;

/**
 * Port of WorldGenGreatwoodTrees: a 2x2 big-oak style tree stacked twice. The origin is the north-west trunk block.
 *
 * <p>During world generation it also rolls the biome's greatwood chance and, 1 time in 8, hides a cave spider
 * spawner and a dungeon chest under the roots (both only happened in worldgen in the original, never from saplings).
 *
 * <p>Port change: other trees' leaves and replaceable plants no longer block the branches, so greatwoods can still
 * grow inside the much denser 1.21 forests.
 */
public class GreatwoodTreeFeature extends Feature<NoneFeatureConfiguration> {
   private static final byte[] OTHER_COORD_PAIRS = {2, 0, 0, 1, 2, 1};

   public GreatwoodTreeFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
      WorldGenLevel level = ctx.level();
      BlockPos pos = ctx.origin();
      RandomSource random = ctx.random();
      boolean spiders = false;
      if (level instanceof WorldGenRegion) {
         if (BiomeHandler.getBiomeSupportsGreatwood(level.getBiome(pos)) <= random.nextFloat()) return false;
         spiders = random.nextInt(8) == 0;
      }
      return new Generator(level, spiders).generate(random, pos);
   }

   private static final class Generator {
      final TreeHelper t;
      final WorldGenLevel world;
      final boolean spiders;
      RandomSource rand;
      final int[] basePos = {0, 0, 0};
      int heightLimit = 0;
      int height;
      final double heightAttenuation = 0.618;
      final double branchSlope = 0.38;
      double scaleWidth = 1.2;
      final double leafDensity = 0.9;
      final int trunkSize = 2;
      final int heightLimitLimit = 11;
      final int leafDistanceLimit = 4;
      int[][] leafNodes;

      Generator(WorldGenLevel world, boolean spiders) {
         this.world = world;
         this.t = new TreeHelper(world);
         this.spiders = spiders;
      }

      BlockState at(int x, int y, int z) {
         return world.getBlockState(new BlockPos(x, y, z));
      }

      /** What branches may pass through: air and leaves (the original: air and greatwood leaves only). */
      static boolean passable(BlockState s) {
         return s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.REPLACEABLE_BY_TREES);
      }

      void generateLeafNodeList() {
         height = (int) (heightLimit * heightAttenuation);
         if (height >= heightLimit) height = heightLimit - 1;
         int perLayer = (int) (1.382 + Math.pow(leafDensity * heightLimit / 13.0, 2.0));
         if (perLayer < 1) perLayer = 1;
         int[][] nodes = new int[perLayer * heightLimit][4];
         int y = basePos[1] + heightLimit - leafDistanceLimit;
         int count = 1;
         int trunkTop = basePos[1] + height;
         int layer = y - basePos[1];
         nodes[0][0] = basePos[0];
         nodes[0][1] = y;
         nodes[0][2] = basePos[2];
         nodes[0][3] = trunkTop;
         y--;

         while (layer >= 0) {
            float size = layerSize(layer);
            if (size >= 0.0F) {
               for (int i = 0; i < perLayer; i++) {
                  double dist = scaleWidth * size * (rand.nextFloat() + 0.328);
                  double angle = rand.nextFloat() * 2.0 * Math.PI;
                  int nx = Mth.floor(dist * Math.sin(angle) + basePos[0] + 0.5);
                  int nz = Mth.floor(dist * Math.cos(angle) + basePos[2] + 0.5);
                  int[] node = {nx, y, nz};
                  int[] nodeTop = {nx, y + leafDistanceLimit, nz};
                  if (checkBlockLine(node, nodeTop) == -1) {
                     int[] branchBase = {basePos[0], basePos[1], basePos[2]};
                     double horiz = Math.sqrt(Math.pow(Math.abs(basePos[0] - node[0]), 2.0) + Math.pow(Math.abs(basePos[2] - node[2]), 2.0));
                     double drop = horiz * branchSlope;
                     branchBase[1] = node[1] - drop > trunkTop ? trunkTop : (int) (node[1] - drop);
                     if (checkBlockLine(branchBase, node) == -1) {
                        nodes[count][0] = nx;
                        nodes[count][1] = y;
                        nodes[count][2] = nz;
                        nodes[count][3] = branchBase[1];
                        count++;
                     }
                  }
               }
            }
            y--;
            layer--;
         }

         leafNodes = new int[count][4];
         System.arraycopy(nodes, 0, leafNodes, 0, count);
      }

      void genTreeLayer(int x, int y, int z, float radius, byte axis) {
         int r = (int) (radius + 0.618);
         byte a1 = OTHER_COORD_PAIRS[axis];
         byte a2 = OTHER_COORD_PAIRS[axis + 3];
         int[] center = {x, y, z};
         int[] p = {0, 0, 0};
         p[axis] = center[axis];
         for (int i = -r; i <= r; i++) {
            p[a1] = center[a1] + i;
            for (int j = -r; j <= r; j++) {
               double d = Math.pow(Math.abs(i) + 0.5, 2.0) + Math.pow(Math.abs(j) + 0.5, 2.0);
               if (d > radius * radius) continue;
               p[a2] = center[a2] + j;
               BlockPos bp = new BlockPos(p[0], p[1], p[2]);
               BlockState s = world.getBlockState(bp);
               if (s.isAir() || s.is(BlockTags.LEAVES)) t.leaf(bp, ModBlocks.LEAVES_GREATWOOD.get());
            }
         }
      }

      float layerSize(int layer) {
         if (layer < heightLimit * 0.3) return -1.618F;
         float half = heightLimit / 2.0F;
         float off = heightLimit / 2.0F - layer;
         float s;
         if (off == 0.0F) s = half;
         else if (Math.abs(off) >= half) s = 0.0F;
         else s = (float) Math.sqrt(Math.pow(Math.abs(half), 2.0) - Math.pow(Math.abs(off), 2.0));
         return s * 0.5F;
      }

      float leafSize(int layer) {
         if (layer < 0 || layer >= leafDistanceLimit) return -1.0F;
         return layer != 0 && layer != leafDistanceLimit - 1 ? 3.0F : 2.0F;
      }

      void generateLeafNode(int x, int y, int z) {
         for (int yy = y; yy < y + leafDistanceLimit; yy++) {
            genTreeLayer(x, yy, z, leafSize(yy - y), (byte) 1);
         }
      }

      void placeBlockLine(int[] from, int[] to) {
         int[] delta = {0, 0, 0};
         byte major = 0;
         for (byte i = 0; i < 3; i++) {
            delta[i] = to[i] - from[i];
            if (Math.abs(delta[i]) > Math.abs(delta[major])) major = i;
         }
         if (delta[major] == 0) return;
         byte o1 = OTHER_COORD_PAIRS[major];
         byte o2 = OTHER_COORD_PAIRS[major + 3];
         int step = delta[major] > 0 ? 1 : -1;
         double r1 = (double) delta[o1] / delta[major];
         double r2 = (double) delta[o2] / delta[major];
         int[] p = {0, 0, 0};
         for (int i = 0, end = delta[major] + step; i != end; i += step) {
            p[major] = Mth.floor(from[major] + i + 0.5);
            p[o1] = Mth.floor(from[o1] + i * r1 + 0.5);
            p[o2] = Mth.floor(from[o2] + i * r2 + 0.5);
            Direction.Axis axis = Direction.Axis.Y;
            int dx = Math.abs(p[0] - from[0]);
            int dz = Math.abs(p[2] - from[2]);
            int dmax = Math.max(dx, dz);
            if (dmax > 0) {
               if (dx == dmax) axis = Direction.Axis.X;
               else if (dz == dmax) axis = Direction.Axis.Z;
            }
            BlockPos bp = new BlockPos(p[0], p[1], p[2]);
            if (TreeHelper.canGrowInto(world.getBlockState(bp))) t.log(bp, ModBlocks.LOG_GREATWOOD.get(), axis);
         }
      }

      void generateLeaves() {
         for (int[] n : leafNodes) generateLeafNode(n[0], n[1], n[2]);
      }

      boolean leafNodeNeedsBase(int h) {
         return h >= heightLimit * 0.2;
      }

      void generateTrunk() {
         int x = basePos[0];
         int z = basePos[2];
         int[] a = {x, basePos[1], z};
         int[] b = {x, basePos[1] + height, z};
         placeBlockLine(a, b);
         if (trunkSize == 2) {
            a[0]++;
            b[0]++;
            placeBlockLine(a, b);
            a[2]++;
            b[2]++;
            placeBlockLine(a, b);
            a[0]--;
            b[0]--;
            placeBlockLine(a, b);
         }
      }

      void generateLeafNodeBases() {
         int[] base = {basePos[0], basePos[1], basePos[2]};
         for (int[] n : leafNodes) {
            int[] node = {n[0], n[1], n[2]};
            base[1] = n[3];
            if (leafNodeNeedsBase(base[1] - basePos[1])) placeBlockLine(base, node);
         }
      }

      /** -1 if the line is clear, otherwise how far along it the first obstruction is. */
      int checkBlockLine(int[] from, int[] to) {
         int[] delta = {0, 0, 0};
         byte major = 0;
         for (byte i = 0; i < 3; i++) {
            delta[i] = to[i] - from[i];
            if (Math.abs(delta[i]) > Math.abs(delta[major])) major = i;
         }
         if (delta[major] == 0) return -1;
         byte o1 = OTHER_COORD_PAIRS[major];
         byte o2 = OTHER_COORD_PAIRS[major + 3];
         int step = delta[major] > 0 ? 1 : -1;
         double r1 = (double) delta[o1] / delta[major];
         double r2 = (double) delta[o2] / delta[major];
         int[] p = {0, 0, 0};
         int i = 0;
         int end = delta[major] + step;
         for (; i != end; i += step) {
            p[major] = from[major] + i;
            p[o1] = Mth.floor(from[o1] + i * r1);
            p[o2] = Mth.floor(from[o2] + i * r2);
            if (!passable(at(p[0], p[1], p[2]))) break;
         }
         return i == end ? -1 : Math.abs(i);
      }

      boolean validTreeLocation(int x, int z) {
         int[] bottom = {basePos[0] + x, basePos[1], basePos[2] + z};
         int[] top = {basePos[0] + x, basePos[1] + heightLimit - 1, basePos[2] + z};
         if (!TreeHelper.isSoil(at(basePos[0] + x, basePos[1] - 1, basePos[2] + z))) return false;
         int clear = checkBlockLine(bottom, top);
         if (clear == -1) return true;
         if (clear < 6) return false;
         heightLimit = clear;
         return true;
      }

      boolean generate(RandomSource random, BlockPos pos) {
         rand = RandomSource.create(random.nextLong());
         basePos[0] = pos.getX();
         basePos[1] = pos.getY();
         basePos[2] = pos.getZ();
         if (heightLimit == 0) heightLimit = heightLimitLimit + rand.nextInt(heightLimitLimit);
         // Port safety: the two stacked halves reach about 1.6x heightLimit; keep them inside the world.
         if (pos.getY() + heightLimit * 2 >= world.getMaxBuildHeight()) return false;

         for (int x = 0; x < trunkSize; x++) {
            for (int z = 0; z < trunkSize; z++) {
               if (!validTreeLocation(x, z)) return false;
            }
         }

         world.setBlock(pos, Blocks.AIR.defaultBlockState(), 19);
         generateLeafNodeList();
         generateLeaves();
         generateLeafNodeBases();
         generateTrunk();
         scaleWidth = 1.66;
         basePos[0] = pos.getX();
         basePos[1] = pos.getY() + height;
         basePos[2] = pos.getZ();
         generateLeafNodeList();
         generateLeaves();
         generateLeafNodeBases();
         generateTrunk();
         t.finish();
         if (spiders) placeSpiderNest(random, pos);
         return true;
      }

      private void placeSpiderNest(RandomSource random, BlockPos pos) {
         world.setBlock(pos.below(), Blocks.SPAWNER.defaultBlockState(), 2);
         BlockEntity be = world.getBlockEntity(pos.below());
         if (!(be instanceof SpawnerBlockEntity spawner)) return;
         spawner.setEntityId(EntityType.CAVE_SPIDER, random);
         for (int a = 0; a < 50; a++) {
            BlockPos p = pos.offset(-7 + random.nextInt(14), random.nextInt(10), -7 + random.nextInt(14));
            if (world.isEmptyBlock(p) && touchesTree(p)) world.setBlock(p, Blocks.COBWEB.defaultBlockState(), 2);
         }
         world.setBlock(pos.below(2), Blocks.CHEST.defaultBlockState(), 2);
         RandomizableContainer.setBlockEntityLootTable(world, rand, pos.below(2), BuiltInLootTables.SIMPLE_DUNGEON);
      }

      private boolean touchesTree(BlockPos p) {
         for (Direction d : Direction.values()) {
            BlockState s = world.getBlockState(p.relative(d));
            if (s.is(ModBlocks.LEAVES_GREATWOOD.get()) || s.is(ModBlocks.LOG_GREATWOOD.get())) return true;
         }
         return false;
      }
   }
}
