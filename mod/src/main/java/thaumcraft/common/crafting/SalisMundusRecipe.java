package thaumcraft.common.crafting;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModRecipes;

/** RecipeMagicDust: flint + bowl + redstone + three vis crystals of different aspects; the flint and bowl are kept. */
public class SalisMundusRecipe extends CustomRecipe {
   public SalisMundusRecipe(CraftingBookCategory category) {
      super(category);
   }

   @Override
   public boolean matches(CraftingInput inv, Level level) {
      boolean bowl = false, flint = false, redstone = false;
      Set<Aspect> crystals = new HashSet<>();
      for (int i = 0; i < inv.size(); i++) {
         ItemStack stack = inv.getItem(i);
         if (stack.isEmpty()) continue;
         if (stack.is(Items.BOWL)) {
            if (bowl) return false;
            bowl = true;
         } else if (stack.is(Items.FLINT)) {
            if (flint) return false;
            flint = true;
         } else if (stack.is(Items.REDSTONE)) {
            if (redstone) return false;
            redstone = true;
         } else if (stack.is(ModItems.CRYSTAL_ESSENCE.get())) {
            Aspect a = VisCrystalItem.getAspect(stack);
            if (a == null || !crystals.add(a) || crystals.size() > 3) return false;
         } else {
            return false;
         }
      }
      return bowl && flint && redstone && crystals.size() == 3;
   }

   @Override
   public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {
      return new ItemStack(ModItems.SALIS_MUNDUS.get());
   }

   @Override
   public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
      NonNullList<ItemStack> ret = super.getRemainingItems(inv);
      for (int i = 0; i < ret.size(); i++) {
         ItemStack s = inv.getItem(i);
         if (s.is(Items.FLINT) || s.is(Items.BOWL)) ret.set(i, s.copyWithCount(1));
      }
      return ret;
   }

   @Override
   public boolean canCraftInDimensions(int width, int height) {
      return width * height >= 6;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return ModRecipes.SALIS_MUNDUS.get();
   }
}
