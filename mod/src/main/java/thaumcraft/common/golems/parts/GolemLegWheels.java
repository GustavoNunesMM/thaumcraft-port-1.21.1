package thaumcraft.common.golems.parts;

import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.GolemLeg;

/** Client side: tracks the wheel's rotation and kicks up dust from the ground. */
public class GolemLegWheels implements GolemLeg.ILegFunction {
   public static final HashMap<Integer, Float> ani = new HashMap<>();

   @Override
   public void onUpdateTick(IGolemAPI golem) {
      LivingEntity e = golem.getGolemEntity();
      if (!golem.getGolemWorld().isClientSide) return;
      double dist = Math.sqrt(e.distanceToSqr(e.xo, e.yo, e.zo));
      float lastRot = ani.getOrDefault(e.getId(), 0.0F);
      double d0 = e.getX() - e.xo;
      double d2 = e.getZ() - e.zo;
      float dirTravel = (float) (Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F;
      double dir = 360.0F - (e.getYRot() - dirTravel);
      lastRot = (float) (lastRot + dist / 1.571 * dir);
      if (lastRot > 360.0F) lastRot -= 360.0F;
      ani.put(e.getId(), lastRot);
      if (e.onGround() && !e.isInWater() && dist > 0.25) {
         BlockPos blockpos = new BlockPos(Mth.floor(e.getX()), Mth.floor(e.getY() - 0.2F), Mth.floor(e.getZ()));
         BlockState state = e.level().getBlockState(blockpos);
         if (state.getRenderShape() != RenderShape.INVISIBLE) {
            e.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
               e.getX() + (e.getRandom().nextFloat() - 0.5) * e.getBbWidth(), e.getBoundingBox().minY + 0.1,
               e.getZ() + (e.getRandom().nextFloat() - 0.5) * e.getBbWidth(), -e.getDeltaMovement().x * 4.0, 1.5, -e.getDeltaMovement().z * 4.0);
         }
      }
   }
}
