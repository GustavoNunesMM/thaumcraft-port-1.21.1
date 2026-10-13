package thaumcraft.common.blocks.world.plants;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.event.terraingen.TerrainGen;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.world.objects.WorldGenGreatwoodTrees;
import thaumcraft.common.world.objects.WorldGenSilverwoodTrees;

public class BlockSaplingTC extends BlockBush implements IGrowable {
   public static final PropertyInteger STAGE = PropertyInteger.create("stage", 0, 1);
   protected static final AxisAlignedBB SAPLING_AABB = new AxisAlignedBB(0.099999994F, 0.0, 0.099999994F, 0.9F, 0.8F, 0.9F);

   public BlockSaplingTC(String name) {
      this.setTranslationKey(name);
      this.setRegistryName("thaumcraft", name);
      this.setDefaultState(this.blockState.getBaseState().withProperty(STAGE, 0));
      this.setCreativeTab(ConfigItems.TABTC);
      this.setSoundType(SoundType.PLANT);
   }

   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      return SAPLING_AABB;
   }

   public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
      return 60;
   }

   public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
      return 30;
   }

   public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
      if (!worldIn.isRemote) {
         super.updateTick(worldIn, pos, state, rand);
         if (worldIn.getLightFromNeighbors(pos.up()) >= 9 && rand.nextInt(7) == 0) {
            this.grow(worldIn, pos, state, rand);
         }
      }
   }

   public void grow(World worldIn, BlockPos pos, IBlockState state, Random rand) {
      if ((Integer)state.getValue(STAGE) == 0) {
         worldIn.setBlockState(pos, state.cycleProperty(STAGE), 4);
      } else {
         this.generateTree(worldIn, pos, state, rand);
      }
   }

   public void generateTree(World worldIn, BlockPos pos, IBlockState state, Random rand) {
      if (TerrainGen.saplingGrowTree(worldIn, rand, pos)) {
         Object object = null;
         int i = 0;
         int j = 0;
         boolean flag = false;
         if (state.getBlock() == BlocksTC.saplingGreatwood) {
            label45:
            for (i = 0; i >= -1; i--) {
               for (j = 0; j >= -1; j--) {
                  if (this.isTwoByTwoOfType(worldIn, pos, i, j, BlocksTC.saplingGreatwood)) {
                     object = new WorldGenGreatwoodTrees(true, false);
                     flag = true;
                     break label45;
                  }
               }
            }
         } else {
            object = new WorldGenSilverwoodTrees(true, 7, 4);
         }

         if (object != null) {
            IBlockState iblockstate1 = Blocks.AIR.getDefaultState();
            if (flag) {
               worldIn.setBlockState(pos.add(i, 0, j), iblockstate1, 4);
               worldIn.setBlockState(pos.add(i + 1, 0, j), iblockstate1, 4);
               worldIn.setBlockState(pos.add(i, 0, j + 1), iblockstate1, 4);
               worldIn.setBlockState(pos.add(i + 1, 0, j + 1), iblockstate1, 4);
            } else {
               worldIn.setBlockState(pos, iblockstate1, 4);
            }

            if (!((WorldGenerator)object).generate(worldIn, rand, pos.add(i, 0, j))) {
               if (flag) {
                  worldIn.setBlockState(pos.add(i, 0, j), state, 4);
                  worldIn.setBlockState(pos.add(i + 1, 0, j), state, 4);
                  worldIn.setBlockState(pos.add(i, 0, j + 1), state, 4);
                  worldIn.setBlockState(pos.add(i + 1, 0, j + 1), state, 4);
               } else {
                  worldIn.setBlockState(pos.add(i, 0, j), state, 4);
               }
            }
         }
      }
   }

   private boolean isTwoByTwoOfType(World worldIn, BlockPos pos, int p_181624_3_, int p_181624_4_, Block type) {
      return this.isTypeAt(worldIn, pos.add(p_181624_3_, 0, p_181624_4_), type)
         && this.isTypeAt(worldIn, pos.add(p_181624_3_ + 1, 0, p_181624_4_), type)
         && this.isTypeAt(worldIn, pos.add(p_181624_3_, 0, p_181624_4_ + 1), type)
         && this.isTypeAt(worldIn, pos.add(p_181624_3_ + 1, 0, p_181624_4_ + 1), type);
   }

   public boolean isTypeAt(World worldIn, BlockPos pos, Block type) {
      IBlockState iblockstate = worldIn.getBlockState(pos);
      return iblockstate.getBlock() == type;
   }

   public int damageDropped(IBlockState state) {
      return 0;
   }

   public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient) {
      return true;
   }

   public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
      return worldIn.rand.nextFloat() < 0.25;
   }

   public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state) {
      this.grow(worldIn, pos, state, rand);
   }

   public IBlockState getStateFromMeta(int meta) {
      return this.getDefaultState().withProperty(STAGE, (meta & 8) >> 3);
   }

   public int getMetaFromState(IBlockState state) {
      int i = 0;
      return i | (Integer)state.getValue(STAGE) << 3;
   }

   protected BlockStateContainer createBlockState() {
      return new BlockStateContainer(this, new IProperty[]{STAGE});
   }
}
