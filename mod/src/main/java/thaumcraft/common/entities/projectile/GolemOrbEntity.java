package thaumcraft.common.entities.projectile;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.common.lib.MobFXBridge;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.registers.ModSounds;

/** EntityGolemOrb: a slow homing orb of energy (red when cast by the cult); it can be batted back. */
public class GolemOrbEntity extends ThrowableProjectile implements IEntityWithComplexSpawn {
   @Nullable
   private LivingEntity target;
   public boolean red;

   public GolemOrbEntity(EntityType<? extends GolemOrbEntity> type, Level level) {
      super(type, level);
   }

   public GolemOrbEntity(Level level, LivingEntity shooter, @Nullable LivingEntity target, boolean red) {
      super(ModMobs.GOLEM_ORB.get(), shooter, level);
      this.target = target;
      this.red = red;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
   }

   @Override
   protected double getDefaultGravity() {
      return 0.0;
   }

   @Override
   public void writeSpawnData(RegistryFriendlyByteBuf buf) {
      buf.writeInt(target == null ? -1 : target.getId());
      buf.writeBoolean(red);
   }

   @Override
   public void readSpawnData(RegistryFriendlyByteBuf buf) {
      int id = buf.readInt();
      if (id >= 0 && level().getEntity(id) instanceof LivingEntity le) target = le;
      red = buf.readBoolean();
   }

   @Override
   protected void onHit(HitResult mop) {
      if (!level().isClientSide && getOwner() instanceof LivingEntity thrower && mop instanceof EntityHitResult eh) {
         double dmg = thrower.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? thrower.getAttributeValue(Attributes.ATTACK_DAMAGE) : 4.0;
         eh.getEntity().hurt(damageSources().indirectMagic(this, thrower), (float) dmg * (red ? 1.0F : 0.6F));
      }
      playSound(ModSounds.SHOCK.get(), 1.0F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F);
      if (level().isClientSide) MobFXBridge.INSTANCE.burst(getX(), getY(), getZ(), 1.0F);
      if (!level().isClientSide) discard();
   }

   @Override
   protected boolean canHitEntity(Entity e) {
      return super.canHitEntity(e) && e != getOwner();
   }

   @Override
   public void tick() {
      super.tick();
      if (tickCount > (red ? 240 : 160) && !level().isClientSide) discard();
      if (target != null) {
         double d = distanceToSqr(target);
         double dx = (target.getX() - getX()) / d;
         double dy = (target.getBoundingBox().minY + target.getBbHeight() * 0.6 - getY()) / d;
         double dz = (target.getZ() - getZ()) / d;
         double d13 = 0.2;
         Vec3 m = getDeltaMovement().add(dx * d13, dy * d13, dz * d13);
         setDeltaMovement(Mth.clamp(m.x, -0.25, 0.25), Mth.clamp(m.y, -0.25, 0.25), Mth.clamp(m.z, -0.25, 0.25));
      }
   }

   @Override
   public boolean isPickable() {
      return true;
   }

   @Override
   public boolean hurt(DamageSource source, float damage) {
      if (isInvulnerableTo(source)) return false;
      if (source.getEntity() != null) {
         Vec3 v = source.getEntity().getLookAngle();
         setDeltaMovement(v.scale(0.9));
         playSound(ModSounds.ZAP.get(), 1.0F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F);
         return true;
      }
      return false;
   }
}
