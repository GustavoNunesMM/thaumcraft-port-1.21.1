package thaumcraft.common.lib.potions;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.lib.damage.DamageSourceThaumcraft;

/**
 * PotionFluxTaint: hurts the living (1 taint damage every 40 &gt;&gt; amplifier ticks) and heals tainted creatures.
 * (Tainted champions - champion modifier 13 - are healed as well in 1.12; champions are not ported yet.)
 */
public class FluxTaintEffect extends TCMobEffect {
   public FluxTaintEffect() {
      super(MobEffectCategory.HARMFUL, 6697847);
   }

   @Override
   public boolean applyEffectTick(LivingEntity target, int amplifier) {
      if (target.level().isClientSide) return true;
      if (target instanceof ITaintedMob) {
         target.heal(1.0F);
      } else if (!target.getType().is(EntityTypeTags.UNDEAD)) {
         target.hurt(DamageSourceThaumcraft.taint(target.level()), 1.0F);
      }
      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      int k = 40 >> amplifier;
      return k <= 0 || duration % k == 0;
   }
}
