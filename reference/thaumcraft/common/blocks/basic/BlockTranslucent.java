package thaumcraft.common.blocks.basic;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.blocks.BlockTC;

public class BlockTranslucent extends BlockTC {
   public BlockTranslucent(String name) {
      super(Material.GLASS, name);
      this.setHardness(0.5F);
      this.setSoundType(SoundType.STONE);
   }

   public boolean isBeaconBase(IBlockAccess world, BlockPos pos, BlockPos beacon) {
      return true;
   }

   public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
      return true;
   }

   public EnumPushReaction getPushReaction(IBlockState state) {
      return EnumPushReaction.NORMAL;
   }

   @SideOnly(Side.CLIENT)
   public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
      IBlockState iblockstate = blockAccess.getBlockState(pos.offset(side));
      Block block = iblockstate.getBlock();
      return block == this ? false : super.shouldSideBeRendered(blockState, blockAccess, pos, side);
   }

   @SideOnly(Side.CLIENT)
   public BlockRenderLayer getRenderLayer() {
      return BlockRenderLayer.TRANSLUCENT;
   }

   public boolean isOpaqueCube(IBlockState iblockstate) {
      return false;
   }
}
