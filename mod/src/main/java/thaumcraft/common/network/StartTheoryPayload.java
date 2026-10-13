package thaumcraft.common.network;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** PacketStartTheoryToServer: start a theory at a research table with the chosen research aids. */
public record StartTheoryPayload(BlockPos pos, List<String> aids) implements CustomPacketPayload {
   public static final Type<StartTheoryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "start_theory"));
   public static final StreamCodec<ByteBuf, StartTheoryPayload> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, StartTheoryPayload::pos, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), StartTheoryPayload::aids,
      StartTheoryPayload::new);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
