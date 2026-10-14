package thaumcraft.common.entities.monster;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.lib.MobFXBridge;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.registers.ModSounds;

/**
 * EntityThaumicSlime: a flux slime that spits pieces of itself at players and bursts into tiny slimes when it dies.
 * STUB: ITaintedMob (taint milestone) is not ported; the slime does not spawn naturally (as in the original).
 */
public class ThaumicSlimeEntity extends Slime {
   int launched = 10;
   int spitCounter = 100;

   public ThaumicSlimeEntity(EntityType<? extends Slime> type, Level level) {
      super(type, level);
   }

   /** A size 1 slime spat from the shooter towards the target. */
   public static ThaumicSlimeEntity spit(Level level, LivingEntity shooter, LivingEntity target) {
      ThaumicSlimeEntity s = new ThaumicSlimeEntity(ModMobs.THAUMIC_SLIME.get(), level);
      s.setSize(1, true);
      double y = (shooter.getBoundingBox().minY + shooter.getBoundingBox().maxY) / 2.0;
      double dx = target.getX() - shooter.getX();
      double dy = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - y;
      double dz = target.getZ() - shooter.getZ();
      double h = Math.sqrt(dx * dx + dz * dz);
      s.setPos(shooter.getX(), y, shooter.getZ());
      if (h >= 1.0E-7) {
         float yaw = (float) (Mth.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
         float pitch = (float) (-(Mth.atan2(dy, h) * 180.0 / Math.PI));
         s.moveTo(shooter.getX() + dx / h, y, shooter.getZ() + dz / h, yaw, pitch);
         s.shoot(dx, dy + h * 0.2F, dz, 1.5F, 1.0F);
      }
      return s;
   }

   public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
      double l = Math.sqrt(x * x + y * y + z * z);
      x /= l;
      y /= l;
      z /= l;
      x += random.nextGaussian() * 0.0075F * inaccuracy;
      y += random.nextGaussian() * 0.0075F * inaccuracy;
      z += random.nextGaussian() * 0.0075F * inaccuracy;
      setDeltaMovement(new Vec3(x, y, z).scale(velocity));
      double h = Math.sqrt(x * x + z * z);
      setYRot((float) (Mth.atan2(x, z) * 180.0 / Math.PI));
      setXRot((float) (Mth.atan2(y, h) * 180.0 / Math.PI));
      yRotO = getYRot();
      xRotO = getXRot();
   }

   @Nullable
   @Override
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData data) {
      return super.finalizeSpawn(level, difficulty, spawnType, data);
   }

   @Override
   public void setSize(int size, boolean resetHealth) {
      super.setSize(size, resetHealth);
      xpReward = size + 2;
   }

   @Override
   protected boolean spawnCustomParticles() {
      if (level().isClientSide) {
         for (int j = 0; j < getSize() * 2; j++) MobFXBridge.INSTANCE.slimeJump(this, getSize());
      }
      return true;
   }

   @Override
   public void tick() {
      super.tick();
      int i = getSize();
      if (level().isClientSide) {
         if (launched > 0) {
            launched--;
            for (int j = 0; j < i * (launched + 1); j++) MobFXBridge.INSTANCE.slimeJump(this, i);
         }
      } else if (isAlive()) {
         Player p = level().getNearestPlayer(this, 16.0);
         if (p != null && !p.isSpectator()) {
            if (spitCounter > 0) spitCounter--;
            lookAt(p, 10.0F, 20.0F);
            if (distanceTo(p) > 4.0F && spitCounter <= 0 && getSize() > 2) {
               spitCounter = 101;
               level().addFreshEntity(spit(level(), this, p));
               playSound(ModSounds.GORE.get(), 1.0F, ((random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F) * 0.8F);
               setSize(getSize() - 1, true);
            }
         }
      }
   }

   /** setDead: a dying slime bursts into as many size 1 slimes as its size (instead of the usual 2-4 halves). */
   @Override
   public void remove(RemovalReason reason) {
      int i = getSize();
      if (!level().isClientSide && i > 1 && isDeadOrDying()) {
         for (int k = 0; k < i; k++) {
            float f = (k % 2 - 0.5F) * i / 4.0F;
            float f1 = (k / 2 - 0.5F) * i / 4.0F;
            ThaumicSlimeEntity s = new ThaumicSlimeEntity(ModMobs.THAUMIC_SLIME.get(), level());
            s.setSize(1, true);
            s.moveTo(getX() + f, getY() + 0.5, getZ() + f1, random.nextFloat() * 360.0F, 0.0F);
            level().addFreshEntity(s);
         }
         // the vanilla split below only happens for sizes above 1
         super.setSize(1, false);
      }
      super.remove(reason);
   }

   @Override
   protected boolean isDealsDamage() {
      return isEffectiveAi();
   }

   @Override
   protected float getAttackDamage() {
      return getSize() + 1;
   }

   /** dealDamage: the reach grows with the size (the original's spit bonus of 2 was always on server side). */
   @Override
   protected void dealDamage(LivingEntity target) {
      int i = getSize() + (launched > 0 ? 2 : 0);
      if (isAlive() && hasLineOfSight(target) && distanceToSqr(target) < 0.6 * i * 0.6 * i) {
         DamageSource ds = damageSources().mobAttack(this);
         if (target.hurt(ds, getAttackDamage())) {
            playSound(SoundEvents.SLIME_ATTACK, 1.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
            if (level() instanceof ServerLevel sl) EnchantmentHelper.doPostAttackEffects(sl, target, ds);
         }
      }
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      super.dropCustomDeathLoot(level, source, recentlyHit);
      if (getSize() > 1) spawnAtLocation(VisCrystalItem.of(Aspect.FLUX, 1), getBbHeight() / 2.0F);
   }
}
