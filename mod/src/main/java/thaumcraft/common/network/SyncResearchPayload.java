package thaumcraft.common.network;

import io.netty.buffer.ByteBuf;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Server -> client: the research JSON files of the datapacks, parsed client-side for the Thaumonomicon. */
public record SyncResearchPayload(Map<String, String> files) implements CustomPacketPayload {
   public static final Type<SyncResearchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_research"));
   public static final StreamCodec<ByteBuf, SyncResearchPayload> STREAM_CODEC =
      ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.stringUtf8(1 << 20))
         .map(m -> new SyncResearchPayload(m), p -> new LinkedHashMap<>(p.files()));

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
