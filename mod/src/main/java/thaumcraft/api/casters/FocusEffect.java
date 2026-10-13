package thaumcraft.api.casters;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** What the spell does where it lands. */
public abstract class FocusEffect extends FocusNode {
   @Override
   public EnumUnitType getType() {
      return EnumUnitType.EFFECT;
   }

   @Override
   public final EnumSupplyType[] mustBeSupplied() {
      return new EnumSupplyType[]{EnumSupplyType.TARGET};
   }

   @Override
   public EnumSupplyType[] willSupply() {
      return null;
   }

   public abstract boolean execute(HitResult target, @Nullable Trajectory trajectory, float finalPower, int num);

   public float getDamageForDisplay(float finalPower) {
      return 0.0F;
   }

   /** Client only: the trail/impact particles of this effect. */
   public abstract void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz);

   public void onCast(Entity caster) {
   }
}
