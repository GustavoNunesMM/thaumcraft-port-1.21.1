package thaumcraft.common.tiles;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.tiles.PacketTileToClient;
import thaumcraft.common.lib.network.tiles.PacketTileToServer;

public class TileThaumcraft extends TileEntity {
   public final void sendMessageToClient(NBTTagCompound nbt, @Nullable EntityPlayerMP player) {
      if (player == null) {
         if (this.getWorld() != null) {
            PacketHandler.INSTANCE
               .sendToAllAround(
                  new PacketTileToClient(this.getPos(), nbt),
                  new TargetPoint(
                     this.getWorld().provider.getDimension(),
                     this.pos.getX() + 0.5,
                     this.pos.getY() + 0.5,
                     this.pos.getZ() + 0.5,
                     128.0
                  )
               );
         }
      } else {
         PacketHandler.INSTANCE.sendTo(new PacketTileToClient(this.getPos(), nbt), player);
      }
   }

   public final void sendMessageToServer(NBTTagCompound nbt) {
      PacketHandler.INSTANCE.sendToServer(new PacketTileToServer(this.getPos(), nbt));
   }

   public void messageFromServer(NBTTagCompound nbt) {
   }

   public void messageFromClient(NBTTagCompound nbt, EntityPlayerMP player) {
   }

   public void readFromNBT(NBTTagCompound nbt) {
      super.readFromNBT(nbt);
      this.readSyncNBT(nbt);
   }

   public void readSyncNBT(NBTTagCompound nbt) {
   }

   public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
      return this.writeSyncNBT(super.writeToNBT(nbt));
   }

   public NBTTagCompound writeSyncNBT(NBTTagCompound nbt) {
      return nbt;
   }

   public void syncTile(boolean rerender) {
      IBlockState state = this.world.getBlockState(this.pos);
      this.world.notifyBlockUpdate(this.pos, state, state, 2 + (rerender ? 4 : 0));
   }

   @Nullable
   public SPacketUpdateTileEntity getUpdatePacket() {
      return new SPacketUpdateTileEntity(this.pos, -9, this.getUpdateTag());
   }

   public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
      this.readSyncNBT(pkt.getNbtCompound());
   }

   public NBTTagCompound getUpdateTag() {
      return this.writeSyncNBT(this.setupNbt());
   }

   private NBTTagCompound setupNbt() {
      NBTTagCompound nbt = super.writeToNBT(new NBTTagCompound());
      nbt.removeTag("ForgeData");
      nbt.removeTag("ForgeCaps");
      return nbt;
   }

   public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
      return oldState.getBlock() != newState.getBlock();
   }

   public EnumFacing getFacing() {
      try {
         return EnumFacing.byIndex(this.getBlockMetadata() & 7);
      } catch (Exception var2) {
         return EnumFacing.UP;
      }
   }

   public boolean gettingPower() {
      return this.world.isBlockPowered(this.getPos());
   }
}
