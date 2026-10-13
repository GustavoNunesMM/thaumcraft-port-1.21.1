package thaumcraft.common.golems.seals;

import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigToggles;

/** The advanced breaker: a bigger filter and silk touch (at a third of the speed). */
public class SealBreakerAdvanced extends SealBreaker {
   protected ISealConfigToggles.SealToggle[] advProps = new ISealConfigToggles.SealToggle[]{
      new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"), new ISealConfigToggles.SealToggle(false, "psilk", "golem.prop.silk")};

   public SealBreakerAdvanced() {
      super("seal_breaker_advanced");
   }

   @Override
   public String getKey() {
      return "thaumcraft:breaker_advanced";
   }

   @Override
   public ISeal newInstance() {
      return new SealBreakerAdvanced();
   }

   @Override
   public int getFilterSize() {
      return 9;
   }

   @Override
   public SealToggle[] getToggles() {
      return advProps;
   }

   @Override
   public void setToggle(int indx, boolean value) {
      advProps[indx].setValue(value);
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.BREAKER, EnumGolemTrait.SMART};
   }
}
