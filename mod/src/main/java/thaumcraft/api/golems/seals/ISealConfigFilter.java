package thaumcraft.api.golems.seals;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public interface ISealConfigFilter {
   NonNullList<ItemStack> getInv();

   NonNullList<Integer> getSizes();

   int getFilterSize();

   ItemStack getFilterSlot(int slot);

   int getFilterSlotSize(int slot);

   void setFilterSlot(int slot, ItemStack stack);

   void setFilterSlotSize(int slot, int size);

   boolean isBlacklist();

   void setBlacklist(boolean blacklist);

   boolean hasStacksizeLimiters();
}
