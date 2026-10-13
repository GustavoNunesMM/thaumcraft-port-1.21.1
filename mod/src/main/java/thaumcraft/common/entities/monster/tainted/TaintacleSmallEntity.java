package thaumcraft.common.entities.monster.tainted;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** EntityTaintacleSmall (taint tendril): a short-lived (10 s) little taintacle summoned under a target. */
public class TaintacleSmallEntity extends TaintacleEntity {
   private int lifetime = 200;

   public TaintacleSmallEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      xpReward = 0;
   }

   public static AttributeSupplier.Builder createSmallAttributes() {
      return createAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   @Override
   public void tick() {
      super.tick();
      if (!level().isClientSide && lifetime-- <= 0) hurt(damageSources().magic(), 10.0F);
   }

   @Override
   public boolean canSpawnHere() {
      return false;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
   }
}
