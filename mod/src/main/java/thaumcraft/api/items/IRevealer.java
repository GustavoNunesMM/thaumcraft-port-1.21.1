package thaumcraft.api.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Already deprecated in 1.12 (aura nodes are gone): items that reveal hidden things such as flux rifts. */
public interface IRevealer {
   boolean showNodes(ItemStack stack, LivingEntity wearer);
}
