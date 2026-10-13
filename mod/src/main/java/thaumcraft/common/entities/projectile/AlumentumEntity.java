package thaumcraft.common.entities.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.registers.ModEntities;
import thaumcraft.common.registers.ModItems;

/** EntityAlumentum: a thrown alumentum that explodes (and sets fires) where it lands. */
public class AlumentumEntity extends ThrowableItemProjectile {
   public AlumentumEntity(EntityType<? extends AlumentumEntity> type, Level level) {
      super(type, level);
   }

   public AlumentumEntity(Level level, LivingEntity thrower) {
      super(ModEntities.ALUMENTUM.get(), thrower, level);
   }

   @Override
   protected Item getDefaultItem() {
      return ModItems.ALUMENTUM.get();
   }

   @Override
   public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
      super.shoot(x, y, z, 0.75F, inaccuracy);
   }

   @Override
   public void tick() {
      super.tick();
      if (level().isClientSide) Client.trail(this);
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (!level().isClientSide) {
         level().explode(this, getX(), getY(), getZ(), 1.1F, true, Level.ExplosionInteraction.TNT);
         discard();
      }
   }

   static final class Client {
      static void trail(AlumentumEntity e) {
         var rand = e.random;
         for (double i = 0.0; i < 3.0; i++) {
            double coeff = i / 3.0;
            thaumcraft.client.fx.FXDispatcher.drawAlumentum((float) (e.xo + (e.getX() - e.xo) * coeff),
               (float) (e.yo + (e.getY() - e.yo) * coeff) + e.getBbHeight() / 2.0F, (float) (e.zo + (e.getZ() - e.zo) * coeff),
               0.0125F * (rand.nextFloat() - 0.5F), 0.0125F * (rand.nextFloat() - 0.5F), 0.0125F * (rand.nextFloat() - 0.5F),
               rand.nextFloat() * 0.2F, rand.nextFloat() * 0.1F, rand.nextFloat() * 0.1F, 0.5F, 4.0F);
            thaumcraft.client.fx.FXDispatcher.drawGenericParticles(e.getX() + rand.nextGaussian() * 0.2F, e.getY() + rand.nextGaussian() * 0.2F,
               e.getZ() + rand.nextGaussian() * 0.2F, 0, 0, 0, 1.0F, 1.0F, 1.0F, 0.7F, false, 448, 8, 1, 8, 0.3F, 0.0F, 1);
         }
      }
   }
}
