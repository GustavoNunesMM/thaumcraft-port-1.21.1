package thaumcraft.common.blocks.devices;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

/** Bits of BlockUtils / ThaumcraftInvHelper / InventoryUtils used by the devices. */
public final class DeviceUtils {
   private DeviceUtils() {
   }

   /** BlockUtils.hasLOS: nothing solid between the centres of the two blocks (the blocks themselves don't count). */
   public static boolean hasLOS(Level level, BlockPos source, BlockPos target) {
      Vec3 from = Vec3.atCenterOf(source);
      Vec3 to = Vec3.atCenterOf(target);
      BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
      return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(source) || hit.getBlockPos().equals(target);
   }

   /** ThaumcraftInvHelper.getItemHandlerAt: the item handler capability, or a wrapper around a plain container. */
   @Nullable
   public static IItemHandler getItemHandlerAt(Level level, BlockPos pos, @Nullable Direction side) {
      IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
      if (h != null) return h;
      BlockEntity te = level.getBlockEntity(pos);
      if (te instanceof WorldlyContainer wc && side != null) return new SidedInvWrapper(wc, side);
      if (te instanceof Container c) return new InvWrapper(c);
      return null;
   }

   /** ThaumcraftInvHelper.insertStackAt: what is left over. */
   public static ItemStack insertStackAt(Level level, BlockPos pos, @Nullable Direction side, ItemStack stack, boolean simulate) {
      IItemHandler inv = getItemHandlerAt(level, pos, side);
      return inv != null ? ItemHandlerHelper.insertItemStacked(inv, stack, simulate) : stack;
   }

   /** InventoryUtils.ejectStackAt: into the inventory on that side, else dropped out of that side. */
   public static void ejectStackAt(Level level, BlockPos pos, Direction side, ItemStack out) {
      out = insertStackAt(level, pos.relative(side), side.getOpposite(), out, false);
      if (out.isEmpty()) return;
      if (level.getBlockState(pos.relative(side)).isCollisionShapeFullBlock(level, pos.relative(side))) pos = pos.relative(side.getOpposite());
      ItemEntity e = new ItemEntity(level, pos.getX() + 0.5 + side.getStepX(), pos.getY() + side.getStepY(), pos.getZ() + 0.5 + side.getStepZ(), out);
      e.setDeltaMovement(0.3 * side.getStepX(), 0.3 * side.getStepY(), 0.3 * side.getStepZ());
      level.addFreshEntity(e);
   }
}
