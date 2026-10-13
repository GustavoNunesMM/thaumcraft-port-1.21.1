package thaumcraft.common.golems.ai;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.StringUtils;

public class AINearestValidTarget extends EntityAITarget {
   protected final Class targetClass;
   private final int targetChance;
   protected final net.minecraft.entity.ai.EntityAINearestAttackableTarget.Sorter theNearestAttackableTargetSorter;
   protected Predicate targetEntitySelector;
   protected EntityLivingBase targetEntity;
   private int targetUnseenTicks;

   public AINearestValidTarget(EntityCreature p_i45878_1_, Class p_i45878_2_, boolean p_i45878_3_) {
      this(p_i45878_1_, p_i45878_2_, p_i45878_3_, false);
   }

   public AINearestValidTarget(EntityCreature p_i45879_1_, Class p_i45879_2_, boolean p_i45879_3_, boolean p_i45879_4_) {
      this(p_i45879_1_, p_i45879_2_, 10, p_i45879_3_, p_i45879_4_, (Predicate)null);
   }

   public AINearestValidTarget(
      EntityCreature p_i45880_1_, Class p_i45880_2_, int p_i45880_3_, boolean p_i45880_4_, boolean p_i45880_5_, final Predicate tselector
   ) {
      super(p_i45880_1_, p_i45880_4_, p_i45880_5_);
      this.targetClass = p_i45880_2_;
      this.targetChance = p_i45880_3_;
      this.theNearestAttackableTargetSorter = new net.minecraft.entity.ai.EntityAINearestAttackableTarget.Sorter(p_i45880_1_);
      this.setMutexBits(1);
      this.targetEntitySelector = new Predicate() {
         private static final String __OBFID = "CL_00001621";

         public boolean applySelection(EntityLivingBase entity) {
            if (tselector != null && !tselector.apply(entity)) {
               return false;
            } else {
               if (entity instanceof EntityPlayer) {
                  double d0 = AINearestValidTarget.this.getTargetDistance();
                  if (entity.isSneaking()) {
                     d0 *= 0.8F;
                  }

                  if (entity.isInvisible()) {
                     float f = ((EntityPlayer)entity).getArmorVisibility();
                     if (f < 0.1F) {
                        f = 0.1F;
                     }

                     d0 *= 0.7F * f;
                  }

                  if (entity.getDistance(AINearestValidTarget.this.taskOwner) > d0) {
                     return false;
                  }
               }

               return AINearestValidTarget.this.isSuitableTarget(entity, false);
            }
         }

         public boolean apply(Object p_apply_1_) {
            return this.applySelection((EntityLivingBase)p_apply_1_);
         }
      };
   }

   public boolean shouldContinueExecuting() {
      EntityLivingBase entitylivingbase = this.taskOwner.getAttackTarget();
      if (entitylivingbase == null) {
         return false;
      } else if (!entitylivingbase.isEntityAlive()) {
         return false;
      } else {
         Team team = this.taskOwner.getTeam();
         Team team1 = entitylivingbase.getTeam();
         if (team != null && team1 == team && !((ITargets)this.taskOwner).getTargetFriendly()) {
            return false;
         } else if (team != null && team1 != team && ((ITargets)this.taskOwner).getTargetFriendly()) {
            return false;
         } else {
            double d0 = this.getTargetDistance();
            if (this.taskOwner.getDistanceSq(entitylivingbase) > d0 * d0) {
               return false;
            } else {
               if (this.shouldCheckSight) {
                  if (this.taskOwner.getEntitySenses().canSee(entitylivingbase)) {
                     this.targetUnseenTicks = 0;
                  } else if (++this.targetUnseenTicks > 60) {
                     return false;
                  }
               }

               return true;
            }
         }
      }
   }

   protected boolean isSuitableTarget(EntityLivingBase target, boolean includeInvincibles) {
      return !this.isGoodTarget(this.taskOwner, target, includeInvincibles, this.shouldCheckSight)
         ? false
         : this.taskOwner.isWithinHomeDistanceFromPosition(new BlockPos(target));
   }

   private boolean isGoodTarget(EntityLiving attacker, EntityLivingBase posTar, boolean includeInvincibles, boolean checkSight) {
      if (posTar == null) {
         return false;
      } else if (posTar == attacker) {
         return false;
      } else if (!posTar.isEntityAlive()) {
         return false;
      } else if (!attacker.canAttackClass(posTar.getClass())) {
         return false;
      } else {
         Team team = attacker.getTeam();
         Team team1 = posTar.getTeam();
         if (team != null && team1 == team && !((ITargets)attacker).getTargetFriendly()) {
            return false;
         } else if (team != null && team1 != team && ((ITargets)attacker).getTargetFriendly()) {
            return false;
         } else {
            if (attacker instanceof IEntityOwnable && StringUtils.isNotEmpty(((IEntityOwnable)attacker).getOwnerId().toString())) {
               if (posTar instanceof IEntityOwnable
                  && ((IEntityOwnable)attacker).getOwnerId().equals(((IEntityOwnable)posTar).getOwnerId())
                  && !((ITargets)attacker).getTargetFriendly()) {
                  return false;
               }

               if (!(posTar instanceof IEntityOwnable) && !(posTar instanceof EntityPlayer) && ((ITargets)attacker).getTargetFriendly()) {
                  return false;
               }

               if (posTar instanceof IEntityOwnable
                  && !((IEntityOwnable)attacker).getOwnerId().equals(((IEntityOwnable)posTar).getOwnerId())
                  && ((ITargets)attacker).getTargetFriendly()) {
                  return false;
               }

               if (posTar == ((IEntityOwnable)attacker).getOwner() && !((ITargets)attacker).getTargetFriendly()) {
                  return false;
               }
            } else if (posTar instanceof EntityPlayer
               && !includeInvincibles
               && ((EntityPlayer)posTar).capabilities.disableDamage
               && !((ITargets)attacker).getTargetFriendly()) {
               return false;
            }

            return !checkSight || attacker.getEntitySenses().canSee(posTar);
         }
      }
   }

   public boolean shouldExecute() {
      if (this.targetChance > 0 && this.taskOwner.getRNG().nextInt(this.targetChance) != 0) {
         return false;
      } else {
         double d0 = this.getTargetDistance();
         List list = this.taskOwner
            .world
            .getEntitiesWithinAABB(
               this.targetClass,
               this.taskOwner.getEntityBoundingBox().grow(d0, 4.0, d0),
               Predicates.and(this.targetEntitySelector, EntitySelectors.NOT_SPECTATING)
            );
         Collections.sort(list, this.theNearestAttackableTargetSorter);
         if (list.isEmpty()) {
            return false;
         } else {
            this.targetEntity = (EntityLivingBase)list.get(0);
            return true;
         }
      }
   }

   public void startExecuting() {
      this.taskOwner.setAttackTarget(this.targetEntity);
      this.targetUnseenTicks = 0;
      super.startExecuting();
   }

   public class Sorter implements Comparator {
      private final Entity theEntity;
      private static final String __OBFID = "CL_00001622";

      public Sorter(Entity p_i1662_1_) {
         this.theEntity = p_i1662_1_;
      }

      public int compare(Entity p_compare_1_, Entity p_compare_2_) {
         double d0 = this.theEntity.getDistanceSq(p_compare_1_);
         double d1 = this.theEntity.getDistanceSq(p_compare_2_);
         return d0 < d1 ? -1 : (d0 > d1 ? 1 : 0);
      }

      @Override
      public int compare(Object p_compare_1_, Object p_compare_2_) {
         return this.compare((Entity)p_compare_1_, (Entity)p_compare_2_);
      }
   }
}
