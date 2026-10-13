package thaumcraft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import thaumcraft.Thaumcraft;

/** Milestone 12 client effects. kind 0: PacketFXPollute (pos, amount = number of puffs). */
public record FluxFXPayload(int kind, BlockPos pos, float amount) implements CustomPacketPayload {
   public static final Type<FluxFXPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "flux_fx"));
   public static final StreamCodec<ByteBuf, FluxFXPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, FluxFXPayload::kind, BlockPos.STREAM_CODEC, FluxFXPayload::pos, ByteBufCodecs.FLOAT, FluxFXPayload::amount,
      FluxFXPayload::new);

   public static FluxFXPayload pollute(BlockPos pos, float amount) {
      return new FluxFXPayload(0, pos, amount);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void register(PayloadRegistrar r) {
      r.playToClient(TYPE, STREAM_CODEC, (payload, ctx) -> thaumcraft.client.fx.TaintFX.handle(payload));
   }
}
