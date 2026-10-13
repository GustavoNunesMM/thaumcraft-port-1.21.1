package thaumcraft.common.entities.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.registers.ModFlux;

/** EntityBottleTaint: shatters into flux taint for everything within 5 blocks and splashes flux goo around. */
public class BottleTaintEntity extends ThrowableItemProjectile {
   public BottleTaintEntity(EntityType<? extends BottleTaintEntity> type, Level level) {
      super(type, level);
   }

   public BottleTaintEntity(Level level, LivingEntity thrower) {
      super(ModFlux.BOTTLE_TAINT_ENTITY.get(), thrower, level);
   }

   @Override
   protected Item getDefaultItem() {
      return ModFlux.BOTTLE_TAINT.get();
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 3) {
         for (int a = 0; a < 100; a++) thaumcraft.client.fx.TaintFX.taintsplosion(this);
         thaumcraft.client.fx.TaintFX.bottleTaintBreak(getX(), getY(), getZ());
      } else {
         super.handleEntityEvent(id);
      }
   }

   @Override
   protected void onHit(HitResult result) {
      super.onHit(result);
      if (level().isClientSide) return;
      for (LivingEntity el : level().getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(5.0))) {
         if (!(el instanceof ITaintedMob) && !el.getType().is(EntityTypeTags.UNDEAD)) {
            el.addEffect(new MobEffectInstance(ModFlux.FLUX_TAINT, 100, 0, false, true));
         }
      }
      for (int a = 0; a < 10; a++) {
         int xx = (int) ((random.nextFloat() - random.nextFloat()) * 4.0F);
         int zz = (int) ((random.nextFloat() - random.nextFloat()) * 4.0F);
         BlockPos p = blockPosition().offset(xx, 0, zz);
         if (level().random.nextBoolean()) {
            if (isNormalCube(p.below()) && level().getBlockState(p).canBeReplaced()) {
               level().setBlockAndUpdate(p, ModFlux.FLUX_GOO.get().defaultBlockState());
            } else {
               p = p.below();
               if (isNormalCube(p.below()) && level().getBlockState(p).canBeReplaced()) {
                  level().setBlockAndUpdate(p, ModFlux.FLUX_GOO.get().defaultBlockState());
               }
            }
         }
      }
      level().broadcastEntityEvent(this, (byte) 3);
      discard();
   }

   private boolean isNormalCube(BlockPos p) {
      return level().getBlockState(p).isRedstoneConductor(level(), p);
   }
}
