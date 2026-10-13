package thaumcraft.common.golems.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemEntity;
import thaumcraft.common.golems.tasks.TaskHandler;

/** Block tasks: walk next to the block. */
public class AIGotoBlock extends AIGoto {
   public AIGotoBlock(GolemEntity g) {
      super(g, (byte) 0);
   }

   @Override
   public void tick() {
      super.tick();
      Task task = golem.getTask();
      if (task != null) {
         BlockPos p = task.getPos();
         golem.getLookControl().setLookAt(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 10.0F, golem.getMaxHeadXRot());
      }
   }

   @Override
   protected void moveTo() {
      BlockPos p = targetBlock != null ? targetBlock : golem.getTask().getPos();
      golem.getNavigation().moveTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, golem.getGolemMoveSpeed());
   }

   @Override
   protected boolean findDestination() {
      for (Task ticket : TaskHandler.getBlockTasksSorted(golem.level(), golem.getUUID(), golem)) {
         if (areGolemTagsValidForTask(ticket) && ticket.canGolemPerformTask(golem) && golem.isWithinRestriction(ticket.getPos())
            && isValidDestination(golem.level(), ticket.getPos()) && canEasilyReach(ticket.getPos())) {
            targetBlock = getAdjacentSpace(ticket.getPos());
            golem.setTask(ticket);
            ticket.setReserved(true);
            if (GolemEntity.showEmotes) golem.level().broadcastEntityEvent(golem, (byte) 5);
            return true;
         }
      }
      return false;
   }

   private BlockPos getAdjacentSpace(BlockPos pos) {
      double d = Double.MAX_VALUE;
      BlockPos closest = null;
      for (Direction face : Direction.Plane.HORIZONTAL) {
         BlockPos q = pos.relative(face);
         if (!golem.level().getBlockState(q).blocksMotion()) {
            double dist = q.distToCenterSqr(golem.getX(), golem.getY(), golem.getZ());
            if (dist < d) {
               closest = q;
               d = dist;
            }
         }
      }
      return closest;
   }

   private boolean canEasilyReach(BlockPos pos) {
      if (distToCenter(golem, pos) < minDist) return true;
      Path path = golem.getNavigation().createPath(pos, 0);
      if (path == null) return false;
      Node end = path.getEndNode();
      if (end == null) return false;
      int i = end.x - pos.getX();
      int j = end.z - pos.getZ();
      int k = end.y - pos.getY();
      if (i == 0 && j == 0 && k == 2) k--;
      return i * i + j * j + k * k < 2.25;
   }
}
