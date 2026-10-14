package thaumcraft.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import thaumcraft.common.blocks.devices.MirrorBlock;
import thaumcraft.common.blocks.devices.MirrorBlockEntity;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModSounds;

/**
 * BlockMirrorItem: using a mirror on a placed, unlinked mirror of the same kind gives a mirror linked to it; placing that
 * one links the pair.
 */
public class MirrorItem extends BlockItem {
   public MirrorItem(Block block, Properties properties) {
      super(block, properties);
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Level level = ctx.getLevel();
      Player player = ctx.getPlayer();
      if (player == null || !(level.getBlockState(ctx.getClickedPos()).getBlock() instanceof MirrorBlock mb) || mb != getBlock()) {
         return InteractionResult.PASS;
      }
      if (level.isClientSide) {
         player.swing(ctx.getHand());
         return InteractionResult.SUCCESS;
      }
      if (level.getBlockEntity(ctx.getClickedPos()) instanceof MirrorBlockEntity tm) {
         if (!tm.isLinkValid()) {
            ItemStack st = stack.copyWithCount(1);
            st.set(ModDevices.MIRROR_LINK.get(), GlobalPos.of(level.dimension(), ctx.getClickedPos()));
            level.playSound(null, ctx.getClickedPos(), ModSounds.JAR.get(), SoundSource.BLOCKS, 1.0F, 2.0F);
            if (!player.getInventory().add(st)) player.drop(st, false);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.inventoryMenu.broadcastChanges();
         } else {
            player.sendSystemMessage(Component.translatable("thaumcraft.mirror.already_linked").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
         }
         return InteractionResult.SUCCESS;
      }
      return InteractionResult.PASS;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      GlobalPos gp = stack.get(ModDevices.MIRROR_LINK.get());
      if (gp != null) {
         tooltip.add(Component.translatable("tc.handmirrorlinkedto").append(" " + gp.pos().getX() + "," + gp.pos().getY() + "," + gp.pos().getZ()
            + " / " + gp.dimension().location()));
      }
   }
}
