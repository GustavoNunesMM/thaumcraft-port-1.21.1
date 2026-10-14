package thaumcraft.common.entities.monster.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.common.entities.projectile.GolemOrbEntity;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.registers.ModSounds;

/** EntityCultistCleric: robed caster of energy orbs and fireballs; ritualists stand focused on an altar. */
public class CultistClericEntity extends CultistEntity implements RangedAttackMob, IEntityWithComplexSpawn {
   private static final EntityDataAccessor<Boolean> RITUALIST = SynchedEntityData.defineId(CultistClericEntity.class, EntityDataSerializers.BOOLEAN);
   public int rage;

   public CultistClericEntity(EntityType<? extends CultistClericEntity> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return CultistEntity.createAttributes().add(Attributes.MAX_HEALTH, 24.0);
   }

   @Override
   protected void registerGoals() {
      goalSelector.addGoal(0, new FloatGoal(this));
      goalSelector.addGoal(1, new CultGoals.AltarFocus(this));
      goalSelector.addGoal(2, new CultGoals.LongRangeAttack(this, 2.0, 1.0, 20, 40, 24.0F));
      goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
      goalSelector.addGoal(5, new OpenDoorGoal(this, true));
      goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
      goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
      goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
      goalSelector.addGoal(8, new RandomLookAroundGoal(this));
      targetSelector.addGoal(1, new CultGoals.HurtByTarget(this, true));
      targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
      targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, thaumcraft.common.entities.monster.eldritch.EldritchGuardianEntity.class, true));
      targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, AbstractIllager.class, true));
   }

   @Override
   protected void setLoot(RandomSource rand, DifficultyInstance diff) {
      setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModGear.CRIMSON_ROBE_HELM.get()));
      setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModGear.CRIMSON_ROBE_CHEST.get()));
      setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModGear.CRIMSON_ROBE_LEGS.get()));
      if (rand.nextFloat() < (level().getDifficulty() == Difficulty.HARD ? 0.3F : 0.1F)) {
         setItemSlot(EquipmentSlot.FEET, new ItemStack(ModGear.CRIMSON_BOOTS.get()));
      }
   }

   @Override
   public void performRangedAttack(LivingEntity target, float f) {
      double d0 = target.getX() - getX();
      double d1 = target.getBoundingBox().minY + target.getBbHeight() / 2.0F - (getY() + getBbHeight() / 2.0F);
      double d2 = target.getZ() - getZ();
      swing(InteractionHand.MAIN_HAND);
      if (random.nextFloat() > 0.66F) {
         GolemOrbEntity blast = new GolemOrbEntity(level(), this, target, true);
         Vec3 v = target.position().add(target.getDeltaMovement().scale(10.0)).subtract(position()).normalize();
         blast.setPos(blast.getX() + v.x, blast.getY() + v.y, blast.getZ() + v.z);
         blast.shoot(v.x, v.y, v.z, 0.66F, 3.0F);
         playSound(ModSounds.EGATTACK.get(), 1.0F, 1.0F + random.nextFloat() * 0.1F);
         level().addFreshEntity(blast);
      } else {
         float f1 = Mth.sqrt(f) * 0.5F;
         level().levelEvent(null, 1018, blockPosition(), 0);
         for (int i = 0; i < 3; i++) {
            SmallFireball fb = new SmallFireball(level(), this, new Vec3(d0 + random.nextGaussian() * f1, d1, d2 + random.nextGaussian() * f1).normalize());
            fb.setPos(fb.getX(), getY() + getBbHeight() / 2.0F + 0.5, fb.getZ());
            level().addFreshEntity(fb);
         }
      }
   }

   @Override
   public boolean removeWhenFarAway(double dist) {
      return !getIsRitualist();
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      super.defineSynchedData(builder);
      builder.define(RITUALIST, false);
   }

   public boolean getIsRitualist() {
      return entityData.get(RITUALIST);
   }

   public void setIsRitualist(boolean b) {
      entityData.set(RITUALIST, b);
   }

   @Override
   public boolean hurt(DamageSource source, float amount) {
      if (isInvulnerableTo(source)) return false;
      setIsRitualist(false);
      return super.hurt(source, amount);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      setIsRitualist(nbt.getBoolean("ritualist"));
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putBoolean("ritualist", getIsRitualist());
   }

   @Override
   public void writeSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeBlockPos(getRestrictCenter());
   }

   @Override
   public void readSpawnData(RegistryFriendlyByteBuf buf) {
      restrictTo(buf.readBlockPos(), 8);
   }

   @Override
   public void tick() {
      super.tick();
      if (level().isClientSide && getIsRitualist()) {
         BlockPos home = getRestrictCenter();
         double d0 = home.getX() + 0.5 - getX();
         double d1 = home.getY() + 1.5 - (getY() + getEyeHeight());
         double d2 = home.getZ() + 0.5 - getZ();
         double d3 = Math.sqrt(d0 * d0 + d2 * d2);
         float f = (float) (Mth.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F;
         float f1 = (float) (-(Mth.atan2(d1, d3) * 180.0 / Math.PI));
         setXRot(updateRotation(getXRot(), f1, 10.0F));
         yHeadRot = updateRotation(yHeadRot, f, getMaxHeadYRot());
      }
      if (!level().isClientSide && getIsRitualist() && rage >= 5) setIsRitualist(false);
   }

   private static float updateRotation(float cur, float target, float max) {
      float f3 = Mth.wrapDegrees(target - cur);
      return cur + Mth.clamp(f3, -max, max);
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.CHANT.get();
   }

   @Override
   public int getAmbientSoundInterval() {
      return 500;
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 19) {
         for (int i = 0; i < 3; i++) {
            level().addParticle(ParticleTypes.ANGRY_VILLAGER, getX() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth(),
               getY() + 0.5 + random.nextFloat() * getBbHeight(), getZ() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth(),
               random.nextGaussian() * 0.02, random.nextGaussian() * 0.02, random.nextGaussian() * 0.02);
         }
      } else {
         super.handleEntityEvent(id);
      }
   }
}
