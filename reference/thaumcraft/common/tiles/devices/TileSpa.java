package thaumcraft.common.tiles.devices;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.items.consumables.ItemBathSalts;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;

public class TileSpa extends TileThaumcraftInventory implements IFluidHandler {
   private boolean mix = true;
   private int counter = 0;
   public FluidTank tank = new FluidTank(5000);

   public TileSpa() {
      super(1);
   }

   public void toggleMix() {
      this.mix = !this.mix;
      this.syncTile(false);
      this.markDirty();
   }

   public boolean getMix() {
      return this.mix;
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      this.mix = nbttagcompound.getBoolean("mix");
      this.tank.readFromNBT(nbttagcompound);
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      nbttagcompound.setBoolean("mix", this.mix);
      this.tank.writeToNBT(nbttagcompound);
      return nbttagcompound;
   }

   @Override
   public boolean isItemValidForSlot(int par1, ItemStack stack) {
      return stack.getItem() instanceof ItemBathSalts;
   }

   @Override
   public String getName() {
      return "thaumcraft.spa";
   }

   @Override
   public boolean hasCustomName() {
      return false;
   }

   @Override
   public ITextComponent getDisplayName() {
      return null;
   }

   @Override
   public int[] getSlotsForFace(EnumFacing side) {
      return side != EnumFacing.UP ? new int[]{0} : new int[0];
   }

   @Override
   public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing side) {
      return side != EnumFacing.UP;
   }

   @Override
   public boolean canExtractItem(int index, ItemStack stack, EnumFacing side) {
      return side != EnumFacing.UP;
   }

   @Override
   public void update() {
      super.update();
      if (!this.world.isRemote && this.counter++ % 40 == 0 && !this.world.isBlockPowered(this.pos) && this.hasIngredients()) {
         Block b = this.world.getBlockState(this.pos.up()).getBlock();
         int m = b.getMetaFromState(this.world.getBlockState(this.pos.up()));
         Block tb = null;
         if (this.mix) {
            tb = BlocksTC.purifyingFluid;
         } else {
            tb = this.tank.getFluid().getFluid().getBlock();
         }

         if (b == tb && m == 0) {
            for (int xx = -2; xx <= 2; xx++) {
               for (int zz = -2; zz <= 2; zz++) {
                  BlockPos p = this.getPos().add(xx, 1, zz);
                  if (this.isValidLocation(p, true, tb)) {
                     this.consumeIngredients();
                     this.world.setBlockState(p, tb.getDefaultState());
                     this.checkQuanta(p);
                     return;
                  }
               }
            }
         } else if (this.isValidLocation(this.pos.up(), false, tb)) {
            this.consumeIngredients();
            this.world.setBlockState(this.pos.up(), tb.getDefaultState());
            this.checkQuanta(this.pos.up());
         }
      }
   }

   private void checkQuanta(BlockPos pos) {
      Block b = this.world.getBlockState(pos).getBlock();
      if (b instanceof BlockFluidBase) {
         float p = ((BlockFluidBase)b).getQuantaPercentage(this.world, pos);
         if (p < 1.0F) {
            int md = (int)(1.0F / p) - 1;
            if (md >= 0 && md < 16) {
               this.world.setBlockState(pos, b.getStateFromMeta(md));
            }
         }
      }
   }

   private boolean hasIngredients() {
      if (this.mix) {
         if (this.tank.getInfo().fluid == null || !this.tank.getInfo().fluid.containsFluid(new FluidStack(FluidRegistry.WATER, 1000))) {
            return false;
         }

         if (!(this.getStackInSlot(0).getItem() instanceof ItemBathSalts)) {
            return false;
         }
      } else if (this.tank.getInfo().fluid == null || !this.tank.getFluid().getFluid().canBePlacedInWorld() || this.tank.getFluidAmount() < 1000) {
         return false;
      }

      return true;
   }

   private void consumeIngredients() {
      if (this.mix) {
         this.decrStackSize(0, 1);
      }

      this.drain(1000, true);
   }

   private boolean isValidLocation(BlockPos pos, boolean mustBeAdjacent, Block target) {
      if ((target == Blocks.WATER || target == Blocks.FLOWING_WATER) && this.world.provider.doesWaterVaporize()) {
         return false;
      } else {
         Block b = this.world.getBlockState(pos).getBlock();
         IBlockState bb = this.world.getBlockState(pos.down());
         int m = b.getMetaFromState(this.world.getBlockState(pos));
         if (bb.isSideSolid(this.world, pos.down(), EnumFacing.UP) && b.isReplaceable(this.world, pos) && (b != target || m != 0)) {
            return !mustBeAdjacent ? true : BlockUtils.isBlockTouching(this.world, pos, target.getStateFromMeta(0));
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
      return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
   }

   @Nullable
   @Override
   public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
      return (T)(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? this.tank : super.getCapability(capability, facing));
   }

   public IFluidTankProperties[] getTankProperties() {
      return this.tank.getTankProperties();
   }

   public int fill(FluidStack resource, boolean doFill) {
      this.markDirty();
      this.syncTile(false);
      return this.tank.fill(resource, doFill);
   }

   public FluidStack drain(FluidStack resource, boolean doDrain) {
      FluidStack fs = this.tank.drain(resource, doDrain);
      this.markDirty();
      this.syncTile(false);
      return fs;
   }

   public FluidStack drain(int maxDrain, boolean doDrain) {
      FluidStack fs = this.tank.drain(maxDrain, doDrain);
      this.markDirty();
      this.syncTile(false);
      return fs;
   }
}
