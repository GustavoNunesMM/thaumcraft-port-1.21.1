package thaumcraft.common.container;

import java.util.TreeMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IInventoryChangedListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import thaumcraft.api.ThaumcraftInvHelper;
import thaumcraft.common.container.slot.SlotGhostFull;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.golems.seals.SealProvide;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketItemToClientContainer;

public class ContainerLogistics extends Container implements IInventoryChangedListener {
   private World worldObj;
   EntityPlayer player = null;
   public IInventory input = new InventoryLogistics(this);
   TreeMap<String, ItemStack> items = new TreeMap<>();
   int lastTotal = 0;
   public int start = 0;
   public int end = 0;
   public String searchText = "";
   int lastStart = 0;
   int lastEnd = 0;
   public boolean updated = false;

   public ContainerLogistics(InventoryPlayer iinventory, World par2World) {
      this.worldObj = par2World;
      this.player = iinventory.player;

      for (int a = 0; a < this.input.getSizeInventory(); a++) {
         this.addSlotToContainer(new SlotGhostFull(this.input, a, 19 + a % 9 * 19, 19 + a / 9 * 19));
      }

      this.refreshItemList(true);
   }

   public void refreshItemList(boolean full) {
      int newTotal = this.lastTotal;
      TreeMap<String, ItemStack> ti = new TreeMap<>();
      if (full) {
         newTotal = 0;

         for (SealEntity seal : SealHandler.getSealsInRange(this.worldObj, this.player.getPosition(), 32)) {
            if (seal.getSeal() instanceof SealProvide && seal.getOwner().equals(this.player.getUniqueID().toString())) {
               IItemHandler handler = ThaumcraftInvHelper.getItemHandlerAt(this.worldObj, seal.getSealPos().pos, seal.getSealPos().face);

               for (int slot = 0; slot < handler.getSlots(); slot++) {
                  ItemStack stack = handler.getStackInSlot(slot).copy();
                  if (((SealProvide)seal.getSeal()).matchesFilters(stack)
                     && (this.searchText.isEmpty() || stack.getDisplayName().toLowerCase().contains(this.searchText.toLowerCase()))) {
                     String key = stack.getDisplayName() + stack.getItemDamage() + stack.getTagCompound();
                     if (ti.containsKey(key)) {
                        stack.grow(ti.get(key).getCount());
                     }

                     ti.put(key, stack);
                     newTotal += stack.getCount();
                  }
               }
            }
         }
      }

      if (this.lastTotal != newTotal || this.start != this.lastStart) {
         this.lastTotal = newTotal;
         if (full) {
            this.items = ti;
         }

         this.input.clear();
         int j = 0;
         int q = 0;

         for (String key : this.items.keySet()) {
            if (++j > this.start * 9) {
               this.input.setInventorySlotContents(q, this.items.get(key));
               if (++q >= this.input.getSizeInventory()) {
                  break;
               }
            }
         }

         this.end = this.items.size() / 9 - 8;
      }
   }

   public void addListener(IContainerListener listener) {
      super.addListener(listener);
      listener.sendAllWindowProperties(this, this.input);
      listener.sendWindowProperty(this, 0, this.start);
   }

   public void detectAndSendChanges() {
      this.sendLargeSlotsToClient();
      super.detectAndSendChanges();

      for (int i = 0; i < this.listeners.size(); i++) {
         IContainerListener icrafting = (IContainerListener)this.listeners.get(i);
         if (this.lastStart != this.start) {
            icrafting.sendWindowProperty(this, 0, this.start);
         }

         if (this.lastEnd != this.end) {
            icrafting.sendWindowProperty(this, 1, this.end);
         }
      }

      this.lastStart = this.start;
      this.lastEnd = this.end;
   }

   private void sendLargeSlotsToClient() {
      for (int i = 0; i < this.inventorySlots.size(); i++) {
         if (this.getSlot(i) instanceof SlotGhostFull) {
            ItemStack itemstack = ((Slot)this.inventorySlots.get(i)).getStack();
            ItemStack itemstack1 = (ItemStack)this.inventoryItemStacks.get(i);
            if (itemstack.getCount() > itemstack.getMaxStackSize()) {
               for (int j = 0; j < this.listeners.size(); j++) {
                  if (this.listeners.get(j) instanceof EntityPlayerMP) {
                     EntityPlayerMP p = (EntityPlayerMP)this.listeners.get(j);
                     PacketHandler.INSTANCE.sendTo(new PacketItemToClientContainer(this.windowId, i, itemstack), p);
                  }
               }
            }
         }
      }
   }

   @SideOnly(Side.CLIENT)
   public void updateProgressBar(int par1, int par2) {
      if (par1 == 0) {
         this.start = par2;
         this.updated = true;
      }

      if (par1 == 1) {
         this.end = par2;
         this.updated = true;
      }
   }

   public boolean enchantItem(EntityPlayer par1EntityPlayer, int par2) {
      if (par2 == 22) {
         this.refreshItemList(true);
         return true;
      } else if (par2 == 0) {
         if (this.start < this.items.size() / 9 - 8) {
            this.start++;
            this.refreshItemList(false);
         }

         return true;
      } else if (par2 == 1) {
         if (this.start > 0) {
            this.start--;
            this.refreshItemList(false);
         }

         return true;
      } else if (par2 >= 100) {
         int s = par2 - 100;
         if (s >= 0 && s <= this.items.size() / 9 - 8) {
            this.start = s;
            this.refreshItemList(false);
         }

         return true;
      } else {
         return super.enchantItem(par1EntityPlayer, par2);
      }
   }

   public ItemStack transferStackInSlot(EntityPlayer par1EntityPlayer, int slot) {
      ItemStack stack = ItemStack.EMPTY;
      Slot slotObject = (Slot)this.inventorySlots.get(slot);
      if (slotObject != null && slotObject.getHasStack()) {
         ItemStack stackInSlot = slotObject.getStack();
         stack = stackInSlot.copy();
         if (slot < this.input.getSizeInventory()) {
            if (!this.input.isItemValidForSlot(slot, stackInSlot) || !this.mergeItemStack(stackInSlot, this.input.getSizeInventory(), this.inventorySlots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.input.isItemValidForSlot(slot, stackInSlot) || !this.mergeItemStack(stackInSlot, 0, this.input.getSizeInventory(), false)) {
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

   public void onInventoryChanged(IInventory invBasic) {
      this.detectAndSendChanges();
   }
}
