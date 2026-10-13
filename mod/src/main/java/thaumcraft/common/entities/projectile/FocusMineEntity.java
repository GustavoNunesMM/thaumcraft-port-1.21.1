package thaumcraft.common.entities.projectile;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
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

/** EntityFocusMine: arms itself where it lands; after two seconds it releases the package at what comes close. */
public class FocusMineEntity extends ThrowableProjectile implements IEntityWithComplexSpawn {
   private static final EntityDataAccessor<Boolean> ARMED = SynchedEntityData.defineId(FocusMineEntity.class, EntityDataSerializers.BOOLEAN);
   public FocusPackage focusPackage = new FocusPackage();
   boolean friendly;
   public int counter = 40;
   public FocusEffect[] effects;

   public FocusMineEntity(EntityType<? extends FocusMineEntity> type, Level level) {
      super(type, level);
   }

   public FocusMineEntity(FocusPackage pack, Trajectory trajectory, boolean friendly) {
      super(ModCasters.FOCUS_MINE.get(), pack.getCaster(), pack.world);
      this.focusPackage = pack;
      this.friendly = friendly;
      setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
      shoot(trajectory.direction.x, trajectory.direction.y, trajectory.direction.z, 0.0F, 0.0F);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(ARMED, false);
   }

   public boolean getIsArmed() {
      return entityData.get(ARMED);
   }

   public void setIsArmed(boolean armed) {
      entityData.set(ARMED, armed);
   }

   @Override
   protected double getDefaultGravity() {
      return 0.01;
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
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putBoolean("armed", getIsArmed());
      nbt.put("pack", focusPackage.serialize());
      nbt.putBoolean("friendly", friendly);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      friendly = nbt.getBoolean("friendly");
      setIsArmed(nbt.getBoolean("armed"));
      if (getIsArmed()) counter = 0;
      focusPackage = new FocusPackage();
      focusPackage.deserialize(nbt.getCompound("pack"));
   }

   /** Sticks where it lands (the 1.12 throwable was pushed back out of the block it fell into). */
   @Override
   protected void onHit(HitResult mop) {
      if (getOwner() != null) setIsArmed(true);
      if (mop instanceof BlockHitResult bhr) {
         Vec3 n = Vec3.atLowerCornerOf(bhr.getDirection().getNormal()).scale(0.08);
         setDeltaMovement(Vec3.ZERO);
         setPos(bhr.getLocation().add(n));
         setNoGravity(true);
      }
   }

   @Override
   protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
      return !getIsArmed() && super.canHitEntity(target);
   }

   @Override
   public void tick() {
      super.tick();
      if (getIsArmed()) setDeltaMovement(getDeltaMovement().scale(0.25));
      if (tickCount > 1200 || !level().isClientSide && getOwner() == null) discard();
      if (!isAlive() || !getIsArmed()) return;
      if (counter > 0) counter--;
      if (counter > 0 || tickCount % 5 != 0) return;
      if (level().isClientSide) {
         thaumcraft.client.fx.FocusEntityFX.mine(this);
         return;
      }
      List<LivingEntity> list2 = FocusUtils.getEntitiesInRange(level(), getX(), getY(), getZ(), this, LivingEntity.class, 1.0);
      int d = 0;
      LivingEntity caster = focusPackage.getCaster();
      for (LivingEntity e : list2) {
         if (e.isAlive() && (friendly == FocusUtils.isFriendly(caster, e))) {
            Vec3 epv = e.position().add(0.0, e.getBbHeight() / 2.0F, 0.0);
            Vec3 here = position();
            LivingEntity thrower = getOwner() instanceof LivingEntity le ? le : caster;
            ServerTasks.schedule(d++, () -> FocusEngine.runFocusPackage(focusPackage.copy(thrower),
               new Trajectory[]{new Trajectory(here, epv.subtract(here).normalize())},
               new HitResult[]{new EntityHitResult(e, e.position().add(0.0, e.getBbHeight() / 2.0F, 0.0))}));
         }
      }
      if (d > 0) discard();
   }
}
