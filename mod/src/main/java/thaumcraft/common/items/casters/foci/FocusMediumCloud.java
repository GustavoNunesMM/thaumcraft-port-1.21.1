package thaumcraft.common.items.casters.foci;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.entities.projectile.FocusCloudEntity;

/** Cloud: a lingering field that keeps applying the rest of the package (at half power). */
public class FocusMediumCloud extends FocusMedium {
   @Override
   public String getResearch() {
      return "FOCUSCLOUD";
   }

   @Override
   public String getKey() {
      return "thaumcraft.CLOUD";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.ALCHEMY;
   }

   @Override
   public int getComplexity() {
      return 4 + getSettingValue("radius") * 2 + getSettingValue("duration") / 5;
   }

   @Override
   public EnumSupplyType[] willSupply() {
      return new EnumSupplyType[]{EnumSupplyType.TARGET};
   }

   @Override
   public boolean execute(Trajectory trajectory) {
      FocusPackage p = getRemainingPackage();
      if (p == null || getPackage().getCaster() == null) return false;
      return getPackage().getCaster().level().addFreshEntity(new FocusCloudEntity(p, trajectory, getSettingValue("radius"), getSettingValue("duration")));
   }

   @Override
   public boolean hasIntermediary() {
      return true;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("radius", "focus.common.radius", new NodeSetting.NodeSettingIntRange(1, 3)),
         new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(5, 30))};
   }

   @Override
   public float getPowerMultiplier() {
      return 0.5F;
   }
}
