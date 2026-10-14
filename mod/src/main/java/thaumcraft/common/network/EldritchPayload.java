package thaumcraft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import thaumcraft.Thaumcraft;

/**
 * Milestone 14 client messages.
 * <ul>
 * <li>kind 0..2: PacketMiscEvent (0 = warp heartbeat, 1 = the warp mist rolls in for 2 minutes, 2 = an eldritch
 * guardian nearby thickens the fog for at least 10 seconds); {@code value} unused.</li>
 * <li>kind 3: PacketFXSonic, the screech wave of guardians and wardens; {@code value} = the entity id.</li>
 * <li>kind 4: the void shield of an eldritch mob absorbs a hit (PacketFXShield); {@code value} = the entity id.</li>
 * </ul>
 */
public record EldritchPayload(int kind, int value) implements CustomPacketPayload {
   public static final int HEARTBEAT = 0;
   public static final int MIST = 1;
   public static final int GUARDIAN_FOG = 2;
   public static final int SONIC = 3;
   public static final int SHIELD = 4;
   public static final Type<EldritchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch"));
   public static final StreamCodec<ByteBuf, EldritchPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, EldritchPayload::kind, ByteBufCodecs.VAR_INT, EldritchPayload::value, EldritchPayload::new);

   public static void send(ServerPlayer player, int kind) {
      ModNetwork.sendTo(player, new EldritchPayload(kind, 0));
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void register(PayloadRegistrar r) {
      r.playToClient(TYPE, STREAM_CODEC, (payload, ctx) -> thaumcraft.client.EldritchClient.handle(payload));
   }
}
