package thaumcraft.common.golems.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import thaumcraft.common.golems.OwnedConstructEntity;

/** Attack whatever hurt the owner. */
public class AIOwnerHurtByTarget extends TargetGoal {
   private final OwnedConstructEntity construct;
   private LivingEntity theOwnerAttacker;
   private int timestamp;

   public AIOwnerHurtByTarget(OwnedConstructEntity construct) {
      super(construct, false);
      this.construct = construct;
      setFlags(EnumSet.of(Flag.TARGET));
   }

   @Override
   public boolean canUse() {
      if (!construct.isOwned()) return false;
      LivingEntity owner = construct.getOwnerEntity();
      if (owner == null) return false;
      theOwnerAttacker = owner.getLastHurtByMob();
      return owner.getLastHurtByMobTimestamp() != timestamp && canAttack(theOwnerAttacker, TargetingConditions.DEFAULT);
   }

   @Override
   public void start() {
      mob.setTarget(theOwnerAttacker);
      LivingEntity owner = construct.getOwnerEntity();
      if (owner != null) timestamp = owner.getLastHurtByMobTimestamp();
      super.start();
   }
}
