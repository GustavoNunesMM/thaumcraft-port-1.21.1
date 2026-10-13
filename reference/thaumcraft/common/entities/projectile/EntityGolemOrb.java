package thaumcraft.common.entities.projectile;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.lib.SoundsTC;

public class EntityGolemOrb extends EntityThrowable implements IEntityAdditionalSpawnData {
   int targetID = 0;
   EntityLivingBase target;
   public boolean red = false;

   public EntityGolemOrb(World par1World) {
      super(par1World);
   }

   public EntityGolemOrb(World par1World, EntityLivingBase par2EntityLiving, EntityLivingBase t, boolean r) {
      super(par1World, par2EntityLiving);
      this.target = t;
      this.red = r;
   }

   protected float getGravityVelocity() {
      return 0.0F;
   }

   public void writeSpawnData(ByteBuf data) {
      int id = -1;
      if (this.target != null) {
         id = this.target.getEntityId();
      }

      data.writeInt(id);
      data.writeBoolean(this.red);
   }

   public void readSpawnData(ByteBuf data) {
      int id = data.readInt();

      try {
         if (id >= 0) {
            this.target = (EntityLivingBase)this.world.getEntityByID(id);
         }
      } catch (Exception var4) {
      }

      this.red = data.readBoolean();
   }

   protected void onImpact(RayTraceResult mop) {
      if (!this.world.isRemote && this.getThrower() != null && mop.typeOfHit == Type.ENTITY) {
         mop.entityHit
            .attackEntityFrom(
               DamageSource.causeIndirectMagicDamage(this, this.getThrower()),
               (float)this.getThrower().getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() * (this.red ? 1.0F : 0.6F)
            );
      }

      this.playSound(SoundsTC.shock, 1.0F, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
      if (this.world.isRemote) {
         FXDispatcher.INSTANCE.burst(this.posX, this.posY, this.posZ, 1.0F);
      }

      this.setDead();
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.ticksExisted > (this.red ? 240 : 160)) {
         this.setDead();
      }

      if (this.target != null) {
         double d = this.getDistanceSq(this.target);
         double dx = this.target.posX - this.posX;
         double var10001 = this.target.height;
         double dy = this.target.getEntityBoundingBox().minY + var10001 * 0.6 - this.posY;
         double dz = this.target.posZ - this.posZ;
         double d13 = 0.2;
         dx /= d;
         dy /= d;
         dz /= d;
         this.motionX += dx * d13;
         this.motionY += dy * d13;
         this.motionZ += dz * d13;
         this.motionX = MathHelper.clamp((float)this.motionX, -0.25F, 0.25F);
         this.motionY = MathHelper.clamp((float)this.motionY, -0.25F, 0.25F);
         this.motionZ = MathHelper.clamp((float)this.motionZ, -0.25F, 0.25F);
      }
   }

   public boolean attackEntityFrom(DamageSource source, float damage) {
      if (this.isEntityInvulnerable(source)) {
         return false;
      } else if (source.getTrueSource() != null) {
         Vec3d vec3 = source.getTrueSource().getLookVec();
         if (vec3 != null) {
            this.motionX = vec3.x;
            this.motionY = vec3.y;
            this.motionZ = vec3.z;
            this.motionX *= 0.9;
            this.motionY *= 0.9;
            this.motionZ *= 0.9;
            this.playSound(SoundsTC.zap, 1.0F, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
         }

         return true;
      } else {
         return false;
      }
   }
}
