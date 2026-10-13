package thaumcraft.common.items.casters.foci;

import java.util.List;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.network.FocusFXPayload;

/** Bolt: like touch with a 16 block reach, drawn as an arc of lightning. */
public class FocusMediumBolt extends FocusMediumTouch {
   @Override
   public String getResearch() {
      return "FOCUSBOLT";
   }

   @Override
   public String getKey() {
      return "thaumcraft.BOLT";
   }

   @Override
   public int getComplexity() {
      return 5;
   }

   @Override
   public Aspect getAspect() {
      return Aspect.ENERGY;
   }

   @Override
   protected double range() {
      return 16.0;
   }

   @Override
   public boolean execute(Trajectory trajectory) {
      float range = 16.0F;
      Vec3 end = trajectory.direction.normalize();
      HitResult ray = trace(trajectory, range);
      if (ray == null) end = end.scale(range).add(trajectory.source);
      else if (ray instanceof EntityHitResult ehr) end = end.scale(trajectory.source.distanceTo(ehr.getEntity().position())).add(trajectory.source);
      else end = ray.getLocation();
      int r = 0, g = 0, b = 0;
      FocusEffect[] effects = getPackage().getFocusEffects();
      for (FocusEffect ef : effects) {
         int c = FocusEngine.getElementColor(ef.getKey());
         r += c >> 16 & 255;
         g += c >> 8 & 255;
         b += c & 255;
      }
      if (effects.length > 0) {
         r /= effects.length;
         g /= effects.length;
         b /= effects.length;
      }
      FocusFXPayload.send(getPackage().world, new FocusFXPayload(FocusFXPayload.ZAP, trajectory.source, end, List.of(), r << 16 | g << 8 | b,
         getPackage().getPower() * 0.66F));
      return true;
   }
}
