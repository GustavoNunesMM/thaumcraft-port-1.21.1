package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** PotionUnnaturalHunger: constant exhaustion. */
public class UnnaturalHungerEffect extends TCMobEffect {
   public UnnaturalHungerEffect() {
      super(MobEffectCategory.HARMFUL, 4482611);
   }

   @Override
   public boolean applyEffectTick(LivingEntity target, int amplifier) {
      if (!target.level().isClientSide && target instanceof Player p) p.causeFoodExhaustion(0.025F * (amplifier + 1));
      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return true;
   }
}
