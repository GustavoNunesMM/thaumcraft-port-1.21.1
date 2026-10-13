package thaumcraft.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registers.ModRecipes;

public class ShapelessArcaneRecipe implements IArcaneRecipe {
   final String group;
   final String research;
   final int vis;
   final AspectList crystals;
   final NonNullList<Ingredient> ingredients;
   final ItemStack result;

   public ShapelessArcaneRecipe(String group, String research, int vis, AspectList crystals, NonNullList<Ingredient> ingredients, ItemStack result) {
      this.group = group;
      this.research = research;
      this.vis = vis;
      this.crystals = crystals;
      this.ingredients = ingredients;
      this.result = result;
   }

   @Override
   public boolean matches(CraftingInput input, Level level) {
      if (input.ingredientCount() != ingredients.size()) return false;
      var items = new ArrayList<ItemStack>(input.ingredientCount());
      for (ItemStack s : input.items()) {
         if (!s.isEmpty()) items.add(s);
      }
      return RecipeMatcher.findMatches(items, ingredients) != null;
   }

   @Override
   public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
      return result.copy();
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return width * height >= ingredients.size();
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider registries) {
      return result;
   }

   @Override
   public NonNullList<Ingredient> getIngredients() {
      return ingredients;
   }

   @Override
   public String getGroup() {
      return group;
   }

   @Override
   public int getVis() {
      return vis;
   }

   @Override
   public String getResearch() {
      return research;
   }

   @Override
   public AspectList getCrystals() {
      return crystals;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipes.ARCANE_SHAPELESS.get();
   }

   public static class Serializer implements RecipeSerializer<ShapelessArcaneRecipe> {
      static final MapCodec<ShapelessArcaneRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
         Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
         Codec.STRING.fieldOf("research").forGetter(r -> r.research),
         Codec.INT.optionalFieldOf("vis", 0).forGetter(r -> r.vis),
         AspectList.CODEC.optionalFieldOf("crystals", new AspectList()).forGetter(r -> r.crystals),
         Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(
            l -> l.isEmpty() || l.size() > 9 ? DataResult.error(() -> "an arcane shapeless recipe takes 1 to 9 ingredients")
               : DataResult.success(NonNullList.of(Ingredient.EMPTY, l.toArray(Ingredient[]::new))),
            DataResult::success).forGetter(r -> r.ingredients),
         ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
      ).apply(i, ShapelessArcaneRecipe::new));
      static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<Ingredient>> INGREDIENTS = Ingredient.CONTENTS_STREAM_CODEC
         .apply(ByteBufCodecs.list()).map(l -> NonNullList.of(Ingredient.EMPTY, l.toArray(Ingredient[]::new)), l -> l);
      static final StreamCodec<RegistryFriendlyByteBuf, ShapelessArcaneRecipe> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8, r -> r.group,
         ByteBufCodecs.STRING_UTF8, r -> r.research,
         ByteBufCodecs.VAR_INT, r -> r.vis,
         AspectList.STREAM_CODEC, r -> r.crystals,
         INGREDIENTS, r -> r.ingredients,
         ItemStack.STREAM_CODEC, r -> r.result,
         ShapelessArcaneRecipe::new);

      @Override
      public MapCodec<ShapelessArcaneRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, ShapelessArcaneRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
