package thaumcraft.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registers.ModRecipes;

public class ShapedArcaneRecipe implements IArcaneRecipe {
   final String group;
   final String research;
   final int vis;
   final AspectList crystals;
   final ShapedRecipePattern pattern;
   final ItemStack result;

   public ShapedArcaneRecipe(String group, String research, int vis, AspectList crystals, ShapedRecipePattern pattern, ItemStack result) {
      this.group = group;
      this.research = research;
      this.vis = vis;
      this.crystals = crystals;
      this.pattern = pattern;
      this.result = result;
   }

   @Override
   public boolean matches(CraftingInput input, Level level) {
      return pattern.matches(input);
   }

   @Override
   public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
      return result.copy();
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return width >= pattern.width() && height >= pattern.height();
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider registries) {
      return result;
   }

   @Override
   public NonNullList<Ingredient> getIngredients() {
      return pattern.ingredients();
   }

   public int getWidth() {
      return pattern.width();
   }

   public int getHeight() {
      return pattern.height();
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
      return ModRecipes.ARCANE_SHAPED.get();
   }

   public static class Serializer implements RecipeSerializer<ShapedArcaneRecipe> {
      static final MapCodec<ShapedArcaneRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
         Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
         Codec.STRING.fieldOf("research").forGetter(r -> r.research),
         Codec.INT.optionalFieldOf("vis", 0).forGetter(r -> r.vis),
         AspectList.CODEC.optionalFieldOf("crystals", new AspectList()).forGetter(r -> r.crystals),
         ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
         ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
      ).apply(i, ShapedArcaneRecipe::new));
      static final StreamCodec<RegistryFriendlyByteBuf, ShapedArcaneRecipe> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8, r -> r.group,
         ByteBufCodecs.STRING_UTF8, r -> r.research,
         ByteBufCodecs.VAR_INT, r -> r.vis,
         AspectList.STREAM_CODEC, r -> r.crystals,
         ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
         ItemStack.STREAM_CODEC, r -> r.result,
         ShapedArcaneRecipe::new);

      @Override
      public MapCodec<ShapedArcaneRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, ShapedArcaneRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
