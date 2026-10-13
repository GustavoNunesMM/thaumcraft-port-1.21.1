package thaumcraft.common.blocks.basic;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import thaumcraft.common.config.ConfigItems;

public class BlockStairsTC extends BlockStairs {
   public BlockStairsTC(String name, IBlockState modelState) {
      super(modelState);
      this.setTranslationKey(name);
      this.setRegistryName("thaumcraft", name);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setLightOpacity(0);
   }

   public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
      return this.getMaterial(this.getDefaultState()) == Material.WOOD ? 20 : super.getFlammability(world, pos, face);
   }

   public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
      return this.getMaterial(this.getDefaultState()) == Material.WOOD ? 5 : super.getFireSpreadSpeed(world, pos, face);
   }
}
