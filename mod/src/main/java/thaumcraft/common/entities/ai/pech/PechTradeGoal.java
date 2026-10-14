package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.monster.PechEntity;

/** AIPechTradePlayer: a tamed pech stands still while a player trades with it. */
public class PechTradeGoal extends Goal {
   private final PechEntity pech;

   public PechTradeGoal(PechEntity pech) {
      this.pech = pech;
      setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
   }

   @Override
   public boolean canUse() {
      if (!pech.isAlive() || pech.isInWater() || !pech.isTamed() || !pech.onGround() || pech.hurtMarked) return false;
      return pech.trading;
   }

   @Override
   public void start() {
      pech.getNavigation().stop();
   }

   @Override
   public void stop() {
      pech.trading = false;
   }
}
