package thaumcraft.common.golems.seals;

import java.util.ArrayList;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigToggles;

public class SealEmptyAdvanced extends SealEmpty implements ISealConfigToggles {
   public SealEmptyAdvanced() {
      super("seal_empty_advanced");
   }

   @Override
   public String getKey() {
      return "thaumcraft:empty_advanced";
   }

   @Override
   public ISeal newInstance() {
      return new SealEmptyAdvanced();
   }

   @Override
   public int getFilterSize() {
      return 9;
   }

   /** With "cycle whitelist" on, each task takes the next filter entry in turn. */
   @Override
   public NonNullList<ItemStack> getInv(int c) {
      if (getToggles()[4].value && !isBlacklist()) {
         ArrayList<ItemStack> w = new ArrayList<>();
         for (ItemStack s : super.getInv()) {
            if (!s.isEmpty()) w.add(s);
         }
         if (!w.isEmpty()) return NonNullList.withSize(1, w.get(Math.abs(c % w.size())));
      }
      return super.getInv();
   }

   @Override
   public SealToggle[] getToggles() {
      return props;
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_FILTER, CAT_TOGGLES, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public void setToggle(int indx, boolean value) {
      props[indx].setValue(value);
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.SMART};
   }
}
