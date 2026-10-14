package thaumcraft.common.golems.seals;

import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigToggles;

public class SealFillAdvanced extends SealFill implements ISealConfigToggles {
   public SealFillAdvanced() {
      super("seal_fill_advanced");
   }

   @Override
   public String getKey() {
      return "thaumcraft:fill_advanced";
   }

   @Override
   public ISeal newInstance() {
      return new SealFillAdvanced();
   }

   @Override
   public int getFilterSize() {
      return 9;
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_FILTER, CAT_TOGGLES, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public SealToggle[] getToggles() {
      return props;
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
