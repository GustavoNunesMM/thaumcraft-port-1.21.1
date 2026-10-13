package thaumcraft.common.golems.parts;

import net.minecraft.world.entity.LivingEntity;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.common.golems.GolemClientHooks;

/** The levitator's exhaust under a flying golem. */
public class GolemLegLevitator implements GolemLeg.ILegFunction {
   @Override
   public void onUpdateTick(IGolemAPI golem) {
      LivingEntity e = golem.getGolemEntity();
      if (golem.getGolemWorld().isClientSide && (!e.onGround() || e.tickCount % 5 == 0)) GolemClientHooks.flyParticles.accept(e);
   }
}
