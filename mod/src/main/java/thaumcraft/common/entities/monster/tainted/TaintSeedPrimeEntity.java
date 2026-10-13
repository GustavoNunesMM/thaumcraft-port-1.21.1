package thaumcraft.common.entities.monster.tainted;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** EntityTaintSeedPrime (giant taint seed): twice the reach, tougher, more crystals. Spawned by flux rifts. */
public class TaintSeedPrimeEntity extends TaintSeedEntity {
   public TaintSeedPrimeEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      xpReward = 12;
   }

   public static AttributeSupplier.Builder createPrimeAttributes() {
      return createAttributes().add(Attributes.MAX_HEALTH, 150.0).add(Attributes.ATTACK_DAMAGE, 7.0);
   }

   @Override
   protected int getArea() {
      return 2;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      dropFluxCrystal();
      if (random.nextBoolean()) dropFluxCrystal();
      if (random.nextBoolean()) dropFluxCrystal();
   }
}
