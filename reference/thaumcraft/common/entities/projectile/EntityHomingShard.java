package thaumcraft.common.entities.projectile;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.EntityUtils;

public class EntityHomingShard extends EntityThrowable implements IEntityAdditionalSpawnData {
   Class tclass = null;
   boolean persistant = false;
   int targetID = 0;
   EntityLivingBase target;
   private static final DataParameter<Byte> STRENGTH = EntityDataManager.createKey(EntityHomingShard.class, DataSerializers.BYTE);
   public ArrayList<UtilsFX.Vector> vl = new ArrayList<>();

   public EntityHomingShard(World par1World) {
      super(par1World);
   }

   public EntityHomingShard(World par1World, EntityLivingBase p, EntityLivingBase t, int strength, boolean b) {
      super(par1World, p);
      this.target = t;
      this.tclass = t.getClass();
      this.persistant = b;
      this.setStrength(strength);
      Vec3d v = p.getLookVec();
      this.setLocationAndAngles(
         p.posX + v.x / 2.0,
         p.posY + p.getEyeHeight() + v.y / 2.0,
         p.posZ + v.z / 2.0,
         p.rotationYaw,
         p.rotationPitch
      );
      float f = 0.5F;
      float ry = p.rotationYaw + (this.rand.nextFloat() - this.rand.nextFloat()) * 60.0F;
      float rp = p.rotationPitch + (this.rand.nextFloat() - this.rand.nextFloat()) * 60.0F;
      this.motionX = -MathHelper.sin(ry / 180.0F * (float) Math.PI) * MathHelper.cos(rp / 180.0F * (float) Math.PI) * f;
      this.motionZ = MathHelper.cos(ry / 180.0F * (float) Math.PI) * MathHelper.cos(rp / 180.0F * (float) Math.PI) * f;
      this.motionY = -MathHelper.sin(rp / 180.0F * (float) Math.PI) * f;
   }

   public void entityInit() {
      super.entityInit();
      this.getDataManager().register(STRENGTH, (byte)0);
   }

   public void setStrength(int str) {
      this.getDataManager().set(STRENGTH, (byte)str);
   }

   public int getStrength() {
      return (Byte)this.getDataManager().get(STRENGTH);
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
   }

   public void readSpawnData(ByteBuf data) {
      int id = data.readInt();

      try {
         if (id >= 0) {
            this.target = (EntityLivingBase)this.world.getEntityByID(id);
         }
      } catch (Exception var4) {
      }
   }

   protected void onImpact(RayTraceResult mop) {
      if (!this.world.isRemote
         && mop.typeOfHit == Type.ENTITY
         && (this.getThrower() == null || this.getThrower() != null && mop.entityHit != this.getThrower())) {
         mop.entityHit.attackEntityFrom(DamageSource.causeIndirectMagicDamage(this, this.getThrower()), 1.0F + this.getStrength() * 0.5F);
         this.playSound(SoundsTC.zap, 1.0F, 1.0F + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F);
         this.world.setEntityState(this, (byte)16);
         this.setDead();
      }

      if (mop.typeOfHit == Type.BLOCK) {
         if (mop.sideHit.getZOffset() != 0) {
            this.motionZ *= -0.8;
         }

         if (mop.sideHit.getXOffset() != 0) {
            this.motionX *= -0.8;
         }

         if (mop.sideHit.getYOffset() != 0) {
            this.motionY *= -0.8;
         }
      }
   }

   @SideOnly(Side.CLIENT)
   public void handleStatusUpdate(byte par1) {
      if (par1 == 16) {
         FXDispatcher.INSTANCE.burst(this.posX, this.posY, this.posZ, 0.3F);
      } else {
         super.handleStatusUpdate(par1);
      }
   }

   public void onUpdate() {
      this.vl.add(0, new UtilsFX.Vector((float)this.lastTickPosX, (float)this.lastTickPosY, (float)this.lastTickPosZ));
      if (this.vl.size() > 6) {
         this.vl.remove(this.vl.size() - 1);
      }

      super.onUpdate();
      if (!this.world.isRemote) {
         if (this.persistant && (this.target == null || this.target.isDead || this.target.getDistanceSq(this) > 1250.0)) {
            for (Entity e : EntityUtils.getEntitiesInRange(
               this.world, this.posX, this.posY, this.posZ, this, this.tclass, 16.0
            )) {
               if (e instanceof EntityLivingBase
                  && !e.isDead
                  && (this.getThrower() == null || e.getEntityId() != this.getThrower().getEntityId())) {
                  this.target = (EntityLivingBase)e;
                  break;
               }
            }
         }

         if (this.target == null || this.target.isDead) {
            this.world.setEntityState(this, (byte)16);
            this.setDead();
         }
      }

      if (this.ticksExisted > 300) {
         this.world.setEntityState(this, (byte)16);
         this.setDead();
      }

      if (this.ticksExisted % 20 == 0 && this.target != null && !this.target.isDead) {
         double d = this.getDistance(this.target);
         double dx = this.target.posX - this.posX;
         double var10001 = this.target.height;
         double dy = this.target.getEntityBoundingBox().minY + var10001 * 0.6 - this.posY;
         double dz = this.target.posZ - this.posZ;
         dx /= d;
         dy /= d;
         dz /= d;
         this.motionX = dx;
         this.motionY = dy;
         this.motionZ = dz;
      }

      this.motionX *= 0.85;
      this.motionY *= 0.85;
      this.motionZ *= 0.85;
   }
}
