package thaumcraft.common.blocks.devices;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.BlockTC;
import thaumcraft.common.blocks.IBlockEnabled;
import thaumcraft.common.blocks.IBlockFacing;
import thaumcraft.common.blocks.IBlockFacingHorizontal;
import thaumcraft.common.lib.utils.BlockStateUtils;

public class BlockBrainBox extends BlockTC implements IBlockFacingHorizontal, IBlockEnabled {
   public BlockBrainBox() {
      super(Material.IRON, "brain_box");
      this.setSoundType(SoundType.METAL);
      IBlockState bs = this.blockState.getBaseState();
      bs.withProperty(IBlockFacing.FACING, EnumFacing.UP);
      this.setDefaultState(bs);
      this.setHardness(1.0F);
      this.setResistance(10.0F);
   }

   public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
      return BlockFaceShape.UNDEFINED;
   }

   public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
      return true;
   }

   @Override
   public int damageDropped(IBlockState state) {
      return 0;
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos pos2) {
      if (worldIn.getBlockState(pos.offset(BlockStateUtils.getFacing(state))).getBlock() != BlocksTC.thaumatorium
         && worldIn.getBlockState(pos.offset(BlockStateUtils.getFacing(state))).getBlock() != BlocksTC.thaumatoriumTop) {
         this.dropBlockAsItem(worldIn, pos, BlocksTC.brainBox.getDefaultState(), 0);
         worldIn.setBlockToAir(pos);
      }
   }

   public boolean canPlaceBlockOnSide(World worldIn, BlockPos pos, EnumFacing side) {
      if (worldIn.getBlockState(pos.offset(side.getOpposite())).getBlock() != BlocksTC.thaumatorium
         && worldIn.getBlockState(pos.offset(side.getOpposite())).getBlock() != BlocksTC.thaumatoriumTop) {
         return false;
      } else {
         return worldIn.getBlockState(pos.offset(side.getOpposite())).getValue(FACING) == side ? false : super.canPlaceBlockOnSide(worldIn, pos, side);
      }
   }

   public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
      IBlockState bs = this.getDefaultState();
      return bs.withProperty(IBlockFacing.FACING, facing.getOpposite());
   }

   public IBlockState getStateFromMeta(int meta) {
      IBlockState bs = this.getDefaultState();
      return bs.withProperty(IBlockFacing.FACING, BlockStateUtils.getFacing(meta));
   }

   public int getMetaFromState(IBlockState state) {
      byte b0 = 0;
      return b0 | ((EnumFacing)state.getValue(IBlockFacing.FACING)).getIndex();
   }

   protected BlockStateContainer createBlockState() {
      return new BlockStateContainer(this, new IProperty[]{IBlockFacing.FACING});
   }

   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      return new AxisAlignedBB(0.1875, 0.1875, 0.1875, 0.8125, 0.8125, 0.8125);
   }
}
