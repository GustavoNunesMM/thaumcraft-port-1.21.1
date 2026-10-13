package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.common.registers.ModFlux;

/** PotionThaumarhia: now and then leaves a puddle of flux goo where you stand. */
public class ThaumarhiaEffect extends TCMobEffect {
   public ThaumarhiaEffect() {
      super(MobEffectCategory.HARMFUL, 6702199);
   }

   @Override
   public boolean applyEffectTick(LivingEntity target, int amplifier) {
      var level = target.level();
      if (!level.isClientSide && level.random.nextInt(15) == 0 && level.isEmptyBlock(target.blockPosition())) {
         level.setBlockAndUpdate(target.blockPosition(), ModFlux.FLUX_GOO.get().defaultBlockState());
      }
      return true;
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
      return duration % 20 == 0;
   }
}
