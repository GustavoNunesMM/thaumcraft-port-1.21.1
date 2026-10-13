package thaumcraft.common.entities.projectile;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
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

/**
 * EntityFocusProjectile: an orb carrying the rest of a focus package; it runs it where it lands. special: 0 plain,
 * 1 bouncy, 2 seeks hostiles, 3 seeks friendlies.
 */
public class FocusProjectileEntity extends ThrowableProjectile implements IEntityWithComplexSpawn {
   private static final EntityDataAccessor<Integer> SPECIAL = SynchedEntityData.defineId(FocusProjectileEntity.class, EntityDataSerializers.INT);
   public FocusPackage focusPackage = new FocusPackage();
   @Nullable
   private Entity target;
   /** Client: the effects drawn as the trail, and the last partial tick a particle was drawn at. */
   public FocusEffect[] effects;
   public float lastRenderTick;
   public boolean firstParticle;

   public FocusProjectileEntity(EntityType<? extends FocusProjectileEntity> type, Level level) {
      super(type, level);
   }

   public FocusProjectileEntity(FocusPackage pack, float speed, Trajectory trajectory, int special) {
      super(ModCasters.FOCUS_PROJECTILE.get(), pack.getCaster(), pack.world);
      this.focusPackage = pack;
      LivingEntity caster = pack.getCaster();
      double w = caster.getBbWidth() * 2.1;
      setPos(trajectory.source.x + trajectory.direction.x * w, trajectory.source.y + trajectory.direction.y * w,
         trajectory.source.z + trajectory.direction.z * w);
      shoot(trajectory.direction.x, trajectory.direction.y, trajectory.direction.z, speed, 0.0F);
      setSpecial(special);
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(SPECIAL, 0);
   }

   @Override
   protected double getDefaultGravity() {
      return getSpecial() > 1 ? 0.005 : 0.01;
   }

   public void setSpecial(int s) {
      entityData.set(SPECIAL, s);
   }

   public int getSpecial() {
      return entityData.get(SPECIAL);
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
      nbt.put("pack", focusPackage.serialize());
      nbt.putInt("special", getSpecial());
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      setSpecial(nbt.getInt("special"));
      focusPackage = new FocusPackage();
      focusPackage.deserialize(nbt.getCompound("pack"));
   }

   @Override
   protected void onHit(HitResult mop) {
      if (getSpecial() == 1 && mop instanceof BlockHitResult bhr) {
         if (level().getBlockState(bhr.getBlockPos()).getCollisionShape(level(), bhr.getBlockPos()).isEmpty()) return;
         Vec3 m = getDeltaMovement();
         double mx = m.x, my = m.y, mz = m.z;
         if (bhr.getDirection().getStepZ() != 0) mz *= -1.0;
         if (bhr.getDirection().getStepX() != 0) mx *= -1.0;
         if (bhr.getDirection().getStepY() != 0) my *= -0.9;
         setDeltaMovement(mx * 0.9, my * 0.9, mz * 0.9);
         if (!level().isClientSide) {
            playSound(SoundEvents.LEASH_KNOT_PLACE, 0.25F, 1.0F);
            if (getDeltaMovement().length() < 0.2) discard();
         }
         return;
      }
      if (!level().isClientSide) {
         HitResult hit = mop instanceof EntityHitResult ehr ? new EntityHitResult(ehr.getEntity(), position()) : mop;
         Vec3 pv = new Vec3(xo, yo, zo);
         Vec3 vf = getDeltaMovement();
         FocusPackage p = focusPackage;
         ServerTasks.schedule(0, () -> FocusEngine.runFocusPackage(p, new Trajectory[]{new Trajectory(pv, vf.normalize())}, new HitResult[]{hit}));
         discard();
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (tickCount > 1200 || !level().isClientSide && getOwner() == null) discard();
      firstParticle = true;
      if (target == null && tickCount % 5 == 0 && getSpecial() > 1) {
         for (LivingEntity pt : FocusUtils.getEntitiesInRangeSorted(level(), this, LivingEntity.class, 16.0)) {
            if (pt.isAlive() && FocusUtils.isVisibleTo(1.75F, this, pt, 16.0F) && FocusUtils.canEntityBeSeen(this, pt)) {
               boolean f = FocusUtils.isFriendly(getOwner(), pt);
               if (f && getSpecial() == 3 || !f && getSpecial() == 2) {
                  target = pt;
                  break;
               }
            }
         }
      }
      if (target != null) {
         double dx = target.getX() - getX();
         double dy = target.getBoundingBox().minY + target.getBbHeight() * 0.6 - getY();
         double dz = target.getZ() - getZ();
         Vec3 v = new Vec3(dx, dy, dz).normalize();
         Vec3 mv = getDeltaMovement();
         double lv = mv.length();
         mv = mv.normalize().add(v.scale(0.275)).normalize().scale(lv);
         setDeltaMovement(mv);
         if (tickCount % 5 == 0 && (!target.isAlive() || !FocusUtils.isVisibleTo(1.75F, this, target, 16.0F) || !FocusUtils.canEntityBeSeen(this, target))) {
            target = null;
         }
      }
   }

   @Override
   public Vec3 getLookAngle() {
      return getDeltaMovement().normalize();
   }
}
