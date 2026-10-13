package thaumcraft.common.entities.monster;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import thaumcraft.api.items.ItemsTC;

public class EntityBrainyZombie extends EntityZombie {
   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(25.0);
      this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(5.0);
      this.getEntityAttribute(SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0);
   }

   public EntityBrainyZombie(World world) {
      super(world);
      this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, false, new Class[0]));
   }

   public int getTotalArmorValue() {
      return super.getTotalArmorValue() + 1;
   }

   protected void dropLoot(boolean wasRecentlyHit, int lootingModifier, DamageSource source) {
      if (this.world.rand.nextInt(10) - lootingModifier <= 4) {
         this.entityDropItem(new ItemStack(ItemsTC.brain), 1.5F);
      }

      super.dropLoot(wasRecentlyHit, lootingModifier, source);
   }
}
