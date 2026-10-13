package thaumcraft.common.items.casters.foci;

import java.util.ArrayList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.codechicken.lib.raytracer.RayTracer;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusEffect;
import thaumcraft.common.lib.utils.EntityUtils;

public class FocusMediumTouch extends FocusMedium {
   @Override
   public String getResearch() {
      return "BASEAUROMANCY";
   }

   @Override
   public String getKey() {
      return "thaumcraft.TOUCH";
   }

   @Override
   public int getComplexity() {
      return 2;
   }

   @Override
   public FocusNode.EnumSupplyType[] willSupply() {
      return new FocusNode.EnumSupplyType[]{FocusNode.EnumSupplyType.TRAJECTORY, FocusNode.EnumSupplyType.TARGET};
   }

   @Override
   public Aspect getAspect() {
      return Aspect.AVERSION;
   }

   @Override
   public Trajectory[] supplyTrajectories() {
      if (this.getParent() == null) {
         return new Trajectory[0];
      } else {
         ArrayList<Trajectory> trajectories = new ArrayList<>();
         double range = this instanceof FocusMediumBolt ? 16.0 : RayTracer.getBlockReachDistance((EntityPlayer)this.getPackage().getCaster());

         for (Trajectory sT : this.getParent().supplyTrajectories()) {
            Vec3d end = sT.direction.normalize();
            RayTraceResult ray = EntityUtils.getPointedEntityRay(
               this.getPackage().world, this.getPackage().getCaster(), sT.source, end, 0.25, range, 0.25F, false
            );
            if (ray == null) {
               Vec3d var10 = end.scale(range);
               end = var10.add(sT.source);
               ray = this.getPackage().world.rayTraceBlocks(sT.source, end);
               if (ray != null) {
                  end = ray.hitVec;
               }
            } else if (ray.entityHit != null) {
               end = end.scale(sT.source.distanceTo(ray.entityHit.getPositionVector()));
               end = end.add(sT.source);
            }

            trajectories.add(new Trajectory(end, sT.direction.normalize()));
         }

         return trajectories.toArray(new Trajectory[0]);
      }
   }

   @Override
   public RayTraceResult[] supplyTargets() {
      if (this.getParent() != null && this.getPackage().getCaster() instanceof EntityPlayer) {
         ArrayList<RayTraceResult> targets = new ArrayList<>();
         double range = this instanceof FocusMediumBolt ? 16.0 : RayTracer.getBlockReachDistance((EntityPlayer)this.getPackage().getCaster());

         for (Trajectory sT : this.getParent().supplyTrajectories()) {
            Vec3d end = sT.direction.normalize();
            RayTraceResult ray = EntityUtils.getPointedEntityRay(
               this.getPackage().world, this.getPackage().getCaster(), sT.source, end, 0.25, range, 0.25F, false
            );
            if (ray == null) {
               end = end.scale(range);
               end = end.add(sT.source);
               ray = this.getPackage().world.rayTraceBlocks(sT.source, end);
            }

            if (ray != null) {
               targets.add(ray);
            }
         }

         return targets.toArray(new RayTraceResult[0]);
      } else {
         return new RayTraceResult[0];
      }
   }

   @Override
   public boolean execute(Trajectory trajectory) {
      FocusEffect[] fe = this.getPackage().getFocusEffects();
      if (fe != null && fe.length > 0) {
         String[] effects = new String[fe.length];

         for (int a = 0; a < fe.length; a++) {
            effects[a] = fe[a].getKey();
         }

         PacketHandler.INSTANCE
            .sendToAllAround(
               new PacketFXFocusEffect(
                  (float)trajectory.source.x,
                  (float)trajectory.source.y,
                  (float)trajectory.source.z,
                  (float)trajectory.direction.x / 2.0F,
                  (float)trajectory.direction.y / 2.0F,
                  (float)trajectory.direction.z / 2.0F,
                  effects
               ),
               new TargetPoint(
                  this.getPackage().world.provider.getDimension(),
                  (float)trajectory.source.x,
                  (float)trajectory.source.y,
                  (float)trajectory.source.z,
                  64.0
               )
            );
      }

      return true;
   }
}
