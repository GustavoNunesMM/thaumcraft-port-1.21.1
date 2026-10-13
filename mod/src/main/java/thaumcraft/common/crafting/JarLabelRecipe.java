package thaumcraft.common.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModRecipes;

/** The label_<aspect> recipes: a blank label and a filled phial make a label marked with the phial's aspect. */
public class JarLabelRecipe extends CustomRecipe {
   public JarLabelRecipe(CraftingBookCategory category) {
      super(category);
   }

   @Override
   public boolean matches(CraftingInput inv, Level level) {
      return find(inv) != null;
   }

   private static AspectList find(CraftingInput inv) {
      ItemStack label = ItemStack.EMPTY, phial = ItemStack.EMPTY;
      for (int i = 0; i < inv.size(); i++) {
         ItemStack s = inv.getItem(i);
         if (s.isEmpty()) continue;
         if (s.is(ModItems.LABEL.get()) && label.isEmpty()) label = s;
         else if (s.is(ModItems.PHIAL_FILLED.get()) && phial.isEmpty()) phial = s;
         else return null;
      }
      AspectList al = phial.get(ModDataComponents.ESSENTIA.get());
      return !label.isEmpty() && al != null && !al.isEmpty() ? al : null;
   }

   @Override
   public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {
      AspectList al = find(inv);
      ItemStack out = new ItemStack(ModItems.LABEL_FILLED.get());
      if (al != null) out.set(ModDataComponents.ASPECT_FILTER.get(), al.getAspects()[0]);
      return out;
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return width * height >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipes.JAR_LABEL.get();
   }
}
