package thaumcraft.common.blocks.devices;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TileWaterJug: holds 1000 mB of water, refilled from the aura (1 vis per bucket). Every 5 ticks it checks one block of
 * the 5x3x5 area around it for something to fill (fluid handlers, cauldrons) and gives water to those it knows.
 */
public class EverfullUrnBlockEntity extends TCBlockEntity {
   private int zone = 0;
   private int counter = 0;
   private List<Integer> handlers = new ArrayList<>();
   private int zc = 0;
   private int tcount = 0;
   public final FluidTank tank = new FluidTank(1000, fs -> fs.is(Fluids.WATER)) {
      @Override
      protected void onContentsChanged() {
         setChanged();
      }
   };

   public EverfullUrnBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.EVERFULL_URN_BE.get(), pos, state);
   }

   private BlockPos zonePos(int z) {
      return worldPosition.offset(z / 5 % 5 - 2, z / 5 / 5 % 3 - 1, z % 5 - 2);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, EverfullUrnBlockEntity be) {
      be.counter++;
      if (level.isClientSide) {
         if (be.tcount > 0) {
            if (be.tcount % 5 == 0) thaumcraft.client.fx.DeviceFX.waterTrail(pos, be.zonePos(be.zc), 2650102);
            be.tcount--;
         }
      } else if (be.counter % 5 == 0) {
         be.serverWork(level);
      }
   }

   private void serverWork(Level level) {
      zone++;
      BlockPos zp = zonePos(zone);
      BlockState bs = level.getBlockState(zp);
      if ((level.getCapability(Capabilities.FluidHandler.BLOCK, zp, Direction.UP) != null || bs.is(Blocks.CAULDRON) || bs.is(Blocks.WATER_CAULDRON))
         && !handlers.contains(zone % 75) && !zp.equals(worldPosition)) {
         handlers.add(zone % 75);
         setChanged();
      }
      int i = 0;
      while (i < handlers.size() && tank.getFluidAmount() >= 25) {
         int zz = handlers.get(i);
         BlockPos p = zonePos(zz);
         BlockState bs2 = level.getBlockState(p);
         // Cauldrons first: their fluid capability only takes whole buckets.
         boolean cauldron = bs2.is(Blocks.CAULDRON) || bs2.is(Blocks.WATER_CAULDRON);
         IFluidHandler fh = p.equals(worldPosition) || cauldron ? null : level.getCapability(Capabilities.FluidHandler.BLOCK, p, Direction.UP);
         if (fh == null) {
            if (!cauldron || tank.getFluidAmount() < 333) {
               handlers.remove(i);
               setChanged();
               continue;
            }
            if (bs2.is(Blocks.CAULDRON)) {
               level.setBlock(p, Blocks.WATER_CAULDRON.defaultBlockState(), 2);
            } else if (bs2.getValue(LayeredCauldronBlock.LEVEL) < 3) {
               level.setBlock(p, bs2.cycle(LayeredCauldronBlock.LEVEL), 2);
            } else {
               i++;
               continue;
            }
            level.updateNeighbourForOutputSignal(p, level.getBlockState(p).getBlock());
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, zz);
            drain(333, IFluidHandler.FluidAction.EXECUTE);
         } else {
            int q = fh.fill(new FluidStack(Fluids.WATER, 25), IFluidHandler.FluidAction.EXECUTE);
            if (q > 0) {
               drain(q, IFluidHandler.FluidAction.EXECUTE);
               level.blockEvent(worldPosition, getBlockState().getBlock(), 1, zz);
               break;
            }
         }
         i++;
      }
      if (tank.getFluidAmount() < 1000) {
         float da = Math.min(0.1F, (1000 - tank.getFluidAmount()) / 1000.0F);
         float dv = AuraHandler.drainVis(level, worldPosition, da, false);
         int wa = (int) (1000.0F * dv);
         if (wa > 0) {
            tank.fill(new FluidStack(Fluids.WATER, wa), IFluidHandler.FluidAction.EXECUTE);
            if (tank.getFluidAmount() >= tank.getCapacity()) sync();
         }
      }
   }

   public FluidStack drain(int amount, IFluidHandler.FluidAction action) {
      boolean full = tank.getFluidAmount() >= tank.getCapacity();
      FluidStack fs = tank.drain(amount, action);
      if (full && tank.getFluidAmount() < tank.getCapacity()) sync();
      return fs;
   }

   @Override
   public boolean triggerEvent(int id, int param) {
      if (id == 1) {
         if (level != null && level.isClientSide) {
            zc = param;
            tcount = 5;
         }
         return true;
      }
      return super.triggerEvent(id, param);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      tank.setFluid(FluidStack.EMPTY);
      tank.readFromNBT(registries, tag);
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tank.writeToNBT(registries, tag);
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      handlers = new ArrayList<>();
      for (int h : tag.getIntArray("handlers")) handlers.add(h);
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      tag.put("handlers", new IntArrayTag(handlers.stream().mapToInt(Integer::intValue).toArray()));
   }
}
