package thaumcraft.common.golems.ai;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.golems.GolemEntity;

/** Idle golems walk back to their home block. */
public class AIGotoHome extends Goal {
   protected final GolemEntity golem;
   private double movePosX;
   private double movePosY;
   private double movePosZ;
   protected int idleCounter = 10;

   public AIGotoHome(GolemEntity g) {
      this.golem = g;
      setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
   }

   @Override
   public boolean canUse() {
      if (idleCounter > 0) {
         idleCounter--;
         return false;
      }
      idleCounter = 50;
      BlockPos home = golem.getRestrictCenter();
      double dd = golem.distanceToSqr(Vec3.atCenterOf(home));
      if (dd < 5.0) return false;
      if (dd > 1024.0) {
         Vec3 vec3 = DefaultRandomPos.getPosTowards(golem, 16, 7, Vec3.atLowerCornerOf(home), Math.PI / 2);
         if (vec3 == null) return false;
         movePosX = vec3.x;
         movePosY = vec3.y;
         movePosZ = vec3.z;
         return true;
      }
      movePosX = home.getX();
      movePosY = home.getY();
      movePosZ = home.getZ();
      return true;
   }

   @Override
   public void start() {
      golem.getNavigation().moveTo(movePosX, movePosY, movePosZ, golem.getGolemMoveSpeed());
   }

   @Override
   public boolean canContinueToUse() {
      return golem.getTask() == null && !golem.getNavigation().isDone() && golem.distanceToSqr(Vec3.atCenterOf(golem.getRestrictCenter())) > 3.0;
   }

   @Override
   public void stop() {
      idleCounter = 50;
      golem.getNavigation().stop();
   }
}
