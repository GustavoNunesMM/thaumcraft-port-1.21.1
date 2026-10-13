package thaumcraft.api.casters;

/** A piece of a focus: a medium, an effect, a modifier, or a nested package. */
public interface IFocusElement {
   String getKey();

   String getResearch();

   EnumUnitType getType();

   enum EnumUnitType {
      EFFECT, MEDIUM, MOD, PACKAGE
   }
}
