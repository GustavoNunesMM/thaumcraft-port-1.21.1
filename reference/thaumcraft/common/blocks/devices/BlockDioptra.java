package thaumcraft.common.blocks.devices;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import thaumcraft.common.blocks.BlockTCDevice;
import thaumcraft.common.blocks.IBlockEnabled;
import thaumcraft.common.tiles.devices.TileDioptra;

public class BlockDioptra extends BlockTCDevice implements IBlockEnabled {
   public BlockDioptra() {
      super(Material.ROCK, TileDioptra.class, "dioptra");
      this.setSoundType(SoundType.STONE);
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

   public boolean hasComparatorInputOverride(IBlockState state) {
      return true;
   }

   public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
      TileEntity tile = world.getTileEntity(pos);
      if (tile != null && tile instanceof TileDioptra) {
         float r = ((TileDioptra)tile).grid_amt[84] / 64.0F;
         return MathHelper.floor(r * 14.0F) + (r > 0.0F ? 1 : 0);
      } else {
         return super.getComparatorInputOverride(state, world, pos);
      }
   }

   @Override
   protected void updateState(World worldIn, BlockPos pos, IBlockState state) {
   }

   public boolean onBlockActivated(
      World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ
   ) {
      boolean b = (Boolean)state.getValue(IBlockEnabled.ENABLED);
      world.setBlockState(pos, state.withProperty(IBlockEnabled.ENABLED, !b), 3);
      return true;
   }
}
