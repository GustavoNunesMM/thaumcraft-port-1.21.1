package thaumcraft.common.registers;

import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;

public final class ModDataComponents {
   public static final DeferredRegister.DataComponents COMPONENTS =
      DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Thaumcraft.MODID);

   /**
    * Essentia held by an item (IEssentiaContainerItem in 1.12): vis crystals, later phials and jars.
    * The stored list must never be mutated; copy it first.
    */
   public static final Supplier<DataComponentType<AspectList>> ESSENTIA = COMPONENTS.registerComponentType(
      "essentia", b -> b.persistent(AspectList.CODEC).networkSynchronized(AspectList.STREAM_CODEC).cacheEncoding()
   );

   /** The aspect a labelled jar (or a filled label) is locked to; "AspectFilter" in 1.12. */
   public static final Supplier<DataComponentType<thaumcraft.api.aspects.Aspect>> ASPECT_FILTER = COMPONENTS.registerComponentType(
      "aspect_filter", b -> b.persistent(thaumcraft.api.aspects.Aspect.CODEC).networkSynchronized(thaumcraft.api.aspects.Aspect.STREAM_CODEC)
   );

   /** Infusion enchantments (EnumInfusionEnchantment id -> level); "infench" in 1.12. */
   public static final Supplier<DataComponentType<java.util.Map<String, Integer>>> INFUSION_ENCHANTMENTS = COMPONENTS.registerComponentType(
      "infusion_enchantments", b -> b.persistent(com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING, com.mojang.serialization.Codec.INT))
         .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.map(java.util.HashMap::new, net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8,
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT)));
   /** Warp an item gives while held or worn ("TC.WARP" in 1.12). */
   public static final Supplier<DataComponentType<Integer>> WARP = COMPONENTS.registerComponentType(
      "warp", b -> b.persistent(com.mojang.serialization.Codec.INT).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));

   /** The focus package stored in a focus ("package" tag in 1.12). */
   public static final Supplier<DataComponentType<net.minecraft.nbt.CompoundTag>> FOCUS_PACKAGE = COMPONENTS.registerComponentType(
      "focus_package", b -> b.persistent(net.minecraft.nbt.CompoundTag.CODEC).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.COMPOUND_TAG));
   /** The focus fitted to a caster's gauntlet. */
   public static final Supplier<DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> CASTER_FOCUS = COMPONENTS.registerComponentType(
      "caster_focus", b -> b.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
         .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC).cacheEncoding());
   /** The block an exchange focus will place ("picked"). */
   public static final Supplier<DataComponentType<net.minecraft.world.item.component.ItemContainerContents>> PICKED_BLOCK = COMPONENTS.registerComponentType(
      "picked_block", b -> b.persistent(net.minecraft.world.item.component.ItemContainerContents.CODEC)
         .networkSynchronized(net.minecraft.world.item.component.ItemContainerContents.STREAM_CODEC).cacheEncoding());
   /** Plan focus area: x, y, z sizes and the axis mode (areax/areay/areaz/aread in 1.12). */
   public static final Supplier<DataComponentType<CasterArea>> CASTER_AREA = COMPONENTS.registerComponentType(
      "caster_area", b -> b.persistent(CasterArea.CODEC).networkSynchronized(CasterArea.STREAM_CODEC));

   public record CasterArea(int x, int y, int z, int dim) {
      public static final CasterArea DEFAULT = new CasterArea(3, 3, 3, 0);
      public static final com.mojang.serialization.Codec<CasterArea> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
         com.mojang.serialization.Codec.INT.fieldOf("x").forGetter(CasterArea::x), com.mojang.serialization.Codec.INT.fieldOf("y").forGetter(CasterArea::y),
         com.mojang.serialization.Codec.INT.fieldOf("z").forGetter(CasterArea::z), com.mojang.serialization.Codec.INT.fieldOf("dim").forGetter(CasterArea::dim)
      ).apply(i, CasterArea::new));
      public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, CasterArea> STREAM_CODEC =
         net.minecraft.network.codec.StreamCodec.composite(net.minecraft.network.codec.ByteBufCodecs.VAR_INT, CasterArea::x,
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT, CasterArea::y, net.minecraft.network.codec.ByteBufCodecs.VAR_INT, CasterArea::z,
            net.minecraft.network.codec.ByteBufCodecs.VAR_INT, CasterArea::dim, CasterArea::new);
   }

   private ModDataComponents() {
   }
}
