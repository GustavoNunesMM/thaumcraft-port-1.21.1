package thaumcraft.common.entities.monster.tainted;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.registers.ModSounds;

/**
 * EntityTaintSwarm: a cloud of tiny biting things roving around the taint (spawned by taint geysers). It is drawn only
 * by its particles. Summoned swarms (e.g. by foci) wither away without a target.
 */
public class TaintSwarmEntity extends TaintedMonster {
   private static final EntityDataAccessor<Boolean> SUMMONED = SynchedEntityData.defineId(TaintSwarmEntity.class, EntityDataSerializers.BOOLEAN);
   private BlockPos currentFlightTarget;
   public int damBonus;
   /** Client: the swarm particles alive (FXSwarm). */
   public final List<Object> swarm = new ArrayList<>();
   private int attackTime;

   public TaintSwarmEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      setNoGravity(false);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 2.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(SUMMONED, false);
   }

   public boolean getIsSummoned() {
      return entityData.get(SUMMONED);
   }

   public void setIsSummoned(boolean b) {
      entityData.set(SUMMONED, b);
   }

   @Override
   protected float getSoundVolume() {
      return 0.1F;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.SWARM.get();
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.SWARMATTACK.get();
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.SWARMATTACK.get();
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   protected void doPush(Entity entity) {
   }

   @Override
   public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
      return false;
   }

   @Override
   protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      Vec3 m = getDeltaMovement();
      setDeltaMovement(m.x, m.y * 0.6F, m.z);
      if (level().isClientSide) {
         swarm.removeIf(p -> !thaumcraft.client.fx.TaintFX.isAlive(p));
         if (swarm.size() < 30) swarm.add(thaumcraft.client.fx.TaintFX.swarmParticle(this, 0.22F, 15.0F, 0.08F));
      }
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (attackTime > 0) attackTime--;
      LivingEntity target = getTarget();
      Vec3 m = getDeltaMovement();
      if (target == null) {
         if (getIsSummoned()) hurt(damageSources().generic(), 5.0F);
         if (currentFlightTarget != null && (!level().isEmptyBlock(currentFlightTarget) || currentFlightTarget.getY() < level().getMinBuildHeight() + 1
            || currentFlightTarget.getY() > level().getHeight(Heightmap.Types.MOTION_BLOCKING, currentFlightTarget.getX(), currentFlightTarget.getZ()) + 2
            || !TaintHelper.isNearTaintSeed(level(), currentFlightTarget))) {
            currentFlightTarget = null;
         }
         if (currentFlightTarget == null || random.nextInt(30) == 0 || currentFlightTarget.getCenter().distanceToSqr(position()) < 4.0) {
            currentFlightTarget = BlockPos.containing(getX() + random.nextInt(7) - random.nextInt(7), getY() + random.nextInt(6) - 2,
               getZ() + random.nextInt(7) - random.nextInt(7));
         }
         double dx = currentFlightTarget.getX() + 0.5 - getX();
         double dy = currentFlightTarget.getY() + 0.1 - getY();
         double dz = currentFlightTarget.getZ() + 0.5 - getZ();
         m = new Vec3(m.x + (Math.signum(dx) * 0.5 - m.x) * 0.015F, m.y + (Math.signum(dy) * 0.7F - m.y) * 0.1F, m.z + (Math.signum(dz) * 0.5 - m.z) * 0.015F);
      } else {
         double dx = target.getX() - getX();
         double dy = target.getY() + target.getEyeHeight() - getY();
         double dz = target.getZ() - getZ();
         m = new Vec3(m.x + (Math.signum(dx) * 0.5 - m.x) * 0.025F, m.y + (Math.signum(dy) * 0.7F - m.y) * 0.1F, m.z + (Math.signum(dz) * 0.5 - m.z) * 0.025F);
      }
      setDeltaMovement(m);
      float yaw = (float) (Math.atan2(m.z, m.x) * 180.0 / Math.PI) - 90.0F;
      setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()));
      zza = 0.1F;
      if (level().isClientSide) return;
      if (target == null) {
         if (!getIsSummoned()) setTarget(level().getNearestPlayer(this, 8.0));
      } else if (isAlive() && target.isAlive()) {
         if (hasLineOfSight(target)) attackEntity(target, distanceTo(target));
      } else {
         setTarget(null);
      }
      if (getTarget() instanceof Player p && (p.getAbilities().invulnerable || p.isSpectator())) setTarget(null);
   }

   private void attackEntity(LivingEntity target, float dist) {
      if (attackTime <= 0 && dist < 3.0F && target.getBoundingBox().maxY > getBoundingBox().minY && target.getBoundingBox().minY < getBoundingBox().maxY) {
         attackTime = 15 + random.nextInt(10);
         Vec3 keep = target.getDeltaMovement();
         if (doHurtTarget(target)) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
         target.setDeltaMovement(keep);
         target.hurtMarked = false;
         playSound(ModSounds.SWARMATTACK.get(), 0.3F, 0.9F + random.nextFloat() * 0.2F);
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      setIsSummoned(tag.getBoolean("summoned"));
      damBonus = tag.getByte("damBonus");
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putBoolean("summoned", getIsSummoned());
      tag.putByte("damBonus", (byte) damBonus);
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      if (random.nextBoolean()) dropFluxCrystal();
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }
}
