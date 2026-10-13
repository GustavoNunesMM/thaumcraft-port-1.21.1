package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.common.entities.monster.PechEntity;

/** AIPechItemEntityGoto: a pech walks to items lying around that it wants and stuffs them in its pack. */
public class PechItemGotoGoal extends Goal {
   private final PechEntity pech;
   @Nullable
   private ItemEntity targetEntity;
   private final float maxTargetDistance = 16.0F;
   private int count;
   private int failedPathFindingPenalty;

   public PechItemGotoGoal(PechEntity pech) {
      this.pech = pech;
      setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      if (--count > 0) return false;
      double range = Double.MAX_VALUE;
      targetEntity = null;
      for (Entity e : pech.level().getEntities(pech, pech.getBoundingBox().inflate(maxTargetDistance))) {
         if (e instanceof ItemEntity ie && !ie.getPersistentData().getBoolean(PechEntity.PECH_DROP) && pech.canPickup(ie.getItem())) {
            double distance = e.distanceToSqr(pech);
            if (distance < range && distance <= maxTargetDistance * maxTargetDistance) {
               range = distance;
               targetEntity = ie;
            }
         }
      }
      return targetEntity != null;
   }

   @Override
   public boolean canContinueToUse() {
      return targetEntity != null && targetEntity.isAlive() && !pech.getNavigation().isDone()
         && targetEntity.distanceToSqr(pech) < maxTargetDistance * maxTargetDistance;
   }

   @Override
   public void stop() {
      targetEntity = null;
   }

   private double speed() {
      return pech.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.5;
   }

   @Override
   public void start() {
      if (targetEntity != null) pech.getNavigation().moveTo(targetEntity, speed());
      count = 0;
   }

   @Override
   public void tick() {
      if (targetEntity == null) return;
      pech.getLookControl().setLookAt(targetEntity, 30.0F, 30.0F);
      if (pech.getSensing().hasLineOfSight(targetEntity) && --count <= 0) {
         count = failedPathFindingPenalty + 4 + pech.getRandom().nextInt(4);
         pech.getNavigation().moveTo(targetEntity, speed());
         Path path = pech.getNavigation().getPath();
         Node end = path == null ? null : path.getEndNode();
         if (end != null && targetEntity.distanceToSqr(end.x, end.y, end.z) < 1.0) {
            failedPathFindingPenalty = 0;
         } else {
            failedPathFindingPenalty += 10;
         }
      }
      double distance = pech.distanceToSqr(targetEntity.getX(), targetEntity.getBoundingBox().minY, targetEntity.getZ());
      if (distance <= 1.5) {
         count = 0;
         ItemStack in = targetEntity.getItem().copy();
         int am = in.getCount();
         ItemStack is = pech.pickupItem(in);
         if (!is.isEmpty()) {
            targetEntity.setItem(is);
         } else {
            targetEntity.discard();
         }
         if (is.isEmpty() || is.getCount() != am) {
            targetEntity.level().playSound(null, targetEntity.getX(), targetEntity.getY(), targetEntity.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL,
               0.2F, ((targetEntity.level().random.nextFloat() - targetEntity.level().random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
         }
      }
   }
}
