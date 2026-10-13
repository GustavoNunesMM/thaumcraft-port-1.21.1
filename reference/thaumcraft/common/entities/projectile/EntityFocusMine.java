package thaumcraft.common.entities.projectile;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.lib.events.ServerEvents;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.lib.utils.Utils;

public class EntityFocusMine extends EntityThrowable implements IEntityAdditionalSpawnData {
   FocusPackage focusPackage;
   boolean friendly = false;
   private static final DataParameter<Boolean> ARMED = EntityDataManager.createKey(EntityFocusMine.class, DataSerializers.BOOLEAN);
   public int counter = 40;
   FocusEffect[] effects = null;

   public EntityFocusMine(World par1World) {
      super(par1World);
      this.setSize(0.15F, 0.15F);
   }

   public EntityFocusMine(FocusPackage pack, Trajectory trajectory, boolean friendly) {
      super(pack.world, pack.getCaster());
      this.focusPackage = pack;
      this.friendly = friendly;
      this.setPosition(trajectory.source.x, trajectory.source.y, trajectory.source.z);
      this.shoot(trajectory.direction.x, trajectory.direction.y, trajectory.direction.z, 0.0F, 0.0F);
      this.setSize(0.15F, 0.15F);
   }

   public void entityInit() {
      super.entityInit();
      this.getDataManager().register(ARMED, false);
   }

   public boolean getIsArmed() {
      return (Boolean)this.getDataManager().get(ARMED);
   }

   public void setIsArmed(boolean par1) {
      this.getDataManager().set(ARMED, par1);
   }

   protected float getGravityVelocity() {
      return 0.01F;
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
      nbt.setBoolean("armed", this.getIsArmed());
      nbt.setTag("pack", this.focusPackage.serialize());
      nbt.setBoolean("friendly", this.friendly);
   }

   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.friendly = nbt.getBoolean("friendly");
      this.setIsArmed(nbt.getBoolean("armed"));
      if (this.getIsArmed()) {
         this.counter = 0;
      }

      try {
         this.focusPackage = new FocusPackage();
         this.focusPackage.deserialize(nbt.getCompoundTag("pack"));
      } catch (Exception var3) {
      }
   }

   protected void onImpact(RayTraceResult mop) {
      if (mop != null && this.getThrower() != null) {
         this.setIsArmed(true);
      }
   }

   public void onUpdate() {
      super.onUpdate();
      if (this.pushOutOfBlocks(this.posX, this.posY, this.posZ)) {
         this.motionX *= 0.25;
         this.motionY *= 0.25;
         this.motionZ *= 0.25;
      }

      if (this.ticksExisted > 1200 || !this.world.isRemote && this.getThrower() == null) {
         this.setDead();
      }

      if (this.isEntityAlive() && this.getIsArmed()) {
         if (this.counter > 0) {
            this.counter--;
         }

         if (this.counter <= 0 && this.ticksExisted % 5 == 0) {
            if (this.world.isRemote) {
               if (this.effects == null) {
                  this.effects = this.focusPackage.getFocusEffects();
               }

               if (this.effects != null && this.effects.length > 0) {
                  FocusEffect eff = this.effects[this.rand.nextInt(this.effects.length)];
                  eff.renderParticleFX(
                     this.world,
                     this.posX + this.world.rand.nextGaussian() * 0.1,
                     this.posY + this.world.rand.nextGaussian() * 0.1,
                     this.posZ + this.world.rand.nextGaussian() * 0.1,
                     this.world.rand.nextGaussian() * 0.01F,
                     this.world.rand.nextGaussian() * 0.01F,
                     this.world.rand.nextGaussian() * 0.01F
                  );
               }
            } else {
               List<EntityLivingBase> list2 = EntityUtils.getEntitiesInRange(
                  this.world, this.posX, this.posY, this.posZ, this, EntityLivingBase.class, 1.0
               );
               int d = 0;

               for (final EntityLivingBase e : list2) {
                  if (!e.isDead
                     && (this.friendly ? EntityUtils.isFriendly(this.focusPackage.getCaster(), e) : !EntityUtils.isFriendly(this.focusPackage.getCaster(), e))) {
                     final Vec3d epv = e.getPositionVector().add(0.0, e.height / 2.0F, 0.0);
                     ServerEvents.addRunnableServer(
                        this.getEntityWorld(),
                        new Runnable() {
                           @Override
                           public void run() {
                              RayTraceResult ray = new RayTraceResult(e);
                              ray.hitVec = e.getPositionVector().add(0.0, e.height / 2.0F, 0.0);
                              FocusEngine.runFocusPackage(
                                 EntityFocusMine.this.focusPackage.copy(EntityFocusMine.this.getThrower()),
                                 new Trajectory[]{
                                    new Trajectory(EntityFocusMine.this.getPositionVector(), epv.subtract(EntityFocusMine.this.getPositionVector()).normalize())
                                 },
                                 new RayTraceResult[]{ray}
                              );
                           }
                        },
                        d++
                     );
                  }
               }

               if (d > 0) {
                  this.setDead();
               }
            }
         }
      }
   }
}
