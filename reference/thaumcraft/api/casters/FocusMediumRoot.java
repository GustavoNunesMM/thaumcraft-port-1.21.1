package thaumcraft.api.casters;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import thaumcraft.api.aspects.Aspect;

public class FocusMediumRoot extends FocusMedium {
   Trajectory[] trajectories = null;
   RayTraceResult[] targets = null;

   public FocusMediumRoot() {
   }

   public FocusMediumRoot(Trajectory[] trajectories, RayTraceResult[] targets) {
      this.trajectories = trajectories;
      this.targets = targets;
   }

   @Override
   public String getResearch() {
      return "BASEAUROMANCY";
   }

   @Override
   public String getKey() {
      return "ROOT";
   }

   @Override
   public int getComplexity() {
      return 0;
   }

   @Override
   public FocusNode.EnumSupplyType[] willSupply() {
      return new FocusNode.EnumSupplyType[]{FocusNode.EnumSupplyType.TARGET, FocusNode.EnumSupplyType.TRAJECTORY};
   }

   @Override
   public RayTraceResult[] supplyTargets() {
      return this.targets;
   }

   @Override
   public Trajectory[] supplyTrajectories() {
      return this.trajectories;
   }

   public void setupFromCaster(EntityLivingBase caster) {
      this.trajectories = new Trajectory[]{new Trajectory(this.generateSourceVector(caster), caster.getLookVec())};
      this.targets = new RayTraceResult[]{new RayTraceResult(caster)};
   }

   public void setupFromCasterToTarget(EntityLivingBase caster, Entity target, double offset) {
      Vec3d sv = this.generateSourceVector(caster);
      double d0 = target.posX - sv.x;
      double d1 = target.getEntityBoundingBox().minY + target.height / 2.0F - sv.y;
      double d2 = target.posZ - sv.z;
      Vec3d lv = new Vec3d(d0, d1 + offset, d2);
      this.trajectories = new Trajectory[]{new Trajectory(sv, lv.normalize())};
      this.targets = new RayTraceResult[]{new RayTraceResult(caster)};
   }

   public void setupFromCasterToTargetLoc(EntityLivingBase caster, double x, double y, double z) {
      Vec3d sv = this.generateSourceVector(caster);
      double d0 = x - sv.x;
      double d1 = y - sv.y;
      double d2 = z - sv.z;
      Vec3d lv = new Vec3d(d0, d1, d2);
      this.trajectories = new Trajectory[]{new Trajectory(sv, lv.normalize())};
      this.targets = new RayTraceResult[]{new RayTraceResult(caster)};
   }

   private Vec3d generateSourceVector(EntityLivingBase e) {
      Vec3d v = e.getPositionVector();
      return v.add(0.0, e.getEyeHeight() - 0.1F, 0.0);
   }

   @Override
   public Aspect getAspect() {
      return null;
   }
}
