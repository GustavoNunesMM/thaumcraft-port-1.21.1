package thaumcraft.common.items.casters.foci;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.entities.monster.SpellBatEntity;

/** Spell bat: a conjured bat that hunts (non-)friendly creatures and applies the rest of the package to them. */
public class FocusMediumSpellBat extends FocusMedium {
   @Override
   public String getResearch() {
      return "FOCUSSPELLBAT";
   }

   @Override
   public String getKey() {
      return "thaumcraft.SPELLBAT";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.BEAST;
   }

   @Override
   public int getComplexity() {
      return 8;
   }

   @Override
   public EnumSupplyType[] willSupply() {
      return new EnumSupplyType[]{EnumSupplyType.TARGET};
   }

   @Override
   public boolean execute(Trajectory trajectory) {
      FocusPackage p = getRemainingPackage();
      if (p == null || getPackage().getCaster() == null) return false;
      SpellBatEntity bat = new SpellBatEntity(p, getSettingValue("target") == 1);
      bat.setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
      return getPackage().getCaster().level().addFreshEntity(bat);
   }

   @Override
   public boolean hasIntermediary() {
      return true;
   }

   @Override
   public float getPowerMultiplier() {
      return 0.33F;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("target", "focus.common.target",
         new NodeSetting.NodeSettingIntList(new int[]{0, 1}, new String[]{"focus.common.enemy", "focus.common.friend"}))};
   }
}
