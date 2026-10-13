package thaumcraft.common.blocks.basic;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import thaumcraft.common.blocks.BlockTC;

public class BlockStoneTC extends BlockTC {
   private boolean spawn;

   public BlockStoneTC(String name, boolean spawn) {
      super(Material.ROCK, name);
      this.spawn = spawn;
      this.setHardness(2.0F);
      this.setResistance(10.0F);
      this.setSoundType(SoundType.STONE);
   }

   public boolean isBeaconBase(IBlockAccess world, BlockPos pos, BlockPos beacon) {
      return true;
   }

   public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
      return this.blockHardness >= 0.0F;
   }
}
