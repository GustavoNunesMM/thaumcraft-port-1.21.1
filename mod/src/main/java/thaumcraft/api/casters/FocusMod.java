package thaumcraft.api.casters;

import thaumcraft.api.aspects.Aspect;

/** Changes the flow of the package: scatter, split. */
public abstract class FocusMod extends FocusNode {
   @Override
   public EnumUnitType getType() {
      return EnumUnitType.MOD;
   }

   public abstract boolean execute();

   @Override
   public Aspect getAspect() {
      return null;
   }
}
