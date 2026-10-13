package thaumcraft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.EnumKnowledgeType;

/** PacketKnowledgeGain: one whole knowledge point gained, for the HUD animation. Category "" = none. */
public record KnowledgeGainPayload(EnumKnowledgeType knowledgeType, String category) implements CustomPacketPayload {
   public static final Type<KnowledgeGainPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "knowledge_gain"));
   public static final StreamCodec<ByteBuf, KnowledgeGainPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT.<EnumKnowledgeType>map(i -> EnumKnowledgeType.values()[i], Enum::ordinal), KnowledgeGainPayload::knowledgeType,
      ByteBufCodecs.STRING_UTF8, KnowledgeGainPayload::category,
      KnowledgeGainPayload::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
