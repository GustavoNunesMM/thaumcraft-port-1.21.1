package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.world.aura.AuraHandler;

/** TileVisGenerator: 1 vis = 1000 FE (buffer 1000), handed out at up to 20 FE/t to the block it faces. */
public class VisGeneratorBlockEntity extends TCBlockEntity implements IEnergyStorage {
   public static final int CAPACITY = 1000;
   public static final int MAX_EXTRACT = 20;
   protected int energy;

   public VisGeneratorBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.VIS_GENERATOR_BE.get(), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, VisGeneratorBlockEntity be) {
      if (!DeviceBlock.isEnabled(state)) return;
      be.recharge();
      Direction face = DeviceBlock.getFacing(state);
      IEnergyStorage cap = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos.relative(face), face.getOpposite());
      if (cap != null && cap.canReceive()) {
         int extracted = cap.receiveEnergy(Math.min(be.energy, MAX_EXTRACT), false);
         if (extracted > 0) {
            be.energy -= extracted;
            be.setChanged();
            if (be.energy == 0) be.sync();
         }
      }
   }

   private void recharge() {
      if (energy == 0) {
         float vis = AuraHandler.drainVis(level, worldPosition, 1.0F, false);
         energy = (int) (vis * CAPACITY);
         if (energy > 0) sync();
      }
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      energy = tag.getInt("energy");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putInt("energy", energy);
   }

   @Override
   public int receiveEnergy(int maxReceive, boolean simulate) {
      return 0;
   }

   @Override
   public int extractEnergy(int maxExtract, boolean simulate) {
      return 0;
   }

   @Override
   public int getEnergyStored() {
      return energy;
   }

   @Override
   public int getMaxEnergyStored() {
      return CAPACITY;
   }

   @Override
   public boolean canExtract() {
      return true;
   }

   @Override
   public boolean canReceive() {
      return false;
   }
}
