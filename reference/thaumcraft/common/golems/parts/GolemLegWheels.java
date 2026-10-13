package thaumcraft.common.golems.parts;

import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.GolemLeg;

public class GolemLegWheels implements GolemLeg.ILegFunction {
   public static HashMap<Integer, Float> ani = new HashMap<>();

   @Override
   public void onUpdateTick(IGolemAPI golem) {
      if (golem.getGolemWorld().isRemote) {
         double dist = golem.getGolemEntity()
            .getDistance(golem.getGolemEntity().prevPosX, golem.getGolemEntity().prevPosY, golem.getGolemEntity().prevPosZ);
         float lastRot = 0.0F;
         if (ani.containsKey(golem.getGolemEntity().getEntityId())) {
            lastRot = ani.get(golem.getGolemEntity().getEntityId());
         }

         double d0 = golem.getGolemEntity().posX - golem.getGolemEntity().prevPosX;
         double d1 = golem.getGolemEntity().posY - golem.getGolemEntity().prevPosY;
         double d2 = golem.getGolemEntity().posZ - golem.getGolemEntity().prevPosZ;
         float dirTravel = (float)(Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F;
         double dir = 360.0F - (golem.getGolemEntity().rotationYaw - dirTravel);
         lastRot = (float)(lastRot + dist / 1.571 * dir);
         if (lastRot > 360.0F) {
            lastRot -= 360.0F;
         }

         ani.put(golem.getGolemEntity().getEntityId(), lastRot);
         if (golem.getGolemEntity().onGround && !golem.getGolemEntity().isInWater() && dist > 0.25) {
            int i = MathHelper.floor(golem.getGolemEntity().posX);
            int j = MathHelper.floor(golem.getGolemEntity().posY - 0.2F);
            int k = MathHelper.floor(golem.getGolemEntity().posZ);
            BlockPos blockpos = new BlockPos(i, j, k);
            IBlockState iblockstate = golem.getGolemEntity().world.getBlockState(blockpos);
            Block block = iblockstate.getBlock();
            if (block.getRenderType(iblockstate) != EnumBlockRenderType.INVISIBLE) {
               golem.getGolemEntity()
                  .world
                  .spawnParticle(
                     EnumParticleTypes.BLOCK_CRACK,
                     golem.getGolemEntity().posX + (golem.getGolemWorld().rand.nextFloat() - 0.5) * golem.getGolemEntity().width,
                     golem.getGolemEntity().getEntityBoundingBox().minY + 0.1,
                     golem.getGolemEntity().posZ + (golem.getGolemWorld().rand.nextFloat() - 0.5) * golem.getGolemEntity().width,
                     -golem.getGolemEntity().motionX * 4.0,
                     1.5,
                     -golem.getGolemEntity().motionZ * 4.0,
                     new int[]{Block.getStateId(iblockstate)}
                  );
            }
         }
      }
   }
}
