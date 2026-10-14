package thaumcraft.common.items.mobs;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * ItemPrimordialPearl: used up a little in every craft (the crafting remainder is the same pearl one step more worn),
 * shown as a pearl (damage 0-2), a nodule (3-5) or a mote (6-7). It cannot be repaired or enchanted.
 */
public class PrimordialPearlItem extends Item {
   public PrimordialPearlItem(Properties properties) {
      super(properties);
   }

   @Override
   public String getDescriptionId(ItemStack stack) {
      int d = stack.getDamageValue();
      return getDescriptionId() + (d < 3 ? ".pearl" : d < 6 ? ".nodule" : ".mote");
   }

   @Override
   public boolean hasCraftingRemainingItem(ItemStack stack) {
      return stack.getDamageValue() < 7;
   }

   @Override
   public ItemStack getCraftingRemainingItem(ItemStack stack) {
      if (!hasCraftingRemainingItem(stack)) return ItemStack.EMPTY;
      ItemStack out = stack.copyWithCount(1);
      out.setDamageValue(stack.getDamageValue() + 1);
      return out;
   }

   @Override
   public boolean isEnchantable(ItemStack stack) {
      return false;
   }

   @Override
   public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
      return false;
   }

   @Override
   public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
      return false;
   }

   @Override
   public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
      return false;
   }

   @Override
   public boolean isBarVisible(ItemStack stack) {
      return false;
   }
}
