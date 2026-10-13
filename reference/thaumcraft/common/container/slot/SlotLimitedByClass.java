package thaumcraft.common.container.slot;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class SlotLimitedByClass extends Slot {
   Class clazz = Object.class;
   int limit = 64;

   public SlotLimitedByClass(Class clazz, IInventory par2IInventory, int par3, int par4, int par5) {
      super(par2IInventory, par3, par4, par5);
      this.clazz = clazz;
   }

   public SlotLimitedByClass(Class clazz, int limit, IInventory par2IInventory, int par3, int par4, int par5) {
      super(par2IInventory, par3, par4, par5);
      this.clazz = clazz;
      this.limit = limit;
   }

   public boolean isItemValid(ItemStack stack) {
      return !stack.isEmpty() && this.clazz.isAssignableFrom(stack.getItem().getClass());
   }

   public int getSlotStackLimit() {
      return this.limit;
   }
}
