package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import thaumcraft.common.menu.SpaMenu;
import thaumcraft.common.registers.ModDevices;

/**
 * TileSpa: every 2 seconds (unpowered) pours 1000 mB into the world above it: either water mixed with bath salts into
 * purifying fluid ("mix"), or just the fluid in its 5000 mB tank. Once the block above is full, it spreads the pool
 * over the 5x5 area next to it.
 */
public class SpaBlockEntity extends DeviceInventoryBlockEntity implements MenuProvider {
   private boolean mix = true;
   private int counter = 0;
   public final FluidTank tank = new FluidTank(5000) {
      @Override
      protected void onContentsChanged() {
         sync();
      }
   };

   public SpaBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.SPA_BE.get(), pos, state, 1);
   }

   public void toggleMix() {
      mix = !mix;
      sync();
   }

   public boolean getMix() {
      return mix;
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, SpaBlockEntity be) {
      if (be.counter++ % 40 != 0 || level.hasNeighborSignal(pos) || !be.hasIngredients()) return;
      Fluid target = be.mix ? DeviceStubs.purifyingFluid() : be.tank.getFluid().getFluid();
      if (target == Fluids.EMPTY) return;
      BlockState targetState = target.defaultFluidState().createLegacyBlock();
      if (level.getBlockState(pos.above()) == targetState) {
         for (int xx = -2; xx <= 2; xx++) {
            for (int zz = -2; zz <= 2; zz++) {
               BlockPos p = pos.offset(xx, 1, zz);
               if (be.isValidLocation(p, true, targetState)) {
                  be.consumeIngredients();
                  level.setBlockAndUpdate(p, targetState);
                  return;
               }
            }
         }
      } else if (be.isValidLocation(pos.above(), false, targetState)) {
         be.consumeIngredients();
         level.setBlockAndUpdate(pos.above(), targetState);
      }
   }

   private boolean hasIngredients() {
      if (mix) {
         FluidStack fs = tank.getFluid();
         return fs.is(Fluids.WATER) && fs.getAmount() >= 1000 && DeviceStubs.isBathSalts(getItem(0));
      }
      FluidStack fs = tank.getFluid();
      return !fs.isEmpty() && fs.getAmount() >= 1000
         && fs.getFluid().getFluidType().canBePlacedInLevel(level, worldPosition.above(), fs.getFluid().defaultFluidState());
   }

   private void consumeIngredients() {
      if (mix) removeItem(0, 1);
      tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
   }

   private boolean isValidLocation(BlockPos pos, boolean mustBeAdjacent, BlockState target) {
      FluidState tf = target.getFluidState();
      if (tf.is(Fluids.WATER) && level.dimensionType().ultraWarm()) return false;
      BlockState b = level.getBlockState(pos);
      if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP) && b.canBeReplaced() && b != target) {
         if (!mustBeAdjacent) return true;
         for (Direction d : Direction.values()) {
            if (level.getBlockState(pos.relative(d)) == target) return true;
         }
      }
      return false;
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      return DeviceStubs.isBathSalts(stack);
   }

   @Override
   public int[] getSlotsForFace(Direction side) {
      return side != Direction.UP ? new int[]{0} : new int[0];
   }

   @Override
   public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
      return dir != Direction.UP;
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.readSync(tag, registries);
      mix = !tag.contains("mix") || tag.getBoolean("mix");
      tank.setFluid(FluidStack.EMPTY);
      tank.readFromNBT(registries, tag);
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.writeSync(tag, registries);
      tag.putBoolean("mix", mix);
      tank.writeToNBT(registries, tag);
   }

   @Override
   public Component getDisplayName() {
      return getBlockState().getBlock().getName();
   }

   @Override
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new SpaMenu(id, inv, this);
   }
}
