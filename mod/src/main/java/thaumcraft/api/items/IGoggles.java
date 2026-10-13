package thaumcraft.api.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Worn (or held) items that show in-world popups: aspects in containers, device readouts. */
public interface IGoggles {
   boolean showIngamePopups(ItemStack stack, LivingEntity wearer);
}
