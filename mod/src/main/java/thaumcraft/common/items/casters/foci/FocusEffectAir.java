package thaumcraft.common.items.casters.foci;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.network.FocusFXPayload;
import thaumcraft.common.registers.ModSounds;

/** Air: a gust that damages and knocks back. */
public class FocusEffectAir extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSELEMENTAL";
   }

   @Override
   public String getKey() {
      return "thaumcraft.AIR";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.AIR;
   }

   @Override
   public int getComplexity() {
      return getSettingValue("power") * 2;
   }

   @Override
   public float getDamageForDisplay(float finalPower) {
      return (1 + getSettingValue("power")) * finalPower;
   }

   @Override
   public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
      Level world = getPackage().world;
      Vec3 at = target.getLocation();
      FocusFXPayload.impact(world, at, getKey());
      world.playSound(null, at.x, at.y, at.z, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5F, 0.66F);
      if (target instanceof EntityHitResult ehr) {
         Entity hit = ehr.getEntity();
         float damage = getDamageForDisplay(finalPower);
         hit.hurt(FocusUtils.thrown(world, hit, getPackage().getCaster()), damage);
         if (hit instanceof LivingEntity le) {
            if (trajectory != null) {
               le.knockback(damage * 0.25F, -trajectory.direction.x, -trajectory.direction.z);
            } else {
               le.knockback(damage * 0.25F, -Mth.sin(hit.getYRot() * Mth.DEG_TO_RAD), Mth.cos(hit.getYRot() * Mth.DEG_TO_RAD));
            }
         }
         return true;
      }
      return false;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5))};
   }

   @Override
   public void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz) {
      thaumcraft.client.fx.FociParticles.air(x, y, z, vx, vy, vz);
   }

   @Override
   public void onCast(Entity caster) {
      caster.level().playSound(null, caster.blockPosition().above(), ModSounds.WIND.get(), SoundSource.PLAYERS, 0.125F, 2.0F);
   }
}
