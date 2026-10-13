package thaumcraft.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.container.slot.SlotOutput;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;

public class ContainerGolemBuilder extends Container {
   private TileGolemBuilder builder;
   public static boolean redo = false;
   private int lastCost;
   private int lastMaxCost;

   public ContainerGolemBuilder(InventoryPlayer par1InventoryPlayer, TileGolemBuilder tileEntity) {
      this.builder = tileEntity;
      this.addSlotToContainer(new SlotOutput(tileEntity, 0, 160, 104));

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlotToContainer(new Slot(par1InventoryPlayer, j + i * 9 + 9, 24 + j * 18, 142 + i * 18));
         }
      }

      for (int var5 = 0; var5 < 9; var5++) {
         this.addSlotToContainer(new Slot(par1InventoryPlayer, var5, 24 + var5 * 18, 200));
      }
   }

   public ItemStack slotClick(int slotId, int clickedButton, ClickType mode, EntityPlayer playerIn) {
      redo = true;
      return super.slotClick(slotId, clickedButton, mode, playerIn);
   }

   public void putStackInSlot(int slotID, ItemStack stack) {
      redo = true;
      super.putStackInSlot(slotID, stack);
   }

   public boolean enchantItem(EntityPlayer p, int button) {
      if (button == 99) {
         redo = true;
      }

      return false;
   }

   public void addListener(IContainerListener par1ICrafting) {
      super.addListener(par1ICrafting);
      par1ICrafting.sendWindowProperty(this, 0, this.builder.cost);
   }

   public void detectAndSendChanges() {
      super.detectAndSendChanges();

      for (int i = 0; i < this.listeners.size(); i++) {
         IContainerListener icrafting = (IContainerListener)this.listeners.get(i);
         if (this.lastCost != this.builder.cost) {
            icrafting.sendWindowProperty(this, 0, this.builder.cost);
         }

         if (this.lastMaxCost != this.builder.maxCost) {
            icrafting.sendWindowProperty(this, 1, this.builder.maxCost);
         }
      }

      this.lastCost = this.builder.cost;
      this.lastMaxCost = this.builder.maxCost;
   }

   @SideOnly(Side.CLIENT)
   public void updateProgressBar(int par1, int par2) {
      if (par1 == 0) {
         this.builder.cost = par2;
      }

      if (par1 == 1) {
         this.builder.maxCost = par2;
      }
   }

   public boolean canInteractWith(EntityPlayer par1EntityPlayer) {
      return this.builder.isUsableByPlayer(par1EntityPlayer);
   }

   public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int slot) {
      ItemStack stack = ItemStack.EMPTY;
      Slot slotObject = (Slot)this.inventorySlots.get(slot);
      if (slotObject != null && slotObject.getHasStack()) {
         ItemStack stackInSlot = slotObject.getStack();
         stack = stackInSlot.copy();
         if (slot == 0) {
            if (!this.builder.isItemValidForSlot(slot, stackInSlot) || !this.mergeItemStack(stackInSlot, 1, this.inventorySlots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.builder.isItemValidForSlot(slot, stackInSlot) || !this.mergeItemStack(stackInSlot, 0, 1, false)) {
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
