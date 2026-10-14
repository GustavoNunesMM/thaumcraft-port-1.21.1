package thaumcraft.common.entities.monster;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import thaumcraft.common.registers.ModDevices;

/** EntityBrainyZombie: a tougher zombie that never calls reinforcements and often drops its brain. */
public class BrainyZombieEntity extends Zombie {
   public BrainyZombieEntity(EntityType<? extends Zombie> type, Level level) {
      super(type, level);
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 25.0).add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.ARMOR, 3.0);
   }

   @Override
   protected void addBehaviourGoals() {
      super.addBehaviourGoals();
      targetSelector.addGoal(1, new HurtByTargetGoal(this));
   }

   @Override
   protected void randomizeReinforcementsChance() {
      getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0);
   }

   @Override
   protected boolean convertsInWater() {
      return false;
   }

   protected static int looting(ServerLevel level, DamageSource source) {
      return source.getEntity() instanceof LivingEntity le
         ? EnchantmentHelper.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), le) : 0;
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      super.dropCustomDeathLoot(level, source, recentlyHit);
      if (random.nextInt(10) - looting(level, source) <= 4) spawnAtLocation(new ItemStack(ModDevices.BRAIN.get()), 1.5F);
   }
}
