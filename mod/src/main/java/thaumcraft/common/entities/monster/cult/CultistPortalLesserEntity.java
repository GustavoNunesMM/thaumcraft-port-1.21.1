package thaumcraft.common.entities.monster.cult;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.registers.ModSounds;

/** EntityCultistPortalLesser: a crimson portal that wakes up when a player comes near and summons cultists, wearing itself out. */
public class CultistPortalLesserEntity extends Monster {
   private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(CultistPortalLesserEntity.class, EntityDataSerializers.BOOLEAN);
   int stagecounter = 100;
   public int activeCounter;
   public int pulse;

   public CultistPortalLesserEntity(EntityType<? extends CultistPortalLesserEntity> type, Level level) {
      super(type, level);
      xpReward = 10;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.ATTACK_DAMAGE, 0.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 4.0);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(ACTIVE, false);
   }

   public boolean isActive() {
      return entityData.get(ACTIVE);
   }

   public void setActive(boolean b) {
      entityData.set(ACTIVE, b);
   }

   @Override
   public boolean removeWhenFarAway(double dist) {
      return false;
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putBoolean("active", isActive());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      setActive(nbt.getBoolean("active"));
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   public void move(MoverType type, Vec3 pos) {
   }

   @Override
   public void aiStep() {
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double d) {
      return d < 4096.0;
   }

   @Override
   public void tick() {
      super.tick();
      if (isActive()) activeCounter++;
      if (!level().isClientSide) {
         if (!isActive()) {
            if (tickCount % 10 == 0 && nearestPlayer(32.0) != null) {
               setActive(true);
               playSound(ModSounds.CRAFTSTART.get(), 1.0F, 1.0F);
            }
         } else if (stagecounter-- <= 0) {
            Player p = nearestPlayer(32.0);
            if (p != null && hasLineOfSight(p)) {
               Difficulty d = level().getDifficulty();
               int count = d == Difficulty.HARD ? 6 : d == Difficulty.NORMAL ? 4 : 2;
               count -= level().getEntitiesOfClass(CultistEntity.class, getBoundingBox().inflate(32.0)).size();
               if (count > 0) {
                  level().broadcastEntityEvent(this, (byte) 16);
                  spawnMinions();
               }
            }
            stagecounter = 50 + random.nextInt(50);
         }
      }
      if (pulse > 0) pulse--;
   }

   private Player nearestPlayer(double range) {
      Player p = level().getNearestPlayer(this, range);
      return p != null && !p.isSpectator() ? p : null;
   }

   void spawnMinions() {
      CultistEntity cultist = random.nextFloat() > 0.33F ? ModMobs.CULTIST_KNIGHT.get().create(level()) : ModMobs.CULTIST_CLERIC.get().create(level());
      if (cultist == null) return;
      cultist.setPos(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25, getZ() + random.nextFloat() - random.nextFloat());
      if (level() instanceof ServerLevel sl) {
         cultist.finalizeSpawn(sl, sl.getCurrentDifficultyAt(cultist.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
      }
      level().addFreshEntity(cultist);
      cultist.spawnAnim();
      cultist.playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);
      hurt(damageSources().fellOutOfWorld(), 5 + random.nextInt(5));
   }

   @Override
   public void playerTouch(Player p) {
      if (distanceToSqr(p) < 3.0 && p.hurt(damageSources().indirectMagic(this, this), 4.0F)) {
         playSound(ModSounds.ZAP.get(), 1.0F, (random.nextFloat() - random.nextFloat()) * 0.1F + 1.0F);
      }
   }

   @Override
   protected float getSoundVolume() {
      return 0.75F;
   }

   @Override
   public int getAmbientSoundInterval() {
      return 540;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.MONOLITH.get();
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource src) {
      return ModSounds.ZAP.get();
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.SHOCK.get();
   }

   @Override
   protected boolean shouldDropLoot() {
      return false;
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 16) pulse = 10;
      else super.handleEntityEvent(id);
   }

   @Override
   public boolean addEffect(MobEffectInstance effect, Entity source) {
      return false;
   }

   @Override
   public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
      return false;
   }

   @Override
   public void die(DamageSource cause) {
      if (!level().isClientSide) level().explode(this, getX(), getY(), getZ(), 1.5F, Level.ExplosionInteraction.NONE);
      super.die(cause);
   }
}
