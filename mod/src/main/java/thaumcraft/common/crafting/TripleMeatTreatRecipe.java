package thaumcraft.common.crafting;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.common.registers.ModMobs;

/** RecipeTripleMeatTreat: one sugar and three different meat nuggets. */
public class TripleMeatTreatRecipe extends CustomRecipe {
   public TripleMeatTreatRecipe(CraftingBookCategory category) {
      super(category);
   }

   private static boolean isChunk(ItemStack s) {
      return ModMobs.CHUNKS.stream().anyMatch(c -> s.is(c.get()));
   }

   @Override
   public boolean matches(CraftingInput inv, Level level) {
      boolean sugar = false;
      Set<Item> meats = new HashSet<>();
      for (int i = 0; i < inv.size(); i++) {
         ItemStack stack = inv.getItem(i);
         if (stack.isEmpty()) continue;
         if (stack.is(Items.SUGAR)) {
            if (sugar) return false;
            sugar = true;
         } else if (isChunk(stack)) {
            if (meats.size() >= 3 || !meats.add(stack.getItem())) return false;
         } else {
            return false;
         }
      }
      return sugar && meats.size() == 3;
   }

   @Override
   public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {
      return new ItemStack(ModMobs.TRIPLE_MEAT_TREAT.get());
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider registries) {
      return new ItemStack(ModMobs.TRIPLE_MEAT_TREAT.get());
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return width * height >= 4;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModMobs.TRIPLE_MEAT_TREAT_RECIPE.get();
   }
}
