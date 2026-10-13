package thaumcraft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.research.PlayerKnowledge;

/** Server -> client: the player's research knowledge. */
public record SyncKnowledgePayload(PlayerKnowledge knowledge) implements CustomPacketPayload {
   public static final Type<SyncKnowledgePayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_knowledge"));

   public static final StreamCodec<ByteBuf, SyncKnowledgePayload> STREAM_CODEC =
      PlayerKnowledge.STREAM_CODEC.map(SyncKnowledgePayload::new, SyncKnowledgePayload::knowledge);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
