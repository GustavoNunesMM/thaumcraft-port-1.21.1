package thaumcraft.common.entities.monster.eldritch;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.registers.ModEldritch;
import thaumcraft.common.registers.ModGear;

/**
 * EntityInhabitedZombie: a husk in crimson knight plate, worn by an eldritch crab. Doesn't call reinforcements and
 * drops nothing; when it dies the helmed crab bursts out of it. One per 32 blocks.
 */
public class InhabitedZombieEntity extends Zombie implements IEldritchMob {
   public InhabitedZombieEntity(EntityType<? extends Zombie> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 5.0)
         .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
   }

   @Override
   protected void addBehaviourGoals() {
      super.addBehaviourGoals();
      targetSelector.addGoal(3, EldritchGuardianEntity.cultistTarget(this));
   }

   @Override
   protected void handleAttributes(float difficulty) {
      // No random reinforcement / health bonuses (applyEntityAttributes set the reinforcement chance to 0).
   }

   @Override
   public boolean killedEntity(ServerLevel level, LivingEntity entity) {
      return true;
   }

   @Override
   protected boolean convertsInWater() {
      return false;
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
      SpawnGroupData d = super.finalizeSpawn(level, diff, reason, data);
      setBaby(false);
      for (EquipmentSlot slot : EquipmentSlot.values()) setItemSlot(slot, ItemStack.EMPTY);
      float chance = level.getDifficulty() == Difficulty.HARD ? 0.9F : 0.6F;
      setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModGear.CRIMSON_PLATE_HELM.get()));
      if (random.nextFloat() <= chance) setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModGear.CRIMSON_PLATE_CHEST.get()));
      if (random.nextFloat() <= chance) setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModGear.CRIMSON_PLATE_LEGS.get()));
      for (EquipmentSlot slot : EquipmentSlot.values()) setDropChance(slot, 0.0F);
      return d;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
   }

   /** onDeathUpdate: the crab bursts out at once and the husk vanishes in a puff. */
   @Override
   protected void tickDeath() {
      if (!level().isClientSide && !isRemoved()) {
         EldritchCrabEntity crab = new EldritchCrabEntity(ModEldritch.ELDRITCH_CRAB.get(), level());
         crab.moveTo(getX(), getY() + getEyeHeight(), getZ(), getYRot(), getXRot());
         crab.setHelm(true);
         level().addFreshEntity(crab);
         dropExperience(getLastHurtByMob());
         if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2.0, getZ(), 20, getBbWidth(), getBbHeight() / 2.0, getBbWidth(), 0.02);
         }
         remove(RemovalReason.KILLED);
      }
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return thaumcraft.common.registers.ModSounds.CRABTALK.get();
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.GENERIC_HURT;
   }

   public static boolean checkSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
      RandomSource random) {
      if (!level.getEntitiesOfClass(InhabitedZombieEntity.class, new AABB(pos).inflate(32.0, 16.0, 32.0)).isEmpty()) return false;
      return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
   }
}
