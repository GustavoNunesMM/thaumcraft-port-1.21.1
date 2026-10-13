package thaumcraft.common.entities.ai.pech;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.SoundCategory;
import thaumcraft.common.entities.monster.EntityPech;

public class AIPechItemEntityGoto extends EntityAIBase {
   private EntityPech pech;
   private Entity targetEntity;
   float maxTargetDistance = 16.0F;
   private int count;
   private int failedPathFindingPenalty;

   public AIPechItemEntityGoto(EntityPech par1EntityCreature) {
      this.pech = par1EntityCreature;
      this.setMutexBits(3);
   }

   public boolean shouldExecute() {
      if (--this.count > 0) {
         return false;
      } else {
         double range = Double.MAX_VALUE;
         List<Entity> targets = this.pech
            .world
            .getEntitiesWithinAABBExcludingEntity(this.pech, this.pech.getEntityBoundingBox().grow(this.maxTargetDistance, this.maxTargetDistance, this.maxTargetDistance));
         if (targets.size() == 0) {
            return false;
         } else {
            for (Entity e : targets) {
               if (e instanceof EntityItem && this.pech.canPickup(((EntityItem)e).getItem())) {
                  NBTTagCompound itemData = ((EntityItem)e).getEntityData();
                  String username = ((EntityItem)e).getThrower();
                  if (username == null || !username.equals("PechDrop")) {
                     double distance = e.getDistanceSq(this.pech.posX, this.pech.posY, this.pech.posZ);
                     if (distance < range && distance <= this.maxTargetDistance * this.maxTargetDistance) {
                        range = distance;
                        this.targetEntity = e;
                     }
                  }
               }
            }

            return this.targetEntity != null;
         }
      }
   }

   public boolean shouldContinueExecuting() {
      return this.targetEntity == null
         ? false
         : (
            !this.targetEntity.isEntityAlive()
               ? false
               : !this.pech.getNavigator().noPath() && this.targetEntity.getDistanceSq(this.pech) < this.maxTargetDistance * this.maxTargetDistance
         );
   }

   public void resetTask() {
      this.targetEntity = null;
   }

   public void startExecuting() {
      this.pech
         .getNavigator()
         .setPath(
            this.pech.getNavigator().getPathToEntityLiving(this.targetEntity), this.pech.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() * 1.5
         );
      this.count = 0;
   }

   public void updateTask() {
      this.pech.getLookHelper().setLookPositionWithEntity(this.targetEntity, 30.0F, 30.0F);
      if (this.pech.getEntitySenses().canSee(this.targetEntity) && --this.count <= 0) {
         this.count = this.failedPathFindingPenalty + 4 + this.pech.getRNG().nextInt(4);
         this.pech.getNavigator().tryMoveToEntityLiving(this.targetEntity, this.pech.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() * 1.5);
         if (this.pech.getNavigator().getPath() != null) {
            PathPoint finalPathPoint = this.pech.getNavigator().getPath().getFinalPathPoint();
            if (finalPathPoint != null
               && this.targetEntity.getDistanceSq(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1.0) {
               this.failedPathFindingPenalty = 0;
            } else {
               this.failedPathFindingPenalty += 10;
            }
         } else {
            this.failedPathFindingPenalty += 10;
         }
      }

      double distance = this.pech
         .getDistanceSq(this.targetEntity.posX, this.targetEntity.getEntityBoundingBox().minY, this.targetEntity.posZ);
      if (distance <= 1.5) {
         this.count = 0;
         int am = ((EntityItem)this.targetEntity).getItem().getCount();
         ItemStack is = this.pech.pickupItem(((EntityItem)this.targetEntity).getItem());
         if (is != null && !is.isEmpty() && is.getCount() > 0) {
            ((EntityItem)this.targetEntity).setItem(is);
         } else {
            this.targetEntity.setDead();
         }

         if (is == null || is.isEmpty() || is.getCount() != am) {
            this.targetEntity
               .world
               .playSound(
                  null,
                  this.targetEntity.posX,
                  this.targetEntity.posY,
                  this.targetEntity.posZ,
                  SoundEvents.ENTITY_ITEM_PICKUP,
                  SoundCategory.NEUTRAL,
                  0.2F,
                  ((this.targetEntity.world.rand.nextFloat() - this.targetEntity.world.rand.nextFloat()) * 0.7F + 1.0F)
                     * 2.0F
               );
         }
      }
   }
}
