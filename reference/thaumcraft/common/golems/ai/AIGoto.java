package thaumcraft.common.golems.ai;

import java.util.Arrays;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.golems.EntityThaumcraftGolem;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.golems.tasks.TaskHandler;

public abstract class AIGoto extends EntityAIBase {
   protected EntityThaumcraftGolem golem;
   protected int taskCounter = -1;
   protected byte type = 0;
   protected int cooldown;
   protected double minDist = 4.0;
   private BlockPos prevRamble;
   protected BlockPos targetBlock;
   int pause = 0;

   public AIGoto(EntityThaumcraftGolem g, byte type) {
      this.golem = g;
      this.type = type;
      this.setMutexBits(5);
   }

   public boolean shouldExecute() {
      if (this.cooldown > 0) {
         this.cooldown--;
         return false;
      } else {
         this.cooldown = 5;
         if (this.golem.getTask() != null && !this.golem.getTask().isSuspended()) {
            return false;
         } else {
            this.targetBlock = null;
            boolean start = this.findDestination();
            if (start && this.golem.getTask() != null && this.golem.getTask().getSealPos() != null) {
               ISealEntity se = GolemHelper.getSealEntity(this.golem.world.provider.getDimension(), this.golem.getTask().getSealPos());
               if (se != null) {
                  se.getSeal().onTaskStarted(this.golem.world, this.golem, this.golem.getTask());
               }
            }

            return start;
         }
      }
   }

   public void startExecuting() {
      this.moveTo();
      this.taskCounter = 0;
   }

   protected abstract void moveTo();

   public boolean shouldContinueExecuting() {
      return this.taskCounter >= 0
         && this.taskCounter <= 1000
         && this.golem.getTask() != null
         && !this.golem.getTask().isSuspended()
         && this.isValidDestination(this.golem.world, this.golem.getTask().getPos());
   }

   public void updateTask() {
      if (this.golem.getTask() != null) {
         if (this.pause-- <= 0) {
            double dist = this.golem.getTask().getType() == 0
               ? this.golem.getDistanceSqToCenter(this.targetBlock == null ? this.golem.getTask().getPos() : this.targetBlock)
               : this.golem.getDistanceSq(this.golem.getTask().getEntity());
            if (dist > this.minDist) {
               this.golem.getTask().setCompletion(false);
               this.taskCounter++;
               if (this.taskCounter % 5 == 0) {
                  if (this.prevRamble != null && this.prevRamble.equals(this.golem.getPosition())) {
                     Vec3d vec3 = RandomPositionGenerator.findRandomTargetBlockTowards(
                        this.golem,
                        6,
                        4,
                        new Vec3d(
                           this.golem.getTask().getPos().getX(),
                           this.golem.getTask().getPos().getY(),
                           this.golem.getTask().getPos().getZ()
                        )
                     );
                     if (vec3 != null) {
                        this.golem
                           .getNavigator()
                           .tryMoveToXYZ(vec3.x + 0.5, vec3.y + 0.5, vec3.z + 0.5, this.golem.getGolemMoveSpeed());
                     }
                  } else {
                     this.moveTo();
                  }

                  this.prevRamble = this.golem.getPosition();
               }
            } else {
               TaskHandler.completeTask(this.golem.getTask(), this.golem);
               if (this.golem.getTask() != null && this.golem.getTask().isCompleted()) {
                  if (this.taskCounter >= 0) {
                     this.taskCounter = 0;
                  }

                  this.pause = 0;
               } else {
                  this.pause = 10;
                  this.taskCounter++;
               }

               this.taskCounter--;
            }
         }
      }
   }

   public void resetTask() {
      if (this.golem.getTask() != null) {
         if (!this.golem.getTask().isCompleted() && this.golem.getTask().isReserved() && ModConfig.CONFIG_GRAPHICS.showGolemEmotes) {
            this.golem.world.setEntityState(this.golem, (byte)6);
         }

         if (this.golem.getTask().isCompleted() && !this.golem.getTask().isSuspended()) {
            this.golem.getTask().setSuspended(true);
         }

         this.golem.getTask().setReserved(false);
      }
   }

   protected abstract boolean findDestination();

   protected boolean isValidDestination(World world, BlockPos pos) {
      return true;
   }

   protected boolean areGolemTagsValidForTask(Task ticket) {
      ISealEntity se = SealHandler.getSealEntity(this.golem.world.provider.getDimension(), ticket.getSealPos());
      if (se != null && se.getSeal() != null) {
         if (se.isLocked() && !this.golem.getOwnerId().equals(se.getOwner())) {
            return false;
         } else if (se.getSeal().getRequiredTags() != null
            && !this.golem.getProperties().getTraits().containsAll(Arrays.asList(se.getSeal().getRequiredTags()))) {
            return false;
         } else {
            if (se.getSeal().getForbiddenTags() != null) {
               for (EnumGolemTrait tag : se.getSeal().getForbiddenTags()) {
                  if (this.golem.getProperties().getTraits().contains(tag)) {
                     return false;
                  }
               }
            }

            return true;
         }
      } else {
         return true;
      }
   }
}
