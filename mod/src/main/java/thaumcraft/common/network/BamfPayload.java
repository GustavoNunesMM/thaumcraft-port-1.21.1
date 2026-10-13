package thaumcraft.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** PacketFXBlockBamf. side: -1 for none, else a Direction index; color: -9999 for the default purple. */
public record BamfPayload(double x, double y, double z, int color, boolean sound, boolean flair, int side) implements CustomPacketPayload {
   public static final Type<BamfPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "bamf"));
   public static final StreamCodec<FriendlyByteBuf, BamfPayload> STREAM_CODEC = StreamCodec.of(
      (buf, p) -> {
         buf.writeDouble(p.x);
         buf.writeDouble(p.y);
         buf.writeDouble(p.z);
         buf.writeInt(p.color);
         buf.writeBoolean(p.sound);
         buf.writeBoolean(p.flair);
         buf.writeByte(p.side);
      },
      buf -> new BamfPayload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readByte()));

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
