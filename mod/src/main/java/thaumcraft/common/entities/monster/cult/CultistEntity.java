package thaumcraft.common.entities.monster.cult;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import thaumcraft.common.lib.MobFXBridge;
import thaumcraft.common.registers.ModMobs;

/** EntityCultist: the Crimson Cult's foot soldiers. They spawn with their crimson armor (5% drop chance each). */
public abstract class CultistEntity extends Monster {
   protected CultistEntity(EntityType<? extends CultistEntity> type, Level level) {
      super(type, level);
      xpReward = 10;
      if (getNavigation() instanceof GroundPathNavigation g) g.setCanOpenDoors(true);
      setDropChance(EquipmentSlot.CHEST, 0.05F);
      setDropChance(EquipmentSlot.FEET, 0.05F);
      setDropChance(EquipmentSlot.HEAD, 0.05F);
      setDropChance(EquipmentSlot.LEGS, 0.05F);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 4.0);
   }

   @Override
   public boolean canPickUpLoot() {
      return false;
   }

   /** setLoot: the cult's uniform. */
   protected abstract void setLoot(RandomSource random, DifficultyInstance diff);

   protected void setEnchantmentBasedOnDifficulty(RandomSource random, DifficultyInstance diff) {
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
      setLoot(random, diff);
      setEnchantmentBasedOnDifficulty(random, diff);
      return data;
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      if (nbt.contains("HomeD")) restrictTo(new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ")), nbt.getInt("HomeD"));
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

   public static boolean isCultist(Entity e) {
      return e instanceof CultistEntity || e instanceof CultistLeaderEntity;
   }

   @Override
   public boolean isAlliedTo(Entity e) {
      return isCultist(e) || super.isAlliedTo(e);
   }

   @Override
   public boolean canAttackType(EntityType<?> type) {
      return !isCultistType(type) && super.canAttackType(type);
   }

   public static boolean isCultistType(EntityType<?> type) {
      return type == ModMobs.CULTIST_KNIGHT.get() || type == ModMobs.CULTIST_CLERIC.get() || type == ModMobs.CULTIST_LEADER.get();
   }

   /** EnchantmentHelper.addRandomEnchantment on the held item, with the enchantments mobs may spawn with. */
   public static void enchantHeld(net.minecraft.world.entity.Mob mob, RandomSource rand, int levels) {
      var reg = mob.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
      mob.setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(rand, mob.getMainHandItem(), levels,
         mob.level().registryAccess(), reg.getTag(net.minecraft.tags.EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT)));
   }

   /** spawnExplosionParticle: a burst of crimson smoke. */
   @Override
   public void spawnAnim() {
      if (level().isClientSide) {
         for (int i = 0; i < 20; i++) {
            double d0 = random.nextGaussian() * 0.05;
            double d1 = random.nextGaussian() * 0.05;
            double d2 = random.nextGaussian() * 0.05;
            double d3 = 2.0;
            MobFXBridge.INSTANCE.cultistSpawn(getX() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth() + d0 * d3,
               getY() + random.nextFloat() * getBbHeight() + d1 * d3, getZ() + random.nextFloat() * getBbWidth() * 2.0F - getBbWidth() + d2 * d3, d0, d1, d2);
         }
      } else {
         level().broadcastEntityEvent(this, (byte) 20);
      }
   }
}
