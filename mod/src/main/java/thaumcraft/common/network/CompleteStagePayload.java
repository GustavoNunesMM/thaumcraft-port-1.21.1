package thaumcraft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * PacketSyncProgressToServer: client -> server request to progress a research from the Thaumonomicon.
 * {@code first} = true to start an unknown research, false to progress a known one (ignored if the server disagrees),
 * {@code checks} = verify and take the stage requirements, {@code noFlags} = don't flag it as new research.
 */
public record CompleteStagePayload(String key, boolean first, boolean checks, boolean noFlags) implements CustomPacketPayload {
   public static final Type<CompleteStagePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "complete_stage"));
   public static final StreamCodec<ByteBuf, CompleteStagePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.STRING_UTF8, CompleteStagePayload::key, ByteBufCodecs.BOOL, CompleteStagePayload::first,
      ByteBufCodecs.BOOL, CompleteStagePayload::checks, ByteBufCodecs.BOOL, CompleteStagePayload::noFlags,
      CompleteStagePayload::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
