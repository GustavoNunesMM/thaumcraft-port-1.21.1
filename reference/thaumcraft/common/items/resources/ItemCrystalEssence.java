package thaumcraft.common.items.resources;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemTCEssentiaContainer;

public class ItemCrystalEssence extends ItemTCEssentiaContainer {
   public ItemCrystalEssence() {
      super("crystal_essence", 1);
   }

   @Override
   public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
      if (tab == ConfigItems.TABTC || tab == CreativeTabs.SEARCH) {
         for (Aspect tag : Aspect.aspects.values()) {
            ItemStack i = new ItemStack(this);
            this.setAspects(i, new AspectList().add(tag, this.base));
            items.add(i);
         }
      }
   }

   public String getItemStackDisplayName(ItemStack stack) {
      return this.getAspects(stack) != null && !this.getAspects(stack).aspects.isEmpty()
         ? String.format(super.getItemStackDisplayName(stack), this.getAspects(stack).getAspects()[0].getName())
         : super.getItemStackDisplayName(stack);
   }
}
