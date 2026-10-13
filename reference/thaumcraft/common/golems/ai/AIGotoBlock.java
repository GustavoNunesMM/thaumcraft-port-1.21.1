package thaumcraft.common.golems.ai;

import net.minecraft.block.state.IBlockState;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.golems.EntityThaumcraftGolem;
import thaumcraft.common.golems.tasks.TaskHandler;

public class AIGotoBlock extends AIGoto {
   public AIGotoBlock(EntityThaumcraftGolem g) {
      super(g, (byte)0);
   }

   @Override
   public void updateTask() {
      super.updateTask();
      if (this.golem.getLookHelper() != null) {
         this.golem
            .getLookHelper()
            .setLookPosition(
               this.golem.getTask().getPos().getX() + 0.5,
               this.golem.getTask().getPos().getY() + 0.5,
               this.golem.getTask().getPos().getZ() + 0.5,
               10.0F,
               this.golem.getVerticalFaceSpeed()
            );
      }
   }

   @Override
   protected void moveTo() {
      if (this.targetBlock != null) {
         this.golem
            .getNavigator()
            .tryMoveToXYZ(
               this.targetBlock.getX() + 0.5,
               this.targetBlock.getY() + 0.5,
               this.targetBlock.getZ() + 0.5,
               this.golem.getGolemMoveSpeed()
            );
      } else {
         this.golem
            .getNavigator()
            .tryMoveToXYZ(
               this.golem.getTask().getPos().getX() + 0.5,
               this.golem.getTask().getPos().getY() + 0.5,
               this.golem.getTask().getPos().getZ() + 0.5,
               this.golem.getGolemMoveSpeed()
            );
      }
   }

   @Override
   protected boolean findDestination() {
      for (Task ticket : TaskHandler.getBlockTasksSorted(this.golem.world.provider.getDimension(), this.golem.getUniqueID(), this.golem)) {
         if (this.areGolemTagsValidForTask(ticket)
            && ticket.canGolemPerformTask(this.golem)
            && this.golem.isWithinHomeDistanceFromPosition(ticket.getPos())
            && this.isValidDestination(this.golem.world, ticket.getPos())
            && this.canEasilyReach(ticket.getPos())) {
            this.targetBlock = this.getAdjacentSpace(ticket.getPos());
            this.golem.setTask(ticket);
            this.golem.getTask().setReserved(true);
            if (ModConfig.CONFIG_GRAPHICS.showGolemEmotes) {
               this.golem.world.setEntityState(this.golem, (byte)5);
            }

            return true;
         }
      }

      return false;
   }

   private BlockPos getAdjacentSpace(BlockPos pos) {
      double d = Double.MAX_VALUE;
      BlockPos closest = null;

      for (EnumFacing face : EnumFacing.HORIZONTALS) {
         IBlockState block = this.golem.world.getBlockState(pos.offset(face));
         if (!block.getMaterial().blocksMovement()) {
            double dist = pos.offset(face).distanceSqToCenter(this.golem.posX, this.golem.posY, this.golem.posZ);
            if (dist < d) {
               closest = pos.offset(face);
               d = dist;
            }
         }
      }

      return closest;
   }

   private boolean canEasilyReach(BlockPos pos) {
      if (this.golem.getDistanceSqToCenter(pos) < this.minDist) {
         return true;
      } else {
         Path pathentity = this.golem.getNavigator().getPathToXYZ(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
         if (pathentity == null) {
            return false;
         } else {
            PathPoint pathpoint = pathentity.getFinalPathPoint();
            if (pathpoint == null) {
               return false;
            } else {
               int i = pathpoint.x - MathHelper.floor(pos.getX());
               int j = pathpoint.z - MathHelper.floor(pos.getZ());
               int k = pathpoint.y - MathHelper.floor(pos.getY());
               if (i == 0 && j == 0 && k == 2) {
                  k--;
               }

               return i * i + j * j + k * k < 2.25;
            }
         }
      }
   }
}
