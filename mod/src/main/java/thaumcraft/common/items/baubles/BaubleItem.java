package thaumcraft.common.items.baubles;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.items.IVisDiscountGear;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * ItemBaubles: the mundane and fancy amulets, rings and girdles, flattened into one item each (amulet_mundane,
 * ring_mundane, girdle_mundane, ring_apprentice, amulet_fancy, ring_fancy, girdle_fancy). They go in the Curios slot
 * matching their 1.12 bauble type (curios:necklace/ring/belt item tags); the apprentice's ring gives 5% vis discount.
 */
public class BaubleItem extends Item implements ICurioItem, IVisDiscountGear {
   private final int discount;

   public BaubleItem(Properties p, int discount) {
      super(p.stacksTo(1));
      this.discount = discount;
   }

   @Override
   public int getVisDiscount(ItemStack stack, Player player) {
      return discount;
   }
}
