package thaumcraft.common.items.baubles;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;

/** ItemVoidseerCharm: vis discount growing with permanent warp (up to 25% at 100), and a fifth of that as warp. */
public class VoidseerCharmItem extends Item implements top.theillusivec4.curios.api.type.capability.ICurioItem,IVisDiscountGear, IWarpingGear {
   public VoidseerCharmItem(Properties p) {
      super(p.stacksTo(1));
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.voidseer_charm.text").withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.ITALIC));
      super.appendHoverText(stack, ctx, tooltip, flag);
   }

   @Override
   public int getVisDiscount(ItemStack stack, Player player) {
      if (player == null) return 0;
      int pw = Math.min(100, ResearchManager.warp(player).get(PlayerWarp.Type.PERMANENT));
      return (int) (pw / 100.0F * 25.0F);
   }

   @Override
   public int getWarp(ItemStack stack, Player player) {
      return getVisDiscount(stack, player) / 5;
   }
}
