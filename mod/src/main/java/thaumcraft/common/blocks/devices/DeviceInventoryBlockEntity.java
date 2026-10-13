package thaumcraft.common.blocks.devices;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.TCBlockEntity;

/**
 * TileThaumcraftInventory: a block entity with a small sided inventory. The slots listed by {@link #syncedSlots()}
 * are also sent to clients (for renderers and GUIs).
 */
public abstract class DeviceInventoryBlockEntity extends TCBlockEntity implements WorldlyContainer {
   protected NonNullList<ItemStack> items;
   private final int[] faceSlots;

   protected DeviceInventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
      super(type, pos, state);
      items = NonNullList.withSize(size, ItemStack.EMPTY);
      faceSlots = new int[size];
      for (int i = 0; i < size; i++) faceSlots[i] = i;
   }

   /** Slots sent to the client with the block entity. */
   protected boolean syncSlots() {
      return true;
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      super.readExtra(tag, registries);
      if (!syncSlots()) {
         items = NonNullList.withSize(items.size(), ItemStack.EMPTY);
         ContainerHelper.loadAllItems(tag, items, registries);
      }
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      super.writeExtra(tag, registries);
      if (!syncSlots()) ContainerHelper.saveAllItems(tag, items, registries);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.readSync(tag, registries);
      if (syncSlots()) {
         items = NonNullList.withSize(items.size(), ItemStack.EMPTY);
         ContainerHelper.loadAllItems(tag, items, registries);
      }
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.writeSync(tag, registries);
      if (syncSlots()) ContainerHelper.saveAllItems(tag, items, registries);
   }

   /** Called when the inventory changes (markDirty in 1.12). */
   protected void onInventoryChanged() {
      if (syncSlots()) sync();
      else setChanged();
   }

   @Override
   public int getContainerSize() {
      return items.size();
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack s : items) {
         if (!s.isEmpty()) return false;
      }
      return true;
   }

   @Override
   public ItemStack getItem(int slot) {
      return items.get(slot);
   }

   @Override
   public ItemStack removeItem(int slot, int amount) {
      ItemStack out = ContainerHelper.removeItem(items, slot, amount);
      if (!out.isEmpty()) onInventoryChanged();
      return out;
   }

   @Override
   public ItemStack removeItemNoUpdate(int slot) {
      return ContainerHelper.takeItem(items, slot);
   }

   @Override
   public void setItem(int slot, ItemStack stack) {
      items.set(slot, stack);
      if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
      onInventoryChanged();
   }

   @Override
   public boolean stillValid(Player player) {
      return Container.stillValidBlockEntity(this, player);
   }

   @Override
   public void clearContent() {
      items.clear();
   }

   @Override
   public int[] getSlotsForFace(Direction side) {
      return faceSlots;
   }

   @Override
   public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
      return canPlaceItem(slot, stack);
   }

   @Override
   public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
      return true;
   }
}
