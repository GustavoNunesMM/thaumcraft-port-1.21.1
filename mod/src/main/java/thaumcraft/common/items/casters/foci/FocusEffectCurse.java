package thaumcraft.common.items.casters.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.lib.FXHelper;
import thaumcraft.common.registers.ModCasters;

/** Curse: poisons and inflicts random maladies; on blocks it leaves a withering field (effect_sap) that fades. */
public class FocusEffectCurse extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSCURSE";
   }

   @Override
   public String getKey() {
      return "thaumcraft.CURSE";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.DEATH;
   }

   @Override
   public int getComplexity() {
      return getSettingValue("duration") + getSettingValue("power") * 3;
   }

   @Override
   public float getDamageForDisplay(float finalPower) {
      return (1.0F + getSettingValue("power")) * finalPower;
   }

   @Override
   public boolean execute(HitResult target, Trajectory trajectory, float finalPower, int num) {
      Level world = getPackage().world;
      Vec3 at = target.getLocation();
      if (world instanceof ServerLevel sl) FXHelper.bamf(sl, at.x, at.y, at.z, 6946821, true, true, null);
      if (target instanceof EntityHitResult ehr) {
         Entity hit = ehr.getEntity();
         float damage = getDamageForDisplay(finalPower);
         int duration = 20 * getSettingValue("duration");
         int eff = Math.max(0, (int) (getSettingValue("power") * finalPower / 2.0F));
         hit.hurt(FocusUtils.indirectMagic(world, hit, getPackage().getCaster()), damage);
         if (hit instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.POISON, duration, eff));
            float c = 0.85F;
            Object[][] maladies = {{MobEffects.MOVEMENT_SLOWDOWN, 1}, {MobEffects.WEAKNESS, 1}, {MobEffects.DIG_SLOWDOWN, 2}, {MobEffects.HUNGER, 3},
               {MobEffects.UNLUCK, 3}};
            for (int i = 0; i < maladies.length; i++) {
               if (world.random.nextFloat() < c) {
                  @SuppressWarnings("unchecked")
                  Holder<MobEffect> e = (Holder<MobEffect>) maladies[i][0];
                  le.addEffect(new MobEffectInstance(e, duration * (Integer) maladies[i][1], eff));
                  if (i < maladies.length - 1) c -= 0.15F;
               }
            }
         }
      } else if (target instanceof BlockHitResult bhr) {
         float f = (float) Math.min(8.0, 1.5 * getSettingValue("power") * finalPower);
         int fi = (int) f;
         BlockPos c = bhr.getBlockPos();
         for (BlockPos p : BlockPos.betweenClosed(c.offset(-fi, -fi, -fi), c.offset(fi, fi, fi))) {
            if (p.distToCenterSqr(at) <= f * f && world.isEmptyBlock(p.above()) && world.getBlockState(p).isCollisionShapeFullBlock(world, p)) {
               world.setBlockAndUpdate(p.above(), ModCasters.EFFECT_SAP.get().defaultBlockState());
            }
         }
      }
      return false;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
         new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(1, 10))};
   }

   @Override
   public void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz) {
      thaumcraft.client.fx.FociParticles.curse(x, y, z, vx, vy, vz);
   }

   @Override
   public void onCast(Entity caster) {
      caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.15F,
         1.0F + caster.level().random.nextFloat() / 2.0F);
   }
}
