package thaumcraft.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.ILabelable;
import thaumcraft.common.registers.ModDataComponents;

/** ItemLabel: blank (label) or marked with an aspect (label_filled, made with a phial). Used on jars and alembics. */
public class LabelItem extends Item {
   public LabelItem(Properties properties) {
      super(properties);
   }

   @Override
   public Component getName(ItemStack stack) {
      Aspect a = stack.get(ModDataComponents.ASPECT_FILTER.get());
      return Component.translatable(getDescriptionId(stack), a == null ? "?" : a.getName());
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Level level = ctx.getLevel();
      Player player = ctx.getPlayer();
      if (player == null) return InteractionResult.PASS;
      ILabelable target = level.getBlockState(ctx.getClickedPos()).getBlock() instanceof ILabelable b ? b
         : level.getBlockEntity(ctx.getClickedPos()) instanceof ILabelable t ? t : null;
      if (target == null) return InteractionResult.PASS;
      if (!level.isClientSide && target.applyLabel(player, ctx.getClickedPos(), ctx.getClickedFace(), stack)) stack.consume(1, player);
      return InteractionResult.sidedSuccess(level.isClientSide);
   }
}
