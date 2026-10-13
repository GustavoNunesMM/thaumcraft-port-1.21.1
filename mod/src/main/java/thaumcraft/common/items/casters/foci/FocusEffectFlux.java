package thaumcraft.common.items.casters.foci;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.network.FocusFXPayload;

/** Flux: raw vis that hurts creatures through armor. */
public class FocusEffectFlux extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSFLUX";
   }

   @Override
   public String getKey() {
      return "thaumcraft.FLUX";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.FLUX;
   }

   @Override
   public int getComplexity() {
      return getSettingValue("power") * 3;
   }

   @Override
   public float getDamageForDisplay(float finalPower) {
      return (3 + getSettingValue("power")) * finalPower;
   }

   @Override
   public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
      Level world = getPackage().world;
      FocusFXPayload.impact(world, target.getLocation(), getKey());
      if (target instanceof EntityHitResult ehr) {
         Entity hit = ehr.getEntity();
         hit.hurt(FocusUtils.indirectMagic(world, hit, getPackage().getCaster()), getDamageForDisplay(finalPower));
      }
      return false;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5))};
   }

   @Override
   public void onCast(Entity caster) {
      caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 2.0F,
         2.0F + (float) (caster.level().random.nextGaussian() * 0.1F));
   }

   @Override
   public void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz) {
      thaumcraft.client.fx.FociParticles.flux(x, y, z, vx, vy, vz);
   }
}
