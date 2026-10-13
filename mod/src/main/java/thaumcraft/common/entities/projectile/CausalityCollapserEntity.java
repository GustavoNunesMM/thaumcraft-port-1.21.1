package thaumcraft.common.entities.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.entities.FluxRiftEntity;
import thaumcraft.common.registers.ModFlux;

/** EntityCausalityCollapser: explodes where it lands and makes every flux rift within 3 blocks collapse. */
public class CausalityCollapserEntity extends ThrowableItemProjectile {
   public CausalityCollapserEntity(EntityType<? extends CausalityCollapserEntity> type, Level level) {
      super(type, level);
   }

   public CausalityCollapserEntity(Level level, LivingEntity thrower) {
      super(ModFlux.CAUSALITY_COLLAPSER_ENTITY.get(), thrower, level);
   }

   @Override
   protected Item getDefaultItem() {
      return ModFlux.CAUSALITY_COLLAPSER.get();
   }

   @Override
   public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
      super.shoot(x, y, z, 0.8F, inaccuracy);
   }

   @Override
   public void tick() {
      super.tick();
      if (level().isClientSide) {
         for (double i = 0.0; i < 3.0; i++) {
            double coeff = i / 3.0;
            thaumcraft.client.fx.FXDispatcher.drawAlumentum((float) (xo + (getX() - xo) * coeff), (float) (yo + (getY() - yo) * coeff) + getBbHeight() / 2.0F,
               (float) (zo + (getZ() - zo) * coeff), 0.0125F * (random.nextFloat() - 0.5F), 0.0125F * (random.nextFloat() - 0.5F),
               0.0125F * (random.nextFloat() - 0.5F), 0.8F + random.nextFloat() * 0.2F, 0.3F + random.nextFloat() * 0.1F, random.nextFloat() * 0.1F, 0.5F,
               4.0F);
            thaumcraft.client.fx.FXDispatcher.drawGenericParticles(getX() + random.nextGaussian() * 0.2F, getY() + random.nextGaussian() * 0.2F,
               getZ() + random.nextGaussian() * 0.2F, 0, 0, 0, 1.0F, 1.0F, 1.0F, 0.7F, false, 448, 8, 1, 8, 0.3F, 0.0F, 1);
         }
      }
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (level().isClientSide) return;
      level().explode(this, getX(), getY(), getZ(), 2.0F, Level.ExplosionInteraction.TNT);
      for (FluxRiftEntity fr : level().getEntitiesOfClass(FluxRiftEntity.class, new AABB(position(), position()).inflate(3.0))) {
         if (fr.distanceToSqr(this) <= 9.0 || fr.getBoundingBox().inflate(3.0).contains(position())) fr.setCollapse(true);
      }
      discard();
   }
}
