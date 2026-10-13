package thaumcraft.common.entities.monster;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.lib.MobFXBridge;
import thaumcraft.common.network.MobNetwork;
import thaumcraft.common.registers.ModSounds;

/** EntityWisp: a floating mote of vis of a random aspect that zaps whoever annoys it and drops a crystal of its aspect. */
public class WispEntity extends FlyingMob implements Enemy {
   private static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(WispEntity.class, EntityDataSerializers.STRING);
   private int aggroCooldown;
   public int prevAttackCounter;
   public int attackCounter;
   @Nullable
   private BlockPos currentFlightTarget;

   public WispEntity(EntityType<? extends WispEntity> type, Level level) {
      super(type, level);
      xpReward = 5;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return FlyingMob.createMobAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 16.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(TYPE, "");
   }

   public String getWispType() {
      return entityData.get(TYPE);
   }

   public void setWispType(String t) {
      entityData.set(TYPE, t);
   }

   @Override
   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   @Override
   protected int decreaseAirSupply(int air) {
      return air;
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (source.getEntity() instanceof LivingEntity le) {
         setTarget(le);
         aggroCooldown = 200;
      }
      return super.hurt(source, amount);
   }

   @Override
   public void die(DamageSource source) {
      super.die(source);
      if (level().isClientSide) MobFXBridge.INSTANCE.burst(getX(), getY() + 0.45F, getZ(), 1.0F);
   }

   @Override
   public void tick() {
      super.tick();
      if (level().isClientSide) {
         if (tickCount <= 1) MobFXBridge.INSTANCE.burst(getX(), getY(), getZ(), 10.0F);
         Aspect a = Aspect.getAspect(getWispType());
         if (random.nextBoolean() && a != null) {
            MobFXBridge.INSTANCE.wispParticles(getX() + (random.nextFloat() - random.nextFloat()) * 0.7F,
               getY() + (random.nextFloat() - random.nextFloat()) * 0.7F, getZ() + (random.nextFloat() - random.nextFloat()) * 0.7F, a.getColor());
         }
      }
      setDeltaMovement(getDeltaMovement().multiply(1.0, 0.6, 1.0));
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (level().isClientSide) return;
      if (Aspect.getAspect(getWispType()) == null) {
         List<Aspect> as = random.nextInt(10) != 0 ? Aspect.getPrimalAspects() : Aspect.getCompoundAspects();
         setWispType(as.get(random.nextInt(as.size())).getTag());
      }
      if (level().getDifficulty() == Difficulty.PEACEFUL) discard();
      prevAttackCounter = attackCounter;
      double attackrange = 16.0;
      LivingEntity target = getTarget();
      if (target != null && hasLineOfSight(target)) {
         if (distanceToSqr(target) > attackrange * attackrange / 2.0) {
            steer(target.getX() - getX(), target.getY() + target.getEyeHeight() * 0.66F - getY(), target.getZ() - getZ(), 0.5F);
         }
      } else {
         if (currentFlightTarget != null && (!level().isEmptyBlock(currentFlightTarget) || currentFlightTarget.getY() < level().getMinBuildHeight() + 1
            || currentFlightTarget.getY() > level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, currentFlightTarget).above(8).getY())) {
            currentFlightTarget = null;
         }
         if (currentFlightTarget == null || random.nextInt(30) == 0 || currentFlightTarget.closerToCenterThan(position(), 2.0)) {
            currentFlightTarget = BlockPos.containing((int) getX() + random.nextInt(7) - random.nextInt(7), (int) getY() + random.nextInt(6) - 2,
               (int) getZ() + random.nextInt(7) - random.nextInt(7));
         }
         steer(currentFlightTarget.getX() + 0.5 - getX(), currentFlightTarget.getY() + 0.1 - getY(), currentFlightTarget.getZ() + 0.5 - getZ(), 0.15F);
      }
      if (getTarget() instanceof Player p && p.getAbilities().invulnerable) setTarget(null);
      if (getTarget() != null && !getTarget().isAlive()) setTarget(null);
      aggroCooldown--;
      if (random.nextInt(1000) == 0 && (getTarget() == null || aggroCooldown-- <= 0)) {
         Player p = level().getNearestPlayer(this, 16.0);
         setTarget(p != null && !p.isCreative() && !p.isSpectator() ? p : null);
         if (getTarget() != null) aggroCooldown = 50;
      }
      target = getTarget();
      if (isAlive() && target != null && target.distanceToSqr(this) < attackrange * attackrange) {
         double d5 = target.getX() - getX();
         double d7 = target.getZ() - getZ();
         float yaw = -((float) Mth.atan2(d5, d7)) * 180.0F / (float) Math.PI;
         setYRot(yaw);
         yBodyRot = yaw;
         if (hasLineOfSight(target)) {
            attackCounter++;
            if (attackCounter == 20) {
               playSound(ModSounds.ZAP.get(), 1.0F, 1.1F);
               if (level() instanceof ServerLevel sl) {
                  MobNetwork.sendNear(sl, getX(), getY(), getZ(), 32.0, new MobNetwork.WispZapPayload(getId(), target.getId()));
               }
               float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
               Vec3 tm = target.getDeltaMovement();
               if (Math.abs(tm.x) <= 0.1F && Math.abs(tm.y) <= 0.1F && Math.abs(tm.z) <= 0.1F) {
                  if (random.nextFloat() < 0.66F) target.hurt(damageSources().mobAttack(this), damage + 1.0F);
               } else if (random.nextFloat() < 0.4F) {
                  target.hurt(damageSources().mobAttack(this), damage);
               }
               attackCounter = -20 + random.nextInt(20);
            }
         } else if (attackCounter > 0) {
            attackCounter--;
         }
      }
   }

   private void steer(double dx, double dy, double dz, float forward) {
      Vec3 m = getDeltaMovement();
      setDeltaMovement(m.x + (Math.signum(dx) * 0.5 - m.x) * 0.1F, m.y + (Math.signum(dy) * 0.7F - m.y) * 0.1F, m.z + (Math.signum(dz) * 0.5 - m.z) * 0.1F);
      Vec3 nm = getDeltaMovement();
      float var7 = (float) (Mth.atan2(nm.z, nm.x) * 180.0 / Math.PI) - 90.0F;
      float var8 = Mth.wrapDegrees(var7 - getYRot());
      zza = forward;
      setYRot(getYRot() + var8);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.WISPLIVE.get();
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource src) {
      return SoundEvents.LAVA_EXTINGUISH;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.WISPDEAD.get();
   }

   @Override
   protected float getSoundVolume() {
      return 0.25F;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      super.dropCustomDeathLoot(level, source, recentlyHit);
      Aspect a = Aspect.getAspect(getWispType());
      if (a != null) spawnAtLocation(VisCrystalItem.of(a, 1), 0.0F);
   }

   @Override
   public int getMaxSpawnClusterSize() {
      return 2;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putString("Type", getWispType());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      setWispType(nbt.getString("Type"));
   }

   /** getCanSpawnHere: at most 8 wisps around, never in peaceful, and only in the dark (isValidLightLevel). */
   public static boolean checkSpawnRules(EntityType<WispEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
      if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
      if (MobSpawnType.isSpawner(reason)) return true;
      if (level.getBrightness(LightLayer.SKY, pos) > random.nextInt(32)) return false;
      int i = level.getLevel().isThundering() ? level.getMaxLocalRawBrightness(pos, 10) : level.getMaxLocalRawBrightness(pos);
      if (i > random.nextInt(8)) return false;
      return level.getEntitiesOfClass(WispEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(16.0)).size() < 8;
   }

   @Override
   public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
      return true;
   }
}
