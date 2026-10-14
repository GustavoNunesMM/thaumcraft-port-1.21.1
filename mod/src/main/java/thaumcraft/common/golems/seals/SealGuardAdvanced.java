package thaumcraft.common.golems.seals;

import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigToggles;

public class SealGuardAdvanced extends SealGuard implements ISealConfigToggles {
   public SealGuardAdvanced() {
      super("seal_guard_advanced");
   }

   @Override
   public String getKey() {
      return "thaumcraft:guard_advanced";
   }

   @Override
   public ISeal newInstance() {
      return new SealGuardAdvanced();
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
   public int[] getGuiCategories() {
      return new int[]{CAT_AREA, CAT_TOGGLES, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.FIGHTER, EnumGolemTrait.SMART};
   }
}
