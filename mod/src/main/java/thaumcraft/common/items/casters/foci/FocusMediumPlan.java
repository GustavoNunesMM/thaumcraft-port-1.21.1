package thaumcraft.common.items.casters.foci;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.api.items.IArchitect;
import thaumcraft.common.items.casters.CasterManager;

/** Plan: like touch with 16 blocks reach, but on a whole area of blocks previewed in the world (G sizes it). */
public class FocusMediumPlan extends FocusMedium implements IArchitect {
   private final Set<BlockPos> checked = new HashSet<>();

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
      return new NodeSetting[]{new NodeSetting("method", "focus.plan.method",
         new NodeSetting.NodeSettingIntList(new int[]{0, 1}, new String[]{"focus.plan.full", "focus.plan.surface"}))};
   }

   @Override
   public HitResult[] supplyTargets() {
      if (getParent() == null || !(getPackage().getCaster() instanceof Player player) || getParent().supplyTrajectories() == null) return new HitResult[0];
      List<HitResult> targets = new ArrayList<>();
      ItemStack casterStack = FocusUtils.heldCaster(player);
      for (Trajectory sT : getParent().supplyTrajectories()) {
         Vec3 end = sT.direction.normalize().scale(16.0).add(sT.source);
         BlockHitResult target = FocusUtils.rayTraceBlocks(getPackage().world, sT.source, end, player);
         if (target != null) {
            List<BlockPos> usl = getArchitectBlocks(casterStack, getPackage().world, target.getBlockPos(), target.getDirection(), player);
            BlockPos origin = target.getBlockPos();
            usl.sort(Comparator.comparingDouble(p -> p.distSqr(origin)));
            for (BlockPos p : usl) targets.add(new BlockHitResult(Vec3.atCenterOf(p), target.getDirection(), p, false));
         }
      }
      return targets.toArray(new HitResult[0]);
   }

   @Override
   public HitResult getArchitectMOP(ItemStack stack, Level world, LivingEntity player) {
      Vec3 start = player.position().add(0.0, player.getEyeHeight(), 0.0);
      Vec3 end = player.getLookAngle().scale(16.0).add(start);
      return FocusUtils.rayTraceBlocks(world, start, end, player);
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
   public boolean showAxis(ItemStack stack, Level world, Player player, Direction side, EnumAxis axis) {
      if (stack == null || stack.isEmpty()) return false;
      int dim = CasterManager.getAreaDim(stack);
      if (getSettingValue("method") == 0) {
         return switch (axis) {
            case Y -> dim == 0 || dim == 3;
            case Z -> dim == 0 || dim == 2;
            case X -> dim == 0 || dim == 1;
         };
      }
      return switch (side.getAxis()) {
         case Y -> axis == EnumAxis.X && (dim == 0 || dim == 1) || axis == EnumAxis.Z && (dim == 0 || dim == 2);
         case Z -> axis == EnumAxis.Y && (dim == 0 || dim == 1) || axis == EnumAxis.X && (dim == 0 || dim == 2);
         case X -> axis == EnumAxis.Y && (dim == 0 || dim == 1) || axis == EnumAxis.Z && (dim == 0 || dim == 2);
      };
   }

   @Override
   public List<BlockPos> getArchitectBlocks(ItemStack stack, Level world, BlockPos pos, Direction side, Player player) {
      List<BlockPos> out = new ArrayList<>();
      if (stack == null || stack.isEmpty()) return out;
      checked.clear();
      if (getSettingValue("method") == 0) {
         checkNeighboursFull(world, pos, pos, side, CasterManager.getAreaX(stack), CasterManager.getAreaY(stack), CasterManager.getAreaZ(stack), out);
      } else {
         BlockState bi = world.getBlockState(pos);
         if (side.getAxis() == Direction.Axis.Z) {
            checkNeighboursSurface(world, pos, bi, pos, side, CasterManager.getAreaZ(stack), CasterManager.getAreaY(stack), CasterManager.getAreaX(stack), out);
         } else {
            checkNeighboursSurface(world, pos, bi, pos, side, CasterManager.getAreaX(stack), CasterManager.getAreaY(stack), CasterManager.getAreaZ(stack), out);
         }
      }
      return out;
   }

   private void checkNeighboursFull(Level world, BlockPos pos1, BlockPos start, Direction side, int sizeX, int sizeY, int sizeZ, List<BlockPos> list) {
      int xs = -sizeX + pos1.getX() - sizeX * side.getStepX(), xe = sizeX + pos1.getX() - sizeX * side.getStepX();
      int ys = -sizeY + pos1.getY() - sizeY * side.getStepY(), ye = sizeY + pos1.getY() - sizeY * side.getStepY();
      int zs = -sizeZ + pos1.getZ() - sizeZ * side.getStepZ(), ze = sizeZ + pos1.getZ() - sizeZ * side.getStepZ();
      // Flood fill (the original recursed; iterative here to stay clear of the stack limit).
      ArrayList<BlockPos> open = new ArrayList<>();
      open.add(start);
      while (!open.isEmpty()) {
         BlockPos pos2 = open.remove(open.size() - 1);
         if (!checked.add(pos2)) continue;
         if (!world.isEmptyBlock(pos2)) list.add(pos2);
         for (Direction dir : Direction.values()) {
            BlockPos q = pos2.relative(dir);
            if (q.getX() >= xs && q.getX() <= xe && q.getY() >= ys && q.getY() <= ye && q.getZ() >= zs && q.getZ() <= ze && !checked.contains(q)) open.add(q);
         }
      }
   }

   private void checkNeighboursSurface(Level world, BlockPos pos1, BlockState bi, BlockPos start, Direction side, int sizeX, int sizeY, int sizeZ,
                                       List<BlockPos> list) {
      ArrayList<BlockPos> open = new ArrayList<>();
      open.add(start);
      while (!open.isEmpty()) {
         BlockPos pos2 = open.remove(open.size() - 1);
         if (!checked.add(pos2)) continue;
         switch (side.getAxis()) {
            case Y -> {
               if (Math.abs(pos2.getX() - pos1.getX()) > sizeX || Math.abs(pos2.getZ() - pos1.getZ()) > sizeZ) continue;
            }
            case Z -> {
               if (Math.abs(pos2.getX() - pos1.getX()) > sizeX || Math.abs(pos2.getY() - pos1.getY()) > sizeZ) continue;
            }
            case X -> {
               if (Math.abs(pos2.getY() - pos1.getY()) > sizeX || Math.abs(pos2.getZ() - pos1.getZ()) > sizeZ) continue;
            }
         }
         if (world.getBlockState(pos2) == bi && FocusUtils.isBlockExposed(world, pos2) && !world.isEmptyBlock(pos2)) {
            list.add(pos2);
            for (Direction dir : Direction.values()) {
               if (dir != side && dir.getOpposite() != side) open.add(pos2.relative(dir));
            }
         }
      }
   }
}
