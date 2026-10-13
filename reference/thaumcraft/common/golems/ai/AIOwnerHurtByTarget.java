package thaumcraft.common.golems.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;

public class AIOwnerHurtByTarget extends EntityAITarget {
   EntityOwnedConstruct theDefendingTameable;
   EntityLivingBase theOwnerAttacker;
   private int timestamp;

   public AIOwnerHurtByTarget(EntityOwnedConstruct p_i1667_1_) {
      super(p_i1667_1_, false);
      this.theDefendingTameable = p_i1667_1_;
      this.setMutexBits(1);
   }

   public boolean shouldExecute() {
      if (!this.theDefendingTameable.isOwned()) {
         return false;
      } else {
         EntityLivingBase entitylivingbase = this.theDefendingTameable.getOwnerEntity();
         if (entitylivingbase == null) {
            return false;
         } else {
            this.theOwnerAttacker = entitylivingbase.getRevengeTarget();
            int i = entitylivingbase.getRevengeTimer();
            return i != this.timestamp && this.isSuitableTarget(this.theOwnerAttacker, false);
         }
      }
   }

   public void startExecuting() {
      this.taskOwner.setAttackTarget(this.theOwnerAttacker);
      EntityLivingBase entitylivingbase = this.theDefendingTameable.getOwnerEntity();
      if (entitylivingbase != null) {
         this.timestamp = entitylivingbase.getRevengeTimer();
      }

      super.startExecuting();
   }
}
