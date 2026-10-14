package thaumcraft.common.entities.projectile;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.registers.ModEldritch;

/**
 * EntityEldritchOrb: the dark orb eldritch guardians and wardens hurl. Weightless, lives 5 seconds; on impact it
 * deals two thirds of the thrower's attack damage as indirect magic to every living, non undead thing within 2 blocks
 * and weakens it.
 */
public class EldritchOrbEntity extends ThrowableProjectile {
   public EldritchOrbEntity(EntityType<? extends EldritchOrbEntity> type, Level level) {
      super(type, level);
   }

   public EldritchOrbEntity(Level level, LivingEntity thrower) {
      super(ModEldritch.ELDRITCH_ORB.get(), thrower, level);
      shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), -5.0F, 0.75F, 0.0F);
   }

   @Override
   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
   }

   @Override
   protected double getDefaultGravity() {
      return 0.0;
   }

   @Override
   public void tick() {
      super.tick();
      if (tickCount > 100) discard();
   }

   @Override
   protected void onHit(HitResult mop) {
      super.onHit(mop);
      Entity thrower = getOwner();
      if (!level().isClientSide && thrower instanceof LivingEntity owner) {
         for (Entity e : level().getEntities(thrower, getBoundingBox().inflate(2.0))) {
            if (e instanceof LivingEntity le && !le.getType().is(EntityTypeTags.UNDEAD)) {
               double atk = owner.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? owner.getAttributeValue(Attributes.ATTACK_DAMAGE) : 2.0;
               le.hurt(damageSources().indirectMagic(this, owner), (float) atk * 0.666F);
               le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
            }
         }
         playSound(SoundEvents.LAVA_EXTINGUISH, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
         discard();
      } else if (!level().isClientSide) {
         discard();
      }
   }
}
