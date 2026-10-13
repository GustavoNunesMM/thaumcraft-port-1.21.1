package thaumcraft.common.blocks.devices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;

/**
 * TileArcaneEar: note (0-24) set by clicking, tone = the instrument of the block it is attached to. Hears the note
 * blocks played this tick (recorded by {@link DeviceEvents}) within 64 blocks.
 */
public class ArcaneEarBlockEntity extends TCBlockEntity {
   /** A note block that played: where, which instrument (ordinal), which note, and a serial number. */
   public record NoteEvent(BlockPos pos, int instrument, int note, long serial) {
   }

   public static final Map<Level, List<NoteEvent>> NOTE_EVENTS = new WeakHashMap<>();
   private static long serials = 0;

   public byte note = 0;
   public byte tone = 0;
   public int redstoneSignal = 0;
   private long lastSerial = -1;

   public ArcaneEarBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.ARCANE_EAR_BE.get(), pos, state);
   }

   public static synchronized void recordNote(Level level, BlockPos pos, int instrument, int note) {
      NOTE_EVENTS.computeIfAbsent(level, l -> new ArrayList<>()).add(new NoteEvent(pos.immutable(), instrument, note, ++serials));
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneEarBlockEntity be) {
      if (be.lastSerial < 0) be.lastSerial = serials;
      if (be.redstoneSignal > 0) {
         be.redstoneSignal--;
         if (be.redstoneSignal == 0) {
            level.setBlock(pos, level.getBlockState(pos).setValue(DeviceBlock.ENABLED, false), 3);
            be.notifyAround();
         }
      }
      List<NoteEvent> nbe = NOTE_EVENTS.get(level);
      if (nbe == null) return;
      for (NoteEvent dat : List.copyOf(nbe)) {
         if (dat.serial() <= be.lastSerial) continue;
         be.lastSerial = dat.serial();
         if (dat.instrument() == be.tone && dat.note() == be.note && pos.getCenter().distanceToSqr(dat.pos().getCenter()) <= 4096.0) {
            be.triggerNote(level, pos, true);
            BlockState s = level.getBlockState(pos);
            if (s.getBlock() instanceof ArcaneEarBlock ear && ear.toggle) {
               level.setBlock(pos, s.setValue(DeviceBlock.ENABLED, !DeviceBlock.isEnabled(s)), 3);
            } else {
               be.redstoneSignal = 10;
               level.setBlock(pos, s.setValue(DeviceBlock.ENABLED, true), 3);
            }
            be.notifyAround();
            break;
         }
      }
   }

   private void notifyAround() {
      Direction facing = DeviceBlock.getFacing(getBlockState()).getOpposite();
      level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
      level.updateNeighborsAt(worldPosition.relative(facing), getBlockState().getBlock());
   }

   /** The instrument of the block it hangs on, as a note block on top of it would play. */
   public int computeTone() {
      if (level == null) return 0;
      Direction facing = DeviceBlock.getFacing(getBlockState()).getOpposite();
      return level.getBlockState(worldPosition.relative(facing)).instrument().ordinal();
   }

   public void updateTone() {
      tone = (byte) computeTone();
      setChanged();
   }

   public void changePitch() {
      note = (byte) ((note + 1) % 25);
      setChanged();
   }

   public void triggerNote(Level level, BlockPos pos, boolean sound) {
      int i = sound ? computeTone() : -1;
      level.blockEvent(pos, getBlockState().getBlock(), i, note);
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      note = (byte) Math.max(0, Math.min(24, tag.getByte("note")));
      tone = tag.getByte("tone");
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putByte("note", note);
      tag.putByte("tone", tone);
   }
}
