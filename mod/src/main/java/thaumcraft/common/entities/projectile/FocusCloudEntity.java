package thaumcraft.common.entities.projectile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.items.casters.foci.FocusUtils;
import thaumcraft.common.lib.ServerTasks;
import thaumcraft.common.registers.ModCasters;

/** EntityFocusCloud: a lingering cloud that applies the rest of the package to what is inside it every few ticks. */
public class FocusCloudEntity extends Entity implements IEntityWithComplexSpawn {
   private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(FocusCloudEntity.class, EntityDataSerializers.FLOAT);
   /** Entity ids and block positions recently hit (shared by all clouds, as in the original). */
   static final Map<Long, Long> cooldownMap = new HashMap<>();
   public FocusPackage focusPackage = new FocusPackage();
   @Nullable
   private LivingEntity owner;
   @Nullable
   private UUID ownerUniqueId;
   private int duration;
   public FocusEffect[] effects;

   public FocusCloudEntity(EntityType<? extends FocusCloudEntity> type, Level level) {
      super(type, level);
   }

   public FocusCloudEntity(FocusPackage pack, Trajectory trajectory, float rad, int dur) {
      super(ModCasters.FOCUS_CLOUD.get(), pack.world);
      this.focusPackage = pack;
      setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
      setOwner(pack.getCaster());
      setRadius(rad);
      this.duration = dur;
   }

   public int getDuration() {
      return duration;
   }

   public void setOwner(@Nullable LivingEntity ownerIn) {
      owner = ownerIn;
      ownerUniqueId = ownerIn == null ? null : ownerIn.getUUID();
   }

   @Nullable
   public LivingEntity getOwner() {
      if (owner == null && ownerUniqueId != null && level() instanceof ServerLevel sl && sl.getEntity(ownerUniqueId) instanceof LivingEntity le) {
         owner = le;
      }
      return owner;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(RADIUS, 0.5F);
   }

   public void setRadius(float radiusIn) {
      if (!level().isClientSide) entityData.set(RADIUS, radiusIn);
      refreshDimensions();
   }

   public float getRadius() {
      return entityData.get(RADIUS);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (RADIUS.equals(key)) refreshDimensions();
   }

   @Override
   public EntityDimensions getDimensions(Pose pose) {
      return EntityDimensions.scalable(getRadius() * 2.0F, 0.5F);
   }

   @Override
   public void writeSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeNbt(focusPackage.serialize());
   }

   @Override
   public void readSpawnData(RegistryFriendlyByteBuf buf) {
      focusPackage = new FocusPackage();
      CompoundTag tag = buf.readNbt();
      if (tag != null) focusPackage.deserialize(tag);
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag nbt) {
      nbt.putInt("Age", tickCount);
      nbt.putInt("Duration", duration);
      nbt.putFloat("Radius", getRadius());
      if (ownerUniqueId != null) nbt.putUUID("OwnerUUID", ownerUniqueId);
      nbt.put("pack", focusPackage.serialize());
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag nbt) {
      tickCount = nbt.getInt("Age");
      duration = nbt.getInt("Duration");
      setRadius(nbt.getFloat("Radius"));
      ownerUniqueId = nbt.hasUUID("OwnerUUID") ? nbt.getUUID("OwnerUUID") : null;
      focusPackage = new FocusPackage();
      focusPackage.deserialize(nbt.getCompound("pack"));
   }

   @Override
   public void tick() {
      super.tick();
      float rad = getRadius();
      if (!level().isClientSide && (tickCount > duration * 20 || getOwner() == null)) discard();
      if (!isAlive()) return;
      if (level().isClientSide) {
         thaumcraft.client.fx.FocusEntityFX.cloud(this);
         return;
      }
      if (tickCount % 5 != 0) return;
      long t = System.currentTimeMillis();
      List<Trajectory> trajectories = new ArrayList<>();
      List<HitResult> targets = new ArrayList<>();
      for (Entity e : FocusUtils.getEntitiesInRange(level(), getX(), getY(), getZ(), this, Entity.class, rad)) {
         if (!e.isAlive()) continue;
         if (e instanceof FocusCloudEntity) {
            Vec3 v = e.position().subtract(position());
            e.move(MoverType.SELF, new Vec3(v.x / 50.0, v.y / 50.0, v.z / 50.0));
         }
         if (e instanceof LivingEntity && (!cooldownMap.containsKey((long) e.getId()) || cooldownMap.get((long) e.getId()) <= t)) {
            cooldownMap.put((long) e.getId(), t + 2000L);
            Vec3 hv = e.position().add(0.0, e.getBbHeight() / 2.0F, 0.0);
            targets.add(new EntityHitResult(e, hv));
            trajectories.add(new Trajectory(position(), hv.subtract(position())));
         }
      }
      for (int a = 0; a < rad; a++) {
         Vec3 dV = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
         BlockHitResult br = FocusUtils.rayTraceBlocks(level(), position(), position().add(dV.scale(rad)), this);
         long bl = 0L;
         if (br != null) {
            bl = br.getBlockPos().asLong();
            if (cooldownMap.containsKey(bl)) {
               if (cooldownMap.get(bl) <= t) cooldownMap.remove(bl);
               else br = null;
            }
         }
         if (br != null) {
            targets.add(br);
            trajectories.add(new Trajectory(position(), dV));
            cooldownMap.put(bl, t + 2000L);
         }
      }
      if (!targets.isEmpty() && getOwner() != null) {
         LivingEntity o = getOwner();
         ServerTasks.schedule(0, () -> FocusEngine.runFocusPackage(focusPackage.copy(o), trajectories.toArray(new Trajectory[0]),
            targets.toArray(new HitResult[0])));
      }
   }

   @Override
   public boolean isPickable() {
      return false;
   }
}
