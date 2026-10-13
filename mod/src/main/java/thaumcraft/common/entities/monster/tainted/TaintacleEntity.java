package thaumcraft.common.entities.monster.tainted;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.registers.ModSounds;

/**
 * EntityTaintacle: an immobile tentacle rooted in taint (it withers away from it). It flails at players and calls up
 * tendrils under targets standing on taint a few blocks away.
 */
public class TaintacleEntity extends TaintedMonster {
   public float flailIntensity = 1.0F;

   public TaintacleEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      xpReward = 8;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.MOVEMENT_SPEED, 0.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
      goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
      goalSelector.addGoal(3, new RandomLookAroundGoal(this));
      targetSelector.addGoal(0, new HurtByTargetGoal(this));
      targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
   }

   public boolean canSpawnHere() {
      return onTaint(level(), blockPosition()) && level().getDifficulty() != Difficulty.PEACEFUL;
   }

   @Override
   public void move(MoverType type, Vec3 movement) {
      super.move(type, new Vec3(0.0, Math.min(movement.y, 0.0), 0.0));
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   public void tick() {
      super.tick();
      if (!level().isClientSide && tickCount % 20 == 0) {
         if (!onTaint(level(), blockPosition())) hurt(damageSources().starve(), 1.0F);
         LivingEntity target = getTarget();
         if (!(this instanceof TaintacleSmallEntity) && tickCount % 40 == 0 && target != null && distanceToSqr(target) > 16.0
            && distanceToSqr(target) < 256.0 && getSensing().hasLineOfSight(target)) {
            spawnTentacles(target);
         }
      }
      if (level().isClientSide) {
         if (flailIntensity > 1.0F) flailIntensity -= 0.01F;
         if (tickCount < getBbHeight() * 10.0F && onGround()) thaumcraft.client.fx.TaintFX.tentacleArise(this);
      }
   }

   @Override
   public int getAmbientSoundInterval() {
      return 200;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.CHORUS_FLOWER_DEATH;
   }

   @Override
   public float getVoicePitch() {
      return 1.3F - getBbHeight() / 10.0F;
   }

   @Override
   protected float getSoundVolume() {
      return getBbHeight() / 8.0F;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.TENTACLE.get();
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.TENTACLE.get();
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      dropFluxCrystal();
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 16) flailIntensity = 3.0F;
      else super.handleEntityEvent(id);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      level().broadcastEntityEvent(this, (byte) 16);
      playSound(ModSounds.TENTACLE.get(), getSoundVolume(), getVoicePitch());
      return super.doHurtTarget(target);
   }
}
