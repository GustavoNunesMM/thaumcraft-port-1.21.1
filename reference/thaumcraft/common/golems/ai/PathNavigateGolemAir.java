package thaumcraft.common.golems.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;

public class PathNavigateGolemAir extends PathNavigate {
   public PathNavigateGolemAir(EntityLiving p_i45873_1_, World worldIn) {
      super(p_i45873_1_, worldIn);
   }

   protected PathFinder getPathFinder() {
      return new PathFinder(new FlightNodeProcessor());
   }

   protected boolean canNavigate() {
      return true;
   }

   protected Vec3d getEntityPosition() {
      return new Vec3d(
         this.entity.posX, this.entity.posY + this.entity.height * 0.5, this.entity.posZ
      );
   }

   protected void pathFollow() {
      Vec3d vec3 = this.getEntityPosition();
      float f = this.entity.width * this.entity.width;
      byte b0 = 6;
      if (vec3.squareDistanceTo(this.currentPath.getVectorFromIndex(this.entity, this.currentPath.getCurrentPathIndex())) < f) {
         this.currentPath.incrementPathIndex();
      }

      for (int i = Math.min(this.currentPath.getCurrentPathIndex() + b0, this.currentPath.getCurrentPathLength() - 1); i > this.currentPath.getCurrentPathIndex(); i--) {
         Vec3d vec31 = this.currentPath.getVectorFromIndex(this.entity, i);
         if (vec31.squareDistanceTo(vec3) <= 36.0 && this.isDirectPathBetweenPoints(vec3, vec31, 0, 0, 0)) {
            this.currentPath.setCurrentPathIndex(i);
            break;
         }
      }

      this.checkForStuck(vec3);
   }

   protected void removeSunnyPath() {
      super.removeSunnyPath();
   }

   protected boolean isDirectPathBetweenPoints(Vec3d posVec31, Vec3d posVec32, int sizeX, int sizeY, int sizeZ) {
      RayTraceResult RayTraceResult = this.world
         .rayTraceBlocks(
            posVec31,
            new Vec3d(posVec32.x, posVec32.y + this.entity.height * 0.5, posVec32.z),
            false,
            true,
            false
         );
      return RayTraceResult == null || RayTraceResult.typeOfHit == Type.MISS;
   }
}
