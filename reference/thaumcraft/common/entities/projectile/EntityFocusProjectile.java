package thaumcraft.common.entities.projectile;

import io.netty.buffer.ByteBuf;
import java.awt.Color;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.lib.events.ServerEvents;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.lib.utils.Utils;

public class EntityFocusProjectile extends EntityThrowable implements IEntityAdditionalSpawnData {
   FocusPackage focusPackage;
   private static final DataParameter<Integer> SPECIAL = EntityDataManager.createKey(EntityFocusProjectile.class, DataSerializers.VARINT);
   private static final DataParameter<Integer> OWNER = EntityDataManager.createKey(EntityFocusProjectile.class, DataSerializers.VARINT);
   boolean noTouchy = false;
   private Entity target;
   boolean firstParticle = false;
   public float lastRenderTick = 0.0F;
   FocusEffect[] effects = null;

   public EntityFocusProjectile(World par1World) {
      super(par1World);
      this.setSize(0.15F, 0.15F);
   }

   public EntityFocusProjectile(FocusPackage pack, float speed, Trajectory trajectory, int special) {
      super(pack.world, pack.getCaster());
      this.focusPackage = pack;
      this.setPosition(
         trajectory.source.x + trajectory.direction.x * pack.getCaster().width * 2.1,
         trajectory.source.y + trajectory.direction.y * pack.getCaster().width * 2.1,
         trajectory.source.z + trajectory.direction.z * pack.getCaster().width * 2.1
      );
      this.shoot(trajectory.direction.x, trajectory.direction.y, trajectory.direction.z, speed, 0.0F);
      this.setSize(0.15F, 0.15F);
      this.setSpecial(special);
      this.ignoreEntity = pack.getCaster();
      this.setOwner(this.getThrower().getEntityId());
   }

   protected float getGravityVelocity() {
      return this.getSpecial() > 1 ? 0.005F : 0.01F;
   }

   public void entityInit() {
      super.entityInit();
      this.getDataManager().register(SPECIAL, 0);
      this.getDataManager().register(OWNER, 0);
   }

   public void setOwner(int s) {
      this.getDataManager().set(OWNER, s);
   }

   public int getOwner() {
      return (Integer)this.getDataManager().get(OWNER);
   }

   public EntityLivingBase getThrower() {
      if (this.world.isRemote) {
         Entity e = this.world.getEntityByID(this.getOwner());
         if (e != null && e instanceof EntityLivingBase) {
            return (EntityLivingBase)e;
         }
      }

      return super.getThrower();
   }

   public void setSpecial(int s) {
      this.getDataManager().set(SPECIAL, s);
   }

   public int getSpecial() {
      return (Integer)this.getDataManager().get(SPECIAL);
   }

   public void writeSpawnData(ByteBuf data) {
      Utils.writeNBTTagCompoundToBuffer(data, this.focusPackage.serialize());
   }

   public void readSpawnData(ByteBuf data) {
      try {
         this.focusPackage = new FocusPackage();
         this.focusPackage.deserialize(Utils.readNBTTagCompoundFromBuffer(data));
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setTag("pack", this.focusPackage.serialize());
      nbt.setInteger("special", this.getSpecial());
   }

   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.setSpecial(nbt.getInteger("special"));

      try {
         this.focusPackage = new FocusPackage();
         this.focusPackage.deserialize(nbt.getCompoundTag("pack"));
      } catch (Exception var3) {
      }

      if (this.getThrower() != null) {
         this.setOwner(this.getThrower().getEntityId());
      }
   }

   protected void onImpact(final RayTraceResult mop) {
      if (mop != null) {
         if (this.getSpecial() == 1 && mop.typeOfHit == Type.BLOCK) {
            IBlockState bs = this.world.getBlockState(mop.getBlockPos());
            AxisAlignedBB bb = bs.getCollisionBoundingBox(this.world, mop.getBlockPos());
            if (bb == null) {
               return;
            }

            this.posX = this.posX - this.motionX;
            this.posY = this.posY - this.motionY;
            this.posZ = this.posZ - this.motionZ;
            if (mop.sideHit.getZOffset() != 0) {
               this.motionZ *= -1.0;
            }

            if (mop.sideHit.getXOffset() != 0) {
               this.motionX *= -1.0;
            }

            if (mop.sideHit.getYOffset() != 0) {
               this.motionY *= -0.9;
            }

            this.motionX *= 0.9;
            this.motionY *= 0.9;
            this.motionZ *= 0.9;
            float var20 = MathHelper.sqrt(
               this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ
            );
            this.posX = this.posX - this.motionX / var20 * 0.05F;
            this.posY = this.posY - this.motionY / var20 * 0.05F;
            this.posZ = this.posZ - this.motionZ / var20 * 0.05F;
            if (!this.world.isRemote) {
               this.playSound(SoundEvents.ENTITY_LEASHKNOT_PLACE, 0.25F, 1.0F);
            }

            if (!this.world.isRemote && new Vec3d(this.motionX, this.motionY, this.motionZ).length() < 0.2) {
               this.setDead();
            }

            return;
         }

         if (!this.world.isRemote) {
            if (mop.entityHit != null) {
               mop.hitVec = this.getPositionVector();
            }

            final Vec3d pv = new Vec3d(this.prevPosX, this.prevPosY, this.prevPosZ);
            final Vec3d vf = new Vec3d(this.motionX, this.motionY, this.motionZ);
            ServerEvents.addRunnableServer(
               this.getEntityWorld(),
               new Runnable() {
                  @Override
                  public void run() {
                     FocusEngine.runFocusPackage(
                        EntityFocusProjectile.this.focusPackage, new Trajectory[]{new Trajectory(pv, vf.normalize())}, new RayTraceResult[]{mop}
                     );
                  }
               },
               0
            );
            this.setDead();
         }
      }
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.ticksExisted > 1200 || !this.world.isRemote && this.getThrower() == null) {
         this.setDead();
      }

      this.firstParticle = true;
      if (this.target == null && this.ticksExisted % 5 == 0 && this.getSpecial() > 1) {
         for (EntityLivingBase pt : EntityUtils.getEntitiesInRangeSorted(this.getEntityWorld(), this, EntityLivingBase.class, 16.0)) {
            if (!pt.isDead && EntityUtils.isVisibleTo(1.75F, this, pt, 16.0F) && EntityUtils.canEntityBeSeen(this, pt)) {
               boolean f = EntityUtils.isFriendly(this.getThrower(), pt);
               if (f && this.getSpecial() == 3) {
                  this.target = pt;
                  break;
               }

               if (!f && this.getSpecial() == 2) {
                  this.target = pt;
                  break;
               }
            }
         }
      }

      if (this.target != null) {
         double d = this.getDistanceSq(this.target);
         double dx = this.target.posX - this.posX;
         double var10001 = this.target.height;
         double dy = this.target.getEntityBoundingBox().minY + var10001 * 0.6 - this.posY;
         double dz = this.target.posZ - this.posZ;
         Vec3d v = new Vec3d(dx, dy, dz);
         v = v.normalize();
         Vec3d mv = new Vec3d(this.motionX, this.motionY, this.motionZ);
         double lv = mv.length();
         mv = mv.normalize().add(v.scale(0.275));
         mv = mv.normalize().scale(lv);
         this.motionX = mv.x;
         this.motionY = mv.y;
         this.motionZ = mv.z;
         if (this.ticksExisted % 5 == 0
            && (this.target.isDead || !EntityUtils.isVisibleTo(1.75F, this, this.target, 16.0F) || !EntityUtils.canEntityBeSeen(this, this.target))) {
            this.target = null;
         }
      }
   }

   public Vec3d getLookVec() {
      return new Vec3d(this.motionX, this.motionY, this.motionZ).normalize();
   }

   public void renderParticle(float coeff) {
      this.lastRenderTick = coeff;
      if (this.effects == null) {
         this.effects = this.focusPackage.getFocusEffects();
      }

      if (this.effects != null && this.effects.length > 0) {
         FocusEffect eff = this.effects[this.rand.nextInt(this.effects.length)];
         float scale = 1.0F;
         Color c1 = new Color(FocusEngine.getElementColor(eff.getKey()));
         FXDispatcher.INSTANCE
            .drawFireMote(
               (float)(this.prevPosX + (this.posX - this.prevPosX) * coeff),
               (float)(this.prevPosY + (this.posY - this.prevPosY) * coeff) + this.height / 2.0F,
               (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * coeff),
               0.0125F * (this.rand.nextFloat() - 0.5F) * scale,
               0.0125F * (this.rand.nextFloat() - 0.5F) * scale,
               0.0125F * (this.rand.nextFloat() - 0.5F) * scale,
               c1.getRed() / 255.0F,
               c1.getGreen() / 255.0F,
               c1.getBlue() / 255.0F,
               0.5F,
               7.0F * scale
            );
         if (this.firstParticle) {
            this.firstParticle = false;
            eff.renderParticleFX(
               this.world,
               this.prevPosX + (this.posX - this.prevPosX) * coeff + this.world.rand.nextGaussian() * 0.1F,
               this.prevPosY
                  + (this.posY - this.prevPosY) * coeff
                  + this.height / 2.0F
                  + this.world.rand.nextGaussian() * 0.1F,
               this.prevPosZ + (this.posZ - this.prevPosZ) * coeff + this.world.rand.nextGaussian() * 0.1F,
               this.world.rand.nextGaussian() * 0.01F,
               this.world.rand.nextGaussian() * 0.01F,
               this.world.rand.nextGaussian() * 0.01F
            );
         }
      }
   }
}
