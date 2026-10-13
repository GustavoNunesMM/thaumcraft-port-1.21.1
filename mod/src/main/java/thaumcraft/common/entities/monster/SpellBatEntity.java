package thaumcraft.common.entities.monster;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.items.casters.foci.FocusUtils;
import thaumcraft.common.registers.ModCasters;

/** EntitySpellBat: a short-lived bat conjured by the spell bat medium; it hunts and applies the package on each attack. */
public class SpellBatEntity extends Monster implements IEntityWithComplexSpawn {
   private static final EntityDataAccessor<Boolean> FRIENDLY = SynchedEntityData.defineId(SpellBatEntity.class, EntityDataSerializers.BOOLEAN);
   @Nullable
   private BlockPos currentFlightTarget;
   @Nullable
   public LivingEntity owner;
   public FocusPackage focusPackage = new FocusPackage();
   @Nullable
   private UUID ownerUniqueId;
   private int attackTime;
   public FocusEffect[] effects;
   public int color = 0xFFFFFF;

   public SpellBatEntity(EntityType<? extends SpellBatEntity> type, Level level) {
      super(type, level);
   }

   public SpellBatEntity(FocusPackage pac, boolean friendly) {
      super(ModCasters.SPELL_BAT.get(), pac.world);
      this.focusPackage = pac;
      setOwner(pac.getCaster());
      setIsFriendly(friendly);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ATTACK_DAMAGE, 1.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(FRIENDLY, false);
   }

   public boolean getIsFriendly() {
      return entityData.get(FRIENDLY);
   }

   public void setIsFriendly(boolean f) {
      entityData.set(FRIENDLY, f);
   }

   @Override
   public void writeSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeNbt(focusPackage.serialize());
   }

   @Override
   public void readSpawnData(RegistryFriendlyByteBuf buf) {
      focusPackage = new FocusPackage();
      CompoundTag tag = buf.readNbt();
      if (tag != null) focusPackage.deserialize(tag);
   }

   public void setOwner(@Nullable LivingEntity ownerIn) {
      owner = ownerIn;
      ownerUniqueId = ownerIn == null ? null : ownerIn.getUUID();
   }

   @Nullable
   public LivingEntity getOwner() {
      if (owner == null && ownerUniqueId != null && level() instanceof ServerLevel sl && sl.getEntity(ownerUniqueId) instanceof LivingEntity le) {
         owner = le;
      }
      return owner;
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

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.BAT_AMBIENT;
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

   @Nullable
   @Override
   public PlayerTeam getTeam() {
      LivingEntity o = getOwner();
      return o != null && o.getTeam() instanceof PlayerTeam pt ? pt : super.getTeam();
   }

   @Override
   public boolean isAlliedTo(Entity other) {
      LivingEntity o = getOwner();
      if (other == o) return true;
      return o == null ? super.isAlliedTo(other) : o.isAlliedTo(other) || other.isAlliedTo(o);
   }

   @Override
   public void tick() {
      super.tick();
      if (!level().isClientSide && (tickCount > 600 || getOwner() == null)) discard();
      setDeltaMovement(getDeltaMovement().multiply(1.0, 0.6, 1.0));
      if (isAlive() && level().isClientSide) {
         if (effects == null) {
            effects = focusPackage.getFocusEffects();
            int r = 0, g = 0, b = 0;
            for (FocusEffect ef : effects) {
               int c = FocusEngine.getElementColor(ef.getKey());
               r += c >> 16 & 255;
               g += c >> 8 & 255;
               b += c & 255;
            }
            if (effects.length > 0) {
               r /= effects.length;
               g /= effects.length;
               b /= effects.length;
            }
            color = r << 16 | g << 8 | b;
         }
         if (effects.length > 0) {
            FocusEffect eff = effects[random.nextInt(effects.length)];
            eff.renderParticleFX(level(), getX() + random.nextGaussian() * 0.125, getY() + getBbHeight() / 2.0F + random.nextGaussian() * 0.125,
               getZ() + random.nextGaussian() * 0.125, 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (attackTime > 0) attackTime--;
      LivingEntity target = getTarget();
      Vec3 m = getDeltaMovement();
      double var1, var3, var5;
      if (target == null) {
         if (currentFlightTarget != null && (!level().isEmptyBlock(currentFlightTarget) || currentFlightTarget.getY() < level().getMinBuildHeight() + 1)) {
            currentFlightTarget = null;
         }
         if (currentFlightTarget == null || random.nextInt(30) == 0 || currentFlightTarget.closerToCenterThan(position(), 2.0)) {
            currentFlightTarget = BlockPos.containing(getX() + random.nextInt(7) - random.nextInt(7), getY() + random.nextInt(6) - 2.0,
               getZ() + random.nextInt(7) - random.nextInt(7));
         }
         var1 = currentFlightTarget.getX() + 0.5 - getX();
         var3 = currentFlightTarget.getY() + 0.1 - getY();
         var5 = currentFlightTarget.getZ() + 0.5 - getZ();
      } else {
         var1 = target.getX() - getX();
         var3 = target.getY() + target.getEyeHeight() * 0.66F - getY();
         var5 = target.getZ() - getZ();
      }
      setDeltaMovement(m.x + (Math.signum(var1) * 0.5 - m.x) * 0.1F, m.y + (Math.signum(var3) * 0.7F - m.y) * 0.1F,
         m.z + (Math.signum(var5) * 0.5 - m.z) * 0.1F);
      Vec3 nm = getDeltaMovement();
      float var7 = (float) (Mth.atan2(nm.z, nm.x) * 180.0 / Math.PI) - 90.0F;
      float var8 = Mth.wrapDegrees(var7 - getYRot());
      zza = 0.5F;
      setYRot(getYRot() + var8);
      if (target == null) {
         setTarget(findTargetToAttack());
      } else if (target.isAlive()) {
         float f = target.distanceTo(this);
         if (isAlive() && hasLineOfSight(target)) attackEntity(target, f);
      } else {
         setTarget(null);
      }
      if (!getIsFriendly() && getTarget() instanceof Player p && p.getAbilities().invulnerable) setTarget(null);
   }

   protected void attackEntity(Entity target, float dist) {
      if (attackTime <= 0 && dist < Math.max(2.5F, target.getBbWidth() * 1.1F) && target.getBoundingBox().maxY > getBoundingBox().minY
         && target.getBoundingBox().minY < getBoundingBox().maxY) {
         attackTime = 40;
         if (!level().isClientSide) {
            Vec3 hv = target.position().add(0.0, target.getBbHeight() / 2.0F, 0.0);
            Trajectory tra = new Trajectory(position(), hv.subtract(position()));
            LivingEntity o = getOwner();
            if (o != null) {
               FocusEngine.runFocusPackage(focusPackage.copy(o), new Trajectory[]{tra}, new HitResult[]{new EntityHitResult(target, hv)});
            }
            setHealth(getHealth() - 1.0F);
         }
         playSound(SoundEvents.BAT_HURT, 0.5F, 0.9F + random.nextFloat() * 0.2F);
      }
   }

   @Override
   protected void doPush(Entity entityIn) {
      if (!getIsFriendly()) super.doPush(entityIn);
   }

   @Nullable
   protected LivingEntity findTargetToAttack() {
      List<LivingEntity> list = FocusUtils.getEntitiesInRange(level(), getX(), getY(), getZ(), this, LivingEntity.class, 12.0);
      double d = Double.MAX_VALUE;
      LivingEntity ret = null;
      for (LivingEntity e : list) {
         if (!e.isAlive()) continue;
         boolean ok = getIsFriendly() ? FocusUtils.isFriendly(getOwner(), e) : !FocusUtils.isFriendly(getOwner(), e) && !isAlliedTo(e);
         if (ok) {
            double ed = distanceToSqr(e);
            if (ed < d) {
               d = ed;
               ret = e;
            }
         }
      }
      return ret;
   }

   @Override
   protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
   }

   @Override
   public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
      return false;
   }

   @Override
   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   @Override
   protected boolean shouldDropLoot() {
      return false;
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      ownerUniqueId = nbt.hasUUID("OwnerUUID") ? nbt.getUUID("OwnerUUID") : null;
      setIsFriendly(nbt.getBoolean("friendly"));
      focusPackage = new FocusPackage();
      focusPackage.deserialize(nbt.getCompound("pack"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      if (ownerUniqueId != null) nbt.putUUID("OwnerUUID", ownerUniqueId);
      nbt.put("pack", focusPackage.serialize());
      nbt.putBoolean("friendly", getIsFriendly());
   }

   @Override
   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return false;
   }
}
