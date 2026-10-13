package thaumcraft.common.golems;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.common.golems.menu.GolemBuilderMenu;
import thaumcraft.common.golems.menu.LogisticsMenu;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.network.ModNetwork;

/**
 * The golem packets: PacketSealToClient (as the seal's NBT), the golem press's component check and craft
 * (messageFromClient/Server of TileGolemBuilder), PacketLogisticsRequestToServer and the logistics search text.
 */
public final class GolemPayloads {
   private GolemPayloads() {
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   public record SealSync(CompoundTag tag) implements CustomPacketPayload {
      public static final Type<SealSync> TYPE = new Type<>(id("seal_sync"));
      public static final StreamCodec<FriendlyByteBuf, SealSync> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, SealSync::tag,
         SealSync::new);

      @Override
      public Type<SealSync> type() {
         return TYPE;
      }
   }

   public record BuilderCheck(BlockPos pos, long golem) implements CustomPacketPayload {
      public static final Type<BuilderCheck> TYPE = new Type<>(id("golem_builder_check"));
      public static final StreamCodec<FriendlyByteBuf, BuilderCheck> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, BuilderCheck::pos,
         ByteBufCodecs.VAR_LONG, BuilderCheck::golem, BuilderCheck::new);

      @Override
      public Type<BuilderCheck> type() {
         return TYPE;
      }
   }

   public record BuilderCraft(BlockPos pos, long golem) implements CustomPacketPayload {
      public static final Type<BuilderCraft> TYPE = new Type<>(id("golem_builder_craft"));
      public static final StreamCodec<FriendlyByteBuf, BuilderCraft> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, BuilderCraft::pos,
         ByteBufCodecs.VAR_LONG, BuilderCraft::golem, BuilderCraft::new);

      @Override
      public Type<BuilderCraft> type() {
         return TYPE;
      }
   }

   public record BuilderStuff(BlockPos pos, byte[] stuff) implements CustomPacketPayload {
      public static final Type<BuilderStuff> TYPE = new Type<>(id("golem_builder_stuff"));
      public static final StreamCodec<FriendlyByteBuf, BuilderStuff> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, BuilderStuff::pos,
         ByteBufCodecs.BYTE_ARRAY, BuilderStuff::stuff, BuilderStuff::new);

      @Override
      public Type<BuilderStuff> type() {
         return TYPE;
      }
   }

   public record LogisticsRequest(Optional<BlockPos> pos, int side, ItemStack stack, int size) implements CustomPacketPayload {
      public static final Type<LogisticsRequest> TYPE = new Type<>(id("logistics_request"));
      public static final StreamCodec<RegistryFriendlyByteBuf, LogisticsRequest> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.optional(BlockPos.STREAM_CODEC), LogisticsRequest::pos, ByteBufCodecs.VAR_INT, LogisticsRequest::side, ItemStack.STREAM_CODEC,
         LogisticsRequest::stack, ByteBufCodecs.VAR_INT, LogisticsRequest::size, LogisticsRequest::new);

      @Override
      public Type<LogisticsRequest> type() {
         return TYPE;
      }
   }

   public record LogisticsSearch(String text) implements CustomPacketPayload {
      public static final Type<LogisticsSearch> TYPE = new Type<>(id("logistics_search"));
      public static final StreamCodec<FriendlyByteBuf, LogisticsSearch> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.stringUtf8(32),
         LogisticsSearch::text, LogisticsSearch::new);

      @Override
      public Type<LogisticsSearch> type() {
         return TYPE;
      }
   }

   public static void sendToServer(CustomPacketPayload p) {
      PacketDistributor.sendToServer(p);
   }

   public static void register(PayloadRegistrar r) {
      r.playToClient(SealSync.TYPE, SealSync.STREAM_CODEC, (p, ctx) -> SealHandler.acceptSync(ctx.player().level(), p.tag()));
      r.playToClient(BuilderStuff.TYPE, BuilderStuff.STREAM_CODEC, (p, ctx) -> {
         if (ctx.player().level().getBlockEntity(p.pos()) instanceof GolemBuilderBlockEntity be) {
            be.hasStuff = new boolean[p.stuff().length];
            for (int a = 0; a < p.stuff().length; a++) be.hasStuff[a] = p.stuff()[a] == 1;
            GolemBuilderMenu.redo = true;
         }
      });
      r.playToServer(BuilderCheck.TYPE, BuilderCheck.STREAM_CODEC, (p, ctx) -> {
         if (ctx.player() instanceof ServerPlayer sp && builder(sp, p.pos()) instanceof GolemBuilderBlockEntity be) {
            boolean[] has = be.checkCraft(p.golem());
            byte[] ba = new byte[has.length];
            for (int a = 0; a < ba.length; a++) ba[a] = (byte) (has[a] ? 1 : 0);
            ModNetwork.sendTo(sp, new BuilderStuff(p.pos(), ba));
         }
      });
      r.playToServer(BuilderCraft.TYPE, BuilderCraft.STREAM_CODEC, (p, ctx) -> {
         if (ctx.player() instanceof ServerPlayer sp && builder(sp, p.pos()) instanceof GolemBuilderBlockEntity be && be.cost <= 0) {
            be.startCraft(p.golem(), sp);
         }
      });
      r.playToServer(LogisticsRequest.TYPE, LogisticsRequest.STREAM_CODEC, (p, ctx) -> {
         var player = ctx.player();
         int left = p.size();
         Direction side = Direction.from3DDataValue(p.side());
         for (int ui = 0; left > 0 && !p.stack().isEmpty(); ui++) {
            ItemStack s = p.stack().copy();
            s.setCount(Math.min(left, s.getMaxStackSize()));
            left -= s.getCount();
            if (p.pos().isPresent()) {
               GolemHelper.requestProvisioning(player.level(), p.pos().get(), side, s, ui);
            } else {
               GolemHelper.requestProvisioning(player.level(), player, s, ui);
            }
         }
      });
      r.playToServer(LogisticsSearch.TYPE, LogisticsSearch.STREAM_CODEC, (p, ctx) -> {
         if (ctx.player().containerMenu instanceof LogisticsMenu m) m.setSearch(p.text());
      });
   }

   private static GolemBuilderBlockEntity builder(ServerPlayer sp, BlockPos pos) {
      if (sp.distanceToSqr(pos.getCenter()) > 64.0) return null;
      return sp.level().getBlockEntity(pos) instanceof GolemBuilderBlockEntity be ? be : null;
   }
}
