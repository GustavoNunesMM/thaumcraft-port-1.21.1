package thaumcraft.common.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import thaumcraft.common.items.tools.ItemHandMirror;

public class ContainerHandMirror extends Container implements IInventoryChangedListener {
   private World worldObj;
   private int posX;
   private int posY;
   private int posZ;
   public IInventory input = new InventoryHandMirror(this);
   ItemStack mirror = null;
   EntityPlayer player = null;

   public ContainerHandMirror(InventoryPlayer iinventory, World par2World, int par3, int par4, int par5) {
      this.worldObj = par2World;
      this.posX = par3;
      this.posY = par4;
      this.posZ = par5;
      this.player = iinventory.player;
      this.mirror = iinventory.getCurrentItem();
      this.addSlotToContainer(new Slot(this.input, 0, 80, 24));
      this.bindPlayerInventory(iinventory);
      this.onCraftMatrixChanged(this.input);
   }

   protected void bindPlayerInventory(InventoryPlayer inventoryPlayer) {
      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 9; j++) {
            this.addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
         }
      }

      for (int i = 0; i < 9; i++) {
         this.addSlotToContainer(new Slot(inventoryPlayer, i, 8 + i * 18, 142));
      }
   }

   public void onCraftMatrixChanged(IInventory par1IInventory) {
      if (!this.input.getStackInSlot(0).isEmpty() && ItemStack.areItemStacksEqual(this.input.getStackInSlot(0), this.mirror)) {
         this.player.openContainer = this.player.inventoryContainer;
      } else if (!this.worldObj.isRemote && !this.input.getStackInSlot(0).isEmpty() && this.player != null) {
         ItemStack is = this.input.getStackInSlot(0).copy();
         this.input.setInventorySlotContents(0, ItemStack.EMPTY);
         this.input.markDirty();
         if (ItemHandMirror.transport(this.mirror, is, this.player, this.worldObj)) {
            for (int var4 = 0; var4 < this.listeners.size(); var4++) {
               ((IContainerListener)this.listeners.get(var4)).sendSlotContents(this, 0, ItemStack.EMPTY);
            }
         } else {
            this.input.setInventorySlotContents(0, is);
            this.input.markDirty();
         }
      }
   }

   public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, EntityPlayer player) {
      try {
         ItemStack s = this.getSlot(slotId).getStack();
         if (s.getItem() instanceof ItemHandMirror) {
            return ItemStack.EMPTY;
         }
      } catch (Exception var6) {
      }

      return super.slotClick(slotId, dragType, clickTypeIn, player);
   }

   public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int slot) {
      ItemStack stack = ItemStack.EMPTY;
      Slot slotObject = (Slot)this.inventorySlots.get(slot);
      if (slotObject != null && slotObject.getHasStack() && !(slotObject.getStack().getItem() instanceof ItemHandMirror)) {
         ItemStack stackInSlot = slotObject.getStack();
         stack = stackInSlot.copy();
         if (slot == 0) {
            if (!this.mergeItemStack(stackInSlot, 1, this.inventorySlots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.mergeItemStack(stackInSlot, 0, 1, false)) {
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

   public boolean canInteractWith(EntityPlayer var1) {
      return true;
   }

   public void onContainerClosed(EntityPlayer par1EntityPlayer) {
      super.onContainerClosed(par1EntityPlayer);
      if (!this.worldObj.isRemote) {
         ItemStack var3 = this.input.removeStackFromSlot(0);
         par1EntityPlayer.dropItem(var3, false);
      }
   }

   protected boolean mergeItemStack(ItemStack stackin, int par2, int par3, boolean par4, int limit) {
      boolean var5x = false;
      int var6 = par2;
      if (par4) {
         var6 = par3 - 1;
      }

      if (stackin.isStackable()) {
         while (stackin.getCount() > 0 && (!par4 && var6 < par3 || par4 && var6 >= par2)) {
            Slot var7 = (Slot)this.inventorySlots.get(var6);
            ItemStack stack8 = var7.getStack();
            if (stack8 != null
               && !stack8.isEmpty()
               && stack8.getItem() == stackin.getItem()
               && (!stackin.getHasSubtypes() || stackin.getItemDamage() == stack8.getItemDamage())
               && ItemStack.areItemStackTagsEqual(stackin, stack8)) {
               int var9 = stack8.getCount() + stackin.getCount();
               if (var9 <= Math.min(stackin.getMaxStackSize(), limit)) {
                  stackin.setCount(0);
                  stack8.setCount(var9);
                  var7.onSlotChanged();
                  var5x = true;
               } else if (stack8.getCount() < Math.min(stackin.getMaxStackSize(), limit)) {
                  stackin.shrink(Math.min(stackin.getMaxStackSize(), limit) - stack8.getCount());
                  stack8.setCount(Math.min(stackin.getMaxStackSize(), limit));
                  var7.onSlotChanged();
                  var5x = true;
               }
            }

            if (par4) {
               var6--;
            } else {
               var6++;
            }
         }
      }

      if (stackin.getCount() > 0) {
         if (par4) {
            var6 = par3 - 1;
         } else {
            var6 = par2;
         }

         while (!par4 && var6 < par3 || par4 && var6 >= par2) {
            Slot var7x = (Slot)this.inventorySlots.get(var6);
            ItemStack stack8x = var7x.getStack();
            if (stack8x == null || stack8x.isEmpty()) {
               ItemStack res = stackin.copy();
               res.setCount(Math.min(res.getCount(), limit));
               var7x.putStack(res);
               var7x.onSlotChanged();
               stackin.shrink(res.getCount());
               var5x = true;
               break;
            }

            if (par4) {
               var6--;
            } else {
               var6++;
            }
         }
      }

      return var5x;
   }

   public void onInventoryChanged(IInventory invBasic) {
      this.detectAndSendChanges();
   }
}
