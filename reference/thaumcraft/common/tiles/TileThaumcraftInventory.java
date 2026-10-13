package thaumcraft.common.tiles;

import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

public class TileThaumcraftInventory extends TileThaumcraft implements ISidedInventory, ITickable {
   private NonNullList<ItemStack> stacks = NonNullList.withSize(1, ItemStack.EMPTY);
   protected int[] syncedSlots = new int[0];
   private NonNullList<ItemStack> syncedStacks = NonNullList.withSize(1, ItemStack.EMPTY);
   protected String customName;
   private int[] faceSlots;
   boolean initial = true;
   IItemHandler handlerTop = new SidedInvWrapper(this, EnumFacing.UP);
   IItemHandler handlerBottom = new SidedInvWrapper(this, EnumFacing.DOWN);
   IItemHandler handlerWest = new SidedInvWrapper(this, EnumFacing.WEST);
   IItemHandler handlerEast = new SidedInvWrapper(this, EnumFacing.EAST);
   IItemHandler handlerNorth = new SidedInvWrapper(this, EnumFacing.NORTH);
   IItemHandler handlerSouth = new SidedInvWrapper(this, EnumFacing.SOUTH);

   public TileThaumcraftInventory(int size) {
      this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
      this.syncedStacks = NonNullList.withSize(size, ItemStack.EMPTY);
      this.faceSlots = new int[size];
      int a = 0;

      while (a < size) {
         this.faceSlots[a] = a++;
      }
   }

   public int getSizeInventory() {
      return this.stacks.size();
   }

   protected NonNullList<ItemStack> getItems() {
      return this.stacks;
   }

   public ItemStack getSyncedStackInSlot(int index) {
      return (ItemStack)this.syncedStacks.get(index);
   }

   public ItemStack getStackInSlot(int index) {
      return (ItemStack)this.getItems().get(index);
   }

   public ItemStack decrStackSize(int index, int count) {
      ItemStack itemstack = ItemStackHelper.getAndSplit(this.getItems(), index, count);
      if (!itemstack.isEmpty() && this.isSyncedSlot(index)) {
         this.syncSlots(null);
      }

      this.markDirty();
      return itemstack;
   }

   public ItemStack removeStackFromSlot(int index) {
      ItemStack s = ItemStackHelper.getAndRemove(this.getItems(), index);
      if (this.isSyncedSlot(index)) {
         this.syncSlots(null);
      }

      this.markDirty();
      return s;
   }

   public void setInventorySlotContents(int index, @Nullable ItemStack stack) {
      this.getItems().set(index, stack);
      if (stack.getCount() > this.getInventoryStackLimit()) {
         stack.setCount(this.getInventoryStackLimit());
      }

      this.markDirty();
      if (this.isSyncedSlot(index)) {
         this.syncSlots(null);
      }
   }

   public String getName() {
      return this.hasCustomName() ? this.customName : "container.thaumcraft";
   }

   public boolean hasCustomName() {
      return this.customName != null && this.customName.length() > 0;
   }

   public ITextComponent getDisplayName() {
      return null;
   }

   private boolean isSyncedSlot(int slot) {
      for (int s : this.syncedSlots) {
         if (s == slot) {
            return true;
         }
      }

      return false;
   }

   protected void syncSlots(EntityPlayerMP player) {
      if (this.syncedSlots.length > 0) {
         NBTTagCompound nbt = new NBTTagCompound();
         NBTTagList nbttaglist = new NBTTagList();

         for (int i = 0; i < this.stacks.size(); i++) {
            if (!((ItemStack)this.stacks.get(i)).isEmpty() && this.isSyncedSlot(i)) {
               NBTTagCompound nbttagcompound1 = new NBTTagCompound();
               nbttagcompound1.setByte("Slot", (byte)i);
               ((ItemStack)this.stacks.get(i)).writeToNBT(nbttagcompound1);
               nbttaglist.appendTag(nbttagcompound1);
            }
         }

         nbt.setTag("ItemsSynced", nbttaglist);
         this.sendMessageToClient(nbt, player);
      }
   }

   @Override
   public void syncTile(boolean rerender) {
      super.syncTile(rerender);
      this.syncSlots(null);
   }

   @Override
   public void messageFromClient(NBTTagCompound nbt, EntityPlayerMP player) {
      super.messageFromClient(nbt, player);
      if (nbt.hasKey("requestSync")) {
         this.syncSlots(player);
      }
   }

   @Override
   public void messageFromServer(NBTTagCompound nbt) {
      super.messageFromServer(nbt);
      if (nbt.hasKey("ItemsSynced")) {
         this.syncedStacks = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
         NBTTagList nbttaglist = nbt.getTagList("ItemsSynced", 10);

         for (int i = 0; i < nbttaglist.tagCount(); i++) {
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
            byte b0 = nbttagcompound1.getByte("Slot");
            if (this.isSyncedSlot(b0)) {
               this.syncedStacks.set(b0, new ItemStack(nbttagcompound1));
            }
         }
      }
   }

   @Override
   public void readFromNBT(NBTTagCompound nbtCompound) {
      super.readFromNBT(nbtCompound);
      if (nbtCompound.hasKey("CustomName")) {
         this.customName = nbtCompound.getString("CustomName");
      }

      this.stacks = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
      ItemStackHelper.loadAllItems(nbtCompound, this.stacks);
   }

   @Override
   public NBTTagCompound writeToNBT(NBTTagCompound nbtCompound) {
      super.writeToNBT(nbtCompound);
      if (this.hasCustomName()) {
         nbtCompound.setString("CustomName", this.customName);
      }

      ItemStackHelper.saveAllItems(nbtCompound, this.stacks);
      return nbtCompound;
   }

   public int getInventoryStackLimit() {
      return 64;
   }

   public boolean isUsableByPlayer(EntityPlayer par1EntityPlayer) {
      return this.world.getTileEntity(this.getPos()) != this ? false : par1EntityPlayer.getDistanceSqToCenter(this.getPos()) <= 64.0;
   }

   public boolean isItemValidForSlot(int par1, ItemStack stack2) {
      return true;
   }

   public int[] getSlotsForFace(EnumFacing par1) {
      return this.faceSlots;
   }

   public void openInventory(EntityPlayer player) {
   }

   public void closeInventory(EntityPlayer player) {
   }

   public int getField(int id) {
      return 0;
   }

   public void setField(int id, int value) {
   }

   public int getFieldCount() {
      return 0;
   }

   public void clear() {
   }

   public boolean canInsertItem(int par1, ItemStack stack2, EnumFacing par3) {
      return this.isItemValidForSlot(par1, stack2);
   }

   public boolean canExtractItem(int par1, ItemStack stack2, EnumFacing par3) {
      return true;
   }

   public boolean isEmpty() {
      for (ItemStack itemstack : this.stacks) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public void update() {
      if (this.initial) {
         this.initial = false;
         if (!this.world.isRemote) {
            this.syncSlots(null);
         } else {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setBoolean("requestSync", true);
            this.sendMessageToServer(nbt);
         }
      }
   }

   @Nullable
   public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
      if (facing == null || capability != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
         return (T)super.getCapability(capability, facing);
      } else if (facing == EnumFacing.DOWN) {
         return (T)this.handlerBottom;
      } else if (facing == EnumFacing.UP) {
         return (T)this.handlerTop;
      } else if (facing == EnumFacing.WEST) {
         return (T)this.handlerWest;
      } else if (facing == EnumFacing.EAST) {
         return (T)this.handlerEast;
      } else {
         return (T)(facing == EnumFacing.NORTH ? this.handlerNorth : this.handlerSouth);
      }
   }

   public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
      return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
   }
}
