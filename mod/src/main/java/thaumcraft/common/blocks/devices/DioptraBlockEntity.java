package thaumcraft.common.blocks.devices;

import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

/** TileDioptra: every second samples the vis (or flux) of the 13x13 chunks around it, scaled to 0-64. */
public class DioptraBlockEntity extends TCBlockEntity {
   public int counter = 0;
   public byte[] gridAmt = new byte[169];

   public DioptraBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.DIOPTRA_BE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, DioptraBlockEntity be) {
      be.counter++;
      if (level.isClientSide) return;
      if (be.counter % 20 == 0) {
         Arrays.fill(be.gridAmt, (byte) 0);
         boolean vis = DeviceBlock.isEnabled(state);
         for (int xx = 0; xx < 13; xx++) {
            for (int zz = 0; zz < 13; zz++) {
               AuraChunk ac = AuraHandler.getAuraChunk(level, (pos.getX() >> 4) + xx - 6, (pos.getZ() >> 4) + zz - 6);
               if (ac != null) {
                  float v = vis ? ac.getVis() : ac.getFlux();
                  be.gridAmt[xx + zz * 13] = (byte) Math.min(64.0F, v / 500.0F * 64.0F);
               }
            }
         }
         be.sync();
      }
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      if (tag.contains("grid_a")) {
         byte[] g = tag.getByteArray("grid_a");
         if (g.length == 169) gridAmt = g;
      }
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putByteArray("grid_a", gridAmt);
   }
}
