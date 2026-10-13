package thaumcraft.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.essentia.AlembicBlockEntity;
import thaumcraft.common.blocks.essentia.JarBlockEntity;
import thaumcraft.common.registers.ModDataComponents;

/** BlockJarItem: a jar keeps its essentia and label as item components; it can be filled straight from an alembic. */
public class JarItem extends BlockItem {
   public JarItem(Block block, Properties properties) {
      super(block, properties);
   }

   public static int amount(ItemStack stack) {
      AspectList al = stack.get(ModDataComponents.ESSENTIA.get());
      return al == null ? 0 : al.visSize();
   }

   @Override
   public boolean isBarVisible(ItemStack stack) {
      return amount(stack) > 0;
   }

   @Override
   public int getBarWidth(ItemStack stack) {
      return Math.round(13.0F * amount(stack) / JarBlockEntity.CAPACITY);
   }

   @Override
   public int getBarColor(ItemStack stack) {
      AspectList al = stack.get(ModDataComponents.ESSENTIA.get());
      return al == null || al.isEmpty() ? 0xFFFFFF : al.getAspects()[0].getColor();
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      Aspect filter = stack.get(ModDataComponents.ASPECT_FILTER.get());
      if (filter != null) tooltip.add(Component.literal(filter.getName()).withStyle(ChatFormatting.DARK_PURPLE));
      AspectList al = stack.get(ModDataComponents.ESSENTIA.get());
      if (al != null && !al.isEmpty()) {
         Aspect a = al.getAspects()[0];
         tooltip.add(Component.literal(a.getName() + " x" + al.getAmount(a)).withColor(a.getColor()));
      }
      super.appendHoverText(stack, ctx, tooltip, flag);
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Level level = ctx.getLevel();
      Player player = ctx.getPlayer();
      if (player == null || !(level.getBlockEntity(ctx.getClickedPos()) instanceof AlembicBlockEntity tile) || tile.amount <= 0) {
         return InteractionResult.PASS;
      }
      if (level.isClientSide) return InteractionResult.SUCCESS;
      Aspect filter = stack.get(ModDataComponents.ASPECT_FILTER.get());
      AspectList al = stack.get(ModDataComponents.ESSENTIA.get());
      if (filter != null && filter != tile.aspect) return InteractionResult.FAIL;
      if (al != null && !al.isEmpty() && al.getAspects()[0] != tile.aspect) return InteractionResult.FAIL;
      int base = al == null ? 0 : al.visSize();
      int amt = Math.min(tile.amount, JarBlockEntity.CAPACITY - base);
      if (amt <= 0) return InteractionResult.FAIL;
      Aspect a = tile.aspect;
      if (tile.takeFromContainer(a, amt)) {
         if (stack.getCount() > 1) {
            ItemStack one = stack.copyWithCount(1);
            one.set(ModDataComponents.ESSENTIA.get(), new AspectList().add(a, base + amt));
            stack.shrink(1);
            if (!player.getInventory().add(one)) player.drop(one, false);
         } else {
            stack.set(ModDataComponents.ESSENTIA.get(), new AspectList().add(a, base + amt));
         }
         player.playSound(SoundEvents.BOTTLE_FILL, 0.25F, 1.0F);
         return InteractionResult.SUCCESS;
      }
      return InteractionResult.PASS;
   }
}
