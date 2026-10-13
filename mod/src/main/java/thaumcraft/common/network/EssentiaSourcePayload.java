package thaumcraft.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** PacketFXEssentiaSource: essentia flowing from a source block to the machine using it. */
public record EssentiaSourcePayload(BlockPos from, BlockPos to, int color, int ext) implements CustomPacketPayload {
   public static final Type<EssentiaSourcePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "essentia_source"));
   public static final StreamCodec<FriendlyByteBuf, EssentiaSourcePayload> STREAM_CODEC = StreamCodec.of(
      (buf, p) -> {
         buf.writeBlockPos(p.from);
         buf.writeBlockPos(p.to);
         buf.writeInt(p.color);
         buf.writeShort(p.ext);
      },
      buf -> new EssentiaSourcePayload(buf.readBlockPos(), buf.readBlockPos(), buf.readInt(), buf.readShort()));

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
