package thaumcraft.common.blocks.devices;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.common.blocks.BlockTCDevice;
import thaumcraft.common.blocks.IBlockEnabled;
import thaumcraft.common.blocks.IBlockFacing;
import thaumcraft.common.tiles.devices.TileBellows;

public class BlockBellows extends BlockTCDevice implements IBlockFacing, IBlockEnabled {
   public BlockBellows() {
      super(Material.WOOD, TileBellows.class, "bellows");
      this.setSoundType(SoundType.WOOD);
      this.setHardness(1.0F);
   }

   @Override
   public int damageDropped(IBlockState state) {
      return 0;
   }

   public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
      return BlockFaceShape.UNDEFINED;
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   public EnumBlockRenderType getRenderType(IBlockState state) {
      return EnumBlockRenderType.INVISIBLE;
   }

   @Override
   public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
      IBlockState bs = this.getDefaultState();
      if (this instanceof IBlockFacing) {
         bs = bs.withProperty(IBlockFacing.FACING, facing.getOpposite());
      }

      if (this instanceof IBlockEnabled) {
         bs = bs.withProperty(IBlockEnabled.ENABLED, true);
      }

      return bs;
   }
}
