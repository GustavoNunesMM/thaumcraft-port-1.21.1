package thaumcraft.api.casters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;

/** The implicit first node: the caster's own position and look direction (or what a projectile etc. passed on). */
public class FocusMediumRoot extends FocusMedium {
   Trajectory[] trajectories;
   HitResult[] targets;

   public FocusMediumRoot() {
   }

   public FocusMediumRoot(Trajectory[] trajectories, HitResult[] targets) {
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
   public EnumSupplyType[] willSupply() {
      return new EnumSupplyType[]{EnumSupplyType.TARGET, EnumSupplyType.TRAJECTORY};
   }

   @Override
   public HitResult[] supplyTargets() {
      return targets;
   }

   @Override
   public Trajectory[] supplyTrajectories() {
      return trajectories;
   }

   public void setupFromCaster(LivingEntity caster) {
      trajectories = new Trajectory[]{new Trajectory(source(caster), caster.getLookAngle())};
      targets = new HitResult[]{new EntityHitResult(caster)};
   }

   public void setupFromCasterToTarget(LivingEntity caster, Entity target, double offset) {
      Vec3 sv = source(caster);
      Vec3 lv = new Vec3(target.getX() - sv.x, target.getBoundingBox().minY + target.getBbHeight() / 2.0F - sv.y + offset, target.getZ() - sv.z);
      trajectories = new Trajectory[]{new Trajectory(sv, lv.normalize())};
      targets = new HitResult[]{new EntityHitResult(caster)};
   }

   public void setupFromCasterToTargetLoc(LivingEntity caster, double x, double y, double z) {
      Vec3 sv = source(caster);
      trajectories = new Trajectory[]{new Trajectory(sv, new Vec3(x - sv.x, y - sv.y, z - sv.z).normalize())};
      targets = new HitResult[]{new EntityHitResult(caster)};
   }

   private static Vec3 source(LivingEntity e) {
      return e.position().add(0.0, e.getEyeHeight() - 0.1F, 0.0);
   }

   @Override
   public Aspect getAspect() {
      return null;
   }
}
