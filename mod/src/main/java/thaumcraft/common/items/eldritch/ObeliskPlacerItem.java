package thaumcraft.common.items.eldritch;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import thaumcraft.common.world.eldritch.ObeliskFeature;

/**
 * ItemCreativePlacer "obelisk" (creative only): builds an eldritch altar with its four obelisks on the clicked surface.
 * In 1.12 the placer only cleared the block; here it builds what the name promises.
 */
public class ObeliskPlacerItem extends Item {
   public ObeliskPlacerItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult useOn(UseOnContext ctx) {
      if (ctx.getPlayer() != null && !ctx.getPlayer().getAbilities().instabuild) return InteractionResult.FAIL;
      if (!ctx.getLevel().getBlockState(ctx.getClickedPos()).isSolid()) return InteractionResult.FAIL;
      if (ctx.getLevel() instanceof ServerLevel sl) {
         BlockPos base = ctx.getClickedPos();
         ObeliskFeature.build(sl, base, sl.random);
      }
      return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.literal("Creative only").withStyle(ChatFormatting.DARK_PURPLE));
   }
}
