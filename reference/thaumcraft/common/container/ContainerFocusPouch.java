package thaumcraft.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import thaumcraft.common.container.slot.SlotLimitedByClass;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.items.casters.ItemFocusPouch;

public class ContainerFocusPouch extends Container implements IInventoryChangedListener {
   private World worldObj;
   private int posX;
   private int posY;
   private int posZ;
   private int blockSlot;
   public IInventory input = new InventoryFocusPouch(this);
   ItemStack pouch = null;
   EntityPlayer player = null;

   public ContainerFocusPouch(InventoryPlayer iinventory, World par2World, int par3, int par4, int par5) {
      this.worldObj = par2World;
      this.posX = par3;
      this.posY = par4;
      this.posZ = par5;
      this.player = iinventory.player;
      this.pouch = iinventory.getCurrentItem();
      this.blockSlot = iinventory.currentItem + 45;

      for (int a = 0; a < 18; a++) {
         this.addSlotToContainer(new SlotLimitedByClass(ItemFocus.class, this.input, a, 37 + a % 6 * 18, 51 + a / 6 * 18));
      }

      this.bindPlayerInventory(iinventory);
      if (!par2World.isRemote) {
         try {
            NonNullList<ItemStack> list = ((ItemFocusPouch)this.pouch.getItem()).getInventory(this.pouch);

            for (int a = 0; a < list.size(); a++) {
               this.input.setInventorySlotContents(a, (ItemStack)list.get(a));
            }
         } catch (Exception var8) {
         }
      }

      this.onCraftMatrixChanged(this.input);
   }

   public void onInventoryChanged(IInventory invBasic) {
      this.detectAndSendChanges();
   }

   protected void bindPlayerInventory(InventoryPlayer inventoryPlayer) {
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 151 + i * 18));
         }
      }

      for (int i = 0; i < 9; i++) {
         this.addSlotToContainer(new Slot(inventoryPlayer, i, 8 + i * 18, 209));
      }
   }

   public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int slot) {
      if (slot == this.blockSlot) {
         return ItemStack.EMPTY;
      } else {
         ItemStack stack = ItemStack.EMPTY;
         Slot slotObject = (Slot)this.inventorySlots.get(slot);
         if (slotObject != null && slotObject.getHasStack()) {
            ItemStack stackInSlot = slotObject.getStack();
            stack = stackInSlot.copy();
            if (slot < 18) {
               if (!this.input.isItemValidForSlot(slot, stackInSlot) || !this.mergeItemStack(stackInSlot, 18, this.inventorySlots.size(), true)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.input.isItemValidForSlot(slot, stackInSlot) || !this.mergeItemStack(stackInSlot, 0, 18, false)) {
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

   public boolean canInteractWith(EntityPlayer var1) {
      return true;
   }

   public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
      return slotId == this.blockSlot ? ItemStack.EMPTY : super.slotClick(slotId, dragType, clickTypeIn, player);
   }

   public void onContainerClosed(EntityPlayer par1EntityPlayer) {
      super.onContainerClosed(par1EntityPlayer);
      if (!this.worldObj.isRemote) {
         NonNullList<ItemStack> list = NonNullList.withSize(18, ItemStack.EMPTY);

         for (int a = 0; a < list.size(); a++) {
            list.set(a, this.input.getStackInSlot(a));
         }

         if (this.pouch.getItem() instanceof ItemFocusPouch) {
            ((ItemFocusPouch)this.pouch.getItem()).setInventory(this.pouch, list);
         }

         if (this.player == null) {
            return;
         }

         if (this.player.getHeldItem(this.player.getActiveHand()).isItemEqual(this.pouch)) {
            this.player.setHeldItem(this.player.getActiveHand(), this.pouch);
         }

         this.player.inventory.markDirty();
      }
   }
}
