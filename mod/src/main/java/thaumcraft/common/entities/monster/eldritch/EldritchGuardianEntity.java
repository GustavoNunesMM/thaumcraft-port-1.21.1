package thaumcraft.common.entities.monster.eldritch;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.entities.monster.cult.CultGoals;
import thaumcraft.common.entities.projectile.EldritchOrbEntity;
import thaumcraft.common.network.EldritchPayload;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.eldritch.OuterLands;

/**
 * EntityEldritchGuardian: a hooded phantom of the Outer Lands. Melee up close, eldritch orbs (85%) or a withering
 * screech that adds temporary warp (15%) from afar; magic hurts it half as much. Outside the Outer Lands it is a
 * faded apparition that thickens the fog around nearby players; inside it gets a regenerating void shield of half its
 * health. Undead (entity type tag).
 */
public class EldritchGuardianEntity extends Monster implements RangedAttackMob, IEldritchMob {
   public float armLiftL = 0.0F;
   public float armLiftR = 0.0F;
   boolean lastBlast = false;

   public EldritchGuardianEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
      this.xpReward = 20;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.FOLLOW_RANGE, 40.0)
         .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 4.0).add(Attributes.MAX_ABSORPTION, 25.0);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(2, new CultGoals.LongRangeAttack(this, 8.0, 1.0, 20, 40, 24.0F));
      goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
      goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
      goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
      goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this));
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      targetSelector.addGoal(3, cultistTarget(this));
   }

   /** EntityAINearestAttackableTarget(EntityCultist). */
   public static NearestAttackableTargetGoal<thaumcraft.common.entities.monster.cult.CultistEntity> cultistTarget(net.minecraft.world.entity.Mob mob) {
      return new NearestAttackableTargetGoal<>(mob, thaumcraft.common.entities.monster.cult.CultistEntity.class, true);
   }

   @Override
   public boolean canPickUpLoot() {
      return false;
   }

   @Override
   public boolean hurt(DamageSource source, float damage) {
      if (source.is(Tags.DamageTypes.IS_MAGIC)) damage /= 2.0F;
      return super.hurt(source, damage);
   }

   @Override
   public void tick() {
      super.tick();
      if (level().isClientSide) {
         if (armLiftL > 0.0F) armLiftL -= 0.05F;
         if (armLiftR > 0.0F) armLiftR -= 0.05F;
         float x = (float) (getX() + (random.nextFloat() - random.nextFloat()) * 0.2F);
         float z = (float) (getZ() + (random.nextFloat() - random.nextFloat()) * 0.2F);
         thaumcraft.client.fx.EldritchFX.wispEG(x, (float) (getY() + 0.22 * getBbHeight()), z, this);
      } else if (!OuterLands.isOuterLands(level()) && (tickCount == 1 || tickCount % 100 == 0) && level().getDifficulty() != Difficulty.EASY) {
         double d6 = level().getDifficulty() == Difficulty.HARD ? 576.0 : 256.0;
         for (Player p : level().players()) {
            if (p.isAlive() && p.distanceToSqr(getX(), getY(), getZ()) < d6 && p instanceof ServerPlayer sp) {
               EldritchPayload.send(sp, EldritchPayload.GUARDIAN_FOG);
            }
         }
      }
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      boolean flag = super.doHurtTarget(target);
      if (flag) {
         int i = level().getDifficulty().getId();
         if (getMainHandItem().isEmpty() && isOnFire() && random.nextFloat() < i * 0.3F) target.igniteForSeconds(2 * i);
      }
      return flag;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.EGIDLE.get();
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.EGDEATH.get();
   }

   @Override
   public int getAmbientSoundInterval() {
      return 500;
   }

   @Override
   protected float getSoundVolume() {
      return 1.5F;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      if (hasRestriction() && getRestrictRadius() > 0.0F) {
         nbt.putInt("HomeD", (int) getRestrictRadius());
         nbt.putInt("HomeX", getRestrictCenter().getX());
         nbt.putInt("HomeY", getRestrictCenter().getY());
         nbt.putInt("HomeZ", getRestrictCenter().getZ());
      }
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      if (nbt.contains("HomeD")) restrictTo(new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ")), nbt.getInt("HomeD"));
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
      SpawnGroupData dd = super.finalizeSpawn(level, diff, reason, data);
      if (OuterLands.isOuterLands(level.getLevel())) {
         int bh = (int) getAttributeBaseValue(Attributes.MAX_HEALTH) / 2;
         setAbsorptionAmount(getAbsorptionAmount() + bh);
      }
      return dd;
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      if (OuterLands.isOuterLands(level()) && invulnerableTime <= 0 && tickCount % 25 == 0) {
         int bh = (int) getAttributeBaseValue(Attributes.MAX_HEALTH) / 2;
         if (getAbsorptionAmount() < bh) setAbsorptionAmount(getAbsorptionAmount() + 1.0F);
      }
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 15) {
         armLiftL = 0.5F;
      } else if (id == 16) {
         armLiftR = 0.5F;
      } else if (id == 17) {
         armLiftL = 0.9F;
         armLiftR = 0.9F;
      } else {
         super.handleEntityEvent(id);
      }
   }

   @Override
   public boolean removeWhenFarAway(double distance) {
      return !hasRestriction();
   }

   /** getCanSpawnHere: never within 32 blocks (16 vertically) of another guardian; any light level will do. */
   public static boolean checkSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
      RandomSource random) {
      if (!level.getEntitiesOfClass(EldritchGuardianEntity.class, new AABB(pos).inflate(32.0, 16.0, 32.0)).isEmpty()) return false;
      return Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random);
   }

   @Override
   public void performRangedAttack(LivingEntity target, float f) {
      if (random.nextFloat() > 0.15F) {
         EldritchOrbEntity blast = new EldritchOrbEntity(level(), this);
         lastBlast = !lastBlast;
         level().broadcastEntityEvent(this, (byte) (lastBlast ? 16 : 15));
         int rr = lastBlast ? 90 : 180;
         double xx = Mth.cos((getYRot() + rr) % 360.0F / 180.0F * (float) Math.PI) * 0.5F;
         double zz = Mth.sin((getYRot() + rr) % 360.0F / 180.0F * (float) Math.PI) * 0.5F;
         blast.setPos(blast.getX() - xx, blast.getY(), blast.getZ() - zz);
         Vec3 v = target.position().add(target.getDeltaMovement().scale(10.0)).subtract(position()).normalize();
         blast.shoot(v.x, v.y, v.z, 1.1F, 2.0F);
         playSound(ModSounds.EGATTACK.get(), 2.0F, 1.0F + random.nextFloat() * 0.1F);
         level().addFreshEntity(blast);
      } else if (hasLineOfSight(target)) {
         screech(this, target, 1);
         playSound(ModSounds.EGSCREECH.get(), 3.0F, 1.0F + random.nextFloat() * 0.1F);
      }
   }

   /** The screech (PacketFXSonic): withers the target and gives a player 1-3 (guardian) or 3-5 (warden) temporary warp. */
   static void screech(LivingEntity source, LivingEntity target, int warpBase) {
      if (source.level() instanceof ServerLevel sl) {
         ModNetwork.sendNear(sl, source.getX(), source.getY(), source.getZ(), 32.0, new EldritchPayload(EldritchPayload.SONIC, source.getId()));
      }
      target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0));
      if (target instanceof Player p) ResearchManager.addWarp(p, warpBase + source.getRandom().nextInt(3), PlayerWarp.Type.TEMPORARY);
   }

   @Override
   public boolean isAlliedTo(Entity e) {
      return EldritchHelper.isEldritch(e) || super.isAlliedTo(e);
   }
}
