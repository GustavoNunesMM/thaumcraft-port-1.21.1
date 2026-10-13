package thaumcraft.common.golems.parts;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.init.SoundEvents;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.common.entities.projectile.EntityGolemDart;
import thaumcraft.common.golems.ai.AIArrowAttack;

public class GolemArmDart implements GolemArm.IArmFunction {
   @Override
   public void onMeleeAttack(IGolemAPI golem, Entity target) {
   }

   @Override
   public void onRangedAttack(IGolemAPI golem, EntityLivingBase target, float range) {
      EntityGolemDart entityarrow = new EntityGolemDart(golem.getGolemWorld(), golem.getGolemEntity());
      float dmg = (float)golem.getGolemEntity().getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() / 3.0F;
      entityarrow.setDamage(dmg + range + golem.getGolemWorld().rand.nextGaussian() * 0.25);
      double d0 = target.posX - golem.getGolemEntity().posX;
      double d1 = target.getEntityBoundingBox().minY + target.getEyeHeight() + range * range - entityarrow.posY;
      double d2 = target.posZ - golem.getGolemEntity().posZ;
      entityarrow.shoot(d0, d1, d2, 1.6F, 3.0F);
      golem.getGolemWorld().spawnEntity(entityarrow);
      golem.getGolemEntity().playSound(SoundEvents.ENTITY_ARROW_SHOOT, 1.0F, 1.0F / (golem.getGolemWorld().rand.nextFloat() * 0.4F + 0.8F));
   }

   @Override
   public EntityAIAttackRanged getRangedAttackAI(IRangedAttackMob golem) {
      return new AIArrowAttack(golem, 1.0, 20, 25, 16.0F);
   }

   @Override
   public void onUpdateTick(IGolemAPI golem) {
   }
}
