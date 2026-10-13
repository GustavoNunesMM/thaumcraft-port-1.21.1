package thaumcraft.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.container.slot.SlotOutput;
import thaumcraft.common.tiles.crafting.TileVoidSiphon;

public class ContainerVoidSiphon extends Container {
   private TileVoidSiphon siphon;
   private int lastProgress;

   public ContainerVoidSiphon(InventoryPlayer par1InventoryPlayer, TileVoidSiphon tileEntity) {
      this.siphon = tileEntity;
      this.addSlotToContainer(new SlotOutput(tileEntity, 0, 80, 32));

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlotToContainer(new Slot(par1InventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
         }
      }

      for (int var5 = 0; var5 < 9; var5++) {
         this.addSlotToContainer(new Slot(par1InventoryPlayer, var5, 8 + var5 * 18, 142));
      }
   }

   public void addListener(IContainerListener par1ICrafting) {
      super.addListener(par1ICrafting);
      par1ICrafting.sendWindowProperty(this, 0, this.siphon.progress);
   }

   public void detectAndSendChanges() {
      super.detectAndSendChanges();

      for (int i = 0; i < this.listeners.size(); i++) {
         IContainerListener icrafting = (IContainerListener)this.listeners.get(i);
         if (this.lastProgress != this.siphon.progress) {
            icrafting.sendWindowProperty(this, 0, this.siphon.progress);
         }
      }

      this.lastProgress = this.siphon.progress;
   }

   @SideOnly(Side.CLIENT)
   public void updateProgressBar(int par1, int par2) {
      if (par1 == 0) {
         this.siphon.progress = par2;
      }
   }

   public boolean canInteractWith(EntityPlayer par1EntityPlayer) {
      return this.siphon.isUsableByPlayer(par1EntityPlayer);
   }

   public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int slot) {
      ItemStack stack = ItemStack.EMPTY;
      Slot slotObject = (Slot)this.inventorySlots.get(slot);
      if (slotObject != null && slotObject.getHasStack()) {
         ItemStack stackInSlot = slotObject.getStack();
         stack = stackInSlot.copy();
         if (slot == 0) {
            if (!this.siphon.isItemValidForSlot(slot, stackInSlot) || !this.mergeItemStack(stackInSlot, 1, this.inventorySlots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.siphon.isItemValidForSlot(slot, stackInSlot) || !this.mergeItemStack(stackInSlot, 0, 1, false)) {
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
