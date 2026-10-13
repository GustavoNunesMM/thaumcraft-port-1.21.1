package thaumcraft.common.entities.ai.combat;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.MathHelper;

public class EntityCritterAIAttackMelee extends EntityAIAttackMelee {
   public EntityCritterAIAttackMelee(EntityCreature creature, double speedIn, boolean useLongMemory) {
      super(creature, speedIn, useLongMemory);
   }

   protected void checkAndPerformAttack(EntityLivingBase target, double range) {
      double d0 = this.getAttackReachSqr(target);
      if (range <= d0 && this.attackTick <= 0) {
         this.attackTick = 20;
         this.attacker.swingArm(EnumHand.MAIN_HAND);
         this.attackEntityAsMob(this.attacker, target);
      }
   }

   protected boolean attackEntityAsMob(EntityLiving attacker, Entity target) {
      float f = Math.max(2.0F, (attacker.height + attacker.width) * 2.0F);
      if (attacker.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE) != null) {
         f = (float)attacker.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
      }

      int i = 0;
      if (target instanceof EntityLivingBase) {
         f += EnchantmentHelper.getModifierForCreature(attacker.getHeldItemMainhand(), ((EntityLivingBase)target).getCreatureAttribute());
         i += EnchantmentHelper.getKnockbackModifier(attacker);
      }

      boolean flag = target.attackEntityFrom(DamageSource.causeMobDamage(attacker), f);
      if (flag) {
         if (i > 0 && target instanceof EntityLivingBase) {
            ((EntityLivingBase)target)
               .knockBack(
                  attacker,
                  i * 0.5F,
                  MathHelper.sin(attacker.rotationYaw * (float) (Math.PI / 180.0)),
                  -MathHelper.cos(attacker.rotationYaw * (float) (Math.PI / 180.0))
               );
            attacker.motionX *= 0.6;
            attacker.motionZ *= 0.6;
         }

         int j = EnchantmentHelper.getFireAspectModifier(attacker);
         if (j > 0) {
            target.setFire(j * 4);
         }

         if (target instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer)target;
            ItemStack itemstack = attacker.getHeldItemMainhand();
            ItemStack itemstack1 = entityplayer.isHandActive() ? entityplayer.getActiveItemStack() : ItemStack.EMPTY;
            if (!itemstack.isEmpty()
               && !itemstack1.isEmpty()
               && itemstack.getItem().canDisableShield(itemstack, itemstack1, entityplayer, attacker)
               && itemstack1.getItem().isShield(itemstack1, entityplayer)) {
               float f1 = 0.25F + EnchantmentHelper.getEfficiencyModifier(attacker) * 0.05F;
               if (attacker.getRNG().nextFloat() < f1) {
                  entityplayer.getCooldownTracker().setCooldown(itemstack1.getItem(), 100);
                  attacker.world.setEntityState(entityplayer, (byte)30);
               }
            }
         }

         if (target instanceof EntityLivingBase) {
            EnchantmentHelper.applyThornEnchantments((EntityLivingBase)target, attacker);
         }

         EnchantmentHelper.applyArthropodEnchantments(attacker, target);
      }

      return flag;
   }
}
