package thaumcraft.common.container.slot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotGhostFull extends Slot {
   int limit = Integer.MAX_VALUE;

   public SlotGhostFull(IInventory par1iInventory, int par2, int par3, int par4, int par5) {
      super(par1iInventory, par2, par3, par4);
      this.limit = par5;
   }

   public SlotGhostFull(IInventory par1iInventory, int par2, int par3, int par4) {
      super(par1iInventory, par2, par3, par4);
   }

   public int getSlotStackLimit() {
      return this.limit;
   }

   public boolean canTakeStack(EntityPlayer par1EntityPlayer) {
      return false;
   }

   public boolean isItemValid(ItemStack stack) {
      return false;
   }
}
