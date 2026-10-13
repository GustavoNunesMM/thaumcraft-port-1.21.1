package thaumcraft.common.items.casters.foci;

import java.util.ArrayList;
import java.util.stream.Collectors;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.api.items.IArchitect;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.lib.utils.BlockUtils;

public class FocusMediumPlan extends FocusMedium implements IArchitect {
   ArrayList<BlockPos> checked = new ArrayList<>();

   @Override
   public String getResearch() {
      return "FOCUSPLAN";
   }

   @Override
   public String getKey() {
      return "thaumcraft.PLAN";
   }

   @Override
   public int getComplexity() {
      return 4;
   }

   @Override
   public Aspect getAspect() {
      return Aspect.CRAFT;
   }

   @Override
   public NodeSetting[] createSettings() {
      int[] method = new int[]{0, 1};
      String[] methodDesc = new String[]{"focus.plan.full", "focus.plan.surface"};
      return new NodeSetting[]{new NodeSetting("method", "focus.plan.method", new NodeSetting.NodeSettingIntList(method, methodDesc))};
   }

   @Override
   public RayTraceResult[] supplyTargets() {
      if (this.getParent() != null && this.getPackage().getCaster() instanceof EntityPlayer) {
         ArrayList<RayTraceResult> targets = new ArrayList<>();
         ItemStack casterStack = ItemStack.EMPTY;
         if (this.getPackage().getCaster().getHeldItemMainhand() != null && this.getPackage().getCaster().getHeldItemMainhand().getItem() instanceof ICaster) {
            casterStack = this.getPackage().getCaster().getHeldItemMainhand();
         } else if (this.getPackage().getCaster().getHeldItemOffhand() != null && this.getPackage().getCaster().getHeldItemOffhand().getItem() instanceof ICaster) {
            casterStack = this.getPackage().getCaster().getHeldItemOffhand();
         }

         for (Trajectory sT : this.getParent().supplyTrajectories()) {
            Vec3d end = sT.direction.normalize();
            end = end.scale(16.0);
            end = end.add(sT.source);
            RayTraceResult target = this.getPackage().world.rayTraceBlocks(sT.source, end);
            if (target != null && target.typeOfHit == Type.BLOCK) {
               ArrayList<BlockPos> usl = this.getArchitectBlocks(
                  casterStack, this.getPackage().world, target.getBlockPos(), target.sideHit, (EntityPlayer)this.getPackage().getCaster()
               );

               for (BlockPos p : (ArrayList)usl.stream().sorted(new BlockUtils.BlockPosComparator(target.getBlockPos())).collect(Collectors.toList())) {
                  targets.add(
                     new RayTraceResult(new Vec3d(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5), target.sideHit, p)
                  );
               }
            }
         }

         return targets.toArray(new RayTraceResult[0]);
      } else {
         return new RayTraceResult[0];
      }
   }

   @Override
   public RayTraceResult getArchitectMOP(ItemStack stack, World world, EntityLivingBase player) {
      Vec3d start = player.getPositionVector();
      start = start.add(0.0, player.getEyeHeight(), 0.0);
      Vec3d end = player.getLookVec();
      end = end.scale(16.0);
      end = end.add(start);
      return world.rayTraceBlocks(start, end);
   }

   @Override
   public boolean useBlockHighlight(ItemStack stack) {
      return false;
   }

   @Override
   public boolean isExclusive() {
      return true;
   }

   @Override
   public boolean showAxis(ItemStack stack, World world, EntityPlayer player, EnumFacing side, IArchitect.EnumAxis axis) {
      if (stack != null && !stack.isEmpty()) {
         int dim = CasterManager.getAreaDim(stack);
         if (this.getSettingValue("method") == 0) {
            switch (axis) {
               case Y:
                  if (dim == 0 || dim == 3) {
                     return true;
                  }
                  break;
               case Z:
                  if (dim == 0 || dim == 2) {
                     return true;
                  }
                  break;
               case X:
                  if (dim == 0 || dim == 1) {
                     return true;
                  }
            }
         } else {
            switch (side.getAxis()) {
               case Y:
                  if (axis == IArchitect.EnumAxis.X && (dim == 0 || dim == 1) || axis == IArchitect.EnumAxis.Z && (dim == 0 || dim == 2)) {
                     return true;
                  }
                  break;
               case Z:
                  if (axis == IArchitect.EnumAxis.Y && (dim == 0 || dim == 1) || axis == IArchitect.EnumAxis.X && (dim == 0 || dim == 2)) {
                     return true;
                  }
                  break;
               case X:
                  if (axis == IArchitect.EnumAxis.Y && (dim == 0 || dim == 1) || axis == IArchitect.EnumAxis.Z && (dim == 0 || dim == 2)) {
                     return true;
                  }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   public ArrayList<BlockPos> getArchitectBlocks(ItemStack stack, World world, BlockPos pos, EnumFacing side, EntityPlayer player) {
      ArrayList<BlockPos> out = new ArrayList<>();
      if (stack != null && !stack.isEmpty()) {
         if (this.getSettingValue("method") == 0) {
            this.checked.clear();
            this.checkNeighboursFull(
               world, pos, new BlockPos(pos), side, CasterManager.getAreaX(stack), CasterManager.getAreaY(stack), CasterManager.getAreaZ(stack), out, player
            );
         } else {
            IBlockState bi = world.getBlockState(pos);
            this.checked.clear();
            if (side.getAxis() == Axis.Z) {
               this.checkNeighboursSurface(
                  world,
                  pos,
                  bi,
                  new BlockPos(pos),
                  side,
                  CasterManager.getAreaZ(stack),
                  CasterManager.getAreaY(stack),
                  CasterManager.getAreaX(stack),
                  out,
                  player
               );
            } else {
               this.checkNeighboursSurface(
                  world,
                  pos,
                  bi,
                  new BlockPos(pos),
                  side,
                  CasterManager.getAreaX(stack),
                  CasterManager.getAreaY(stack),
                  CasterManager.getAreaZ(stack),
                  out,
                  player
               );
            }
         }

         return out;
      } else {
         return out;
      }
   }

   public void checkNeighboursFull(
      World world, BlockPos pos1, BlockPos pos2, EnumFacing side, int sizeX, int sizeY, int sizeZ, ArrayList<BlockPos> list, EntityPlayer player
   ) {
      if (!this.checked.contains(pos2)) {
         this.checked.add(pos2);
         if (!world.isAirBlock(pos2)) {
            list.add(pos2);
         }

         int xs = -sizeX + pos1.getX();
         int xe = sizeX + pos1.getX();
         int ys = -sizeY + pos1.getY();
         int ye = sizeY + pos1.getY();
         int zs = -sizeZ + pos1.getZ();
         int ze = sizeZ + pos1.getZ();
         xs -= sizeX * side.getXOffset();
         xe -= sizeX * side.getXOffset();
         ys -= sizeY * side.getYOffset();
         ye -= sizeY * side.getYOffset();
         zs -= sizeZ * side.getZOffset();
         ze -= sizeZ * side.getZOffset();

         for (EnumFacing dir : EnumFacing.values()) {
            BlockPos q = pos2.offset(dir);
            if (q.getX() >= xs
               && q.getX() <= xe
               && q.getY() >= ys
               && q.getY() <= ye
               && q.getZ() >= zs
               && q.getZ() <= ze) {
               this.checkNeighboursFull(world, pos1, q, side, sizeX, sizeY, sizeZ, list, player);
            }
         }
      }
   }

   public void checkNeighboursSurface(
      World world,
      BlockPos pos1,
      IBlockState bi,
      BlockPos pos2,
      EnumFacing side,
      int sizeX,
      int sizeY,
      int sizeZ,
      ArrayList<BlockPos> list,
      EntityPlayer player
   ) {
      if (!this.checked.contains(pos2)) {
         this.checked.add(pos2);
         switch (side.getAxis()) {
            case Y:
               if (Math.abs(pos2.getX() - pos1.getX()) > sizeX) {
                  return;
               }

               if (Math.abs(pos2.getZ() - pos1.getZ()) > sizeZ) {
                  return;
               }
               break;
            case Z:
               if (Math.abs(pos2.getX() - pos1.getX()) > sizeX) {
                  return;
               }

               if (Math.abs(pos2.getY() - pos1.getY()) > sizeZ) {
                  return;
               }
               break;
            case X:
               if (Math.abs(pos2.getY() - pos1.getY()) > sizeX) {
                  return;
               }

               if (Math.abs(pos2.getZ() - pos1.getZ()) > sizeZ) {
                  return;
               }
         }

         if (world.getBlockState(pos2) == bi && BlockUtils.isBlockExposed(world, pos2) && !world.isAirBlock(pos2)) {
            list.add(pos2);

            for (EnumFacing dir : EnumFacing.values()) {
               if (dir != side && dir.getOpposite() != side) {
                  this.checkNeighboursSurface(world, pos1, bi, pos2.offset(dir), side, sizeX, sizeY, sizeZ, list, player);
               }
            }
         }
      }
   }
}
