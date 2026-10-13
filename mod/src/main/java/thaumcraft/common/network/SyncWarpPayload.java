package thaumcraft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.research.PlayerWarp;

/** Server -> client: the player's warp. */
public record SyncWarpPayload(PlayerWarp warp) implements CustomPacketPayload {
   public static final Type<SyncWarpPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_warp"));
   public static final StreamCodec<ByteBuf, SyncWarpPayload> STREAM_CODEC = PlayerWarp.STREAM_CODEC.map(SyncWarpPayload::new, SyncWarpPayload::warp);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
