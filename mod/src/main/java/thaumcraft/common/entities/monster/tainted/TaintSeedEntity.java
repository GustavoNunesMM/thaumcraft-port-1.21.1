package thaumcraft.common.entities.monster.tainted;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.world.taint.FluxEffects;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * EntityTaintSeed: the immobile heart of a taint growth. While the aura around it is saturated with flux it spreads
 * taint fibres around itself (TaintHelper only lets taint live near seeds); without flux it starves. It lashes at
 * players, poisons everything around with flux taint and calls up tendrils under targets standing on taint.
 */
public class TaintSeedEntity extends TaintedMonster {
   public int boost;
   private boolean firstRun;
   public float attackAnim;

   public TaintSeedEntity(EntityType<? extends Monster> type, Level level) {
      super(type, level);
      xpReward = 8;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 75.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.MOVEMENT_SPEED, 0.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
   }

   protected int getArea() {
      return 1;
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
      targetSelector.addGoal(0, new HurtByTargetGoal(this));
      targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
   }

   @Override
   public void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      boost = tag.getInt("boost");
   }

   @Override
   public void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("boost", boost);
   }

   @Override
   public boolean doHurtTarget(Entity target) {
      level().broadcastEntityEvent(this, (byte) 16);
      playSound(ModSounds.TENTACLE.get(), getSoundVolume(), getVoicePitch());
      return super.doHurtTarget(target);
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 16) attackAnim = 0.5F;
      else super.handleEntityEvent(id);
   }

   /** getCanSpawnHere: not peaceful, nothing in the way, and no other seed within 80% of the taint spread area. */
   public boolean canSpawnHere() {
      return level().getDifficulty() != Difficulty.PEACEFUL && level().noCollision(this) && !level().containsAnyLiquid(getBoundingBox())
         && level().getEntitiesOfClass(TaintSeedEntity.class, new AABB(blockPosition()).inflate(TaintHelper.taintSpreadArea * 0.8)).isEmpty();
   }

   @Override
   public void die(DamageSource cause) {
      TaintHelper.removeTaintSeed(level(), blockPosition());
      super.die(cause);
   }

   @Override
   public void remove(RemovalReason reason) {
      if (!level().isClientSide) TaintHelper.removeTaintSeed(level(), blockPosition());
      super.remove(reason);
   }

   @Override
   public void tick() {
      super.tick();
      if (!level().isClientSide) {
         if (!firstRun || tickCount % 1200 == 0) {
            TaintHelper.removeTaintSeed(level(), blockPosition());
            TaintHelper.addTaintSeed(level(), blockPosition());
            firstRun = true;
         }
         if (isAlive()) {
            boolean tickFlag = tickCount % 20 == 0;
            if (boost > 0 || tickFlag) {
               float mod = boost > 0 ? 1.0F : AuraHandler.getFluxSaturation(level(), blockPosition());
               if (boost > 0) boost--;
               if (mod <= 0.0F) {
                  hurt(damageSources().starve(), 0.5F);
                  FluxEffects.pollute(level(), blockPosition(), 0.1F, false);
               } else {
                  int a = getArea();
                  TaintHelper.spreadFibres(level(), blockPosition().offset(Mth.nextInt(random, -a * 3, a * 3), Mth.nextInt(random, -a, a),
                     Mth.nextInt(random, -a * 3, a * 3)), true);
               }
            }
            if (tickFlag) {
               LivingEntity target = getTarget();
               if (target != null && distanceToSqr(target) < getArea() * 256 && getSensing().hasLineOfSight(target)) spawnTentacles(target);
               for (LivingEntity elb : level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(getArea() * 4), e -> e != this)) {
                  elb.addEffect(new MobEffectInstance(ModFlux.FLUX_TAINT, 100, getArea() - 1, false, true));
               }
            }
         }
      } else {
         if (attackAnim > 0.0F) attackAnim *= 0.75F;
         if (attackAnim < 0.001) attackAnim = 0.0F;
         float xx = Mth.sin(tickCount * 0.05F - 0.5F) / 5.0F;
         float zz = Mth.sin(tickCount * 0.06F - 0.5F) / 5.0F + hurtTime / 200.0F + attackAnim;
         if (random.nextFloat() < 0.033) {
            thaumcraft.client.fx.TaintFX.lightningFlash(getX() + xx, getY() + getBbHeight() + 0.25F, getZ() + zz, 0.7F, 0.1F, 0.9F, 0.5F,
               1.5F + random.nextFloat());
         } else {
            thaumcraft.client.fx.TaintFX.taintParticles(getX() + xx, getY() + getBbHeight() + 0.25F, getZ() + zz, (float) random.nextGaussian() * 0.05F,
               0.1F + 0.01F * random.nextFloat(), (float) random.nextGaussian() * 0.05F, 2.0F);
         }
      }
   }

   @Override
   public int getAmbientSoundInterval() {
      return 200;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return SoundEvents.CHORUS_FLOWER_DEATH;
   }

   @Override
   public float getVoicePitch() {
      return 1.3F - getBbHeight() / 10.0F;
   }

   @Override
   protected float getSoundVolume() {
      return getBbHeight() / 8.0F;
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return ModSounds.TENTACLE.get();
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.TENTACLE.get();
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      dropFluxCrystal();
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   public void move(MoverType type, Vec3 movement) {
      super.move(type, new Vec3(0.0, Math.min(movement.y, 0.0), 0.0));
   }

   @Override
   protected int decreaseAirSupply(int air) {
      return air;
   }

   @Override
   public boolean removeWhenFarAway(double distance) {
      return false;
   }
}
