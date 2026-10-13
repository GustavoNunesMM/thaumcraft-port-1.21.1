package thaumcraft.common.world.objects;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenerator;
import thaumcraft.api.blocks.BlocksTC;

public class WorldGenSilverwoodTrees extends WorldGenAbstractTree {
   private final int minTreeHeight;
   private final int randomTreeHeight;
   boolean worldgen = false;

   public WorldGenSilverwoodTrees(boolean doBlockNotify, int minTreeHeight, int randomTreeHeight) {
      super(doBlockNotify);
      this.worldgen = !doBlockNotify;
      this.minTreeHeight = minTreeHeight;
      this.randomTreeHeight = randomTreeHeight;
   }

   public boolean generate(World world, Random random, BlockPos pos) {
      int height = random.nextInt(this.randomTreeHeight) + this.minTreeHeight;
      boolean flag = true;
      int x = pos.getX();
      int y = pos.getY();
      int z = pos.getZ();
      if (y >= 1 && y + height + 1 <= 256) {
         for (int i1 = y; i1 <= y + 1 + height; i1++) {
            byte spread = 1;
            if (i1 == y) {
               spread = 0;
            }

            if (i1 >= y + 1 + height - 2) {
               spread = 3;
            }

            for (int j1 = x - spread; j1 <= x + spread && flag; j1++) {
               for (int k1 = z - spread; k1 <= z + spread && flag; k1++) {
                  if (i1 >= 0 && i1 < 256) {
                     IBlockState state = world.getBlockState(new BlockPos(j1, i1, k1));
                     Block block = state.getBlock();
                     if (!block.isAir(state, world, new BlockPos(j1, i1, k1))
                        && !block.isLeaves(state, world, new BlockPos(j1, i1, k1))
                        && !block.isReplaceable(world, new BlockPos(j1, i1, k1))
                        && i1 > y) {
                        flag = false;
                     }
                  } else {
                     flag = false;
                  }
               }
            }
         }

         if (!flag) {
            return false;
         } else {
            IBlockState state = world.getBlockState(new BlockPos(x, y - 1, z));
            Block block1 = state.getBlock();
            boolean isSoil = block1.canSustainPlant(state, world, new BlockPos(x, y - 1, z), EnumFacing.UP, (BlockSapling)Blocks.SAPLING);
            if (isSoil && y < 256 - height - 1) {
               block1.onPlantGrow(state, world, new BlockPos(x, y - 1, z), new BlockPos(x, y, z));
               int start = y + height - 5;
               int end = y + height + 3 + random.nextInt(3);

               for (int k2 = start; k2 <= end; k2++) {
                  int cty = MathHelper.clamp(k2, y + height - 3, y + height);

                  for (int xx = x - 5; xx <= x + 5; xx++) {
                     for (int zz = z - 5; zz <= z + 5; zz++) {
                        double d3 = xx - x;
                        double d4 = k2 - cty;
                        double d5 = zz - z;
                        double dist = d3 * d3 + d4 * d4 + d5 * d5;
                        IBlockState s2 = world.getBlockState(new BlockPos(xx, k2, zz));
                        if (dist < 10 + random.nextInt(8) && s2.getBlock().canBeReplacedByLeaves(s2, world, new BlockPos(xx, k2, zz))) {
                           this.setBlockAndNotifyAdequately(world, new BlockPos(xx, k2, zz), BlocksTC.leafSilverwood.getStateFromMeta(1));
                        }
                     }
                  }
               }

               int var33;
               for (var33 = 0; var33 < height; var33++) {
                  IBlockState s3 = world.getBlockState(new BlockPos(x, y + var33, z));
                  Block block2 = s3.getBlock();
                  if (block2.isAir(s3, world, new BlockPos(x, y + var33, z))
                     || block2.isLeaves(s3, world, new BlockPos(x, y + var33, z))
                     || block2.isReplaceable(world, new BlockPos(x, y + var33, z))) {
                     this.setBlockAndNotifyAdequately(world, new BlockPos(x, y + var33, z), BlocksTC.logSilverwood.getStateFromMeta(1));
                     this.setBlockAndNotifyAdequately(world, new BlockPos(x - 1, y + var33, z), BlocksTC.logSilverwood.getStateFromMeta(1));
                     this.setBlockAndNotifyAdequately(world, new BlockPos(x + 1, y + var33, z), BlocksTC.logSilverwood.getStateFromMeta(1));
                     this.setBlockAndNotifyAdequately(world, new BlockPos(x, y + var33, z - 1), BlocksTC.logSilverwood.getStateFromMeta(1));
                     this.setBlockAndNotifyAdequately(world, new BlockPos(x, y + var33, z + 1), BlocksTC.logSilverwood.getStateFromMeta(1));
                  }
               }

               this.setBlockAndNotifyAdequately(world, new BlockPos(x, y + var33, z), BlocksTC.logSilverwood.getStateFromMeta(1));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x - 1, y, z - 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x + 1, y, z + 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x - 1, y, z + 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x + 1, y, z - 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               if (random.nextInt(3) != 0) {
                  this.setBlockAndNotifyAdequately(world, new BlockPos(x - 1, y + 1, z - 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               }

               if (random.nextInt(3) != 0) {
                  this.setBlockAndNotifyAdequately(world, new BlockPos(x + 1, y + 1, z + 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               }

               if (random.nextInt(3) != 0) {
                  this.setBlockAndNotifyAdequately(world, new BlockPos(x - 1, y + 1, z + 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               }

               if (random.nextInt(3) != 0) {
                  this.setBlockAndNotifyAdequately(world, new BlockPos(x + 1, y + 1, z - 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               }

               this.setBlockAndNotifyAdequately(world, new BlockPos(x - 2, y, z), BlocksTC.logSilverwood.getStateFromMeta(0));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x + 2, y, z), BlocksTC.logSilverwood.getStateFromMeta(0));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x, y, z - 2), BlocksTC.logSilverwood.getStateFromMeta(2));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x, y, z + 2), BlocksTC.logSilverwood.getStateFromMeta(2));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x - 2, y - 1, z), BlocksTC.logSilverwood.getStateFromMeta(1));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x + 2, y - 1, z), BlocksTC.logSilverwood.getStateFromMeta(1));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x, y - 1, z - 2), BlocksTC.logSilverwood.getStateFromMeta(1));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x, y - 1, z + 2), BlocksTC.logSilverwood.getStateFromMeta(1));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x - 1, y + (height - 4), z - 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x + 1, y + (height - 4), z + 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x - 1, y + (height - 4), z + 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x + 1, y + (height - 4), z - 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               if (random.nextInt(3) == 0) {
                  this.setBlockAndNotifyAdequately(world, new BlockPos(x - 1, y + (height - 5), z - 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               }

               if (random.nextInt(3) == 0) {
                  this.setBlockAndNotifyAdequately(world, new BlockPos(x + 1, y + (height - 5), z + 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               }

               if (random.nextInt(3) == 0) {
                  this.setBlockAndNotifyAdequately(world, new BlockPos(x - 1, y + (height - 5), z + 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               }

               if (random.nextInt(3) == 0) {
                  this.setBlockAndNotifyAdequately(world, new BlockPos(x + 1, y + (height - 5), z - 1), BlocksTC.logSilverwood.getStateFromMeta(1));
               }

               this.setBlockAndNotifyAdequately(world, new BlockPos(x - 2, y + (height - 4), z), BlocksTC.logSilverwood.getStateFromMeta(0));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x + 2, y + (height - 4), z), BlocksTC.logSilverwood.getStateFromMeta(0));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x, y + (height - 4), z - 2), BlocksTC.logSilverwood.getStateFromMeta(2));
               this.setBlockAndNotifyAdequately(world, new BlockPos(x, y + (height - 4), z + 2), BlocksTC.logSilverwood.getStateFromMeta(2));
               if (this.worldgen) {
                  WorldGenerator flowers = new WorldGenCustomFlowers(BlocksTC.shimmerleaf, 0);
                  flowers.generate(world, random, new BlockPos(x, y, z));
               }

               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   protected void setBlockAndNotifyAdequately(World worldIn, BlockPos pos, IBlockState state) {
      IBlockState bs = worldIn.getBlockState(pos);
      if (worldIn.isAirBlock(pos) || bs.getBlock().isLeaves(bs, worldIn, pos) || bs.getBlock().isReplaceable(worldIn, pos)) {
         super.setBlockAndNotifyAdequately(worldIn, pos, state);
      }
   }
}
