package thaumcraft.common.entities.projectile;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
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
import thaumcraft.codechicken.lib.vec.Quat;
import thaumcraft.common.lib.SoundsTC;

public class EntityRiftBlast extends EntityThrowable implements IEntityAdditionalSpawnData {
   int targetID = 0;
   EntityLivingBase target;
   public boolean red = false;
   public double[][] points;
   public float[][] colours;
   public double[] radii;
   int growing = -1;
   ArrayList<Quat> vecs = new ArrayList<>();

   public EntityRiftBlast(World par1World) {
      super(par1World);
   }

   public EntityRiftBlast(World par1World, EntityLivingBase par2EntityLiving, EntityLivingBase t, boolean r) {
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
         FXDispatcher.INSTANCE.burst(this.posX, this.posY, this.posZ, 6.0F);
      }

      this.setDead();
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.ticksExisted > (this.red ? 240 : 160)) {
         this.setDead();
      }

      if (this.target != null) {
         if (this.target == null || this.target.isDead) {
            this.setDead();
         }

         double d = this.getDistanceSq(this.target);
         double dx = this.target.posX - this.posX;
         double var10001 = this.target.height;
         double dy = this.target.getEntityBoundingBox().minY + var10001 * 0.6 - this.posY;
         double dz = this.target.posZ - this.posZ;
         double d13 = 1.0;
         dx /= d;
         dy /= d;
         dz /= d;
         this.motionX += dx * d13;
         this.motionY += dy * d13;
         this.motionZ += dz * d13;
         this.motionX = MathHelper.clamp((float)this.motionX, -0.33F, 0.33F);
         this.motionY = MathHelper.clamp((float)this.motionY, -0.33F, 0.33F);
         this.motionZ = MathHelper.clamp((float)this.motionZ, -0.33F, 0.33F);
         if (this.world.isRemote) {
            Quat q = new Quat(
               0.1,
               this.posX + this.rand.nextGaussian() * 0.05,
               this.posY + this.rand.nextGaussian() * 0.05,
               this.posZ + this.rand.nextGaussian() * 0.05
            );
            this.vecs.add(q);
            FXDispatcher.INSTANCE
               .drawCurlyWisp(
                  q.x,
                  q.y,
                  q.z,
                  0.0,
                  0.0,
                  0.0,
                  0.3F + this.rand.nextFloat() * 0.2F,
                  this.rand.nextFloat(),
                  this.rand.nextFloat() * 0.2F,
                  this.rand.nextFloat() * 0.2F,
                  0.5F,
                  null,
                  1,
                  this.rand.nextInt(2),
                  0
               );
            if (this.vecs.size() > 9) {
               this.vecs.remove(0);
            }

            this.points = new double[this.vecs.size()][3];
            this.colours = new float[this.vecs.size()][4];
            this.radii = new double[this.vecs.size()];
            int c = 0;
            if (this.vecs.size() > 1) {
               float vv = (float)(Math.PI / (this.vecs.size() - 1));

               for (Quat v : this.vecs) {
                  float variance = 1.0F + MathHelper.sin((c + this.ticksExisted) / 3.0F) * 0.2F;
                  float xx = MathHelper.sin((c + this.ticksExisted) / 6.0F) * 0.01F;
                  float yy = MathHelper.sin((c + this.ticksExisted) / 7.0F) * 0.01F;
                  float zz = MathHelper.sin((c + this.ticksExisted) / 8.0F) * 0.01F;
                  this.points[c][0] = v.x + xx;
                  this.points[c][1] = v.y + yy;
                  this.points[c][2] = v.z + zz;
                  this.radii[c] = v.s * variance;
                  this.radii[c] = this.radii[c] * MathHelper.sin(c * vv);
                  this.colours[c][0] = 1.0F;
                  this.colours[c][1] = 0.0F;
                  this.colours[c][2] = 0.0F;
                  this.colours[c][3] = 1.0F;
                  c++;
               }
            }
         }
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
