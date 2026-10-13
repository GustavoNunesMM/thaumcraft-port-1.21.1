package thaumcraft.common.items.casters.foci;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.entities.projectile.FocusMineEntity;

/** Mine: a trap that releases the rest of the package at the first (non-)friendly creature to come near. */
public class FocusMediumMine extends FocusMedium {
   @Override
   public String getResearch() {
      return "FOCUSMINE";
   }

   @Override
   public String getKey() {
      return "thaumcraft.MINE";
   }

   @Override
   public int getComplexity() {
      return 4;
   }

   @Override
   public Aspect getAspect() {
      return Aspect.TRAP;
   }

   @Override
   public EnumSupplyType[] willSupply() {
      return new EnumSupplyType[]{EnumSupplyType.TARGET, EnumSupplyType.TRAJECTORY};
   }

   @Override
   public boolean execute(Trajectory trajectory) {
      FocusPackage p = getRemainingPackage();
      if (p == null || getPackage().getCaster() == null) return false;
      return getPackage().getCaster().level().addFreshEntity(new FocusMineEntity(p, trajectory, getSettingValue("target") == 1));
   }

   @Override
   public boolean hasIntermediary() {
      return true;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("target", "focus.common.target",
         new NodeSetting.NodeSettingIntList(new int[]{0, 1}, new String[]{"focus.common.enemy", "focus.common.friend"}))};
   }
}
