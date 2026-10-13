package thaumcraft.common.entities.projectile;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.lib.events.ServerEvents;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.lib.utils.Utils;

public class EntityFocusCloud extends Entity implements IEntityAdditionalSpawnData {
   FocusPackage focusPackage;
   private EntityLivingBase owner;
   private UUID ownerUniqueId;
   private int duration;
   private static final DataParameter<Float> RADIUS = EntityDataManager.createKey(EntityFocusCloud.class, DataSerializers.FLOAT);
   static HashMap<Long, Long> cooldownMap = new HashMap<>();
   FocusEffect[] effects = null;

   public EntityFocusCloud(World par1World) {
      super(par1World);
   }

   public EntityFocusCloud(FocusPackage pack, Trajectory trajectory, float rad, int dur) {
      super(pack.world);
      this.focusPackage = pack;
      this.setPosition(trajectory.source.x, trajectory.source.y, trajectory.source.z);
      this.setSize(0.15F, 0.15F);
      this.setOwner(pack.getCaster());
      this.setRadius(rad);
      this.setDuration(dur);
   }

   public int getDuration() {
      return this.duration;
   }

   public void setDuration(int durationIn) {
      this.duration = durationIn;
   }

   public void setOwner(@Nullable EntityLivingBase ownerIn) {
      this.owner = ownerIn;
      this.ownerUniqueId = ownerIn == null ? null : ownerIn.getUniqueID();
   }

   @Nullable
   public EntityLivingBase getOwner() {
      if (this.owner == null && this.ownerUniqueId != null && this.world instanceof WorldServer) {
         Entity entity = ((WorldServer)this.world).getEntityFromUuid(this.ownerUniqueId);
         if (entity instanceof EntityLivingBase) {
            this.owner = (EntityLivingBase)entity;
         }
      }

      return this.owner;
   }

   public void entityInit() {
      this.getDataManager().register(RADIUS, 0.5F);
   }

   public void setRadius(float radiusIn) {
      double d0 = this.posX;
      double d1 = this.posY;
      double d2 = this.posZ;
      this.setSize(radiusIn * 2.0F, 0.5F);
      this.setPosition(d0, d1, d2);
      if (!this.world.isRemote) {
         this.getDataManager().set(RADIUS, radiusIn);
      }
   }

   public float getRadius() {
      return (Float)this.getDataManager().get(RADIUS);
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
      nbt.setInteger("Age", this.ticksExisted);
      nbt.setInteger("Duration", this.duration);
      nbt.setFloat("Radius", this.getRadius());
      if (this.ownerUniqueId != null) {
         nbt.setUniqueId("OwnerUUID", this.ownerUniqueId);
      }

      nbt.setTag("pack", this.focusPackage.serialize());
   }

   public void readEntityFromNBT(NBTTagCompound nbt) {
      this.ticksExisted = nbt.getInteger("Age");
      this.duration = nbt.getInteger("Duration");
      this.setRadius(nbt.getFloat("Radius"));
      this.ownerUniqueId = nbt.getUniqueId("OwnerUUID");

      try {
         this.focusPackage = new FocusPackage();
         this.focusPackage.deserialize(nbt.getCompoundTag("pack"));
      } catch (Exception var3) {
      }
   }

   public void onUpdate() {
      super.onUpdate();
      float rad = this.getRadius();
      int dur = this.getDuration();
      if (!this.world.isRemote && (this.ticksExisted > dur * 20 || this.getOwner() == null)) {
         this.setDead();
      }

      if (this.isEntityAlive()) {
         if (this.world.isRemote) {
            if (this.effects == null) {
               this.effects = this.focusPackage.getFocusEffects();
            }

            if (this.effects != null && this.effects.length > 0) {
               for (int a = 0; a < rad; a++) {
                  FocusEffect eff = this.effects[this.rand.nextInt(this.effects.length)];
                  FXDispatcher.INSTANCE
                     .drawFocusCloudParticle(
                        this.posX + this.world.rand.nextGaussian() * rad / 2.0 * 0.85,
                        this.posY + this.world.rand.nextGaussian() * rad / 2.0 * 0.85,
                        this.posZ + this.world.rand.nextGaussian() * rad / 2.0 * 0.85,
                        this.world.rand.nextGaussian() * 0.01,
                        this.world.rand.nextGaussian() * 0.01,
                        this.world.rand.nextGaussian() * 0.01,
                        FocusEngine.getElementColor(eff.getKey())
                     );
                  eff.renderParticleFX(
                     this.world,
                     this.posX + this.world.rand.nextGaussian() * rad / 2.0,
                     this.posY + this.world.rand.nextGaussian() * rad / 2.0,
                     this.posZ + this.world.rand.nextGaussian() * rad / 2.0,
                     this.world.rand.nextGaussian() * 0.01F,
                     this.world.rand.nextGaussian() * 0.01F,
                     this.world.rand.nextGaussian() * 0.01F
                  );
               }
            }
         } else if (this.ticksExisted % 5 == 0) {
            long t = System.currentTimeMillis();
            final ArrayList<Trajectory> trajectories = new ArrayList<>();
            final ArrayList<RayTraceResult> targets = new ArrayList<>();

            for (Entity e : EntityUtils.getEntitiesInRange(
               this.world, this.posX, this.posY, this.posZ, this, Entity.class, rad
            )) {
               if (!e.isDead) {
                  if (e instanceof EntityFocusCloud) {
                     Vec3d v = e.getPositionVector().subtract(this.getPositionVector());
                     e.move(MoverType.SELF, v.x / 50.0, v.y / 50.0, v.z / 50.0);
                     ((EntityFocusCloud)e).pushOutOfBlocks(this.posX, this.posY, this.posZ);
                  }

                  if (e instanceof EntityLivingBase && (!cooldownMap.containsKey(e.getEntityId()) || cooldownMap.get(e.getEntityId()) <= t)) {
                     cooldownMap.put((long)e.getEntityId(), t + 2000L);
                     RayTraceResult ray = new RayTraceResult(e);
                     ray.hitVec = e.getPositionVector().add(0.0, e.height / 2.0F, 0.0);
                     Trajectory tra = new Trajectory(this.getPositionVector(), this.getPositionVector().subtractReverse(ray.hitVec));
                     targets.add(ray);
                     trajectories.add(tra);
                  }
               }
            }

            for (int a = 0; a < rad; a++) {
               Vec3d dV = new Vec3d(this.rand.nextGaussian(), this.rand.nextGaussian(), this.rand.nextGaussian());
               dV = dV.normalize();
               RayTraceResult br = this.world.rayTraceBlocks(this.getPositionVector(), this.getPositionVector().add(dV.scale(rad)));
               long bl = 0L;
               if (br != null) {
                  bl = br.getBlockPos().toLong();
                  if (cooldownMap.containsKey(bl)) {
                     if (cooldownMap.get(bl) <= t) {
                        cooldownMap.remove(bl);
                     } else {
                        br = null;
                     }
                  }
               }

               if (br != null) {
                  targets.add(br);
                  Trajectory tra = new Trajectory(this.getPositionVector(), dV);
                  trajectories.add(tra);
                  cooldownMap.put(bl, t + 2000L);
               }
            }

            if (!targets.isEmpty()) {
               ServerEvents.addRunnableServer(
                  this.getEntityWorld(),
                  new Runnable() {
                     @Override
                     public void run() {
                        FocusEngine.runFocusPackage(
                           EntityFocusCloud.this.focusPackage.copy(EntityFocusCloud.this.getOwner()),
                           trajectories.toArray(new Trajectory[0]),
                           targets.toArray(new RayTraceResult[0])
                        );
                     }
                  },
                  0
               );
            }
         }
      }
   }
}
