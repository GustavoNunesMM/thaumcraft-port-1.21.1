package thaumcraft.common.entities.projectile;

import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntityGrapple extends EntityThrowable implements IEntityAdditionalSpawnData {
   public EnumHand hand = EnumHand.MAIN_HAND;
   EntityLivingBase cthrower;
   boolean p = false;
   boolean boost;
   int prevDist = 0;
   int count = 0;
   boolean added = false;
   public float ampl = 0.0F;
   public static HashMap<Integer, Integer> grapples = new HashMap<>();

   public EntityGrapple(World par1World) {
      super(par1World);
      this.setSize(0.1F, 0.1F);
   }

   public boolean isInRangeToRenderDist(double distance) {
      return distance < 4096.0;
   }

   public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
      super.shoot(x, y, z, velocity, 0.0F);
   }

   public EntityGrapple(World par1World, EntityLivingBase par2EntityLiving, EnumHand hand) {
      super(par1World, par2EntityLiving);
      this.setSize(0.1F, 0.1F);
      this.hand = hand;
   }

   public EntityGrapple(World par1World, double par2, double par4, double par6) {
      super(par1World, par2, par4, par6);
      this.setSize(0.1F, 0.1F);
   }

   public void writeSpawnData(ByteBuf data) {
      int id = -1;
      if (this.getThrower() != null) {
         id = this.getThrower().getEntityId();
      }

      data.writeInt(id);
      data.writeBoolean(this.hand == EnumHand.MAIN_HAND);
   }

   public void readSpawnData(ByteBuf data) {
      int id = data.readInt();
      this.hand = data.readBoolean() ? EnumHand.MAIN_HAND : EnumHand.OFF_HAND;

      try {
         if (id >= 0) {
            this.cthrower = (EntityLivingBase)this.world.getEntityByID(id);
         }
      } catch (Exception var4) {
      }
   }

   public EntityLivingBase getThrower() {
      return this.cthrower != null ? this.cthrower : super.getThrower();
   }

   protected float getGravityVelocity() {
      return this.getPulling() ? 0.0F : 0.03F;
   }

   public void entityInit() {
      super.entityInit();
   }

   public void setPulling() {
      this.p = true;
   }

   public boolean getPulling() {
      return this.p;
   }

   public void onUpdate() {
      super.onUpdate();
      if (!this.getPulling() && !this.isDead && (this.ticksExisted > 30 || this.getThrower() == null)) {
         if (this.getThrower() != null) {
            grapples.remove(this.getThrower().getEntityId());
         }

         this.setDead();
      }

      if (this.getThrower() != null) {
         if (!this.world.isRemote && !this.isDead && !this.added) {
            if (grapples.containsKey(this.getThrower().getEntityId())) {
               int ii = grapples.get(this.getThrower().getEntityId());
               if (ii != this.getEntityId()) {
                  Entity e = this.world.getEntityByID(ii);
                  if (e != null) {
                     e.setDead();
                  }
               }
            }

            grapples.put(this.getThrower().getEntityId(), this.getEntityId());
            this.added = true;
         }

         try {
            if (this.getThrower() != null
               && grapples.containsKey(this.getThrower().getEntityId())
               && grapples.get(this.getThrower().getEntityId()) != this.getEntityId()) {
               this.setDead();
            }
         } catch (Exception var13) {
         }

         double dis = this.getThrower().getDistance(this);
         if (this.getThrower() != null && this.getPulling() && !this.isDead) {
            if (this.getThrower().isSneaking()) {
               grapples.remove(this.getThrower().getEntityId());
               this.setDead();
            } else {
               if (!this.world.isRemote && this.getThrower() instanceof EntityPlayerMP) {
                  ((EntityPlayerMP)this.getThrower()).connection.floatingTickCount = 0;
               }

               this.getThrower().fallDistance = 0.0F;
               double mx = this.posX - this.getThrower().posX;
               double my = this.posY - this.getThrower().posY;
               double mz = this.posZ - this.getThrower().posZ;
               double dd = dis;
               if (dis < 8.0) {
                  dd = dis * (8.0 - dis);
               }

               dd = Math.max(1.0E-9, dd);
               mx /= dd * 5.0;
               my /= dd * 5.0;
               mz /= dd * 5.0;
               Vec3d v2 = new Vec3d(mx, my, mz);
               if (v2.length() > 0.25) {
                  v2 = v2.normalize();
                  mx = v2.x / 4.0;
                  my = v2.y / 4.0;
                  mz = v2.z / 4.0;
               }

               this.getThrower().motionX += mx;
               this.getThrower().motionY += my + 0.033;
               this.getThrower().motionZ += mz;
               if (!this.boost) {
                  this.getThrower().motionY += 0.4F;
                  this.boost = true;
               }

               int d = (int)(dis / 2.0);
               if (d == this.prevDist) {
                  this.count++;
               } else {
                  this.count = 0;
               }

               this.prevDist = d;
            }
         }

         if (this.world.isRemote) {
            if (!this.getPulling()) {
               this.ampl += 0.02F;
            } else {
               this.ampl *= 0.66F;
            }
         }
      }
   }

   @SideOnly(Side.CLIENT)
   public void handleStatusUpdate(byte id) {
      if (id == 6) {
         this.setPulling();
         this.motionX = 0.0;
         this.motionY = 0.0;
         this.motionZ = 0.0;
      }
   }

   protected void onImpact(RayTraceResult mop) {
      if (!this.world.isRemote) {
         this.setPulling();
         this.motionX = 0.0;
         this.motionY = 0.0;
         this.motionZ = 0.0;
         this.posX = mop.hitVec.x;
         this.posY = mop.hitVec.y;
         this.posZ = mop.hitVec.z;
         this.world.setEntityState(this, (byte)6);
      }
   }
}
