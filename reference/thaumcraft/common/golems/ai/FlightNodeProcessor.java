package thaumcraft.common.golems.ai;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.IBlockAccess;

public class FlightNodeProcessor extends NodeProcessor {
   public PathPoint getPathPointToCoords(double x, double y, double z) {
      return this.openPoint(
         MathHelper.floor(x - this.entity.width / 2.0F),
         MathHelper.floor(y + 0.5),
         MathHelper.floor(z - this.entity.width / 2.0F)
      );
   }

   public PathPoint getStart() {
      return this.openPoint(
         MathHelper.floor(this.entity.getEntityBoundingBox().minX),
         MathHelper.floor(this.entity.getEntityBoundingBox().minY + 0.5),
         MathHelper.floor(this.entity.getEntityBoundingBox().minZ)
      );
   }

   public int findPathOptions(PathPoint[] pathOptions, PathPoint currentPoint, PathPoint targetPoint, float maxDistance) {
      int i = 0;

      for (EnumFacing enumfacing : EnumFacing.values()) {
         PathPoint pathpoint = this.getWaterNode(
            currentPoint.x + enumfacing.getXOffset(),
            currentPoint.y + enumfacing.getYOffset(),
            currentPoint.z + enumfacing.getZOffset()
         );
         if (pathpoint != null && !pathpoint.visited && pathpoint.distanceTo(targetPoint) < maxDistance) {
            pathOptions[i++] = pathpoint;
         }
      }

      return i;
   }

   public PathNodeType getPathNodeType(
      IBlockAccess blockaccessIn,
      int x,
      int y,
      int z,
      EntityLiving entitylivingIn,
      int xSize,
      int ySize,
      int zSize,
      boolean canBreakDoorsIn,
      boolean canEnterDoorsIn
   ) {
      return PathNodeType.WATER;
   }

   public PathNodeType getPathNodeType(IBlockAccess x, int y, int z, int z) {
      return PathNodeType.WATER;
   }

   private PathPoint getWaterNode(int p_186328_1_, int p_186328_2_, int p_186328_3_) {
      PathNodeType pathnodetype = this.isFree(p_186328_1_, p_186328_2_, p_186328_3_);
      return pathnodetype == PathNodeType.WALKABLE ? this.openPoint(p_186328_1_, p_186328_2_, p_186328_3_) : null;
   }

   private PathNodeType isFree(int p_186327_1_, int p_186327_2_, int p_186327_3_) {
      MutableBlockPos blockpos$mutableblockpos = new MutableBlockPos();

      for (int i = p_186327_1_; i < p_186327_1_ + this.entitySizeX; i++) {
         for (int j = p_186327_2_; j < p_186327_2_ + this.entitySizeY; j++) {
            for (int k = p_186327_3_; k < p_186327_3_ + this.entitySizeZ; k++) {
               IBlockState iblockstate = this.blockaccess.getBlockState(blockpos$mutableblockpos.setPos(i, j, k));
               if (!this.blockaccess.isAirBlock(blockpos$mutableblockpos.setPos(i, j, k))
                  && !iblockstate.getBlock().isPassable(this.blockaccess, blockpos$mutableblockpos.setPos(i, j, k))) {
                  return PathNodeType.BLOCKED;
               }
            }
         }
      }

      return PathNodeType.WALKABLE;
   }
}
