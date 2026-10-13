package thaumcraft.common.tiles.devices;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.common.world.aura.AuraHandler;

public class TileVisGenerator extends TileThaumcraft implements ITickable, IEnergyStorage {
   protected int energy;
   protected final int capacity = 1000;
   protected final int maxExtract = 20;

   public void update() {
      if (!this.world.isRemote && BlockStateUtils.isEnabled(this.getBlockMetadata())) {
         this.recharge();
         EnumFacing face = BlockStateUtils.getFacing(this.getBlockMetadata());
         IBlockState state = this.getWorld().getBlockState(this.getPos().offset(face));
         Block block = state.getBlock();
         if (block.hasTileEntity(state)) {
            TileEntity tileentity = this.getWorld().getTileEntity(this.getPos().offset(face));
            if (tileentity != null && tileentity.hasCapability(CapabilityEnergy.ENERGY, face.getOpposite())) {
               IEnergyStorage capability = (IEnergyStorage)tileentity.getCapability(CapabilityEnergy.ENERGY, face.getOpposite());
               if (capability.canReceive()) {
                  int energyExtracted = Math.min(this.energy, 20);
                  energyExtracted = capability.receiveEnergy(energyExtracted, false);
                  if (energyExtracted > 0) {
                     this.energy -= energyExtracted;
                     this.markDirty();
                     if (this.energy == 0) {
                        this.syncTile(false);
                     }
                  }
               }
            }
         }
      }
   }

   private void recharge() {
      if (this.energy == 0) {
         float vis = AuraHandler.drainVis(this.getWorld(), this.getPos(), 1.0F, false);
         this.energy = (int)(vis * 1000.0F);
         this.markDirty();
         this.syncTile(false);
      }
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbt) {
      this.energy = nbt.getInteger("energy");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbt) {
      nbt.setInteger("energy", this.energy);
      return nbt;
   }

   public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
      EnumFacing face = BlockStateUtils.getFacing(this.getBlockMetadata());
      return face == facing && capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
   }

   @Nullable
   public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
      EnumFacing face = BlockStateUtils.getFacing(this.getBlockMetadata());
      return (T)(face == facing && capability == CapabilityEnergy.ENERGY ? this : super.getCapability(capability, facing));
   }

   public int receiveEnergy(int maxReceive, boolean simulate) {
      return 0;
   }

   public int extractEnergy(int maxExtract, boolean simulate) {
      return 0;
   }

   public int getEnergyStored() {
      return this.energy;
   }

   public int getMaxEnergyStored() {
      return 1000;
   }

   public boolean canExtract() {
      return true;
   }

   public boolean canReceive() {
      return false;
   }
}
