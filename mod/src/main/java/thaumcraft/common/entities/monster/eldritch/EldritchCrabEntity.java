package thaumcraft.common.entities.monster.eldritch;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.registers.ModSounds;

/**
 * EntityEldritchCrab, the helmed crab. Leaps at its prey; a bare crab that lands on a target's head rides it and
 * keeps clawing (20% chance to fall off after each bite). Its crimson helm (always on hard, otherwise one in three)
 * gives 5 armour and breaks at half health. Poison doesn't affect it; may drop an ender pearl. Arthropod (tag).
 */
public class EldritchCrabEntity extends Monster implements IEldritchMob {
   private static final EntityDataAccessor<Boolean> HELM = SynchedEntityData.defineId(EldritchCrabEntity.class, EntityDataSerializers.BOOLEAN);
   private int attackTime = 0;

   public EldritchCrabEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      this.xpReward = 6;
      ((GroundPathNavigation) getNavigation()).setCanOpenDoors(true);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.MOVEMENT_SPEED, 0.3);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.63F));
      goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
      goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
      goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      targetSelector.addGoal(3, EldritchGuardianEntity.cultistTarget(this));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(HELM, false);
   }

   public boolean hasHelm() {
      return entityData.get(HELM);
   }

   public void setHelm(boolean helm) {
      entityData.set(HELM, helm);
      if (getAttribute(Attributes.MOVEMENT_SPEED) != null) getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(helm ? 0.275 : 0.3);
      if (getAttribute(Attributes.ARMOR) != null) getAttribute(Attributes.ARMOR).setBaseValue(helm ? 5.0 : 0.0);
   }

   @Override
   public boolean canPickUpLoot() {
      return false;
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
      if (level.getDifficulty() == Difficulty.HARD) {
         setHelm(true);
      } else {
         setHelm(random.nextFloat() < 0.33F);
      }
      if (data == null) {
         data = new Spider.SpiderEffectsGroupData();
         if (level.getDifficulty() == Difficulty.HARD && level.getRandom().nextFloat() < 0.1F * diff.getSpecialMultiplier()) {
            ((Spider.SpiderEffectsGroupData) data).setRandomEffect(level.getRandom());
         }
      }
      if (data instanceof Spider.SpiderEffectsGroupData g && g.effect != null) addEffect(new MobEffectInstance(g.effect, -1));
      return super.finalizeSpawn(level, diff, reason, data);
   }

   @Override
   public void tick() {
      super.tick();
      attackTime--;
      if (tickCount < 20) fallDistance = 0.0F;
      if (!level().isClientSide) {
         LivingEntity t = getTarget();
         if (getVehicle() == null && t != null && !t.isVehicle() && !onGround() && !hasHelm() && t.isAlive()
            && getY() - t.getY() >= t.getBbHeight() / 2.0F && distanceToSqr(t) < 4.0) {
            startRiding(t, true);
         }
         if (getVehicle() != null && isAlive() && attackTime <= 0) {
            attackTime = 10 + random.nextInt(10);
            doHurtTarget(getVehicle());
            if (random.nextFloat() < 0.2) stopRiding();
         }
      }
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      if (super.doHurtTarget(target)) {
         playSound(ModSounds.CRABCLAW.get(), 1.0F, 0.9F + level().random.nextFloat() * 0.2F);
         return true;
      }
      return false;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      super.dropCustomDeathLoot(level, source, recentlyHit);
      int looting = 0;
      if (source.getEntity() instanceof LivingEntity killer) {
         looting = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
            level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
               .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), killer);
      }
      if (recentlyHit && (random.nextInt(3) == 0 || random.nextInt(1 + looting) > 0)) spawnAtLocation(Items.ENDER_PEARL);
   }

   @Override
   public boolean hurt(DamageSource source, float damage) {
      boolean b = super.hurt(source, damage);
      if (!level().isClientSide && hasHelm() && getHealth() / getMaxHealth() <= 0.5F) {
         setHelm(false);
         playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + random.nextFloat() * 0.4F);
         if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModGear.CRIMSON_PLATE_CHEST.get())), getX(), getY() + 0.4, getZ(),
               5, 0.2, 0.2, 0.2, 0.1);
         }
      }
      return b;
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      setHelm(nbt.getBoolean("helm"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putBoolean("helm", hasHelm());
   }

   @Override
   public int getAmbientSoundInterval() {
      return 160;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.CRABTALK.get();
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.GENERIC_HURT;
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.CRABDEATH.get();
   }

   @Override
   protected void playStepSound(BlockPos pos, BlockState block) {
      playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
   }

   @Override
   public boolean canBeAffected(MobEffectInstance effect) {
      return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
   }

   @Override
   public boolean isAlliedTo(Entity e) {
      return e instanceof EldritchCrabEntity || super.isAlliedTo(e);
   }
}
