package thaumcraft.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** PacketFXScanSource: a sounding pickaxe shows the ore veins around the clicked block. */
public record ScanSourcePayload(BlockPos pos, int size) implements CustomPacketPayload {
   public static final Type<ScanSourcePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "scan_source"));
   public static final StreamCodec<FriendlyByteBuf, ScanSourcePayload> STREAM_CODEC = StreamCodec.of(
      (buf, p) -> {
         buf.writeBlockPos(p.pos);
         buf.writeByte(p.size);
      },
      buf -> new ScanSourcePayload(buf.readBlockPos(), buf.readByte()));

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
