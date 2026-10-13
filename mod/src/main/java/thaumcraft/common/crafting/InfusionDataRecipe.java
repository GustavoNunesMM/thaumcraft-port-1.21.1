package thaumcraft.common.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.registers.ModGear;

/**
 * The infusion recipes whose 1.12 output was an NBT tag set on the central item (new Object[]{"mask", 0}, {"goggles", 1},
 * {"type", 1}...): the result is the central item itself with a data component patch applied.
 */
public class InfusionDataRecipe extends InfusionRecipe {
   public final DataComponentPatch patch;

   public InfusionDataRecipe(String group, String research, int instability, AspectList aspects, Ingredient central, List<Ingredient> components,
                             DataComponentPatch patch) {
      super(group, research, instability, aspects, central, components, ItemStack.EMPTY);
      this.patch = patch;
   }

   @Override
   public ItemStack getRecipeOutput(Player player, ItemStack input, List<ItemStack> comps) {
      ItemStack out = input.copyWithCount(1);
      out.applyComponents(patch);
      return out;
   }

   @Override
   public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
      ItemStack[] items = central.getItems();
      if (items.length == 0) return ItemStack.EMPTY;
      ItemStack out = items[0].copyWithCount(1);
      out.applyComponents(patch);
      return out;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModGear.INFUSION_DATA_SERIALIZER.get();
   }

   public static class Serializer implements RecipeSerializer<InfusionDataRecipe> {
      static final MapCodec<InfusionDataRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
         Codec.STRING.optionalFieldOf("group", "").forGetter(InfusionDataRecipe::getGroup),
         Codec.STRING.fieldOf("research").forGetter(r -> r.research),
         Codec.INT.optionalFieldOf("instability", 0).forGetter(r -> r.instability),
         AspectList.CODEC.optionalFieldOf("aspects", new AspectList()).forGetter(r -> r.aspects),
         Ingredient.CODEC_NONEMPTY.fieldOf("central").forGetter(r -> r.central),
         Ingredient.CODEC_NONEMPTY.listOf().fieldOf("components").forGetter(r -> r.components),
         DataComponentPatch.CODEC.fieldOf("patch").forGetter(r -> r.patch)
      ).apply(i, InfusionDataRecipe::new));
      static final StreamCodec<RegistryFriendlyByteBuf, InfusionDataRecipe> STREAM_CODEC = StreamCodec.of(
         (buf, r) -> {
            buf.writeUtf(r.getGroup());
            buf.writeUtf(r.research);
            buf.writeVarInt(r.instability);
            AspectList.STREAM_CODEC.encode(buf, r.aspects);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.central);
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.components);
            DataComponentPatch.STREAM_CODEC.encode(buf, r.patch);
         },
         buf -> new InfusionDataRecipe(buf.readUtf(), buf.readUtf(), buf.readVarInt(), AspectList.STREAM_CODEC.decode(buf),
            Ingredient.CONTENTS_STREAM_CODEC.decode(buf), Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
            DataComponentPatch.STREAM_CODEC.decode(buf)));

      @Override
      public MapCodec<InfusionDataRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, InfusionDataRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
