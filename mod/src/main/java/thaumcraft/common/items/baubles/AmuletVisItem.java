package thaumcraft.common.items.baubles;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.lib.CuriosHelper;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * ItemAmuletVis: worn as a necklace it feeds one point of aura vis to a rechargeable item (hotbar first, then curios,
 * then armor) every 2 seconds (found variant) or every 5 ticks (crafted).
 */
public class AmuletVisItem extends Item implements ICurioItem {
   private final boolean crafted;

   public AmuletVisItem(Properties p, boolean crafted) {
      super(p.stacksTo(1));
      this.crafted = crafted;
   }

   @Override
   public void curioTick(SlotContext ctx, ItemStack stack) {
      if (!(ctx.entity() instanceof Player player) || player.level().isClientSide || player.tickCount % (crafted ? 5 : 40) != 0) return;
      var inv = player.getInventory();
      for (int a = 0; a < Inventory.getSelectionSize(); a++) {
         if (RechargeHelper.rechargeItem(player.level(), inv.items.get(a), player.blockPosition(), player, 1) > 0.0F) return;
      }
      for (ItemStack s : CuriosHelper.getCurios(player)) {
         if (RechargeHelper.rechargeItem(player.level(), s, player.blockPosition(), player, 1) > 0.0F) return;
      }
      for (ItemStack s : inv.armor) {
         if (RechargeHelper.rechargeItem(player.level(), s, player.blockPosition(), player, 1) > 0.0F) return;
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.amulet_vis.text").withStyle(ChatFormatting.AQUA));
   }
}
