package thaumcraft.common.container.slot;

import net.minecraft.entity.EntityLiving;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;

public class SlotMobEquipment extends Slot {
   EntityLiving entity;

   public SlotMobEquipment(EntityLiving entity, int par3, int par4, int par5) {
      super(null, par3, par4, par5);
      this.entity = entity;
   }

   public ItemStack getStack() {
      return this.entity.getHeldItem(EnumHand.MAIN_HAND);
   }

   public void putStack(ItemStack stack) {
      this.entity.setHeldItem(EnumHand.MAIN_HAND, stack);
      if (stack != null && !stack.isEmpty() && stack.getCount() > this.getSlotStackLimit()) {
         stack.setCount(this.getSlotStackLimit());
      }

      this.onSlotChanged();
   }

   public void onSlotChanged() {
   }

   public int getSlotStackLimit() {
      return 64;
   }

   public ItemStack decrStackSize(int amount) {
      if (!this.getStack().isEmpty()) {
         if (this.getStack().getCount() <= amount) {
            ItemStack itemstack = this.getStack();
            this.putStack(ItemStack.EMPTY);
            return itemstack;
         } else {
            ItemStack itemstack = this.getStack().splitStack(amount);
            if (this.getStack().getCount() == 0) {
               this.putStack(ItemStack.EMPTY);
            }

            return itemstack;
         }
      } else {
         return ItemStack.EMPTY;
      }
   }

   public boolean isHere(IInventory inv, int slotIn) {
      return slotIn == this.getSlotIndex();
   }
}
