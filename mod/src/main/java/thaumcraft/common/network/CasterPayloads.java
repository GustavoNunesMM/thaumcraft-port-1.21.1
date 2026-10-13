package thaumcraft.common.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.ICaster;
import thaumcraft.common.blocks.crafting.FocalManipulatorBlockEntity;
import thaumcraft.common.blocks.crafting.FocusElementNode;
import thaumcraft.common.items.casters.CasterManager;

/** Client to server packets of the casters milestone (PacketFocusChangeToServer, PacketItemKeyToServer, PacketFocusNodes/NameToServer). */
public final class CasterPayloads {
   private CasterPayloads() {
   }

   private static ResourceLocation id(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   /** Swap the held caster's focus for the one with this sorting key ("REMOVE" takes it out). */
   public record FocusChange(String focus) implements CustomPacketPayload {
      public static final Type<FocusChange> TYPE = new Type<>(id("focus_change"));
      public static final StreamCodec<FriendlyByteBuf, FocusChange> STREAM_CODEC = StreamCodec.of((b, p) -> b.writeUtf(p.focus),
         b -> new FocusChange(b.readUtf()));

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   /** An item key: 1 is the misc caster toggle (G; mod 1 with CTRL, 2 with SHIFT). */
   public record ItemKey(int key, int mod) implements CustomPacketPayload {
      public static final Type<ItemKey> TYPE = new Type<>(id("item_key"));
      public static final StreamCodec<FriendlyByteBuf, ItemKey> STREAM_CODEC = StreamCodec.of((b, p) -> {
         b.writeByte(p.key);
         b.writeByte(p.mod);
      }, b -> new ItemKey(b.readByte(), b.readByte()));

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   /** The focal manipulator's node tree and focus name, as edited in its screen. */
   public record FocusNodes(BlockPos pos, List<CompoundTag> nodes, String name) implements CustomPacketPayload {
      public static final Type<FocusNodes> TYPE = new Type<>(id("focus_nodes"));
      public static final StreamCodec<FriendlyByteBuf, FocusNodes> STREAM_CODEC = StreamCodec.of((b, p) -> {
         b.writeBlockPos(p.pos);
         b.writeCollection(p.nodes, (bb, t) -> bb.writeNbt(t));
         b.writeUtf(p.name);
      }, b -> new FocusNodes(b.readBlockPos(), b.readList(bb -> bb.readNbt()), b.readUtf()));

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }

      public static FocusNodes of(BlockPos pos, Map<Integer, FocusElementNode> data, String name) {
         List<CompoundTag> l = new ArrayList<>();
         for (FocusElementNode n : data.values()) l.add(n.serialize());
         return new FocusNodes(pos, l, name);
      }
   }

   public record FocusName(BlockPos pos, String name) implements CustomPacketPayload {
      public static final Type<FocusName> TYPE = new Type<>(id("focus_name"));
      public static final StreamCodec<FriendlyByteBuf, FocusName> STREAM_CODEC = StreamCodec.of((b, p) -> {
         b.writeBlockPos(p.pos);
         b.writeUtf(p.name, 64);
      }, b -> new FocusName(b.readBlockPos(), b.readUtf(64)));

      @Override
      public Type<? extends CustomPacketPayload> type() {
         return TYPE;
      }
   }

   /** Client side: sends a packet to the server. */
   public static void send(CustomPacketPayload payload) {
      PacketDistributor.sendToServer(payload);
   }

   private static FocalManipulatorBlockEntity manipulator(Player player, BlockPos pos) {
      if (player.distanceToSqr(pos.getCenter()) > 64.0) return null;
      return player.level().getBlockEntity(pos) instanceof FocalManipulatorBlockEntity be ? be : null;
   }

   public static void register(PayloadRegistrar r) {
      r.playToServer(FocusChange.TYPE, FocusChange.STREAM_CODEC, (p, ctx) -> {
         Player player = ctx.player();
         ItemStack main = player.getMainHandItem();
         if (main.getItem() instanceof ICaster) CasterManager.changeFocus(main, player.level(), player, p.focus());
         else if (player.getOffhandItem().getItem() instanceof ICaster) CasterManager.changeFocus(player.getOffhandItem(), player.level(), player, p.focus());
      });
      r.playToServer(ItemKey.TYPE, ItemKey.STREAM_CODEC, (p, ctx) -> {
         Player player = ctx.player();
         if (p.key() != 1) return;
         if (player.getMainHandItem().getItem() instanceof ICaster) CasterManager.toggleMisc(player.getMainHandItem(), player.level(), player, p.mod());
         else if (player.getOffhandItem().getItem() instanceof ICaster) CasterManager.toggleMisc(player.getOffhandItem(), player.level(), player, p.mod());
         // TODO(elemental tools milestone): the elemental shovel's orientation toggle also listens to this key.
      });
      r.playToServer(FocusNodes.TYPE, FocusNodes.STREAM_CODEC, (p, ctx) -> {
         FocalManipulatorBlockEntity be = manipulator(ctx.player(), p.pos());
         if (be == null || be.vis > 0.0F) return;
         Map<Integer, FocusElementNode> data = new HashMap<>();
         for (CompoundTag t : p.nodes()) {
            FocusElementNode node = new FocusElementNode();
            node.deserialize(t);
            data.put(node.id, node);
         }
         be.data = data;
         be.focusName = p.name().length() > 64 ? p.name().substring(0, 64) : p.name();
         be.setChanged();
      });
      r.playToServer(FocusName.TYPE, FocusName.STREAM_CODEC, (p, ctx) -> {
         FocalManipulatorBlockEntity be = manipulator(ctx.player(), p.pos());
         if (be == null) return;
         be.focusName = p.name();
         be.setChanged();
      });
   }
}
