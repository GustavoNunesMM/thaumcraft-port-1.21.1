package thaumcraft.common.items.casters.foci;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.network.FocusFXPayload;

/** Frost: damages and slows creatures; freezes still water into frosted ice. */
public class FocusEffectFrost extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSELEMENTAL";
   }

   @Override
   public String getKey() {
      return "thaumcraft.FROST";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.COLD;
   }

   @Override
   public int getComplexity() {
      return getSettingValue("duration") + getSettingValue("power") * 2;
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
         float damage = getDamageForDisplay(finalPower);
         int duration = 20 * getSettingValue("duration");
         int potency = (int) (1.0F + getSettingValue("power") * finalPower / 3.0F);
         hit.hurt(FocusUtils.thrown(world, hit, getPackage().getCaster()), damage);
         if (hit instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, potency));
      } else if (target instanceof BlockHitResult bhr) {
         float f = Math.min(16.0F, 2 * getSettingValue("power") * finalPower);
         BlockPos c = bhr.getBlockPos();
         int fi = (int) f;
         for (BlockPos p : BlockPos.betweenClosed(c.offset(-fi, -fi, -fi), c.offset(fi, fi, fi))) {
            if (p.distToCenterSqr(target.getLocation()) <= f * f) {
               BlockState bs = world.getBlockState(p);
               BlockState ice = Blocks.FROSTED_ICE.defaultBlockState();
               if (bs.is(Blocks.WATER) && bs.getFluidState().isSource() && ice.canSurvive(world, p)
                  && world.isUnobstructed(ice, p, CollisionContext.empty())) {
                  world.setBlockAndUpdate(p, ice);
                  world.scheduleTick(p.immutable(), Blocks.FROSTED_ICE, Mth.nextInt(world.random, 60, 120));
               }
            }
         }
      }
      return false;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
         new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(2, 10))};
   }

   @Override
   public void renderParticleFX(Level level, double x, double y, double z, double vx, double vy, double vz) {
      thaumcraft.client.fx.FociParticles.frost(x, y, z, vx, vy, vz);
   }

   @Override
   public void onCast(Entity caster) {
      caster.level().playSound(null, caster.blockPosition().above(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.2F,
         1.0F + (float) (caster.level().random.nextGaussian() * 0.05F));
   }
}
