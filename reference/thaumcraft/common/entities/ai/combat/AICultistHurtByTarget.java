package thaumcraft.common.entities.ai.combat;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.util.math.AxisAlignedBB;
import thaumcraft.common.entities.monster.cult.EntityCultist;

public class AICultistHurtByTarget extends EntityAITarget {
   boolean entityCallsForHelp;
   private int revengeTimerOld;

   public AICultistHurtByTarget(EntityCreature owner, boolean callsHelp) {
      super(owner, false);
      this.entityCallsForHelp = callsHelp;
      this.setMutexBits(1);
   }

   public boolean shouldExecute() {
      int i = this.taskOwner.getRevengeTimer();
      EntityLivingBase entitylivingbase = this.taskOwner.getRevengeTarget();
      return i != this.revengeTimerOld && entitylivingbase != null && this.isSuitableTarget(entitylivingbase, false);
   }

   public void startExecuting() {
      this.taskOwner.setAttackTarget(this.taskOwner.getRevengeTarget());
      this.target = this.taskOwner.getAttackTarget();
      this.revengeTimerOld = this.taskOwner.getRevengeTimer();
      this.unseenMemoryTicks = 300;
      if (this.entityCallsForHelp) {
         this.alertOthers();
      }

      super.startExecuting();
   }

   protected void alertOthers() {
      double d0 = this.getTargetDistance();

      for (EntityCreature entitycreature : this.taskOwner
         .world
         .getEntitiesWithinAABB(
            EntityCultist.class,
            new AxisAlignedBB(
                  this.taskOwner.posX,
                  this.taskOwner.posY,
                  this.taskOwner.posZ,
                  this.taskOwner.posX + 1.0,
                  this.taskOwner.posY + 1.0,
                  this.taskOwner.posZ + 1.0
               )
               .grow(d0, 10.0, d0)
         )) {
         if (this.taskOwner != entitycreature
            && entitycreature.getAttackTarget() == null
            && (
               !(this.taskOwner instanceof EntityTameable)
                  || ((EntityTameable)this.taskOwner).getOwner() == ((EntityTameable)entitycreature).getOwner()
            )
            && !entitycreature.isOnSameTeam(this.taskOwner.getRevengeTarget())) {
            this.setEntityAttackTarget(entitycreature, this.taskOwner.getRevengeTarget());
         }
      }
   }

   protected void setEntityAttackTarget(EntityCreature creatureIn, EntityLivingBase entityLivingBaseIn) {
      creatureIn.setAttackTarget(entityLivingBaseIn);
   }
}
