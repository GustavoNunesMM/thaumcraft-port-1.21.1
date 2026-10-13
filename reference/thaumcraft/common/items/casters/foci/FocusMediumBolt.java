package thaumcraft.common.items.casters.foci;

import java.awt.Color;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXZap;
import thaumcraft.common.lib.utils.EntityUtils;

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
   public boolean execute(Trajectory trajectory) {
      float range = 16.0F;
      Vec3d end = trajectory.direction.normalize();
      RayTraceResult ray = EntityUtils.getPointedEntityRay(
         this.getPackage().world, this.getPackage().getCaster(), trajectory.source, end, 0.25, range, 0.25F, false
      );
      if (ray == null) {
         Vec3d var13 = end.scale(range);
         end = var13.add(trajectory.source);
         ray = this.getPackage().world.rayTraceBlocks(trajectory.source, end);
         if (ray != null) {
            end = ray.hitVec;
         }
      } else if (ray.entityHit != null) {
         end = end.scale(trajectory.source.distanceTo(ray.entityHit.getPositionVector()));
         end = end.add(trajectory.source);
      }

      int r = 0;
      int g = 0;
      int b = 0;

      for (FocusEffect ef : this.getPackage().getFocusEffects()) {
         Color c = new Color(FocusEngine.getElementColor(ef.getKey()));
         r += c.getRed();
         g += c.getGreen();
         b += c.getBlue();
      }

      r /= this.getPackage().getFocusEffects().length;
      g /= this.getPackage().getFocusEffects().length;
      b /= this.getPackage().getFocusEffects().length;
      Color c = new Color(r, g, b);
      PacketHandler.INSTANCE
         .sendToAllAround(
            new PacketFXZap(trajectory.source, end, c.getRGB(), this.getPackage().getPower() * 0.66F),
            new TargetPoint(
               this.getPackage().world.provider.getDimension(),
               trajectory.source.x,
               trajectory.source.y,
               trajectory.source.z,
               64.0
            )
         );
      return true;
   }
}
