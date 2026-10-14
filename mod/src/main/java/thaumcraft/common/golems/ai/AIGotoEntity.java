package thaumcraft.common.golems.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemEntity;
import thaumcraft.common.golems.tasks.TaskHandler;

/** Entity tasks: go to the entity (an item to pick up, a target...). */
public class AIGotoEntity extends AIGoto {
   public AIGotoEntity(GolemEntity g) {
      super(g, (byte) 1);
   }

   @Override
   public void tick() {
      super.tick();
      Task task = golem.getTask();
      if (task != null && task.getEntity() != null) golem.getLookControl().setLookAt(task.getEntity(), 10.0F, golem.getMaxHeadXRot());
   }

   @Override
   protected void moveTo() {
      Task task = golem.getTask();
      if (task != null && task.getEntity() != null) golem.getNavigation().moveTo(task.getEntity(), golem.getGolemMoveSpeed());
   }

   @Override
   protected boolean findDestination() {
      for (Task ticket : TaskHandler.getEntityTasksSorted(golem.level(), golem.getUUID(), golem)) {
         if (areGolemTagsValidForTask(ticket) && ticket.canGolemPerformTask(golem) && golem.isWithinRestriction(ticket.getEntity().blockPosition())
            && isValidDestination(golem.level(), ticket.getEntity().blockPosition()) && canEasilyReach(ticket.getEntity())) {
            golem.setTask(ticket);
            ticket.setReserved(true);
            float w = ticket.getEntity().getBbWidth() / 2.0F;
            minDist = 3.5 + w * w;
            if (GolemEntity.showEmotes) golem.level().broadcastEntityEvent(golem, (byte) 5);
            return true;
         }
      }
      return false;
   }

   private boolean canEasilyReach(Entity e) {
      if (golem.distanceToSqr(e) < minDist) return true;
      Path path = golem.getNavigation().createPath(e, 0);
      if (path == null) return false;
      Node end = path.getEndNode();
      if (end == null) return false;
      int i = end.x - Mth.floor(e.getX());
      int j = end.z - Mth.floor(e.getZ());
      return i * i + j * j < minDist;
   }
}
