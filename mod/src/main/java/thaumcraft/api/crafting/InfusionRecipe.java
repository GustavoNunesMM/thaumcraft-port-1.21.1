package thaumcraft.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registers.ModRecipes;
import thaumcraft.common.research.ResearchManager;

/**
 * InfusionRecipe: the central item on the pedestal under the matrix, the components on the pedestals around it and
 * essentia from nearby jars make the result. Subclasses (infusion enchantments) compute the result from the input.
 */
public class InfusionRecipe implements Recipe<SingleRecipeInput> {
   final String group;
   public final String research;
   public final int instability;
   public final AspectList aspects;
   /** Ingredient.EMPTY means any central item (checked by the subclass). */
   public final Ingredient central;
   public final NonNullList<Ingredient> components;
   final ItemStack result;

   public InfusionRecipe(String group, String research, int instability, AspectList aspects, Ingredient central, List<Ingredient> components,
                         ItemStack result) {
      this.group = group;
      this.research = research;
      this.instability = instability;
      this.aspects = aspects;
      this.central = central;
      this.components = NonNullList.copyOf(components);
      this.result = result;
   }

   public boolean matches(List<ItemStack> input, ItemStack centralItem, Level level, Player player) {
      if (!ResearchManager.knowsResearch(player, research)) return false;
      return (central.isEmpty() || central.test(centralItem)) && RecipeMatcher.findMatches(input, components) != null;
   }

   public String getResearch() {
      return research;
   }

   /** What the central item becomes. */
   public ItemStack getRecipeOutput(Player player, ItemStack input, List<ItemStack> comps) {
      return result.copy();
   }

   public AspectList getAspects(Player player, ItemStack input, List<ItemStack> comps) {
      return aspects;
   }

   public int getInstability(Player player, ItemStack input, List<ItemStack> comps) {
      return instability;
   }

   public NonNullList<Ingredient> getComponents() {
      return components;
   }

   @Override
   public boolean matches(SingleRecipeInput input, Level level) {
      return central.isEmpty() || central.test(input.item());
   }

   @Override
   public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
      return result.copy();
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return true;
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider registries) {
      return result;
   }

   @Override
   public NonNullList<Ingredient> getIngredients() {
      NonNullList<Ingredient> all = NonNullList.create();
      all.add(central);
      all.addAll(components);
      return all;
   }

   @Override
   public String getGroup() {
      return group;
   }

   @Override
   public boolean isSpecial() {
      return true;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipes.INFUSION_SERIALIZER.get();
   }

   @Override
   public RecipeType<?> getType() {
      return ModRecipes.INFUSION.get();
   }

   public static class Serializer implements RecipeSerializer<InfusionRecipe> {
      static final MapCodec<InfusionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
         Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
         Codec.STRING.fieldOf("research").forGetter(r -> r.research),
         Codec.INT.optionalFieldOf("instability", 0).forGetter(r -> r.instability),
         AspectList.CODEC.optionalFieldOf("aspects", new AspectList()).forGetter(r -> r.aspects),
         Ingredient.CODEC.optionalFieldOf("central", Ingredient.EMPTY).forGetter(r -> r.central),
         Ingredient.CODEC_NONEMPTY.listOf().fieldOf("components").forGetter(r -> r.components),
         ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
      ).apply(i, InfusionRecipe::new));
      static final StreamCodec<RegistryFriendlyByteBuf, InfusionRecipe> STREAM_CODEC = StreamCodec.of(
         (buf, r) -> {
            buf.writeUtf(r.group);
            buf.writeUtf(r.research);
            buf.writeVarInt(r.instability);
            AspectList.STREAM_CODEC.encode(buf, r.aspects);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.central);
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, r.components);
            ItemStack.STREAM_CODEC.encode(buf, r.result);
         },
         buf -> new InfusionRecipe(buf.readUtf(), buf.readUtf(), buf.readVarInt(), AspectList.STREAM_CODEC.decode(buf),
            Ingredient.CONTENTS_STREAM_CODEC.decode(buf), Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf),
            ItemStack.STREAM_CODEC.decode(buf)));

      @Override
      public MapCodec<InfusionRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, InfusionRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
