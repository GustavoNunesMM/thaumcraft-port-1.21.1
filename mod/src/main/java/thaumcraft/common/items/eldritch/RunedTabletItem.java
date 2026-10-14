package thaumcraft.common.items.eldritch;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Runed tablet: the key to an ancient locking mechanism, found in a reliquary chest deep in each Outer Lands maze.
 * Also the eldritch eye (a plain glowing item with a different tooltip).
 */
public class RunedTabletItem extends Item {
   private final String tooltip;

   public RunedTabletItem(Properties properties, String tooltip) {
      super(properties);
      this.tooltip = tooltip;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltipComponents, TooltipFlag flag) {
      tooltipComponents.add(Component.translatable(tooltip).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
   }
}
