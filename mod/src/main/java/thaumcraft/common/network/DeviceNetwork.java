package thaumcraft.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.devices.ThaumatoriumBlockEntity;

/** Payloads of the devices milestone. */
public final class DeviceNetwork {
   private DeviceNetwork() {
   }

   /** PacketSelectThaumotoriumRecipeToServer: toggle a crucible recipe on a thaumatorium. */
   public record ThaumatoriumSelectPayload(BlockPos pos, ResourceLocation recipe) implements CustomPacketPayload {
      public static final Type<ThaumatoriumSelectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "thaumatorium_select"));
      public static final StreamCodec<RegistryFriendlyByteBuf, ThaumatoriumSelectPayload> STREAM_CODEC = StreamCodec.composite(
         BlockPos.STREAM_CODEC, ThaumatoriumSelectPayload::pos, ResourceLocation.STREAM_CODEC, ThaumatoriumSelectPayload::recipe,
         ThaumatoriumSelectPayload::new);

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   public static void register(PayloadRegistrar r) {
      r.playToServer(ThaumatoriumSelectPayload.TYPE, ThaumatoriumSelectPayload.STREAM_CODEC, (payload, ctx) -> {
         var player = ctx.player();
         if (player.distanceToSqr(payload.pos().getCenter()) > 64.0) return;
         if (player.level().getBlockEntity(payload.pos()) instanceof ThaumatoriumBlockEntity t) t.selectRecipe(player, payload.recipe());
      });
   }
}
