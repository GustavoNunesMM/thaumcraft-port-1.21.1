package thaumcraft.api.crafting;

import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registers.ModRecipes;

/**
 * A recipe of the arcane workbench. The grid is matched like a crafting recipe (3x3 {@link CraftingInput});
 * the vis cost, the crystals in the six crystal slots and the research are checked by the workbench.
 */
public interface IArcaneRecipe extends Recipe<CraftingInput> {
   int getVis();

   String getResearch();

   /** Crystals used up by the craft (may be empty). */
   AspectList getCrystals();

   @Override
   default RecipeType<?> getType() {
      return ModRecipes.ARCANE_CRAFTING.get();
   }

   @Override
   default boolean isSpecial() {
      return true; // keeps them out of the vanilla recipe book
   }
}
