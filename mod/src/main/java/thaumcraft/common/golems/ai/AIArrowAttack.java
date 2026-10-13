package thaumcraft.common.golems.ai;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;

/** Ranged attack: keep the target in sight and fire at intervals that grow with the distance. */
public class AIArrowAttack extends Goal {
   private final Mob entityHost;
   private final RangedAttackMob rangedAttackEntityHost;
   private int rangedAttackTime = -1;
   private final double entityMoveSpeed;
   private int seeTime;
   private final int attackIntervalMin;
   private final int maxRangedAttackTime;
   private final float attackRadius;
   private final float maxAttackDistance;

   public AIArrowAttack(RangedAttackMob attacker, double movespeed, int intervalMin, int maxAttackTime, float maxAttackDistanceIn) {
      if (!(attacker instanceof Mob mob)) throw new IllegalArgumentException("ArrowAttackGoal requires Mob implements RangedAttackMob");
      this.rangedAttackEntityHost = attacker;
      this.entityHost = mob;
      this.entityMoveSpeed = movespeed;
      this.attackIntervalMin = intervalMin;
      this.maxRangedAttackTime = maxAttackTime;
      this.attackRadius = maxAttackDistanceIn;
      this.maxAttackDistance = maxAttackDistanceIn * maxAttackDistanceIn;
      setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
   }

   @Override
   public boolean canUse() {
      return entityHost.getTarget() != null;
   }

   @Override
   public boolean canContinueToUse() {
      return canUse() || !entityHost.getNavigation().isDone();
   }

   @Override
   public void stop() {
      seeTime = 0;
      rangedAttackTime = -1;
   }

   @Override
   public boolean requiresUpdateEveryTick() {
      return true;
   }

   @Override
   public void tick() {
      LivingEntity target = entityHost.getTarget();
      if (target == null) return;
      double d0 = entityHost.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
      boolean flag = entityHost.getSensing().hasLineOfSight(target);
      seeTime = flag ? seeTime + 1 : 0;
      if (d0 <= maxAttackDistance && seeTime >= 20) {
         entityHost.getNavigation().stop();
      } else {
         entityHost.getNavigation().moveTo(target, entityMoveSpeed);
      }
      entityHost.getLookControl().setLookAt(target, 10.0F, 30.0F);
      if (--rangedAttackTime == 0) {
         if (d0 > maxAttackDistance || !flag) return;
         float f = (float) Math.sqrt(d0) / attackRadius;
         rangedAttackEntityHost.performRangedAttack(target, Mth.clamp(f, 0.1F, 1.0F));
         rangedAttackTime = Mth.floor(f * (maxRangedAttackTime - attackIntervalMin) + attackIntervalMin);
      } else if (rangedAttackTime < 0) {
         float f2 = (float) Math.sqrt(d0) / attackRadius;
         rangedAttackTime = Mth.floor(f2 * (maxRangedAttackTime - attackIntervalMin) + attackIntervalMin);
      }
   }
}
