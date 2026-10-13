package thaumcraft.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import thaumcraft.common.container.slot.SlotLimitedByClass;
import thaumcraft.common.items.consumables.ItemBathSalts;
import thaumcraft.common.tiles.devices.TileSpa;

public class ContainerSpa extends Container {
   private TileSpa spa;
   private int lastBreakTime;

   public ContainerSpa(InventoryPlayer par1InventoryPlayer, TileSpa tileEntity) {
      this.spa = tileEntity;
      this.addSlotToContainer(new SlotLimitedByClass(ItemBathSalts.class, tileEntity, 0, 65, 31));

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlotToContainer(new Slot(par1InventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
         }
      }

      for (int var5 = 0; var5 < 9; var5++) {
         this.addSlotToContainer(new Slot(par1InventoryPlayer, var5, 8 + var5 * 18, 142));
      }
   }

   public boolean enchantItem(EntityPlayer p, int button) {
      if (button == 1) {
         this.spa.toggleMix();
      }

      return false;
   }

   public boolean canInteractWith(EntityPlayer par1EntityPlayer) {
      return this.spa.isUsableByPlayer(par1EntityPlayer);
   }

   public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int slot) {
      ItemStack stack = ItemStack.EMPTY;
      Slot slotObject = (Slot)this.inventorySlots.get(slot);
      if (slotObject != null && slotObject.getHasStack()) {
         ItemStack stackInSlot = slotObject.getStack();
         stack = stackInSlot.copy();
         if (slot == 0) {
            if (!this.spa.isItemValidForSlot(slot, stackInSlot) || !this.mergeItemStack(stackInSlot, 1, this.inventorySlots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.spa.isItemValidForSlot(slot, stackInSlot) || !this.mergeItemStack(stackInSlot, 0, 1, false)) {
            return ItemStack.EMPTY;
         }

         if (stackInSlot.getCount() == 0) {
            slotObject.putStack(ItemStack.EMPTY);
         } else {
            slotObject.onSlotChanged();
         }
      }

      return stack;
   }
}
