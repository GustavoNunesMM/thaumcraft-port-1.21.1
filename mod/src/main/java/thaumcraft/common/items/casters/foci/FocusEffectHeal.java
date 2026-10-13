package thaumcraft.common.items.casters.foci;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.network.FocusFXPayload;

/** Heal: heals the living, hurts the undead. */
public class FocusEffectHeal extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSHEAL";
   }

   @Override
   public String getKey() {
      return "thaumcraft.HEAL";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.LIFE;
   }

   @Override
   public int getComplexity() {
      return getSettingValue("power") * 4;
   }

   @Override
   public float getDamageForDisplay(float finalPower) {
      return -getSettingValue("power") * finalPower;
   }

   @Override
   public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
      Level world = getPackage().world;
      FocusFXPayload.impact(world, target.getLocation(), getKey());
      if (target instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
         if (le.isInvertedHealAndHarm()) {
            le.hurt(FocusUtils.indirectMagic(world, getPackage().getCaster(), getPackage().getCaster()), getSettingValue("power") * finalPower * 1.5F);
         } else {
            le.heal(getSettingValue("power") * finalPower);
         }
      }
      return false;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("power", "focus.heal.power", new NodeSetting.NodeSettingIntRange(1, 5))};
   }

   @Override
   public void onCast(Entity caster) {
      caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.CHORUS_FLOWER_GROW, SoundSource.PLAYERS, 2.0F,
         2.0F + (float) (caster.level().random.nextGaussian() * 0.1F));
   }

   @Override
   public void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz) {
      thaumcraft.client.fx.FociParticles.heal(x, y, z, vx, vy, vz);
   }
}
