package thaumcraft.common.items.consumables;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;

/**
 * ItemSanitySoap: scrub for five seconds to lose all temporary warp and one point of sticky warp (two under a warp
 * ward, one more standing in purifying fluid).
 */
public class SanitySoapItem extends Item {
   public SanitySoapItem(Properties properties) {
      super(properties);
   }

   @Override
   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 100;
   }

   @Override
   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.BLOCK;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      player.startUsingItem(hand);
      return InteractionResultHolder.consume(player.getItemInHand(hand));
   }

   @Override
   public void onUseTick(Level level, LivingEntity player, ItemStack stack, int count) {
      int ticks = getUseDuration(stack, player) - count;
      if (ticks > 95) player.releaseUsingItem();
      if (level.isClientSide) {
         if (level.random.nextFloat() < 0.2F) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.PLAYERS, 0.1F,
               1.5F + level.random.nextFloat() * 0.2F, false);
         }
         for (int a = 0; a < 10; a++) {
            thaumcraft.client.fx.FXDispatcher.crucibleBubble((float) player.getX() - 0.5F + level.random.nextFloat(),
               (float) player.getBoundingBox().minY + level.random.nextFloat() * player.getBbHeight(), (float) player.getZ() - 0.5F + level.random.nextFloat(),
               1.0F, 0.8F, 0.9F);
         }
      }
   }

   @Override
   public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      int qq = getUseDuration(stack, entity) - timeLeft;
      if (qq <= 95 || !(entity instanceof Player player)) return;
      stack.shrink(1);
      if (!level.isClientSide) {
         PlayerWarp warp = ResearchManager.warp(player);
         int amt = 1;
         if (player.hasEffect(ModFlux.WARP_WARD)) amt++;
         if (level.getBlockState(player.blockPosition()).is(ModFlux.PURIFYING_FLUID_BLOCK.get())) amt++;
         if (warp.get(PlayerWarp.Type.NORMAL) > 0) ResearchManager.addWarp(player, -amt, PlayerWarp.Type.NORMAL);
         if (warp.get(PlayerWarp.Type.TEMPORARY) > 0) ResearchManager.addWarp(player, -warp.get(PlayerWarp.Type.TEMPORARY), PlayerWarp.Type.TEMPORARY);
      } else {
         level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.CRAFTSTART.get(), SoundSource.PLAYERS, 0.25F, 1.0F, false);
         for (int a = 0; a < 40; a++) {
            thaumcraft.client.fx.FXDispatcher.crucibleBubble((float) player.getX() - 0.5F + level.random.nextFloat() * 1.5F,
               (float) player.getBoundingBox().minY + level.random.nextFloat() * player.getBbHeight(),
               (float) player.getZ() - 0.5F + level.random.nextFloat() * 1.5F, 1.0F, 0.7F, 0.9F);
         }
      }
   }
}
