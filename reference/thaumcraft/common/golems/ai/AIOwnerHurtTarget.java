package thaumcraft.common.golems.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;

public class AIOwnerHurtTarget extends EntityAITarget {
   EntityOwnedConstruct theEntityTameable;
   EntityLivingBase theTarget;
   private int timestamp;

   public AIOwnerHurtTarget(EntityOwnedConstruct p_i1668_1_) {
      super(p_i1668_1_, false);
      this.theEntityTameable = p_i1668_1_;
      this.setMutexBits(1);
   }

   public boolean shouldExecute() {
      if (!this.theEntityTameable.isOwned()) {
         return false;
      } else {
         EntityLivingBase entitylivingbase = this.theEntityTameable.getOwnerEntity();
         if (entitylivingbase == null) {
            return false;
         } else {
            this.theTarget = entitylivingbase.getLastAttackedEntity();
            int i = entitylivingbase.getLastAttackedEntityTime();
            return i != this.timestamp && this.isSuitableTarget(this.theTarget, false);
         }
      }
   }

   public void startExecuting() {
      this.taskOwner.setAttackTarget(this.theTarget);
      EntityLivingBase entitylivingbase = this.theEntityTameable.getOwnerEntity();
      if (entitylivingbase != null) {
         this.timestamp = entitylivingbase.getLastAttackedEntityTime();
      }

      super.startExecuting();
   }
}
