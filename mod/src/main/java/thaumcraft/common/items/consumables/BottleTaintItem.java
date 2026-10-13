package thaumcraft.common.items.consumables;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.projectile.BottleTaintEntity;

/** ItemBottleTaint: thrown; shatters into taint. */
public class BottleTaintItem extends Item {
   public BottleTaintItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      player.playSound(SoundEvents.EGG_THROW, 0.5F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
      if (!level.isClientSide) {
         BottleTaintEntity e = new BottleTaintEntity(level, player);
         e.setItem(stack.copyWithCount(1));
         e.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0F, 0.66F, 1.0F);
         level.addFreshEntity(e);
      }
      stack.consume(1, player);
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }
}
