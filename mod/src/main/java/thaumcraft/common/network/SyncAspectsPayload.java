package thaumcraft.common.network;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;

/** Server -> client: the full computed aspect tables, sent on login and after /reload. */
public record SyncAspectsPayload(
   Map<Item, AspectList> items,
   Map<Block, AspectList> blocks,
   Map<EntityType<?>, AspectList> entities,
   Map<ResourceKey<Potion>, AspectList> potions
) implements CustomPacketPayload {
   public static final Type<SyncAspectsPayload> TYPE =
      new Type<>(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sync_aspects"));

   public static final StreamCodec<RegistryFriendlyByteBuf, SyncAspectsPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(HashMap::new, ByteBufCodecs.registry(Registries.ITEM), AspectList.STREAM_CODEC), SyncAspectsPayload::items,
      ByteBufCodecs.map(HashMap::new, ByteBufCodecs.registry(Registries.BLOCK), AspectList.STREAM_CODEC), SyncAspectsPayload::blocks,
      ByteBufCodecs.map(HashMap::new, ByteBufCodecs.registry(Registries.ENTITY_TYPE), AspectList.STREAM_CODEC), SyncAspectsPayload::entities,
      ByteBufCodecs.map(HashMap::new, ResourceKey.streamCodec(Registries.POTION), AspectList.STREAM_CODEC), SyncAspectsPayload::potions,
      SyncAspectsPayload::new
   );

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
