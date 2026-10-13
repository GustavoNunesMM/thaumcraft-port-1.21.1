package thaumcraft.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import thaumcraft.common.tiles.crafting.TileThaumatorium;

public class ContainerThaumatorium extends Container {
   private TileThaumatorium thaumatorium;
   private EntityPlayer player = null;

   public ContainerThaumatorium(InventoryPlayer par1InventoryPlayer, TileThaumatorium tileEntity) {
      this.player = par1InventoryPlayer.player;
      this.thaumatorium = tileEntity;
      this.thaumatorium.eventHandler = this;
      this.addSlotToContainer(new Slot(tileEntity, 0, 55, 24));

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlotToContainer(new Slot(par1InventoryPlayer, j + i * 9 + 9, 8 + j * 18, 135 + i * 18));
         }
      }

      for (int var5 = 0; var5 < 9; var5++) {
         this.addSlotToContainer(new Slot(par1InventoryPlayer, var5, 8 + var5 * 18, 193));
      }

      this.thaumatorium.updateRecipes(this.player);
   }

   public void detectAndSendChanges() {
      super.detectAndSendChanges();
      this.thaumatorium.updateRecipes(this.player);
   }

   public void onContainerClosed(EntityPlayer par1EntityPlayer) {
      super.onContainerClosed(par1EntityPlayer);
      if (!this.thaumatorium.getWorld().isRemote) {
         this.thaumatorium.eventHandler = null;
      }
   }

   public boolean canInteractWith(EntityPlayer par1EntityPlayer) {
      return this.thaumatorium.isUsableByPlayer(par1EntityPlayer);
   }

   public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int par2) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = (Slot)this.inventorySlots.get(par2);
      if (slot != null && slot.getHasStack()) {
         ItemStack itemstack1 = slot.getStack();
         itemstack = itemstack1.copy();
         if (par2 != 0) {
            if (!this.mergeItemStack(itemstack1, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (par2 >= 1 && par2 < 28) {
            if (!this.mergeItemStack(itemstack1, 28, 37, false)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (par2 >= 28 && par2 < 37 && !this.mergeItemStack(itemstack1, 1, 28, false)) {
               return ItemStack.EMPTY;
            }

            if (!this.mergeItemStack(itemstack1, 1, 37, false)) {
               return ItemStack.EMPTY;
            }
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
