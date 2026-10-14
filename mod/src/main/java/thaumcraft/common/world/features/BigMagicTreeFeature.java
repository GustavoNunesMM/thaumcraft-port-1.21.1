package thaumcraft.common.world.features;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of WorldGenBigMagicTree: the big oaks of the magical forest (the 1.12 big tree with a short trunk, height
 * 11-21, 4-block-deep leaf clusters). Placed by the magical forest's tree feature (milestone 13).
 */
public class BigMagicTreeFeature extends Feature<NoneFeatureConfiguration> {
   public BigMagicTreeFeature() {
      super(NoneFeatureConfiguration.CODEC);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
      return generate(ctx.level(), ctx.random(), ctx.origin());
   }

   public static boolean generate(WorldGenLevel level, RandomSource random, BlockPos pos) {
      return new Generator(level).generate(random, pos);
   }

   private record Foliage(BlockPos pos, int branchBase) {
   }

   private static final class Generator {
      final TreeHelper t;
      final WorldGenLevel world;
      RandomSource rand;
      BlockPos basePos = BlockPos.ZERO;
      int heightLimit;
      int height;
      final double heightAttenuation = 0.6618;
      final double branchSlope = 0.381;
      final double scaleWidth = 1.25;
      final double leafDensity = 0.9;
      final int trunkSize = 1;
      final int heightLimitLimit = 11;
      final int leafDistanceLimit = 4;
      List<Foliage> foliageCoords;

      Generator(WorldGenLevel world) {
         this.world = world;
         this.t = new TreeHelper(world);
      }

      static BlockPos add(BlockPos p, double x, double y, double z) {
         return BlockPos.containing(p.getX() + x, p.getY() + y, p.getZ() + z);
      }

      void generateLeafNodeList() {
         height = (int) (heightLimit * heightAttenuation);
         if (height >= heightLimit) height = heightLimit - 1;
         int i = (int) (1.382 + Math.pow(leafDensity * heightLimit / 13.0, 2.0));
         if (i < 1) i = 1;
         int j = basePos.getY() + height;
         int k = heightLimit - leafDistanceLimit;
         foliageCoords = new ArrayList<>();
         foliageCoords.add(new Foliage(basePos.above(k), j));
         for (; k >= 0; k--) {
            float f = layerSize(k);
            if (f >= 0.0F) {
               for (int l = 0; l < i; l++) {
                  double d0 = scaleWidth * f * (rand.nextFloat() + 0.328);
                  double d1 = rand.nextFloat() * 2.0F * Math.PI;
                  double d2 = d0 * Math.sin(d1) + 0.5;
                  double d3 = d0 * Math.cos(d1) + 0.5;
                  BlockPos blockpos = add(basePos, d2, k - 1, d3);
                  BlockPos blockpos1 = blockpos.above(leafDistanceLimit);
                  if (checkBlockLine(blockpos, blockpos1) == -1) {
                     int i1 = basePos.getX() - blockpos.getX();
                     int j1 = basePos.getZ() - blockpos.getZ();
                     double d4 = blockpos.getY() - Math.sqrt(i1 * i1 + j1 * j1) * branchSlope;
                     int k1 = d4 > j ? j : (int) d4;
                     BlockPos blockpos2 = new BlockPos(basePos.getX(), k1, basePos.getZ());
                     if (checkBlockLine(blockpos2, blockpos) == -1) foliageCoords.add(new Foliage(blockpos, blockpos2.getY()));
                  }
               }
            }
         }
      }

      void crossSection(BlockPos pos, float radius) {
         int i = (int) (radius + 0.618);
         for (int j = -i; j <= i; j++) {
            for (int k = -i; k <= i; k++) {
               if (Math.pow(Math.abs(j) + 0.5, 2.0) + Math.pow(Math.abs(k) + 0.5, 2.0) <= radius * radius) {
                  BlockPos bp = pos.offset(j, 0, k);
                  BlockState state = world.getBlockState(bp);
                  if (state.isAir() || state.is(net.minecraft.tags.BlockTags.LEAVES)) t.leaf(bp, Blocks.OAK_LEAVES);
               }
            }
         }
      }

      float layerSize(int y) {
         if (y < heightLimit * 0.3F) return -1.0F;
         float f = heightLimit / 2.0F;
         float f1 = f - y;
         float f2 = Mth.sqrt(f * f - f1 * f1);
         if (f1 == 0.0F) {
            f2 = f;
         } else if (Math.abs(f1) >= f) {
            return 0.0F;
         }
         return f2 * 0.5F;
      }

      float leafSize(int y) {
         return y >= 0 && y < leafDistanceLimit ? (y != 0 && y != leafDistanceLimit - 1 ? 3.0F : 2.0F) : -1.0F;
      }

      void generateLeafNode(BlockPos pos) {
         for (int i = 0; i < leafDistanceLimit; i++) crossSection(pos.above(i), leafSize(i));
      }

      void limb(BlockPos from, BlockPos to) {
         BlockPos d = to.offset(-from.getX(), -from.getY(), -from.getZ());
         int i = greatestDistance(d);
         if (i == 0) {
            t.log(from, Blocks.OAK_LOG, Direction.Axis.Y);
            return;
         }
         float f = (float) d.getX() / i;
         float f1 = (float) d.getY() / i;
         float f2 = (float) d.getZ() / i;
         for (int j = 0; j <= i; j++) {
            BlockPos p = add(from, 0.5F + j * f, 0.5F + j * f1, 0.5F + j * f2);
            t.log(p, Blocks.OAK_LOG, logAxis(from, p));
         }
      }

      private static int greatestDistance(BlockPos p) {
         int i = Mth.abs(p.getX());
         int j = Mth.abs(p.getY());
         int k = Mth.abs(p.getZ());
         return k > i && k > j ? k : (j > i ? j : i);
      }

      private static Direction.Axis logAxis(BlockPos a, BlockPos b) {
         Direction.Axis axis = Direction.Axis.Y;
         int i = Math.abs(b.getX() - a.getX());
         int j = Math.abs(b.getZ() - a.getZ());
         int k = Math.max(i, j);
         if (k > 0) {
            if (i == k) {
               axis = Direction.Axis.X;
            } else if (j == k) {
               axis = Direction.Axis.Z;
            }
         }
         return axis;
      }

      void generateLeaves() {
         for (Foliage f : foliageCoords) generateLeafNode(f.pos());
      }

      boolean leafNodeNeedsBase(int y) {
         return y >= heightLimit * 0.2;
      }

      void generateTrunk() {
         limb(basePos, basePos.above(height));
      }

      void generateLeafNodeBases() {
         for (Foliage f : foliageCoords) {
            int i = f.branchBase();
            BlockPos bp = new BlockPos(basePos.getX(), i, basePos.getZ());
            if (leafNodeNeedsBase(i - basePos.getY())) limb(bp, f.pos());
         }
      }

      int checkBlockLine(BlockPos a, BlockPos b) {
         BlockPos d = b.offset(-a.getX(), -a.getY(), -a.getZ());
         int i = greatestDistance(d);
         if (i == 0) return -1;
         float f = (float) d.getX() / i;
         float f1 = (float) d.getY() / i;
         float f2 = (float) d.getZ() / i;
         for (int j = 0; j <= i; j++) {
            BlockPos p = add(a, 0.5F + j * f, 0.5F + j * f1, 0.5F + j * f2);
            if (!TreeHelper.canGrowInto(world.getBlockState(p))) return j;
         }
         return -1;
      }

      boolean generate(RandomSource random, BlockPos position) {
         basePos = position;
         rand = RandomSource.create(random.nextLong());
         heightLimit = 11 + rand.nextInt(heightLimitLimit);
         if (position.getY() < world.getMinBuildHeight() + 1 || position.getY() + heightLimit + 1 > world.getMaxBuildHeight()) return false;
         if (!validTreeLocation()) return false;
         generateLeafNodeList();
         generateLeaves();
         generateTrunk();
         generateLeafNodeBases();
         t.finish();
         return true;
      }

      private boolean validTreeLocation() {
         if (!TreeHelper.isSoil(world.getBlockState(basePos.below()))) return false;
         int i = checkBlockLine(basePos, basePos.above(heightLimit - 1));
         if (i == -1) return true;
         if (i < 6) return false;
         heightLimit = i;
         return true;
      }
   }
}
