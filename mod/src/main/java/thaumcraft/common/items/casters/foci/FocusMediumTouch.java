package thaumcraft.common.items.casters.foci;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.network.FocusFXPayload;

/** Touch: whatever the caster is looking at within reach. */
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
   public EnumSupplyType[] willSupply() {
      return new EnumSupplyType[]{EnumSupplyType.TRAJECTORY, EnumSupplyType.TARGET};
   }

   @Override
   public Aspect getAspect() {
      return Aspect.AVERSION;
   }

   protected double range() {
      LivingEntity c = getPackage().getCaster();
      return c instanceof Player p ? p.blockInteractionRange() : 4.5;
   }

   /** The ray of one trajectory: the entity pointed at, else the block, else null. */
   protected HitResult trace(Trajectory sT, double range) {
      Vec3 dir = sT.direction.normalize();
      HitResult ray = FocusUtils.getPointedEntityRay(getPackage().world, getPackage().getCaster(), sT.source, dir, 0.25, range, 0.25F, false);
      if (ray == null) ray = FocusUtils.rayTraceBlocks(getPackage().world, sT.source, dir.scale(range).add(sT.source), getPackage().getCaster());
      return ray;
   }

   @Override
   public Trajectory[] supplyTrajectories() {
      if (getParent() == null || getParent().supplyTrajectories() == null) return new Trajectory[0];
      List<Trajectory> trajectories = new ArrayList<>();
      double range = range();
      for (Trajectory sT : getParent().supplyTrajectories()) {
         Vec3 end = sT.direction.normalize();
         HitResult ray = trace(sT, range);
         if (ray == null) end = end.scale(range).add(sT.source);
         else if (ray.getType() == HitResult.Type.ENTITY) end = end.scale(sT.source.distanceTo(((net.minecraft.world.phys.EntityHitResult) ray).getEntity().position())).add(sT.source);
         else end = ray.getLocation();
         trajectories.add(new Trajectory(end, sT.direction.normalize()));
      }
      return trajectories.toArray(new Trajectory[0]);
   }

   @Override
   public HitResult[] supplyTargets() {
      if (getParent() == null || !(getPackage().getCaster() instanceof Player) || getParent().supplyTrajectories() == null) return new HitResult[0];
      List<HitResult> targets = new ArrayList<>();
      double range = range();
      for (Trajectory sT : getParent().supplyTrajectories()) {
         HitResult ray = trace(sT, range);
         if (ray != null) targets.add(ray);
      }
      return targets.toArray(new HitResult[0]);
   }

   @Override
   public boolean execute(Trajectory trajectory) {
      FocusEffect[] fe = getPackage().getFocusEffects();
      if (fe != null && fe.length > 0) {
         List<String> effects = new ArrayList<>();
         for (FocusEffect e : fe) effects.add(e.getKey());
         FocusFXPayload.send(getPackage().world, new FocusFXPayload(FocusFXPayload.TRAIL, trajectory.source, trajectory.direction.scale(0.5), effects, 0, 0));
      }
      return true;
   }
}
