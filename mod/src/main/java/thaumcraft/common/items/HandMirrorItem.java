package thaumcraft.common.items;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.common.blocks.devices.MirrorBlockEntity;
import thaumcraft.common.menu.HandMirrorMenu;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModSounds;

/** ItemHandMirror: linked to a magic mirror (use it on one), it opens a slot that sends whatever is put in it there. */
public class HandMirrorItem extends Item {
   public HandMirrorItem(Properties properties) {
      super(properties);
   }

   @Override
   public boolean isFoil(ItemStack stack) {
      return stack.has(ModDevices.MIRROR_LINK.get());
   }

   @Override
   public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
      Level level = ctx.getLevel();
      Player player = ctx.getPlayer();
      if (player == null || !level.getBlockState(ctx.getClickedPos()).is(ModDevices.MIRROR.get())) return InteractionResult.PASS;
      if (level.isClientSide) {
         player.swing(ctx.getHand());
         return InteractionResult.SUCCESS;
      }
      if (level.getBlockEntity(ctx.getClickedPos()) instanceof MirrorBlockEntity) {
         stack.set(ModDevices.MIRROR_LINK.get(), GlobalPos.of(level.dimension(), ctx.getClickedPos()));
         level.playSound(null, ctx.getClickedPos(), ModSounds.JAR.get(), SoundSource.BLOCKS, 1.0F, 2.0F);
         player.sendSystemMessage(Component.translatable("tc.handmirrorlinked"));
      }
      return InteractionResult.SUCCESS;
   }

   /** The linked magic mirror, if its world is there (null otherwise). */
   @Nullable
   public static MirrorBlockEntity linkedMirror(ItemStack mirror, Level level) {
      GlobalPos gp = mirror.get(ModDevices.MIRROR_LINK.get());
      if (gp == null || level.getServer() == null) return null;
      ServerLevel tl = level.getServer().getLevel(gp.dimension());
      if (tl == null) return null;
      return tl.getBlockEntity(gp.pos()) instanceof MirrorBlockEntity m && m.getClass() == MirrorBlockEntity.class ? m : null;
   }

   private static void broken(ItemStack mirror, Player player) {
      mirror.remove(ModDevices.MIRROR_LINK.get());
      player.level().playSound(null, player.blockPosition(), ModSounds.ZAP.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
      player.sendSystemMessage(Component.translatable("tc.handmirrorerror"));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (!level.isClientSide && stack.has(ModDevices.MIRROR_LINK.get())) {
         GlobalPos gp = stack.get(ModDevices.MIRROR_LINK.get());
         if (level.getServer().getLevel(gp.dimension()) == null) return InteractionResultHolder.pass(stack);
         if (linkedMirror(stack, level) == null) {
            broken(stack, player);
            return InteractionResultHolder.pass(stack);
         }
         player.openMenu(new SimpleMenuProvider((id, inv, p) -> new HandMirrorMenu(id, inv), stack.getHoverName()));
      }
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }

   /** ItemHandMirror.transport: sends the items to the linked mirror. */
   public static boolean transport(ItemStack mirror, ItemStack items, Player player) {
      if (!mirror.has(ModDevices.MIRROR_LINK.get())) return false;
      MirrorBlockEntity tm = linkedMirror(mirror, player.level());
      if (tm == null) {
         if (player instanceof ServerPlayer) broken(mirror, player);
         return false;
      }
      if (tm.transportDirect(items)) {
         player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.1F, 1.0F);
      }
      return true;
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
