package thaumcraft.common.golems.ai;

import net.minecraft.entity.Entity;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.math.MathHelper;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.golems.EntityThaumcraftGolem;
import thaumcraft.common.golems.tasks.TaskHandler;

public class AIGotoEntity extends AIGoto {
   public AIGotoEntity(EntityThaumcraftGolem g) {
      super(g, (byte)1);
   }

   @Override
   public void updateTask() {
      super.updateTask();
      if (this.golem.getLookHelper() != null && this.golem.getTask() != null && this.golem.getTask().getEntity() != null) {
         this.golem.getLookHelper().setLookPositionWithEntity(this.golem.getTask().getEntity(), 10.0F, this.golem.getVerticalFaceSpeed());
      }
   }

   @Override
   protected void moveTo() {
      if (this.golem.getNavigator() != null && this.golem.getTask() != null && this.golem.getTask().getEntity() != null) {
         this.golem.getNavigator().tryMoveToEntityLiving(this.golem.getTask().getEntity(), this.golem.getGolemMoveSpeed());
      }
   }

   @Override
   protected boolean findDestination() {
      for (Task ticket : TaskHandler.getEntityTasksSorted(this.golem.world.provider.getDimension(), this.golem.getUniqueID(), this.golem)) {
         if (this.areGolemTagsValidForTask(ticket)
            && ticket.canGolemPerformTask(this.golem)
            && this.golem.isWithinHomeDistanceFromPosition(ticket.getEntity().getPosition())
            && this.isValidDestination(this.golem.world, ticket.getEntity().getPosition())
            && this.canEasilyReach(ticket.getEntity())) {
            this.golem.setTask(ticket);
            this.golem.getTask().setReserved(true);
            this.minDist = 3.5 + this.golem.getTask().getEntity().width / 2.0F * (this.golem.getTask().getEntity().width / 2.0F);
            if (ModConfig.CONFIG_GRAPHICS.showGolemEmotes) {
               this.golem.world.setEntityState(this.golem, (byte)5);
            }

            return true;
         }
      }

      return false;
   }

   private boolean canEasilyReach(Entity e) {
      if (this.golem.getDistanceSq(e) < this.minDist) {
         return true;
      } else {
         Path pathentity = this.golem.getNavigator().getPathToEntityLiving(e);
         if (pathentity == null) {
            return false;
         } else {
            PathPoint pathpoint = pathentity.getFinalPathPoint();
            if (pathpoint == null) {
               return false;
            } else {
               int i = pathpoint.x - MathHelper.floor(e.posX);
               int j = pathpoint.z - MathHelper.floor(e.posZ);
               return i * i + j * j < this.minDist;
            }
         }
      }
   }
}
