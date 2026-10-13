package thaumcraft.common.items.consumables;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * ItemBathSalts: dissolves after 10 seconds when dropped; dissolving in a water source turns it into purifying fluid
 * (FluxEvents.itemExpire). The arcane spa (devices milestone) mixes them into water as well.
 */
public class BathSaltsItem extends Item {
   public BathSaltsItem(Properties properties) {
      super(properties);
   }

   @Override
   public int getEntityLifespan(ItemStack stack, Level level) {
      return 200;
   }
}
