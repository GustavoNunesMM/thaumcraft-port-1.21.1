package thaumcraft.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import thaumcraft.Thaumcraft;

/**
 * Infusion effects: PacketFXInfusionSource (an item, or a player's experience, flowing into the matrix) and
 * PacketFXBlockArc (a lightning arc from a block to a block or an entity).
 */
public record InfusionFXPayload(BlockPos pos, BlockPos target, int entity, int kind) implements CustomPacketPayload {
   public static final int SOURCE = 0;
   public static final int ARC_BLOCK = 1;
   public static final int ARC_ENTITY = 2;
   public static final Type<InfusionFXPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "infusion_fx"));
   public static final StreamCodec<FriendlyByteBuf, InfusionFXPayload> STREAM_CODEC = StreamCodec.of(
      (buf, p) -> {
         buf.writeBlockPos(p.pos);
         buf.writeBlockPos(p.target);
         buf.writeVarInt(p.entity);
         buf.writeByte(p.kind);
      },
      buf -> new InfusionFXPayload(buf.readBlockPos(), buf.readBlockPos(), buf.readVarInt(), buf.readByte()));

   public static InfusionFXPayload arc(BlockPos from, Entity target) {
      return new InfusionFXPayload(from, from, target.getId(), ARC_ENTITY);
   }

   public static InfusionFXPayload arcBlocks(BlockPos from, BlockPos to) {
      return new InfusionFXPayload(from, to, 0, ARC_BLOCK);
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
