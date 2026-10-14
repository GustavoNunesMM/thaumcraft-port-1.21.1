package thaumcraft.common.items.tools;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.entities.projectile.GrappleEntity;
import thaumcraft.common.registers.ModExtras;
import thaumcraft.common.registers.ModSounds;

/**
 * ItemGrappleGun: fires an arcanomagnetic grapple (1 vis of its 100 charge) that reels the user in once it sticks;
 * sneaking lets go. The "loaded" flag (the gun's model without its head) stays while a grapple of the holder exists.
 */
public class GrappleGunItem extends Item implements IRechargable {
   public GrappleGunItem(Properties properties) {
      super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
   }

   public static boolean isLoaded(ItemStack stack) {
      return stack.getOrDefault(ModExtras.GRAPPLE_LOADED.get(), false);
   }

   @Override
   public int getMaxCharge(ItemStack stack, LivingEntity player) {
      return 100;
   }

   @Override
   public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player) {
      return EnumChargeDisplay.NORMAL;
   }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      if (!level.isClientSide && isLoaded(stack) && !GrappleEntity.hasGrapple(level, entity)) stack.remove(ModExtras.GRAPPLE_LOADED.get());
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      player.playSound(ModSounds.ICE.get(), 3.0F, 0.8F + level.random.nextFloat() * 0.1F);
      if (level instanceof ServerLevel && RechargeHelper.getCharge(stack) > 0) {
         GrappleEntity grapple = new GrappleEntity(level, player, hand);
         grapple.shootFromRotation(player, player.getXRot(), player.getYRot(), -5.0F, 1.5F, 0.0F);
         double side = grapple.hand == InteractionHand.MAIN_HAND ? 1 : -1;
         double px = -Mth.cos((player.getYRot() - 0.5F) / 180.0F * (float) Math.PI) * 0.2F * side;
         double pz = -Mth.sin((player.getYRot() - 0.5F) / 180.0F * (float) Math.PI) * 0.3F * side;
         Vec3 vl = player.getLookAngle();
         grapple.setPos(grapple.getX() + px + vl.x, grapple.getY(), grapple.getZ() + pz + vl.y);
         if (level.addFreshEntity(grapple)) {
            RechargeHelper.consumeCharge(stack, player, 1);
            stack.set(ModExtras.GRAPPLE_LOADED.get(), true);
         }
      }
      return InteractionResultHolder.success(stack);
   }

   @Override
   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      if (oldStack.is(this) && newStack.is(this)) return isLoaded(oldStack) != isLoaded(newStack);
      return !ItemStack.isSameItem(oldStack, newStack);
   }
}
