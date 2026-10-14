package thaumcraft.common.entities.ai.pech;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import thaumcraft.common.entities.monster.PechEntity;

/** AINearestAttackableTargetPech: only an angry pech looks for players to attack. */
public class PechNearestAttackableTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
   public PechNearestAttackableTargetGoal(PechEntity pech, Class<T> target, boolean mustSee) {
      super(pech, target, mustSee);
   }

   @Override
   public boolean canUse() {
      return mob instanceof PechEntity p && p.getAnger() == 0 ? false : super.canUse();
   }
}
