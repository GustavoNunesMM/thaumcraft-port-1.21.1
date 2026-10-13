package thaumcraft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * PacketSyncResearchFlagsToServer: the player looked at an entry, so its "new research"/"new page" markers go away;
 * popupOnly = only the "show the completion toast" flag (the toast was shown).
 */
public record ClearResearchFlagsPayload(String key, boolean popupOnly) implements CustomPacketPayload {
   public static final Type<ClearResearchFlagsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "clear_research_flags"));
   public static final StreamCodec<ByteBuf, ClearResearchFlagsPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, ClearResearchFlagsPayload::key, ByteBufCodecs.BOOL, ClearResearchFlagsPayload::popupOnly,
      ClearResearchFlagsPayload::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
