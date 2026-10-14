package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;

/** TileRedstoneRelay: the input threshold (1-15) and output strength (1-15). */
public class RedstoneRelayBlockEntity extends TCBlockEntity {
   private int in = 1;
   private int out = 15;

   public RedstoneRelayBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.REDSTONE_RELAY_BE.get(), pos, state);
   }

   public void increaseIn() {
      in = in >= 15 ? 1 : in + 1;
      sync();
   }

   public void increaseOut() {
      out = out >= 15 ? 1 : out + 1;
      sync();
   }

   public int getIn() {
      return in;
   }

   public void setIn(int in) {
      this.in = in;
   }

   public int getOut() {
      return out;
   }

   public void setOut(int out) {
      this.out = out;
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      in = tag.contains("in") ? tag.getByte("in") : 1;
      out = tag.contains("out") ? tag.getByte("out") : 15;
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putByte("in", (byte) in);
      tag.putByte("out", (byte) out);
   }
}
