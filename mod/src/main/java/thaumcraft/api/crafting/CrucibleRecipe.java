package thaumcraft.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.registers.ModRecipes;

/**
 * CrucibleRecipe: a catalyst dropped into a crucible holding at least these aspects turns into the result.
 * The hedge alchemy recipes take the aspects of an item (aspects_of), optionally minus another's (remove_aspects_of);
 * those are only known once the aspect tables are computed, so {@link #getAspects()} resolves them lazily.
 */
public class CrucibleRecipe implements Recipe<SingleRecipeInput> {
   final String group;
   final String research;
   final Ingredient catalyst;
   final AspectList baseAspects;
   final Optional<ResourceLocation> aspectsOf;
   final Optional<ResourceLocation> removeAspectsOf;
   final ItemStack result;

   public CrucibleRecipe(String group, String research, Ingredient catalyst, AspectList aspects, Optional<ResourceLocation> aspectsOf,
                         Optional<ResourceLocation> removeAspectsOf, ItemStack result) {
      this.group = group;
      this.research = research;
      this.catalyst = catalyst;
      this.baseAspects = aspects;
      this.aspectsOf = aspectsOf;
      this.removeAspectsOf = removeAspectsOf;
      this.result = result;
   }

   /** The aspects needed (and used up), with the runtime part resolved against the side's aspect tables. */
   public AspectList getAspects(Level level) {
      if (aspectsOf.isEmpty() && removeAspectsOf.isEmpty()) return baseAspects;
      AspectManager m = AspectManager.get(level);
      AspectList out = new AspectList();
      aspectsOf.ifPresent(id -> {
         AspectList of = m.getItemAspects(BuiltInRegistries.ITEM.get(id));
         if (of != null) out.add(of);
      });
      removeAspectsOf.ifPresent(id -> {
         AspectList minus = m.getItemAspects(BuiltInRegistries.ITEM.get(id));
         if (minus != null) for (Aspect a : minus.getAspects()) out.reduce(a, minus.getAmount(a));
      });
      out.aspects.values().removeIf(v -> v <= 0);
      out.add(baseAspects);
      return out;
   }

   public boolean hasRuntimeAspects() {
      return aspectsOf.isPresent() || removeAspectsOf.isPresent();
   }

   /** The fixed part of the aspects (all of them unless {@link #hasRuntimeAspects()}). */
   public AspectList getBaseAspects() {
      return baseAspects;
   }

   public String getResearch() {
      return research;
   }

   public Ingredient getCatalyst() {
      return catalyst;
   }

   public boolean catalystMatches(ItemStack stack) {
      return catalyst.test(stack);
   }

   /** matches(AspectList, ItemStack) */
   public boolean matches(Level level, AspectList available, ItemStack cat) {
      if (!catalyst.test(cat) || available == null) return false;
      AspectList need = getAspects(level);
      for (Aspect a : need.getAspects()) {
         if (available.getAmount(a) < need.getAmount(a)) return false;
      }
      return true;
   }

   public AspectList removeMatching(Level level, AspectList available) {
      AspectList out = available.copy();
      AspectList need = getAspects(level);
      for (Aspect a : need.getAspects()) out.remove(a, need.getAmount(a));
      return out;
   }

   @Override
   public boolean matches(SingleRecipeInput input, Level level) {
      return catalyst.test(input.item());
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
      return NonNullList.of(Ingredient.EMPTY, catalyst);
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
      return ModRecipes.CRUCIBLE_SERIALIZER.get();
   }

   @Override
   public RecipeType<?> getType() {
      return ModRecipes.CRUCIBLE.get();
   }

   public static class Serializer implements RecipeSerializer<CrucibleRecipe> {
      static final MapCodec<CrucibleRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
         Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
         Codec.STRING.fieldOf("research").forGetter(r -> r.research),
         Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(r -> r.catalyst),
         AspectList.CODEC.optionalFieldOf("aspects", new AspectList()).forGetter(r -> r.baseAspects),
         ResourceLocation.CODEC.optionalFieldOf("aspects_of").forGetter(r -> r.aspectsOf),
         ResourceLocation.CODEC.optionalFieldOf("remove_aspects_of").forGetter(r -> r.removeAspectsOf),
         ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
      ).apply(i, CrucibleRecipe::new));
      static final StreamCodec<RegistryFriendlyByteBuf, Optional<ResourceLocation>> OPT_ID = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).cast();
      static final StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> STREAM_CODEC = StreamCodec.of(
         (buf, r) -> {
            buf.writeUtf(r.group);
            buf.writeUtf(r.research);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, r.catalyst);
            AspectList.STREAM_CODEC.encode(buf, r.baseAspects);
            OPT_ID.encode(buf, r.aspectsOf);
            OPT_ID.encode(buf, r.removeAspectsOf);
            ItemStack.STREAM_CODEC.encode(buf, r.result);
         },
         buf -> new CrucibleRecipe(buf.readUtf(), buf.readUtf(), Ingredient.CONTENTS_STREAM_CODEC.decode(buf), AspectList.STREAM_CODEC.decode(buf),
            OPT_ID.decode(buf), OPT_ID.decode(buf), ItemStack.STREAM_CODEC.decode(buf)));

      @Override
      public MapCodec<CrucibleRecipe> codec() {
         return CODEC;
      }

      @Override
      public StreamCodec<RegistryFriendlyByteBuf, CrucibleRecipe> streamCodec() {
         return STREAM_CODEC;
      }
   }
}
