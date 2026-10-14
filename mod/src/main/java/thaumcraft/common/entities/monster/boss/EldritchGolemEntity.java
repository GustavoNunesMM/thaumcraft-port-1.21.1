package thaumcraft.common.entities.monster.boss;

import thaumcraft.common.entities.monster.cult.ThaumcraftBossEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchHelper;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.blocks.devices.LootBlock;
import thaumcraft.common.entities.monster.cult.CultGoals;
import thaumcraft.common.entities.monster.eldritch.EldritchHelper;
import thaumcraft.common.entities.projectile.EldritchOrbEntity;
import thaumcraft.common.registers.ModSounds;

/**
 * EntityEldritchGolem, the Eldritch Construct: a huge armoured golem that smashes urns and frail blocks as it walks
 * and hurls its foes into the air. The first killing blow blows its head off instead (an explosion, 100 more spawn
 * ticks); headless, it charges a beam and fires homing electric orbs (EntityGolemOrb) while arcs crackle around it.
 */
public class EldritchGolemEntity extends ThaumcraftBossEntity implements IEldritchMob, RangedAttackMob {
   private static final EntityDataAccessor<Boolean> HEADLESS = SynchedEntityData.defineId(EldritchGolemEntity.class, EntityDataSerializers.BOOLEAN);
   int beamCharge = 0;
   boolean chargingBeam = false;
   public int arcing = 0;
   public int ax = 0;
   public int ay = 0;
   public int az = 0;
   private int attackTimer;
   private boolean headlessGoals = false;

   public EldritchGolemEntity(EntityType<? extends ThaumcraftBossEntity> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return ThaumcraftBossEntity.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.MAX_HEALTH, 400.0)
         .add(Attributes.ARMOR, 6.0);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
      goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
      goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
      goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(HEADLESS, false);
   }

   public boolean isHeadless() {
      return entityData.get(HEADLESS);
   }

   public void setHeadless(boolean b) {
      entityData.set(HEADLESS, b);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putBoolean("headless", isHeadless());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      setHeadless(nbt.getBoolean("headless"));
      if (isHeadless()) makeHeadless();
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.IRON_GOLEM_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.IRON_GOLEM_DEATH;
   }

   @Override
   protected void playStepSound(BlockPos pos, BlockState block) {
      playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
      spawnTimer = 100;
      return super.finalizeSpawn(level, diff, reason, data);
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (attackTimer > 0) attackTimer--;
      Vec3 m = getDeltaMovement();
      if (m.x * m.x + m.z * m.z > 2.5000003E-7F && random.nextInt(5) == 0) {
         BlockState bs = level().getBlockState(blockPosition());
         if (!bs.isAir()) {
            level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bs), getX() + (random.nextFloat() - 0.5) * getBbWidth(),
               getBoundingBox().minY + 0.1, getZ() + (random.nextFloat() - 0.5) * getBbWidth(), 4.0 * (random.nextFloat() - 0.5), 0.5,
               (random.nextFloat() - 0.5) * 4.0);
         }
         if (!level().isClientSide && bs.getBlock() instanceof LootBlock) level().destroyBlock(blockPosition(), true);
      }
      if (!level().isClientSide) {
         BlockState bsx = level().getBlockState(blockPosition());
         float h = bsx.getDestroySpeed(level(), blockPosition());
         if (!bsx.isAir() && h >= 0.0F && h <= 0.15F) level().destroyBlock(blockPosition(), true);
      }
   }

   @Override
   public boolean hurt(DamageSource source, float damage) {
      if (!level().isClientSide && damage > getHealth() && !isHeadless() && !isInvulnerableTo(source)) {
         setHeadless(true);
         spawnTimer = 100;
         double xx = Mth.cos(getYRot() % 360.0F / 180.0F * (float) Math.PI) * 0.75F;
         double zz = Mth.sin(getYRot() % 360.0F / 180.0F * (float) Math.PI) * 0.75F;
         level().explode(this, getX() + xx, getY() + getEyeHeight(), getZ() + zz, 2.0F, Level.ExplosionInteraction.NONE);
         makeHeadless();
         return false;
      }
      return super.hurt(source, damage);
   }

   void makeHeadless() {
      if (headlessGoals) return;
      headlessGoals = true;
      goalSelector.addGoal(2, new CultGoals.LongRangeAttack(this, 3.0, 1.0, 5, 5, 24.0F));
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (attackTimer > 0) return false;
      attackTimer = 10;
      level().broadcastEntityEvent(this, (byte) 4);
      boolean flag = target.hurt(damageSources().mobAttack(this), (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75F);
      if (flag) {
         target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.2000000059604645, 0.0));
         if (isHeadless()) {
            target.push(-Mth.sin(getYRot() * (float) Math.PI / 180.0F) * 1.5F, 0.1, Mth.cos(getYRot() * (float) Math.PI / 180.0F) * 1.5F);
         }
         target.hurtMarked = true;
      }
      return flag;
   }

   @Override
   public void performRangedAttack(LivingEntity target, float f) {
      if (hasLineOfSight(target) && !chargingBeam && beamCharge > 0) {
         beamCharge -= 15 + random.nextInt(5);
         getLookControl().setLookAt(target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2.0F, target.getZ(), 30.0F, 30.0F);
         Vec3 v = getViewVector(1.0F);
         Projectile blast = golemOrb(target);
         blast.setPos(blast.getX() + v.x, blast.getY(), blast.getZ() + v.z);
         double d0 = target.getX() + target.getDeltaMovement().x - getX();
         double d1 = target.getY() - getY() - target.getBbHeight() / 2.0F;
         double d2 = target.getZ() + target.getDeltaMovement().z - getZ();
         blast.shoot(d0, d1, d2, 0.66F, 5.0F);
         playSound(ModSounds.EGATTACK.get(), 1.0F, 1.0F + random.nextFloat() * 0.1F);
         level().addFreshEntity(blast);
      }
   }

   /** new EntityGolemOrb(world, this, target, false). */
   private Projectile golemOrb(LivingEntity target) {
      return new thaumcraft.common.entities.projectile.GolemOrbEntity(level(), this, target, false);
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 4) {
         attackTimer = 10;
         playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
      } else if (id == 18) {
         spawnTimer = 150;
      } else if (id == 19) {
         if (arcing == 0) {
            float radius = 2.0F + random.nextFloat() * 2.0F;
            double radians = Math.toRadians(random.nextInt(360));
            int bx = Mth.floor(getX() + radius * Math.cos(radians));
            int by = Mth.floor(getY());
            int bz = Mth.floor(getZ() + radius * Math.sin(radians));
            BlockPos bp = new BlockPos(bx, by, bz);
            // (1.12 lowered "by" without moving bp, so the probe never went down; kept.)
            if (level().isEmptyBlock(bp.above()) && !level().isEmptyBlock(bp)) {
               ax = bx;
               ay = by;
               az = bz;
               arcing = 8 + random.nextInt(5);
               playSound(ModSounds.JACOBS.get(), 0.8F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.05F);
            }
         }
      } else {
         super.handleEntityEvent(id);
      }
   }

   @Override
   public void tick() {
      if (getSpawnTimer() == 150) level().broadcastEntityEvent(this, (byte) 18);
      if (getSpawnTimer() > 0) heal(2.0F);
      super.tick();
      if (level().isClientSide) {
         if (isHeadless()) {
            float f1 = Mth.cos(-yBodyRot * (float) (Math.PI / 180.0) - (float) Math.PI);
            float f2 = Mth.sin(-yBodyRot * (float) (Math.PI / 180.0) - (float) Math.PI);
            float f3 = -1.0F;
            Vec3 v = new Vec3(f2 * f3, 0.0, f1 * f3);
            thaumcraft.client.fx.EldritchFX.golemHeadless(this, v);
         }
      } else {
         if (isHeadless() && beamCharge <= 0) chargingBeam = true;
         if (isHeadless() && chargingBeam) {
            beamCharge++;
            level().broadcastEntityEvent(this, (byte) 19);
            if (beamCharge == 150) chargingBeam = false;
         }
      }
   }

   public int getAttackTimer() {
      return attackTimer;
   }

   @Override
   public boolean isAlliedTo(net.minecraft.world.entity.Entity e) {
      return EldritchHelper.isEldritch(e) || super.isAlliedTo(e);
   }
}
