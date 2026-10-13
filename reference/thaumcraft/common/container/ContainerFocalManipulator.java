package thaumcraft.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import thaumcraft.common.container.slot.SlotFocus;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

public class ContainerFocalManipulator extends Container {
   private TileFocalManipulator table;
   private int lastBreakTime;

   public ContainerFocalManipulator(InventoryPlayer inventoryPlayer, TileFocalManipulator tileEntity) {
      this.table = tileEntity;
      this.addSlotToContainer(new SlotFocus(tileEntity, 0, 31, 191));

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9, i * 18 - 62, 64 + j * 18));
         }
      }

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) {
            this.addSlotToContainer(new Slot(inventoryPlayer, i + j * 3, i * 18 - 62, j * 18 + 7));
         }
      }
   }

   public boolean enchantItem(EntityPlayer p, int button) {
      if (button == 0 && !this.table.startCraft(button, p)) {
         this.table.getWorld().playSound(p, this.table.getPos(), SoundsTC.craftfail, SoundCategory.BLOCKS, 0.33F, 1.0F);
      }

      return false;
   }

   public boolean canInteractWith(EntityPlayer par1EntityPlayer) {
      return this.table.isUsableByPlayer(par1EntityPlayer);
   }

   public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par2) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = (Slot)this.inventorySlots.get(par2);
      if (slot != null && slot.getHasStack()) {
         ItemStack itemstack1 = slot.getStack();
         itemstack = itemstack1.copy();
         if (par2 != 0) {
            if (itemstack1.getItem() instanceof ItemFocus) {
               if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (par2 >= 1 && par2 < 28) {
               if (!this.mergeItemStack(itemstack1, 28, 37, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (par2 >= 28 && par2 < 37 && !this.mergeItemStack(itemstack1, 1, 28, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.mergeItemStack(itemstack1, 1, 37, false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.getCount() == 0) {
            slot.putStack(ItemStack.EMPTY);
         } else {
            slot.onSlotChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(par1EntityPlayer, itemstack1);
      }

      return itemstack;
   }
}
