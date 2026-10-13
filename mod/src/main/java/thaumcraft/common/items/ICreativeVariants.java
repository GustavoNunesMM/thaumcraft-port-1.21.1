package thaumcraft.common.items;

import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;

/** getSubItems: items listing several stacks in the creative tab (one filled phial per aspect...). */
public interface ICreativeVariants {
   void addCreativeStacks(Consumer<ItemStack> output);
}
