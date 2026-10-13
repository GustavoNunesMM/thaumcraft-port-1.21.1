package thaumcraft.common.items;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.entities.projectile.AlumentumEntity;

/** ItemAlumentum: thrown like an egg; explodes on impact. */
public class AlumentumItem extends Item {
   public AlumentumItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      player.playSound(SoundEvents.EGG_THROW, 0.3F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));
      if (!level.isClientSide) {
         AlumentumEntity e = new AlumentumEntity(level, player);
         e.setItem(stack.copyWithCount(1));
         e.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0F, 0.4F, 2.0F);
         level.addFreshEntity(e);
      }
      stack.consume(1, player);
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }
}
