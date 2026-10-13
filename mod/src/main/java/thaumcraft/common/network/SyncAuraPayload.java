package thaumcraft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.world.aura.AuraChunk;

/** PacketAuraToClient: the aura of the chunk the player stands in, for the thaumometer HUD. */
public record SyncAuraPayload(short base, float vis, float flux) implements CustomPacketPayload {
   public static final Type<SyncAuraPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "aura"));

   public static final StreamCodec<ByteBuf, SyncAuraPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.SHORT, SyncAuraPayload::base,
      ByteBufCodecs.FLOAT, SyncAuraPayload::vis,
      ByteBufCodecs.FLOAT, SyncAuraPayload::flux,
      SyncAuraPayload::new
   );

   public static SyncAuraPayload of(AuraChunk ac) {
      return new SyncAuraPayload(ac.getBase(), ac.getVis(), ac.getFlux());
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
