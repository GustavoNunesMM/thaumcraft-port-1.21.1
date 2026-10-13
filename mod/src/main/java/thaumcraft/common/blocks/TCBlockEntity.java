package thaumcraft.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * TileThaumcraft: block entities split their data into a synced part (sent to clients, readSyncNBT/writeSyncNBT)
 * and a server-only part (readExtra/writeExtra).
 */
public abstract class TCBlockEntity extends BlockEntity {
   protected TCBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
   }

   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
   }

   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
   }

   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
   }

   @Override
   protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      readSync(tag, registries);
      readExtra(tag, registries);
   }

   @Override
   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      writeSync(tag, registries);
      writeExtra(tag, registries);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      CompoundTag tag = new CompoundTag();
      writeSync(tag, registries);
      return tag;
   }

   @Override
   public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
      readSync(tag, registries);
   }

   @Override
   public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
      readSync(pkt.getTag(), registries);
   }

   @Override
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   /** syncTile: sends the synced data to the clients tracking this block (and saves). */
   public void sync() {
      setChanged();
      if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
   }

   protected static void putAspect(CompoundTag tag, String key, @javax.annotation.Nullable thaumcraft.api.aspects.Aspect a) {
      if (a != null) tag.putString(key, a.getTag());
   }

   @javax.annotation.Nullable
   protected static thaumcraft.api.aspects.Aspect getAspect(CompoundTag tag, String key) {
      return tag.contains(key) ? thaumcraft.api.aspects.Aspect.getAspect(tag.getString(key)) : null;
   }

   public boolean gettingPower() {
      return level != null && level.hasNeighborSignal(worldPosition);
   }
}
