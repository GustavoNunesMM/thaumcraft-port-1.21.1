package thaumcraft.common.tiles.crafting;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.blocks.devices.BlockInlay;
import thaumcraft.common.tiles.TileThaumcraftInventory;

public class TilePedestal extends TileThaumcraftInventory {
   public TilePedestal() {
      super(1);
      this.syncedSlots = new int[]{0};
   }

   @Override
   public int getInventoryStackLimit() {
      return 1;
   }

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getRenderBoundingBox() {
      return new AxisAlignedBB(
         this.getPos().getX(),
         this.getPos().getY(),
         this.getPos().getZ(),
         this.getPos().getX() + 1,
         this.getPos().getY() + 2,
         this.getPos().getZ() + 1
      );
   }

   @Override
   public boolean isItemValidForSlot(int par1, ItemStack stack2) {
      return stack2.isEmpty() || this.getStackInSlot(par1).isEmpty();
   }

   public void setInventorySlotContentsFromInfusion(int par1, ItemStack stack2) {
      this.setInventorySlotContents(par1, stack2);
      this.markDirty();
      if (!this.world.isRemote) {
         this.syncTile(false);
      }
   }

   public BlockPos findInstabilityMitigator() {
      if (this.getBlockMetadata() > 0) {
         BlockPos pp = this.seekSourceRecursive(this.pos, this.getBlockMetadata());
         if (pp != null) {
            return pp;
         }
      }

      return null;
   }

   private BlockPos seekSourceRecursive(BlockPos pos, int lastCharge) {
      for (EnumFacing face : EnumFacing.HORIZONTALS) {
         BlockPos pp = pos.offset(face);
         int ss = BlockInlay.getSourceStrengthAt(this.world, pp);
         if (ss >= 5) {
            return pp;
         }

         IBlockState bs = this.world.getBlockState(pp);
         if (bs.getProperties().containsKey(BlockInlay.CHARGE)) {
            int charge = (Integer)bs.getValue(BlockInlay.CHARGE);
            if (charge > lastCharge) {
               BlockPos ob = this.seekSourceRecursive(pp, charge);
               if (ob != null) {
                  return ob;
               }
            }
         }
      }

      return null;
   }

   public boolean receiveClientEvent(int i, int j) {
      if (i == 11) {
         if (this.world.isRemote) {
            FXDispatcher.INSTANCE.drawBamf(this.pos.up(), 0.75F, 0.0F, 0.5F, true, true, null);
         }

         return true;
      } else if (i == 12) {
         if (this.world.isRemote) {
            FXDispatcher.INSTANCE.drawBamf(this.pos.up(), true, true, null);
         }

         return true;
      } else if (i == 5) {
         if (this.world.isRemote) {
            FXDispatcher.INSTANCE.drawPedestalShield(this.pos);
         }

         return true;
      } else {
         return super.receiveClientEvent(i, j);
      }
   }
}
