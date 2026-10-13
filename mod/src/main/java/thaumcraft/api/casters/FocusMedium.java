package thaumcraft.api.casters;

/** How the spell travels: touch, bolt, projectile, cloud, mine, spell bat, plan. */
public abstract class FocusMedium extends FocusNode {
   @Override
   public EnumUnitType getType() {
      return EnumUnitType.MEDIUM;
   }

   @Override
   public final EnumSupplyType[] mustBeSupplied() {
      return this instanceof FocusMediumRoot ? null : new EnumSupplyType[]{EnumSupplyType.TRAJECTORY};
   }

   @Override
   public EnumSupplyType[] willSupply() {
      return new EnumSupplyType[]{EnumSupplyType.TARGET};
   }

   /** Mediums with an intermediary (projectile, cloud...) carry the rest of the package themselves. */
   public boolean hasIntermediary() {
      return false;
   }

   public boolean execute(Trajectory trajectory) {
      return true;
   }
}
