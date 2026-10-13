package thaumcraft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.CuriosHelper;
import thaumcraft.common.registers.ModGear;

/** PacketPlayerFlagToServer flag 1: the cloud ring jumped in mid-air, so the server resets the fall distance. */
public record CloudJumpPayload() implements CustomPacketPayload {
   public static final CloudJumpPayload INSTANCE = new CloudJumpPayload();
   public static final Type<CloudJumpPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "cloud_jump"));
   public static final StreamCodec<ByteBuf, CloudJumpPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   public static void sendToServer() {
      PacketDistributor.sendToServer(INSTANCE);
   }

   public static void register(PayloadRegistrar r) {
      r.playToServer(TYPE, STREAM_CODEC, (payload, ctx) -> {
         if (CuriosHelper.isEquipped(ctx.player(), ModGear.CLOUD_RING.get())) ctx.player().fallDistance = 0.0F;
      });
   }
}
