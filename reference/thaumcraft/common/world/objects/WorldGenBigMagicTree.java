package thaumcraft.common.world.objects;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockLog.EnumAxis;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

public class WorldGenBigMagicTree extends WorldGenAbstractTree {
   private Random rand;
   private World world;
   private BlockPos basePos = BlockPos.ORIGIN;
   int heightLimit;
   int height;
   double heightAttenuation = 0.6618;
   double branchSlope = 0.381;
   double scaleWidth = 1.25;
   double leafDensity = 0.9;
   int trunkSize = 1;
   int heightLimitLimit = 11;
   int leafDistanceLimit = 4;
   List foliageCoords;
   private static final String __OBFID = "CL_00000400";

   public WorldGenBigMagicTree(boolean p_i2008_1_) {
      super(p_i2008_1_);
   }

   void generateLeafNodeList() {
      this.height = (int)(this.heightLimit * this.heightAttenuation);
      if (this.height >= this.heightLimit) {
         this.height = this.heightLimit - 1;
      }

      int i = (int)(1.382 + Math.pow(this.leafDensity * this.heightLimit / 13.0, 2.0));
      if (i < 1) {
         i = 1;
      }

      int j = this.basePos.getY() + this.height;
      int k = this.heightLimit - this.leafDistanceLimit;
      this.foliageCoords = Lists.newArrayList();
      this.foliageCoords.add(new WorldGenBigMagicTree.FoliageCoordinates(this.basePos.up(k), j));

      for (; k >= 0; k--) {
         float f = this.layerSize(k);
         if (f >= 0.0F) {
            for (int l = 0; l < i; l++) {
               double d0 = this.scaleWidth * f * (this.rand.nextFloat() + 0.328);
               double d1 = this.rand.nextFloat() * 2.0F * Math.PI;
               double d2 = d0 * Math.sin(d1) + 0.5;
               double d3 = d0 * Math.cos(d1) + 0.5;
               BlockPos blockpos = this.basePos.add(d2, k - 1, d3);
               BlockPos blockpos1 = blockpos.up(this.leafDistanceLimit);
               if (this.checkBlockLine(blockpos, blockpos1) == -1) {
                  int i1 = this.basePos.getX() - blockpos.getX();
                  int j1 = this.basePos.getZ() - blockpos.getZ();
                  double d4 = blockpos.getY() - Math.sqrt(i1 * i1 + j1 * j1) * this.branchSlope;
                  int k1 = d4 > j ? j : (int)d4;
                  BlockPos blockpos2 = new BlockPos(this.basePos.getX(), k1, this.basePos.getZ());
                  if (this.checkBlockLine(blockpos2, blockpos) == -1) {
                     this.foliageCoords.add(new WorldGenBigMagicTree.FoliageCoordinates(blockpos, blockpos2.getY()));
                  }
               }
            }
         }
      }
   }

   void crosSection(BlockPos pos, float p_181631_2_, IBlockState p_181631_3_) {
      int i = (int)(p_181631_2_ + 0.618);

      for (int j = -i; j <= i; j++) {
         for (int k = -i; k <= i; k++) {
            if (Math.pow(Math.abs(j) + 0.5, 2.0) + Math.pow(Math.abs(k) + 0.5, 2.0) <= p_181631_2_ * p_181631_2_) {
               BlockPos blockpos = pos.add(j, 0, k);
               IBlockState state = this.world.getBlockState(blockpos);
               if (state.getBlock().isAir(state, this.world, blockpos) || state.getBlock().isLeaves(state, this.world, blockpos)) {
                  this.setBlockAndNotifyAdequately(this.world, blockpos, p_181631_3_);
               }
            }
         }
      }
   }

   float layerSize(int y) {
      if (y < this.heightLimit * 0.3F) {
         return -1.0F;
      } else {
         float f = this.heightLimit / 2.0F;
         float f1 = f - y;
         float f2 = MathHelper.sqrt(f * f - f1 * f1);
         if (f1 == 0.0F) {
            f2 = f;
         } else if (Math.abs(f1) >= f) {
            return 0.0F;
         }

         return f2 * 0.5F;
      }
   }

   float leafSize(int y) {
      return y >= 0 && y < this.leafDistanceLimit ? (y != 0 && y != this.leafDistanceLimit - 1 ? 3.0F : 2.0F) : -1.0F;
   }

   void generateLeafNode(BlockPos pos) {
      for (int i = 0; i < this.leafDistanceLimit; i++) {
         this.crosSection(pos.up(i), this.leafSize(i), Blocks.LEAVES.getDefaultState().withProperty(BlockLeaves.CHECK_DECAY, false));
      }
   }

   void limb(BlockPos p_175937_1_, BlockPos p_175937_2_, Block p_175937_3_) {
      BlockPos blockpos2 = p_175937_2_.add(-p_175937_1_.getX(), -p_175937_1_.getY(), -p_175937_1_.getZ());
      int i = this.getGreatestDistance(blockpos2);
      float f = (float)blockpos2.getX() / i;
      float f1 = (float)blockpos2.getY() / i;
      float f2 = (float)blockpos2.getZ() / i;

      for (int j = 0; j <= i; j++) {
         BlockPos blockpos3 = p_175937_1_.add(0.5F + j * f, 0.5F + j * f1, 0.5F + j * f2);
         EnumAxis enumaxis = this.getLogAxis(p_175937_1_, blockpos3);
         this.setBlockAndNotifyAdequately(this.world, blockpos3, p_175937_3_.getDefaultState().withProperty(BlockLog.LOG_AXIS, enumaxis));
      }
   }

   private int getGreatestDistance(BlockPos posIn) {
      int i = MathHelper.abs(posIn.getX());
      int j = MathHelper.abs(posIn.getY());
      int k = MathHelper.abs(posIn.getZ());
      return k > i && k > j ? k : (j > i ? j : i);
   }

   private EnumAxis getLogAxis(BlockPos p_175938_1_, BlockPos p_175938_2_) {
      EnumAxis enumaxis = EnumAxis.Y;
      int i = Math.abs(p_175938_2_.getX() - p_175938_1_.getX());
      int j = Math.abs(p_175938_2_.getZ() - p_175938_1_.getZ());
      int k = Math.max(i, j);
      if (k > 0) {
         if (i == k) {
            enumaxis = EnumAxis.X;
         } else if (j == k) {
            enumaxis = EnumAxis.Z;
         }
      }

      return enumaxis;
   }

   void generateLeaves() {
      for (WorldGenBigMagicTree.FoliageCoordinates foliagecoordinates : this.foliageCoords) {
         this.generateLeafNode(foliagecoordinates);
      }
   }

   boolean leafNodeNeedsBase(int p_76493_1_) {
      return p_76493_1_ >= this.heightLimit * 0.2;
   }

   void generateTrunk() {
      BlockPos blockpos = this.basePos;
      BlockPos blockpos1 = this.basePos.up(this.height);
      Block block = Blocks.LOG;
      this.limb(blockpos, blockpos1, block);
      if (this.trunkSize == 2) {
         this.limb(blockpos.east(), blockpos1.east(), block);
         this.limb(blockpos.east().south(), blockpos1.east().south(), block);
         this.limb(blockpos.south(), blockpos1.south(), block);
      }
   }

   void generateLeafNodeBases() {
      for (WorldGenBigMagicTree.FoliageCoordinates foliagecoordinates : this.foliageCoords) {
         int i = foliagecoordinates.getBranchBase();
         BlockPos blockpos = new BlockPos(this.basePos.getX(), i, this.basePos.getZ());
         if (this.leafNodeNeedsBase(i - this.basePos.getY())) {
            this.limb(blockpos, foliagecoordinates, Blocks.LOG);
         }
      }
   }

   int checkBlockLine(BlockPos posOne, BlockPos posTwo) {
      BlockPos blockpos2 = posTwo.add(-posOne.getX(), -posOne.getY(), -posOne.getZ());
      int i = this.getGreatestDistance(blockpos2);
      float f = (float)blockpos2.getX() / i;
      float f1 = (float)blockpos2.getY() / i;
      float f2 = (float)blockpos2.getZ() / i;
      if (i == 0) {
         return -1;
      } else {
         for (int j = 0; j <= i; j++) {
            BlockPos blockpos3 = posOne.add(0.5F + j * f, 0.5F + j * f1, 0.5F + j * f2);
            if (!this.isReplaceable(this.world, blockpos3)) {
               return j;
            }
         }

         return -1;
      }
   }

   public void setDecorationDefaults() {
      this.leafDistanceLimit = 4;
   }

   public boolean generate(World worldIn, Random rand, BlockPos position) {
      this.world = worldIn;
      this.basePos = position;
      this.rand = new Random(rand.nextLong());
      if (this.heightLimit == 0) {
         this.heightLimit = 11 + this.rand.nextInt(this.heightLimitLimit);
      }

      if (!this.validTreeLocation()) {
         this.world = null;
         return false;
      } else {
         this.generateLeafNodeList();
         this.generateLeaves();
         this.generateTrunk();
         this.generateLeafNodeBases();
         this.world = null;
         return true;
      }
   }

   private boolean validTreeLocation() {
      BlockPos down = this.basePos.down();
      IBlockState state = this.world.getBlockState(down);
      boolean isSoil = state.getBlock().canSustainPlant(state, this.world, down, EnumFacing.UP, (BlockSapling)Blocks.SAPLING);
      if (!isSoil) {
         return false;
      } else {
         int i = this.checkBlockLine(this.basePos, this.basePos.up(this.heightLimit - 1));
         if (i == -1) {
            return true;
         } else if (i < 6) {
            return false;
         } else {
            this.heightLimit = i;
            return true;
         }
      }
   }

   static class FoliageCoordinates extends BlockPos {
      private final int branchBase;
      private static final String __OBFID = "CL_00002001";

      public FoliageCoordinates(BlockPos p_i45635_1_, int p_i45635_2_) {
         super(p_i45635_1_.getX(), p_i45635_1_.getY(), p_i45635_1_.getZ());
         this.branchBase = p_i45635_2_;
      }

      public int getBranchBase() {
         return this.branchBase;
      }
   }
}
