package thaumcraft.api.crafting;

import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import thaumcraft.api.ThaumcraftInvHelper;

public class IngredientNBTTC extends Ingredient {
   private final ItemStack stack;

   public IngredientNBTTC(ItemStack stack) {
      super(new ItemStack[]{stack});
      this.stack = stack;
   }

   public boolean apply(@Nullable ItemStack input) {
      return input == null
         ? false
         : this.stack.getItem() == input.getItem()
            && this.stack.getItemDamage() == input.getItemDamage()
            && ThaumcraftInvHelper.areItemStackTagsEqualRelaxed(this.stack, input);
   }

   public boolean isSimple() {
      return false;
   }
}
