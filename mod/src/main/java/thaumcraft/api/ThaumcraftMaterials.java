package thaumcraft.api;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import thaumcraft.common.registers.ModItems;

/**
 * ThaumcraftMaterials: the tool tiers. 1.12 harvest level 3 is diamond, 4 and up netherite; the attack bonus, uses, speed
 * and enchantability are the original ones. The armor materials are registry entries in 1.21 and live in ModGear.
 */
public final class ThaumcraftMaterials {
   public static final Tier TOOLMAT_THAUMIUM = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 500, 7.0F, 2.5F, 22,
      () -> Ingredient.of(ModItems.INGOT_THAUMIUM.get()));
   public static final Tier TOOLMAT_VOID = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 150, 8.0F, 3.0F, 10,
      () -> Ingredient.of(ModItems.INGOT_VOID.get()));
   public static final Tier TOOLMAT_ELEMENTAL = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1500, 9.0F, 3.0F, 18,
      () -> Ingredient.of(ModItems.INGOT_THAUMIUM.get()));
   /** ItemPrimalCrusher.material ("PRIMALVOID"). */
   public static final Tier TOOLMAT_PRIMALVOID = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 500, 8.0F, 4.0F, 20,
      () -> Ingredient.of(ModItems.INGOT_VOID.get()));
   /** ItemCrimsonBlade.toolMatCrimsonVoid ("CVOID"). */
   public static final Tier TOOLMAT_CRIMSONVOID = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 200, 8.0F, 3.5F, 20,
      () -> Ingredient.of(ModItems.INGOT_VOID.get()));

   private ThaumcraftMaterials() {
   }
}
