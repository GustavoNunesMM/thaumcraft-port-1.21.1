package thaumcraft.common.entities.monster;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** EntityGiantBrainyZombie: grows (and hits harder) the more it is hurt; drops a pile of rotten flesh. */
public class GiantBrainyZombieEntity extends BrainyZombieEntity {
   private static final EntityDataAccessor<Float> ANGER = SynchedEntityData.defineId(GiantBrainyZombieEntity.class, EntityDataSerializers.FLOAT);

   public GiantBrainyZombieEntity(EntityType<? extends Zombie> type, Level level) {
      super(type, level);
      xpReward = 15;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return BrainyZombieEntity.createAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ATTACK_DAMAGE, 7.0);
   }

   @Override
   protected void addBehaviourGoals() {
      super.addBehaviourGoals();
      goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ANGER, 0.0F);
   }

   public float getAnger() {
      return entityData.get(ANGER);
   }

   public void setAnger(float a) {
      entityData.set(ANGER, a);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (ANGER.equals(key)) refreshDimensions();
   }

   /** The original only resized above anger 1 (setSize(0.6 + anger * 0.6, 1.95 + anger * 1.95)). */
   public float sizeScale() {
      return getAnger() > 1.0F ? 1.0F + getAnger() : 1.0F;
   }

   @Override
   public EntityDimensions getDefaultDimensions(Pose pose) {
      return super.getDefaultDimensions(pose).scale(sizeScale());
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!level().isClientSide) {
         if (getAnger() > 1.0F) setAnger(getAnger() - 0.002F);
         getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7.0F + (getAnger() - 1.0F) * 5.0F);
      }
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (!level().isClientSide && !isInvulnerableTo(source)) setAnger(Math.min(2.0F, getAnger() + 0.1F));
      return super.hurt(source, amount);
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      super.dropCustomDeathLoot(level, source, recentlyHit);
      for (int a = 0; a < 12; a++) {
         if (random.nextBoolean()) spawnAtLocation(Items.ROTTEN_FLESH, 2);
      }
   }
}
