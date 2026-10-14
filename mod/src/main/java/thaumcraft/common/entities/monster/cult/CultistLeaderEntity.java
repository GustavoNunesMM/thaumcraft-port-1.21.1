package thaumcraft.common.entities.monster.cult;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.common.entities.projectile.GolemOrbEntity;
import thaumcraft.common.lib.MobFXBridge;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.registers.ModSounds;

/**
 * EntityCultistLeader: the Crimson Praetor. A boss in praetor armor wielding the crimson blade (a void sword on easy),
 * throwing energy orbs from afar and keeping nearby cultists regenerating. Drops a rare loot bag.
 */
public class CultistLeaderEntity extends ThaumcraftBossEntity implements RangedAttackMob {
   private static final EntityDataAccessor<Byte> NAME = SynchedEntityData.defineId(CultistLeaderEntity.class, EntityDataSerializers.BYTE);
   static final String[] TITLES = {"Alberic", "Anselm", "Bastian", "Beturian", "Chabier", "Chorache", "Chuse", "Dodorol", "Ebardo", "Ferrando",
      "Fertus", "Guillen", "Larpe", "Obano", "Zelipe"};

   public CultistLeaderEntity(EntityType<? extends CultistLeaderEntity> type, Level level) {
      super(type, level);
      xpReward = 40;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return ThaumcraftBossEntity.createAttributes().add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.MAX_HEALTH, 150.0)
         .add(Attributes.ATTACK_DAMAGE, 5.0);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(2, new CultGoals.LongRangeAttack(this, 16.0, 1.0, 30, 40, 24.0F));
      goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
      goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
      goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
      goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new CultGoals.HurtByTarget(this, true));
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(NAME, (byte) 0);
   }

   /**
    * generateName: "Praetor <title>". STUB: the 1.12 name also carried the boss's champion modifier ("the Mighty"...);
    * champion modifiers are not ported yet.
    */
   @Override
   public void generateName() {
      setCustomName(Component.translatable("entity.thaumcraft.cultist_leader.custom", getTitle()));
   }

   public String getTitle() {
      int t = entityData.get(NAME);
      return TITLES[Math.floorMod(t, TITLES.length)];
   }

   private void setTitle(int title) {
      entityData.set(NAME, (byte) title);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putByte("title", entityData.get(NAME));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      setTitle(nbt.getByte("title"));
      super.readAdditionalSaveData(nbt);
   }

   protected void setEquipment(DifficultyInstance diff) {
      setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModGear.CRIMSON_PRAETOR_HELM.get()));
      setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModGear.CRIMSON_PRAETOR_CHEST.get()));
      setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModGear.CRIMSON_PRAETOR_LEGS.get()));
      setItemSlot(EquipmentSlot.FEET, new ItemStack(ModGear.CRIMSON_BOOTS.get()));
      setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(level().getDifficulty() == Difficulty.EASY ? ModGear.VOID_SWORD.get() : ModGear.CRIMSON_BLADE.get()));
   }

   protected void enchantEquipment(DifficultyInstance diff) {
      float f = diff.getSpecialMultiplier();
      if (!getMainHandItem().isEmpty() && random.nextFloat() < 0.5F * f) {
         CultistEntity.enchantHeld(this, random, (int) (7.0F + f * random.nextInt(22)));
      }
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
      setEquipment(diff);
      enchantEquipment(diff);
      setTitle(random.nextInt(TITLES.length));
      return super.finalizeSpawn(level, diff, reason, data);
   }

   @Override
   public boolean isAlliedTo(Entity e) {
      return CultistEntity.isCultist(e) || super.isAlliedTo(e);
   }

   @Override
   public boolean canAttackType(EntityType<?> type) {
      return !CultistEntity.isCultistType(type) && super.canAttackType(type);
   }

   /** dropFewItems: only a rare loot bag (no primordial pearl, unlike the other bosses); the armor drops at the vanilla 8.5%. */
   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      spawnAtLocation(new ItemStack(ModMobs.LOOT_BAG_RARE.get()), 1.5F);
   }

   @Override
   protected void customServerAiStep() {
      super.customServerAiStep();
      for (CultistEntity e : level().getEntitiesOfClass(CultistEntity.class, getBoundingBox().inflate(8.0), c -> c.distanceToSqr(this) <= 64.0)) {
         if (!e.hasEffect(MobEffects.REGENERATION)) e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
      }
   }

   @Override
   public void performRangedAttack(LivingEntity target, float f) {
      if (!hasLineOfSight(target)) return;
      swing(InteractionHand.MAIN_HAND);
      getLookControl().setLookAt(target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2.0F, target.getZ(), 30.0F, 30.0F);
      GolemOrbEntity blast = new GolemOrbEntity(level(), this, target, true);
      blast.setPos(blast.getX() + blast.getDeltaMovement().x / 2.0, blast.getY(), blast.getZ() + blast.getDeltaMovement().z / 2.0);
      double d0 = target.getX() - getX();
      double d1 = target.getBoundingBox().minY + target.getBbHeight() / 2.0F - (getY() + getBbHeight() / 2.0F);
      double d2 = target.getZ() - getZ();
      blast.shoot(d0, d1 + 2.0, d2, 0.66F, 3.0F);
      playSound(ModSounds.EGATTACK.get(), 1.0F, 1.0F + random.nextFloat() * 0.1F);
      level().addFreshEntity(blast);
   }

   @Override
   public void spawnAnim() {
      if (level().isClientSide) {
         for (int i = 0; i < 20; i++) {
            double d0 = random.nextGaussian() * 0.05;
            double d1 = random.nextGaussian() * 0.05;
            double d2 = random.nextGaussian() * 0.05;
            double d3 = 2.0;
            MobFXBridge.INSTANCE.cultistSpawn(getX() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth() + d0 * d3,
               getY() + random.nextFloat() * getBbHeight() + d1 * d3, getZ() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth() + d2 * d3, d0,
               d1, d2);
         }
      } else {
         level().broadcastEntityEvent(this, (byte) 20);
      }
   }
}
