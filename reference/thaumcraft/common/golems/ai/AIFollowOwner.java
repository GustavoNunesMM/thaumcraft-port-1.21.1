package thaumcraft.common.golems.ai;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;

public class AIFollowOwner extends EntityAIBase {
   private EntityOwnedConstruct thePet;
   private EntityLivingBase theOwner;
   World theWorld;
   private double followSpeed;
   private PathNavigate petPathfinder;
   private int timeToRecalcPath;
   float maxDist;
   float minDist;
   private float oldWaterCost;

   public AIFollowOwner(EntityOwnedConstruct p_i1625_1_, double p_i1625_2_, float p_i1625_4_, float p_i1625_5_) {
      this.thePet = p_i1625_1_;
      this.theWorld = p_i1625_1_.world;
      this.followSpeed = p_i1625_2_;
      this.petPathfinder = p_i1625_1_.getNavigator();
      this.minDist = p_i1625_4_;
      this.maxDist = p_i1625_5_;
      this.setMutexBits(3);
      if (!(p_i1625_1_.getNavigator() instanceof PathNavigateGround) && !(p_i1625_1_.getNavigator() instanceof PathNavigateGolemAir)) {
         throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
      }
   }

   public boolean shouldExecute() {
      EntityLivingBase entitylivingbase = this.thePet.getOwnerEntity();
      if (entitylivingbase == null) {
         return false;
      } else if (this.thePet.getDistanceSq(entitylivingbase) < this.minDist * this.minDist) {
         return false;
      } else {
         this.theOwner = entitylivingbase;
         return true;
      }
   }

   public boolean shouldContinueExecuting() {
      return !this.petPathfinder.noPath() && this.thePet.getDistanceSq(this.theOwner) > this.maxDist * this.maxDist;
   }

   public void startExecuting() {
      this.timeToRecalcPath = 0;
      this.oldWaterCost = this.thePet.getPathPriority(PathNodeType.WATER);
      this.thePet.setPathPriority(PathNodeType.WATER, 0.0F);
   }

   public void resetTask() {
      this.theOwner = null;
      this.petPathfinder.clearPath();
      this.thePet.setPathPriority(PathNodeType.WATER, this.oldWaterCost);
   }

   private boolean func_181065_a(BlockPos p_181065_1_) {
      IBlockState iblockstate = this.theWorld.getBlockState(p_181065_1_);
      Block block = iblockstate.getBlock();
      return block == Blocks.AIR ? true : !iblockstate.isFullCube();
   }

   public void updateTask() {
      this.thePet.getLookHelper().setLookPositionWithEntity(this.theOwner, 10.0F, this.thePet.getVerticalFaceSpeed());
      if (--this.timeToRecalcPath <= 0) {
         this.timeToRecalcPath = 10;
         if (!this.petPathfinder.tryMoveToEntityLiving(this.theOwner, this.followSpeed)
            && !this.thePet.getLeashed()
            && this.thePet.getDistanceSq(this.theOwner) >= 144.0) {
            int i = MathHelper.floor(this.theOwner.posX) - 2;
            int j = MathHelper.floor(this.theOwner.posZ) - 2;
            int k = MathHelper.floor(this.theOwner.getEntityBoundingBox().minY);

            for (int l = 0; l <= 4; l++) {
               for (int i1 = 0; i1 <= 4; i1++) {
                  if ((l < 1 || i1 < 1 || l > 3 || i1 > 3)
                     && this.theWorld.getBlockState(new BlockPos(i + l, k - 1, j + i1)).isFullCube()
                     && this.func_181065_a(new BlockPos(i + l, k, j + i1))
                     && this.func_181065_a(new BlockPos(i + l, k + 1, j + i1))) {
                     this.thePet.setLocationAndAngles(i + l + 0.5F, k, j + i1 + 0.5F, this.thePet.rotationYaw, this.thePet.rotationPitch);
                     this.petPathfinder.clearPath();
                     return;
                  }
               }
            }
         }
      }
   }
}
