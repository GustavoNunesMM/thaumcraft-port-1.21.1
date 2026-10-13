package thaumcraft.api.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Worn gear that lowers the vis cost of arcane crafting and casting, in percent. */
public interface IVisDiscountGear {
   int getVisDiscount(ItemStack stack, Player player);
}
