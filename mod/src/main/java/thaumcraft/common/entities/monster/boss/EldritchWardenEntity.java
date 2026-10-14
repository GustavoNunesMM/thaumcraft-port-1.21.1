package thaumcraft.common.entities.monster.boss;

import thaumcraft.common.entities.monster.cult.ThaumcraftBossEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchHelper;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.entities.monster.cult.CultGoals;
import thaumcraft.common.entities.monster.eldritch.EldritchGuardianEntity;
import thaumcraft.common.entities.projectile.EldritchOrbEntity;
import thaumcraft.common.network.EldritchPayload;
import thaumcraft.common.network.InfusionFXPayload;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.registers.ModCasters;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;

/**
 * EntityEldritchWarden: the hooded lord of an Outer Lands maze, named after one of fifteen Great Old Ones. Rises out
 * of the floor over 150 ticks, carries a regenerating void shield of two thirds of its health (a second, blue boss
 * bar), leaves a trail of withering sap, and when the shield breaks it teleports home and unleashes a field frenzy:
 * rings of sap and arcs spreading out from it while it is invulnerable. Orbs (80%) or a knock back screech (20%).
 */
public class EldritchWardenEntity extends ThaumcraftBossEntity implements RangedAttackMob, IEldritchMob {
   private static final EntityDataAccessor<Byte> NAME = SynchedEntityData.defineId(EldritchWardenEntity.class, EntityDataSerializers.BYTE);
   public static final String[] TITLES = {"Aphoom-Zhah", "Basatan", "Chaugnar Faugn", "Mnomquah", "Nyogtha", "Oorn", "Shaikorth", "Rhan-Tegoth",
      "Rhogog", "Shudde M'ell", "Vulthoom", "Yag-Kosha", "Yibb-Tstll", "Zathog", "Zushakon"};

   protected final ServerBossEvent bossInfo2 = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
   boolean fieldFrenzy = false;
   int fieldFrenzyCounter = 0;
   boolean lastBlast = false;
   public float armLiftL = 0.0F;
   public float armLiftR = 0.0F;

   public EldritchWardenEntity(EntityType<? extends ThaumcraftBossEntity> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return ThaumcraftBossEntity.createAttributes().add(Attributes.MAX_HEALTH, 400.0).add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.ATTACK_DAMAGE, 10.0)
         .add(Attributes.ARMOR, 4.0).add(Attributes.MAX_ABSORPTION, 264.0);
   }

   @Override
   public void startSeenByPlayer(ServerPlayer player) {
      super.startSeenByPlayer(player);
      bossInfo2.addPlayer(player);
   }

   @Override
   public void stopSeenByPlayer(ServerPlayer player) {
      super.stopSeenByPlayer(player);
      bossInfo2.removePlayer(player);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(2, new CultGoals.LongRangeAttack(this, 3.0, 1.0, 20, 40, 24.0F));
      goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
      goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
      goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
      goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this));
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      targetSelector.addGoal(3, EldritchGuardianEntity.cultistTarget(this));
   }

   /** generateName: its title, plus its champion modifier if it is one ("Title the Mighty"). */
   @Override
   public void generateName() {
      var mod = thaumcraft.common.entities.monster.mods.ChampionModifier.get(this);
      setCustomName(mod == null ? Component.literal(getTitle())
         : Component.translatable("entity.thaumcraft.eldritch_warden.champion", getTitle(), mod.localizedName()));
   }

   public String getTitle() {
      return TITLES[Mth.clamp(entityData.get(NAME), 0, TITLES.length - 1)];
   }

   private void setTitle(int title) {
      entityData.set(NAME, (byte) title);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(NAME, (byte) 0);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putByte("title", entityData.get(NAME));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      setTitle(nbt.getByte("title"));
   }

   @Override
   protected void customServerAiStep() {
      if (fieldFrenzyCounter == 0) super.customServerAiStep();
      int bh = (int) (getAttributeBaseValue(Attributes.MAX_HEALTH) * 0.66);
      if (invulnerableTime <= 0 && tickCount % 25 == 0 && getAbsorptionAmount() < bh) setAbsorptionAmount(getAbsorptionAmount() + 1.0F);
      bossInfo2.setProgress(Mth.clamp(getAbsorptionAmount() / bh, 0.0F, 1.0F));
   }

   @Override
   public void tick() {
      if (getSpawnTimer() == 150) level().broadcastEntityEvent(this, (byte) 18);
      super.tick();
      if (level().isClientSide) {
         if (armLiftL > 0.0F) armLiftL -= 0.05F;
         if (armLiftR > 0.0F) armLiftR -= 0.05F;
         float x = (float) (getX() + (random.nextFloat() - random.nextFloat()) * 0.2F);
         float z = (float) (getZ() + (random.nextFloat() - random.nextFloat()) * 0.2F);
         thaumcraft.client.fx.EldritchFX.wispEG(x, (float) (getY() + 0.25 * getBbHeight()), z, this);
         if (spawnTimer > 0) {
            float he = Math.max(1.0F, getBbHeight() * ((150 - spawnTimer) / 150.0F));
            for (int a = 0; a < 33; a++) {
               thaumcraft.client.fx.EldritchFX.smokeSpiral(getX(), getBoundingBox().minY + he / 2.0F, getZ(), he, random.nextInt(360),
                  Mth.floor(getBoundingBox().minY) - 1, 2232623);
            }
         }
      }
   }

   @Override
   public void aiStep() {
      super.aiStep();
      if (!level().isClientSide) {
         for (int l = 0; l < 4; l++) {
            int i = Mth.floor(getX() + (l % 2 * 2 - 1) * 0.25F);
            int j = Mth.floor(getY());
            int k = Mth.floor(getZ() + (l / 2 % 2 * 2 - 1) * 0.25F);
            BlockPos bp = new BlockPos(i, j, k);
            if (level().isEmptyBlock(bp)) level().setBlockAndUpdate(bp, ModCasters.EFFECT_SAP.get().defaultBlockState());
         }
         if (fieldFrenzyCounter > 0) {
            if (fieldFrenzyCounter == 150) teleportHome();
            performFieldFrenzy();
         }
      }
   }

   private void performFieldFrenzy() {
      if (fieldFrenzyCounter < 121 && fieldFrenzyCounter % 10 == 0) {
         level().broadcastEntityEvent(this, (byte) 17);
         double radius = (150 - fieldFrenzyCounter) / 8.0;
         int d = 1 + fieldFrenzyCounter / 8;
         int i = Mth.floor(getX());
         int j = Mth.floor(getY());
         int k = Mth.floor(getZ());
         for (int q = 0; q < 180 / d; q++) {
            double radians = Math.toRadians(q * 2 * d);
            int deltaX = (int) (radius * Math.cos(radians));
            int deltaZ = (int) (radius * Math.sin(radians));
            BlockPos bp = new BlockPos(i + deltaX, j, k + deltaZ);
            if (level().isEmptyBlock(bp) && level().getBlockState(bp.below()).isRedstoneConductor(level(), bp.below())) {
               level().setBlockAndUpdate(bp, ModCasters.EFFECT_SAP.get().defaultBlockState());
               if (random.nextFloat() < 0.3F && level() instanceof ServerLevel sl) {
                  ModNetwork.sendNear(sl, getX(), getY(), getZ(), 32.0, InfusionFXPayload.arc(bp, this));
               }
            }
         }
         playSound(ModSounds.ZAP.get(), 1.0F, 0.9F + random.nextFloat() * 0.1F);
      }
      fieldFrenzyCounter--;
   }

   protected void teleportHome() {
      if (!hasRestriction()) return;
      BlockPos home = getRestrictCenter();
      double d3 = getX();
      double d4 = getY();
      double d5 = getZ();
      int i = home.getX();
      int j = home.getY();
      int k = home.getZ();
      boolean flag = false;
      if (level().isLoaded(home)) {
         boolean found = false;
         int tries = 20;
         while (!found && tries > 0) {
            BlockState below = level().getBlockState(new BlockPos(i, j - 1, k));
            BlockState at = level().getBlockState(new BlockPos(i, j, k));
            if (below.blocksMotion() && !at.blocksMotion()) {
               found = true;
            } else {
               i = home.getX() + random.nextInt(8) - random.nextInt(8);
               k = home.getZ() + random.nextInt(8) - random.nextInt(8);
               tries--;
            }
         }
         if (found) {
            setPos(i + 0.5, j + 0.1, k + 0.5);
            if (level().noCollision(this, getBoundingBox())) flag = true;
         }
      }
      if (!flag) {
         setPos(d3, d4, d5);
      } else if (level() instanceof ServerLevel sl) {
         for (int l = 0; l < 128; l++) {
            double d6 = l / 127.0;
            double d7 = d3 + (getX() - d3) * d6 + (random.nextDouble() - 0.5) * getBbWidth() * 2.0;
            double d8 = d4 + (getY() - d4) * d6 + random.nextDouble() * getBbHeight();
            double d9 = d5 + (getZ() - d5) * d6 + (random.nextDouble() - 0.5) * getBbWidth() * 2.0;
            sl.sendParticles(ParticleTypes.PORTAL, d7, d8, d9, 1, 0.0, 0.0, 0.0, 0.0);
         }
         playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
      }
   }

   @Override
   public boolean isInvulnerableTo(DamageSource ds) {
      return fieldFrenzyCounter > 0 || super.isInvulnerableTo(ds);
   }

   @Override
   public boolean hurt(DamageSource source, float damage) {
      if (!isInvulnerableTo(source) && !source.is(DamageTypes.DROWN) && !source.is(DamageTypes.WITHER)) {
         boolean aef = super.hurt(source, damage);
         if (!level().isClientSide && aef && !fieldFrenzy && getAbsorptionAmount() <= 0.0F) {
            fieldFrenzy = true;
            fieldFrenzyCounter = 150;
         }
         return aef;
      }
      return false;
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
      spawnTimer = 150;
      setTitle(random.nextInt(TITLES.length));
      setAbsorptionAmount((float) (getAbsorptionAmount() + getAttributeBaseValue(Attributes.MAX_HEALTH) * 0.66));
      return super.finalizeSpawn(level, diff, reason, data);
   }

   @Override
   public void performRangedAttack(LivingEntity target, float f) {
      if (random.nextFloat() > 0.2F) {
         EldritchOrbEntity blast = new EldritchOrbEntity(level(), this);
         lastBlast = !lastBlast;
         level().broadcastEntityEvent(this, (byte) (lastBlast ? 16 : 15));
         int rr = lastBlast ? 90 : 180;
         double xx = Mth.cos((getYRot() + rr) % 360.0F / 180.0F * (float) Math.PI) * 0.5F;
         double zz = Mth.sin((getYRot() + rr) % 360.0F / 180.0F * (float) Math.PI) * 0.5F;
         blast.setPos(blast.getX() - xx, blast.getY() - 0.13, blast.getZ() - zz);
         double d0 = target.getX() + target.getDeltaMovement().x - getX();
         double d1 = target.getY() - getY() - target.getBbHeight() / 2.0F;
         double d2 = target.getZ() + target.getDeltaMovement().z - getZ();
         blast.shoot(d0, d1, d2, 1.0F, 2.0F);
         playSound(ModSounds.EGATTACK.get(), 2.0F, 1.0F + random.nextFloat() * 0.1F);
         level().addFreshEntity(blast);
      } else if (hasLineOfSight(target)) {
         if (level() instanceof ServerLevel sl) {
            ModNetwork.sendNear(sl, getX(), getY(), getZ(), 32.0, new EldritchPayload(EldritchPayload.SONIC, getId()));
         }
         target.push(-Mth.sin(getYRot() * (float) Math.PI / 180.0F) * 1.5F, 0.1, Mth.cos(getYRot() * (float) Math.PI / 180.0F) * 1.5F);
         target.hurtMarked = true;
         target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0));
         target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 0));
         if (target instanceof Player p) ResearchManager.addWarp(p, 3 + random.nextInt(3), PlayerWarp.Type.TEMPORARY);
         playSound(ModSounds.EGSCREECH.get(), 4.0F, 1.0F + random.nextFloat() * 0.1F);
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
      } else if (id == 18) {
         spawnTimer = 150;
      } else {
         super.handleEntityEvent(id);
      }
   }

   @Override
   public boolean canAttackType(EntityType<?> type) {
      return !(type == thaumcraft.common.registers.ModEldritch.ELDRITCH_GUARDIAN.get()) && super.canAttackType(type);
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
   public boolean isAlliedTo(net.minecraft.world.entity.Entity e) {
      return EldritchHelper.isEldritch(e) || super.isAlliedTo(e);
   }
}
