package thaumcraft.common.golems.seals;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.common.golems.GolemInvHelper;

/** Seals with an item filter (a white- or blacklist, optionally with a stack size per slot). */
public abstract class SealFiltered extends SealBase implements ISealConfigFilter {
   NonNullList<ItemStack> filter = NonNullList.withSize(getFilterSize(), ItemStack.EMPTY);
   NonNullList<Integer> filterSize = NonNullList.withSize(getFilterSize(), 0);
   boolean blacklist = true;

   protected SealFiltered(String icon) {
      super(icon);
   }

   /** The meta/nbt/ore/mod toggles of the seal as an InvFilter. */
   protected static GolemInvHelper.InvFilter invFilter(ISealConfigToggles.SealToggle[] props) {
      return new GolemInvHelper.InvFilter(!props[0].value, !props[1].value, props[2].value, props[3].value);
   }

   @Override
   public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
      filter = NonNullList.withSize(getFilterSize(), ItemStack.EMPTY);
      ContainerHelper.loadAllItems(nbt, filter, registries);
      for (ItemStack s : filter) {
         if (s.getCount() > 1) s.setCount(1);
      }
      blacklist = nbt.getBoolean("bl");
      filterSize = NonNullList.withSize(getFilterSize(), 0);
      ListTag list = nbt.getList("Sizes", Tag.TAG_COMPOUND);
      for (int i = 0; i < list.size(); i++) {
         CompoundTag t = list.getCompound(i);
         int j = t.getByte("Slot") & 255;
         if (j < filterSize.size()) filterSize.set(j, t.getInt("Size"));
      }
   }

   @Override
   public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
      ContainerHelper.saveAllItems(nbt, filter, registries);
      nbt.putBoolean("bl", blacklist);
      ListTag list = new ListTag();
      for (int i = 0; i < filterSize.size(); i++) {
         int size = filterSize.get(i);
         if (size != 0) {
            CompoundTag t = new CompoundTag();
            t.putByte("Slot", (byte) i);
            t.putInt("Size", size);
            list.add(t);
         }
      }
      nbt.put("Sizes", list);
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_PRIORITY};
   }

   @Override
   public int getFilterSize() {
      return 1;
   }

   @Override
   public NonNullList<ItemStack> getInv() {
      return filter;
   }

   @Override
   public NonNullList<Integer> getSizes() {
      return filterSize;
   }

   @Override
   public ItemStack getFilterSlot(int i) {
      return filter.get(i);
   }

   @Override
   public int getFilterSlotSize(int i) {
      return filterSize.get(i);
   }

   @Override
   public void setFilterSlot(int i, ItemStack stack) {
      filter.set(i, stack.copy());
   }

   @Override
   public void setFilterSlotSize(int i, int size) {
      filterSize.set(i, size);
   }

   @Override
   public boolean isBlacklist() {
      return blacklist;
   }

   @Override
   public void setBlacklist(boolean black) {
      blacklist = black;
   }

   @Override
   public boolean hasStacksizeLimiters() {
      return false;
   }
}
