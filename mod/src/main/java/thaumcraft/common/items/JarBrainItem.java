package thaumcraft.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.registers.ModDevices;

/** BlockJarBrainItem: keeps the jar's experience when broken and placed again. */
public class JarBrainItem extends BlockItem {
   public JarBrainItem(Block block, Properties properties) {
      super(block, properties);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      int xp = stack.getOrDefault(ModDevices.BRAIN_XP.get(), 0);
      if (xp > 0) tooltip.add(Component.literal(xp + " xp").withStyle(ChatFormatting.GREEN));
      super.appendHoverText(stack, ctx, tooltip, flag);
   }
}
