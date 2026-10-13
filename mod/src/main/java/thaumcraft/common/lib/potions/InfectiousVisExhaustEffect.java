package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.common.registers.ModFlux;

/** PotionInfectiousVisExhaust (flux phage): spreads to everything within 4 blocks, one level weaker each hop. */
public class InfectiousVisExhaustEffect extends TCMobEffect {
   public InfectiousVisExhaustEffect() {
      super(MobEffectCategory.HARMFUL, 6706551);
   }

   @Override
   public boolean applyEffectTick(LivingEntity target, int amplifier) {
      if (target.level().isClientSide) return true;
      for (LivingEntity e : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4.0))) {
         if (!e.hasEffect(ModFlux.INFECTIOUS_VIS_EXHAUST)) {
            if (amplifier > 0) e.addEffect(new MobEffectInstance(ModFlux.INFECTIOUS_VIS_EXHAUST, 6000, amplifier - 1, false, true));
            else e.addEffect(new MobEffectInstance(ModFlux.VIS_EXHAUST, 6000, 0, false, true));
         }
      }
      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return duration % 40 == 0;
   }
}
