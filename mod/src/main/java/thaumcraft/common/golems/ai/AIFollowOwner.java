package thaumcraft.common.golems.ai;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import thaumcraft.common.golems.OwnedConstructEntity;

/** A construct following its owner, teleporting next to them when too far behind. */
public class AIFollowOwner extends Goal {
   private final OwnedConstructEntity thePet;
   private LivingEntity theOwner;
   private final Level theWorld;
   private final double followSpeed;
   private int timeToRecalcPath;
   private final float maxDist;
   private final float minDist;
   private float oldWaterCost;

   public AIFollowOwner(OwnedConstructEntity pet, double speed, float minDist, float maxDist) {
      this.thePet = pet;
      this.theWorld = pet.level();
      this.followSpeed = speed;
      this.minDist = minDist;
      this.maxDist = maxDist;
      setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   private PathNavigation nav() {
      return thePet.getNavigation();
   }

   @Override
   public boolean canUse() {
      LivingEntity owner = thePet.getOwnerEntity();
      if (owner == null || thePet.distanceToSqr(owner) < minDist * minDist) return false;
      theOwner = owner;
      return true;
   }

   @Override
   public boolean canContinueToUse() {
      return !nav().isDone() && thePet.distanceToSqr(theOwner) > maxDist * maxDist;
   }

   @Override
   public void start() {
      timeToRecalcPath = 0;
      oldWaterCost = thePet.getPathfindingMalus(PathType.WATER);
      thePet.setPathfindingMalus(PathType.WATER, 0.0F);
   }

   @Override
   public void stop() {
      theOwner = null;
      nav().stop();
      thePet.setPathfindingMalus(PathType.WATER, oldWaterCost);
   }

   private boolean free(BlockPos p) {
      BlockState s = theWorld.getBlockState(p);
      return s.isAir() || !s.isCollisionShapeFullBlock(theWorld, p);
   }

   @Override
   public void tick() {
      thePet.getLookControl().setLookAt(theOwner, 10.0F, thePet.getMaxHeadXRot());
      if (--timeToRecalcPath > 0) return;
      timeToRecalcPath = 10;
      if (!nav().moveTo(theOwner, followSpeed) && !thePet.isLeashed() && thePet.distanceToSqr(theOwner) >= 144.0) {
         int i = Mth.floor(theOwner.getX()) - 2;
         int j = Mth.floor(theOwner.getZ()) - 2;
         int k = Mth.floor(theOwner.getBoundingBox().minY);
         for (int l = 0; l <= 4; l++) {
            for (int i1 = 0; i1 <= 4; i1++) {
               BlockPos below = new BlockPos(i + l, k - 1, j + i1);
               if ((l < 1 || i1 < 1 || l > 3 || i1 > 3) && theWorld.getBlockState(below).isCollisionShapeFullBlock(theWorld, below)
                  && free(new BlockPos(i + l, k, j + i1)) && free(new BlockPos(i + l, k + 1, j + i1))) {
                  thePet.moveTo(i + l + 0.5F, k, j + i1 + 0.5F, thePet.getYRot(), thePet.getXRot());
                  nav().stop();
                  return;
               }
            }
         }
      }
   }
}
