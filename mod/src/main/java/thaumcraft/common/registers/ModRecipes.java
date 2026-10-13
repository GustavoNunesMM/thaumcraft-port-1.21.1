package thaumcraft.common.registers;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.common.crafting.SalisMundusRecipe;

public final class ModRecipes {
   public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Thaumcraft.MODID);

   public static final Supplier<RecipeSerializer<SalisMundusRecipe>> SALIS_MUNDUS =
      SERIALIZERS.register("salis_mundus", () -> new SimpleCraftingRecipeSerializer<>(SalisMundusRecipe::new));

   public static final Supplier<RecipeSerializer<thaumcraft.common.crafting.JarLabelRecipe>> JAR_LABEL =
      SERIALIZERS.register("jar_label", () -> new SimpleCraftingRecipeSerializer<>(thaumcraft.common.crafting.JarLabelRecipe::new));

   public static final Supplier<RecipeSerializer<ShapedArcaneRecipe>> ARCANE_SHAPED =
      SERIALIZERS.register("arcane_shaped", ShapedArcaneRecipe.Serializer::new);
   public static final Supplier<RecipeSerializer<ShapelessArcaneRecipe>> ARCANE_SHAPELESS =
      SERIALIZERS.register("arcane_shapeless", ShapelessArcaneRecipe.Serializer::new);

   public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Thaumcraft.MODID);
   public static final Supplier<RecipeType<IArcaneRecipe>> ARCANE_CRAFTING = TYPES.register("arcane_crafting",
      () -> RecipeType.simple(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "arcane_crafting")));

   public static final Supplier<RecipeSerializer<CrucibleRecipe>> CRUCIBLE_SERIALIZER =
      SERIALIZERS.register("crucible", CrucibleRecipe.Serializer::new);
   public static final Supplier<RecipeType<CrucibleRecipe>> CRUCIBLE = TYPES.register("crucible",
      () -> RecipeType.simple(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "crucible")));

   public static final Supplier<RecipeSerializer<thaumcraft.api.crafting.InfusionRecipe>> INFUSION_SERIALIZER =
      SERIALIZERS.register("infusion", thaumcraft.api.crafting.InfusionRecipe.Serializer::new);
   public static final Supplier<RecipeSerializer<thaumcraft.common.crafting.InfusionEnchantmentRecipe>> INFUSION_ENCHANTMENT_SERIALIZER =
      SERIALIZERS.register("infusion_enchantment", thaumcraft.common.crafting.InfusionEnchantmentRecipe.Serializer::new);
   @SuppressWarnings({"unchecked", "rawtypes"})
   public static final Supplier<RecipeType<thaumcraft.api.crafting.InfusionRecipe>> INFUSION = TYPES.register("infusion",
      () -> RecipeType.simple(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "infusion")));

   private ModRecipes() {
   }
}
