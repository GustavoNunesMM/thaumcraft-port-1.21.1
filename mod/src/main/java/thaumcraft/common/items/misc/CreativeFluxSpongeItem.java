package thaumcraft.common.items.misc;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.FluxRiftEntity;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.world.aura.AuraHandler;

/** ItemCreativeFluxSponge: drains all flux from the 9x9 chunks around the user; sneaking also removes nearby flux rifts. */
public class CreativeFluxSpongeItem extends Item {
   public CreativeFluxSpongeItem(Properties properties) {
      super(properties.stacksTo(1).rarity(Rarity.EPIC));
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.thaumcraft.creative_flux_sponge.tip1").withStyle(ChatFormatting.GREEN));
      tooltip.add(Component.translatable("item.thaumcraft.creative_flux_sponge.tip2").withStyle(ChatFormatting.GREEN));
      tooltip.add(Component.translatable("item.thaumcraft.creative_flux_sponge.tip3").withStyle(ChatFormatting.DARK_AQUA));
      tooltip.add(Component.translatable("item.thaumcraft.creative_flux_sponge.tip4").withStyle(ChatFormatting.DARK_AQUA));
      tooltip.add(Component.translatable("item.thaumcraft.creative_flux_sponge.tip5").withStyle(ChatFormatting.DARK_PURPLE));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (level.isClientSide) {
         player.swing(hand);
         level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.CRAFTSTART.get(), SoundSource.PLAYERS, 0.15F, 1.0F, false);
      } else {
         int q = 0;
         BlockPos p = player.blockPosition();
         for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) q += (int) AuraHandler.drainFlux(level, p.offset(16 * x, 0, 16 * z), 500.0F, false);
         }
         player.sendSystemMessage(Component.translatable("item.thaumcraft.creative_flux_sponge.drained", q).withStyle(ChatFormatting.GREEN));
         if (player.isShiftKeyDown()) {
            List<FluxRiftEntity> list = level.getEntitiesOfClass(FluxRiftEntity.class, player.getBoundingBox().inflate(32.0),
               e -> e.distanceToSqr(player) <= 32.0 * 32.0);
            list.forEach(FluxRiftEntity::discard);
            player.sendSystemMessage(Component.translatable("item.thaumcraft.creative_flux_sponge.rifts", list.size()).withStyle(ChatFormatting.DARK_AQUA));
         }
      }
      return InteractionResultHolder.pass(player.getItemInHand(hand));
   }
}
