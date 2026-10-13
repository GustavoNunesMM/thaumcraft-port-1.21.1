package thaumcraft.common.golems;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.codechicken.lib.raytracer.RayTracer;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.items.ItemTCBase;
import thaumcraft.common.lib.SoundsTC;

public class ItemGolemBell extends ItemTCBase implements ISealDisplayer {
   public ItemGolemBell() {
      super("golem_bell");
      this.setHasSubtypes(false);
      this.setMaxStackSize(1);
   }

   public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand) {
      playerIn.swingArm(hand);
      if (worldIn.isRemote) {
         playerIn.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.0F + worldIn.rand.nextFloat() * 0.1F);
      } else {
         RayTraceResult mop = RayTracer.retrace(playerIn);
         if (mop != null && (mop.typeOfHit == Type.BLOCK || mop.typeOfHit == Type.ENTITY)) {
            ISealEntity se = getSeal(playerIn);
            if (se != null) {
               if (playerIn.isSneaking()) {
                  SealHandler.removeSealEntity(playerIn.world, se.getSealPos(), false);
                  worldIn.playSound(null, se.getSealPos().pos, SoundsTC.zap, SoundCategory.BLOCKS, 0.5F, 1.0F);
               } else {
                  playerIn.openGui(
                     Thaumcraft.instance,
                     18,
                     playerIn.world,
                     se.getSealPos().pos.getX(),
                     se.getSealPos().pos.getY(),
                     se.getSealPos().pos.getZ()
                  );
               }
            }

            return new ActionResult(EnumActionResult.FAIL, playerIn.getHeldItem(hand));
         }

         if (playerIn.isSneaking() && ThaumcraftCapabilities.knowsResearch(playerIn, "GOLEMLOGISTICS")) {
            playerIn.openGui(
               Thaumcraft.instance, 20, playerIn.world, (int)playerIn.posX, (int)playerIn.posY, (int)playerIn.posZ
            );
            return new ActionResult(EnumActionResult.FAIL, playerIn.getHeldItem(hand));
         }
      }

      return super.onItemRightClick(worldIn, playerIn, hand);
   }

   public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
      player.swingArm(hand);
      if (!world.isRemote) {
         ISealEntity se = SealHandler.getSealEntity(world.provider.getDimension(), new SealPos(pos, side));
         if (se != null) {
            if (player.isSneaking()) {
               SealHandler.removeSealEntity(world, se.getSealPos(), false);
               world.playSound(null, pos, SoundsTC.zap, SoundCategory.BLOCKS, 0.5F, 1.0F);
            } else {
               player.openGui(Thaumcraft.instance, 18, world, pos.getX(), pos.getY(), pos.getZ());
            }

            return EnumActionResult.SUCCESS;
         }

         if (player.isSneaking() && ThaumcraftCapabilities.knowsResearch(player, "GOLEMLOGISTICS")) {
            player.openGui(Thaumcraft.instance, 20, world, pos.getX(), pos.getY(), pos.getZ());
            return EnumActionResult.SUCCESS;
         }
      }

      return EnumActionResult.PASS;
   }

   public static ISealEntity getSeal(EntityPlayer playerIn) {
      float f = playerIn.rotationPitch;
      float f1 = playerIn.rotationYaw;
      double d0 = playerIn.posX;
      double d1 = playerIn.posY + playerIn.getEyeHeight();
      double d2 = playerIn.posZ;
      Vec3d vec0 = new Vec3d(d0, d1, d2);
      float f2 = MathHelper.cos(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f3 = MathHelper.sin(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f4 = -MathHelper.cos(-f * (float) (Math.PI / 180.0));
      float f5 = MathHelper.sin(-f * (float) (Math.PI / 180.0));
      float f6 = f3 * f4;
      float f7 = f2 * f4;
      double d3 = 5.0;
      Vec3d vec1 = vec0.add(f6 * d3, f5 * d3, f7 * d3);
      Vec3d vec2 = new Vec3d(f6 * d3, f5 * d3, f7 * d3);
      Vec3d vec3 = vec0.add(vec2.x / 10.0, vec2.y / 10.0, vec2.z / 10.0);

      for (int a = 0; a < vec2.length() * 10.0; a++) {
         BlockPos pos = new BlockPos(vec3);
         RayTraceResult mop = collisionRayTrace(playerIn.world, pos, vec0, vec1);
         if (mop != null) {
            ISealEntity se = SealHandler.getSealEntity(playerIn.world.provider.getDimension(), new SealPos(pos, mop.sideHit));
            if (se != null) {
               return se;
            }
         }

         vec3 = vec3.add(vec2.x / 10.0, vec2.y / 10.0, vec2.z / 10.0);
      }

      return null;
   }

   private static boolean isVecInsideYZBounds(Vec3d point, BlockPos pos) {
      return point == null
         ? false
         : point.y >= pos.getY()
            && point.y <= pos.getY() + 1
            && point.z >= pos.getZ()
            && point.z <= pos.getZ() + 1;
   }

   private static boolean isVecInsideXZBounds(Vec3d point, BlockPos pos) {
      return point == null
         ? false
         : point.x >= pos.getX()
            && point.x <= pos.getX() + 1
            && point.z >= pos.getZ()
            && point.z <= pos.getZ() + 1;
   }

   private static boolean isVecInsideXYBounds(Vec3d point, BlockPos pos) {
      return point == null
         ? false
         : point.x >= pos.getX()
            && point.x <= pos.getX() + 1
            && point.y >= pos.getY()
            && point.y <= pos.getY() + 1;
   }

   private static RayTraceResult collisionRayTrace(World worldIn, BlockPos pos, Vec3d start, Vec3d end) {
      Vec3d vec3 = start.getIntermediateWithXValue(end, pos.getX());
      Vec3d vec31 = start.getIntermediateWithXValue(end, pos.getX() + 1);
      Vec3d vec32 = start.getIntermediateWithYValue(end, pos.getY());
      Vec3d vec33 = start.getIntermediateWithYValue(end, pos.getY() + 1);
      Vec3d vec34 = start.getIntermediateWithZValue(end, pos.getZ());
      Vec3d vec35 = start.getIntermediateWithZValue(end, pos.getZ() + 1);
      if (!isVecInsideYZBounds(vec3, pos)) {
         vec3 = null;
      }

      if (!isVecInsideYZBounds(vec31, pos)) {
         vec31 = null;
      }

      if (!isVecInsideXZBounds(vec32, pos)) {
         vec32 = null;
      }

      if (!isVecInsideXZBounds(vec33, pos)) {
         vec33 = null;
      }

      if (!isVecInsideXYBounds(vec34, pos)) {
         vec34 = null;
      }

      if (!isVecInsideXYBounds(vec35, pos)) {
         vec35 = null;
      }

      Vec3d vec36 = null;
      if (vec3 != null && (vec36 == null || start.squareDistanceTo(vec3) < start.squareDistanceTo(vec36))) {
         vec36 = vec3;
      }

      if (vec31 != null && (vec36 == null || start.squareDistanceTo(vec31) < start.squareDistanceTo(vec36))) {
         vec36 = vec31;
      }

      if (vec32 != null && (vec36 == null || start.squareDistanceTo(vec32) < start.squareDistanceTo(vec36))) {
         vec36 = vec32;
      }

      if (vec33 != null && (vec36 == null || start.squareDistanceTo(vec33) < start.squareDistanceTo(vec36))) {
         vec36 = vec33;
      }

      if (vec34 != null && (vec36 == null || start.squareDistanceTo(vec34) < start.squareDistanceTo(vec36))) {
         vec36 = vec34;
      }

      if (vec35 != null && (vec36 == null || start.squareDistanceTo(vec35) < start.squareDistanceTo(vec36))) {
         vec36 = vec35;
      }

      if (vec36 == null) {
         return null;
      } else {
         EnumFacing enumfacing = null;
         if (vec36 == vec3) {
            enumfacing = EnumFacing.WEST;
         }

         if (vec36 == vec31) {
            enumfacing = EnumFacing.EAST;
         }

         if (vec36 == vec32) {
            enumfacing = EnumFacing.DOWN;
         }

         if (vec36 == vec33) {
            enumfacing = EnumFacing.UP;
         }

         if (vec36 == vec34) {
            enumfacing = EnumFacing.NORTH;
         }

         if (vec36 == vec35) {
            enumfacing = EnumFacing.SOUTH;
         }

         return new RayTraceResult(vec36.add(pos.getX(), pos.getY(), pos.getZ()), enumfacing, pos);
      }
   }
}
