package thaumcraft.common.tiles.misc;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumcraft.client.fx.FXDispatcher;

public class TileNitor extends TileEntity implements ITickable {
   int count = 0;

   public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
      return oldState.getBlock() != newState.getBlock();
   }

   public void update() {
      if (this.world.isRemote) {
         IBlockState state = this.world.getBlockState(this.getPos());
         FXDispatcher.INSTANCE
            .drawNitorFlames(
               this.pos.getX() + 0.5F + this.world.rand.nextGaussian() * 0.025,
               this.pos.getY() + 0.45F + this.world.rand.nextGaussian() * 0.025,
               this.pos.getZ() + 0.5F + this.world.rand.nextGaussian() * 0.025,
               this.world.rand.nextGaussian() * 0.0025,
               this.world.rand.nextFloat() * 0.06,
               this.world.rand.nextGaussian() * 0.0025,
               state.getBlock().getMapColor(state, this.world, this.getPos()).colorValue,
               0
            );
         if (this.count++ % 10 == 0) {
            FXDispatcher.INSTANCE
               .drawNitorCore(
                  this.pos.getX() + 0.5F,
                  this.pos.getY() + 0.49F,
                  this.pos.getZ() + 0.5F,
                  0.0,
                  0.0,
                  0.0
               );
         }
      }
   }
}
