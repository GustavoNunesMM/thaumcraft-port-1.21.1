package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** PotionSunScorned: bright sunlight sets you on fire, darkness heals. */
public class SunScornedEffect extends TCMobEffect {
   public SunScornedEffect() {
      super(MobEffectCategory.HARMFUL, 16308330);
   }

   @Override
   public boolean applyEffectTick(LivingEntity target, int amplifier) {
      if (!target.level().isClientSide) {
         float f = target.getLightLevelDependentMagicValue();
         var rand = target.level().random;
         if (f > 0.5F && rand.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && target.level().canSeeSky(target.blockPosition())) {
            target.igniteForSeconds(4);
         } else if (f < 0.25F && rand.nextFloat() > f * 2.0F) {
            target.heal(1.0F);
         }
      }
      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return duration % 40 == 0;
   }
}
