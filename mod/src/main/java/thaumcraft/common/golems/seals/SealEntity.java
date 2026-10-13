package thaumcraft.common.golems.seals;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.tasks.TaskHandler;

public class SealEntity implements ISealEntity {
   SealPos sealPos;
   ISeal seal;
   byte priority = 0;
   byte color = 0;
   boolean locked = false;
   boolean redstone = false;
   String owner = "";
   boolean stopped = false;
   private BlockPos area = new BlockPos(1, 1, 1);

   public SealEntity() {
   }

   public SealEntity(SealPos sealPos, @Nullable ISeal seal) {
      this.sealPos = sealPos;
      this.seal = seal;
      if (seal instanceof ISealConfigArea) {
         int x = sealPos.face.getStepX() == 0 ? 3 : 1;
         int y = sealPos.face.getStepY() == 0 ? 3 : 1;
         int z = sealPos.face.getStepZ() == 0 ? 3 : 1;
         this.area = new BlockPos(x, y, z);
      }
   }

   @Override
   public void tickSealEntity(Level level) {
      if (seal == null) return;
      if (isStoppedByRedstone(level)) {
         if (!stopped) {
            for (Task t : TaskHandler.getTasks(level).values()) {
               if (t.getSealPos() != null && t.getSealPos().equals(sealPos)) t.setSuspended(true);
            }
         }
         stopped = true;
         return;
      }
      stopped = false;
      seal.tickSeal(level, this);
   }

   @Override
   public boolean isStoppedByRedstone(Level level) {
      return isRedstoneSensitive() && (level.hasNeighborSignal(sealPos.pos) || level.hasNeighborSignal(sealPos.pos.relative(sealPos.face)));
   }

   @Override
   public ISeal getSeal() {
      return seal;
   }

   @Override
   public SealPos getSealPos() {
      return sealPos;
   }

   @Override
   public byte getPriority() {
      return priority;
   }

   @Override
   public void setPriority(byte priority) {
      this.priority = priority;
   }

   @Override
   public byte getColor() {
      return color;
   }

   @Override
   public void setColor(byte color) {
      this.color = color;
   }

   @Override
   public String getOwner() {
      return owner;
   }

   @Override
   public void setOwner(String owner) {
      this.owner = owner;
   }

   @Override
   public boolean isLocked() {
      return locked;
   }

   @Override
   public void setLocked(boolean locked) {
      this.locked = locked;
   }

   @Override
   public boolean isRedstoneSensitive() {
      return redstone;
   }

   @Override
   public void setRedstoneSensitive(boolean redstone) {
      this.redstone = redstone;
   }

   @Override
   public void readNBT(CompoundTag nbt, HolderLookup.Provider registries) {
      BlockPos p = BlockPos.of(nbt.getLong("pos"));
      Direction face = Direction.from3DDataValue(nbt.getByte("face"));
      this.sealPos = new SealPos(p, face);
      setPriority(nbt.getByte("priority"));
      setColor(nbt.getByte("color"));
      setLocked(nbt.getBoolean("locked"));
      setRedstoneSensitive(nbt.getBoolean("redstone"));
      setOwner(nbt.getString("owner"));
      ISeal type = SealHandler.getSeal(nbt.getString("type"));
      this.seal = type == null ? null : type.newInstance();
      if (seal != null) {
         seal.readCustomNBT(nbt, registries);
         if (seal instanceof ISealConfigArea) this.area = BlockPos.of(nbt.getLong("area"));
         if (seal instanceof ISealConfigToggles t) {
            for (ISealConfigToggles.SealToggle prop : t.getToggles()) {
               if (nbt.contains(prop.getKey())) prop.setValue(nbt.getBoolean(prop.getKey()));
            }
         }
      }
   }

   @Override
   public CompoundTag writeNBT(HolderLookup.Provider registries) {
      CompoundTag nbt = new CompoundTag();
      nbt.putLong("pos", sealPos.pos.asLong());
      nbt.putByte("face", (byte) sealPos.face.get3DDataValue());
      nbt.putString("type", seal == null ? "REMOVE" : seal.getKey());
      nbt.putByte("priority", getPriority());
      nbt.putByte("color", getColor());
      nbt.putBoolean("locked", isLocked());
      nbt.putBoolean("redstone", isRedstoneSensitive());
      nbt.putString("owner", getOwner());
      if (seal != null) {
         seal.writeCustomNBT(nbt, registries);
         if (seal instanceof ISealConfigArea) nbt.putLong("area", area.asLong());
         if (seal instanceof ISealConfigToggles t) {
            for (ISealConfigToggles.SealToggle prop : t.getToggles()) nbt.putBoolean(prop.getKey(), prop.getValue());
         }
      }
      return nbt;
   }

   @Override
   public void syncToClient(Level level) {
      if (level instanceof ServerLevel sl) SealHandler.sendToDimension(sl, this);
   }

   @Override
   public BlockPos getArea() {
      return area;
   }

   @Override
   public void setArea(BlockPos v) {
      this.area = v;
   }
}
