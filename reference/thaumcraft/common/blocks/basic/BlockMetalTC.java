package thaumcraft.common.blocks.basic;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import thaumcraft.common.blocks.BlockTC;

public class BlockMetalTC extends BlockTC {
   public BlockMetalTC(String name) {
      super(Material.IRON, name);
      this.setHardness(4.0F);
      this.setResistance(10.0F);
      this.setSoundType(SoundType.METAL);
   }

   public boolean isBeaconBase(IBlockAccess world, BlockPos pos, BlockPos beacon) {
      return true;
   }
}
