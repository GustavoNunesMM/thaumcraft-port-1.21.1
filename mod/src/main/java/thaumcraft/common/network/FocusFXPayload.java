package thaumcraft.common.network;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;

/**
 * Focus effects on the client. kind: TRAIL (PacketFXFocusEffect: particles along a direction),
 * IMPACT / BURST (PacketFXFocusPartImpact(Burst): a puff where it hits), ZAP (PacketFXZap: an arc bolt to the target,
 * colour and width in color/width). keys: the focus effect keys whose renderParticleFX draws the particles.
 */
public record FocusFXPayload(int kind, Vec3 pos, Vec3 vec, List<String> keys, int color, float width) implements CustomPacketPayload {
   public static final int TRAIL = 0, IMPACT = 1, BURST = 2, ZAP = 3;
   public static final Type<FocusFXPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "focus_fx"));
   public static final StreamCodec<FriendlyByteBuf, FocusFXPayload> STREAM_CODEC = StreamCodec.of(
      (buf, p) -> {
         buf.writeByte(p.kind);
         buf.writeVec3(p.pos);
         buf.writeVec3(p.vec);
         buf.writeCollection(p.keys, FriendlyByteBuf::writeUtf);
         buf.writeInt(p.color);
         buf.writeFloat(p.width);
      },
      buf -> new FocusFXPayload(buf.readByte(), buf.readVec3(), buf.readVec3(), buf.readList(FriendlyByteBuf::readUtf), buf.readInt(), buf.readFloat()));

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   /** Sends to everyone within 64 blocks of pos. */
   public static void send(Level level, FocusFXPayload p) {
      if (level instanceof ServerLevel sl) ModNetwork.sendNear(sl, p.pos.x, p.pos.y, p.pos.z, 64.0, p);
   }

   public static void impact(Level level, Vec3 at, String... keys) {
      send(level, new FocusFXPayload(IMPACT, at, Vec3.ZERO, List.of(keys), 0, 0));
   }

   public static void burst(Level level, Vec3 at, String... keys) {
      send(level, new FocusFXPayload(BURST, at, Vec3.ZERO, List.of(keys), 0, 0));
   }
}
