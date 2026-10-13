package thaumcraft.common.entities.monster;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** EntityFireBat: a burning, exploding bat of the nether. It hangs from ceilings until a player comes close. */
public class FireBatEntity extends Monster {
   private static final EntityDataAccessor<Boolean> HANGING = SynchedEntityData.defineId(FireBatEntity.class, EntityDataSerializers.BOOLEAN);
   @Nullable
   private BlockPos currentFlightTarget;
   @Nullable
   public LivingEntity owner;
   public int damBonus;
   private int attackTime;

   public FireBatEntity(EntityType<? extends FireBatEntity> type, Level level) {
      super(type, level);
      setIsBatHanging(true);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ATTACK_DAMAGE, 1.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(HANGING, false);
   }

   public boolean getIsBatHanging() {
      return entityData.get(HANGING);
   }

   public void setIsBatHanging(boolean b) {
      entityData.set(HANGING, b);
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Override
   protected float getSoundVolume() {
      return 0.1F;
   }

   @Override
   public float getVoicePitch() {
      return super.getVoicePitch() * 0.95F;
   }

   @Nullable
   @Override
   protected SoundEvent getAmbientSound() {
      return getIsBatHanging() && random.nextInt(4) != 0 ? null : SoundEvents.BAT_AMBIENT;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource src) {
      return SoundEvents.BAT_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return SoundEvents.BAT_DEATH;
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   protected void doPush(Entity e) {
   }

   @Override
   public void aiStep() {
      if (isInWaterRainOrBubble()) hurt(damageSources().drown(), 1.0F);
      super.aiStep();
   }

   @Override
   public void tick() {
      super.tick();
      if (getIsBatHanging()) {
         setDeltaMovement(Vec3.ZERO);
         setPosRaw(getX(), Mth.floor(getY()) + 1.0 - getBbHeight(), getZ());
      } else {
         setDeltaMovement(getDeltaMovement().multiply(1.0, 0.6, 1.0));
      }
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (attackTime > 0) attackTime--;
      BlockPos blockpos = blockPosition();
      BlockPos above = blockpos.above();
      LivingEntity target = getTarget();
      if (getIsBatHanging()) {
         if (!level().getBlockState(above).isRedstoneConductor(level(), above)) {
            setIsBatHanging(false);
            level().levelEvent(null, 1025, blockpos, 0);
         } else {
            if (random.nextInt(200) == 0) yHeadRot = random.nextInt(360);
            if (level().getNearestPlayer(this, 4.0) != null) {
               setIsBatHanging(false);
               level().levelEvent(null, 1025, blockpos, 0);
            }
         }
      } else if (target == null) {
         if (currentFlightTarget != null && (!level().isEmptyBlock(currentFlightTarget) || currentFlightTarget.getY() < level().getMinBuildHeight() + 1)) {
            currentFlightTarget = null;
         }
         if (currentFlightTarget == null || random.nextInt(30) == 0 || currentFlightTarget.closerToCenterThan(position(), 2.0)) {
            currentFlightTarget = BlockPos.containing((int) getX() + random.nextInt(7) - random.nextInt(7), (int) getY() + random.nextInt(6) - 2,
               (int) getZ() + random.nextInt(7) - random.nextInt(7));
         }
         steer(currentFlightTarget.getX() + 0.5 - getX(), currentFlightTarget.getY() + 0.1 - getY(), currentFlightTarget.getZ() + 0.5 - getZ());
         if (random.nextInt(100) == 0 && level().getBlockState(above).isRedstoneConductor(level(), above)) setIsBatHanging(true);
      } else {
         steer(target.getX() - getX(), target.getY() + target.getEyeHeight() * 0.66F - getY(), target.getZ() - getZ());
      }
      target = getTarget();
      if (target == null) {
         setTarget(findPlayerToAttack());
      } else if (target.isAlive()) {
         float f = target.distanceTo(this);
         if (isAlive() && hasLineOfSight(target)) attackEntity(target, f);
      } else {
         setTarget(null);
      }
      if (getTarget() instanceof Player p && p.getAbilities().invulnerable) setTarget(null);
   }

   private void steer(double dx, double dy, double dz) {
      Vec3 m = getDeltaMovement();
      setDeltaMovement(m.x + (Math.signum(dx) * 0.5 - m.x) * 0.1F, m.y + (Math.signum(dy) * 0.7F - m.y) * 0.1F, m.z + (Math.signum(dz) * 0.5 - m.z) * 0.1F);
      Vec3 nm = getDeltaMovement();
      float var7 = (float) (Mth.atan2(nm.z, nm.x) * 180.0 / Math.PI) - 90.0F;
      float var8 = Mth.wrapDegrees(var7 - getYRot());
      zza = 0.5F;
      setYRot(getYRot() + var8);
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   @Override
   public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
      return false;
   }

   @Override
   protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) return false;
      if (!level().isClientSide && getIsBatHanging()) setIsBatHanging(false);
      return super.hurt(source, amount);
   }

   protected void attackEntity(Entity entity, float dist) {
      if (attackTime <= 0 && dist < Math.max(2.5F, entity.getBbWidth() * 1.1F) && entity.getBoundingBox().maxY > getBoundingBox().minY
         && entity.getBoundingBox().minY < getBoundingBox().maxY) {
         attackTime = 20 + random.nextInt(20);
         if (random.nextInt(10) == 0 && !level().isClientSide) {
            entity.invulnerableTime = 0;
            level().explode(this, getX(), getY(), getZ(), 1.5F, Level.ExplosionInteraction.NONE);
            discard();
         }
         playSound(SoundEvents.BAT_HURT, 0.5F, 0.9F + random.nextFloat() * 0.2F);
         doHurtTarget(entity);
      }
   }

   @Nullable
   protected LivingEntity findPlayerToAttack() {
      Player p = level().getNearestPlayer(this, 12.0);
      return p != null && !p.isCreative() && !p.isSpectator() ? p : null;
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      setIsBatHanging(nbt.getBoolean("hang"));
      damBonus = nbt.getByte("damBonus");
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putBoolean("hang", getIsBatHanging());
      nbt.putByte("damBonus", (byte) damBonus);
   }

   /** getCanSpawnHere: only in the dark (the light level check of monsters is replaced by this one). */
   public static boolean checkSpawnRules(EntityType<FireBatEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
                                         RandomSource random) {
      if (level.getMaxLocalRawBrightness(pos) > random.nextInt(7)) return false;
      return checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
   }

   @Override
   public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
      return true;
   }
}
