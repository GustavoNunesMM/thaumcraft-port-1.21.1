package thaumcraft.common.entities.projectile;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class EntityEldritchOrb extends EntityThrowable {
   public EntityEldritchOrb(World par1World) {
      super(par1World);
   }

   public EntityEldritchOrb(World par1World, EntityLivingBase p) {
      super(par1World, p);
      this.shoot(p, p.rotationPitch, p.rotationYaw, -5.0F, 0.75F, 0.0F);
   }

   protected float getGravityVelocity() {
      return 0.0F;
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.ticksExisted > 100) {
         this.setDead();
      }
   }

   protected void onImpact(RayTraceResult mop) {
      if (!this.world.isRemote && this.getThrower() != null) {
         List list = this.world.getEntitiesWithinAABBExcludingEntity(this.getThrower(), this.getEntityBoundingBox().grow(2.0, 2.0, 2.0));

         for (int i = 0; i < list.size(); i++) {
            Entity entity1 = (Entity)list.get(i);
            if (entity1 != null && entity1 instanceof EntityLivingBase && !((EntityLivingBase)entity1).isEntityUndead()) {
               ((EntityLivingBase)entity1)
                  .attackEntityFrom(
                     DamageSource.causeIndirectMagicDamage(this, this.getThrower()),
                     (float)this.getThrower().getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() * 0.666F
                  );

               try {
                  ((EntityLivingBase)entity1).addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 160, 0));
               } catch (Exception var6) {
               }
            }
         }

         this.playSound(SoundEvents.BLOCK_LAVA_EXTINGUISH, 0.5F, 2.6F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.8F);
         this.setDead();
      }
   }
}
