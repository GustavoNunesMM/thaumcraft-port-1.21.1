package thaumcraft.common.golems.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import thaumcraft.common.golems.OwnedConstructEntity;

/** Attack whatever the owner attacked. */
public class AIOwnerHurtTarget extends TargetGoal {
   private final OwnedConstructEntity construct;
   private LivingEntity theTarget;
   private int timestamp;

   public AIOwnerHurtTarget(OwnedConstructEntity construct) {
      super(construct, false);
      this.construct = construct;
      setFlags(EnumSet.of(Flag.TARGET));
   }

   @Override
   public boolean canUse() {
      if (!construct.isOwned()) return false;
      LivingEntity owner = construct.getOwnerEntity();
      if (owner == null) return false;
      theTarget = owner.getLastHurtMob();
      return owner.getLastHurtMobTimestamp() != timestamp && canAttack(theTarget, TargetingConditions.DEFAULT);
   }

   @Override
   public void start() {
      mob.setTarget(theTarget);
      LivingEntity owner = construct.getOwnerEntity();
      if (owner != null) timestamp = owner.getLastHurtMobTimestamp();
      super.start();
   }
}
