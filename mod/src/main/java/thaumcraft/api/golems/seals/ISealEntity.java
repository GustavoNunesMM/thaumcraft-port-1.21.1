package thaumcraft.api.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

/** A seal placed on a block face: the seal instance plus its common settings. */
public interface ISealEntity {
   void tickSealEntity(Level level);

   ISeal getSeal();

   SealPos getSealPos();

   byte getPriority();

   void setPriority(byte priority);

   void readNBT(CompoundTag nbt, HolderLookup.Provider registries);

   CompoundTag writeNBT(HolderLookup.Provider registries);

   void syncToClient(Level level);

   BlockPos getArea();

   void setArea(BlockPos area);

   boolean isLocked();

   void setLocked(boolean locked);

   boolean isRedstoneSensitive();

   void setRedstoneSensitive(boolean redstone);

   String getOwner();

   void setOwner(String owner);

   byte getColor();

   void setColor(byte color);

   boolean isStoppedByRedstone(Level level);
}
