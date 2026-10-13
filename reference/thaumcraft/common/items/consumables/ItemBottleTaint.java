package thaumcraft.common.items.consumables;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.entities.projectile.EntityBottleTaint;
import thaumcraft.common.items.ItemTCBase;

public class ItemBottleTaint extends ItemTCBase {
   public ItemBottleTaint() {
      super("bottle_taint");
      this.maxStackSize = 8;
      this.setMaxDamage(0);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setHasSubtypes(false);
   }

   public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
      if (!player.capabilities.isCreativeMode) {
         player.getHeldItem(hand).shrink(1);
      }

      player.playSound(SoundEvents.ENTITY_EGG_THROW, 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
      if (!world.isRemote) {
         EntityBottleTaint entityBottle = new EntityBottleTaint(world, player);
         entityBottle.shoot(player, player.rotationPitch, player.rotationYaw, -5.0F, 0.66F, 1.0F);
         world.spawnEntity(entityBottle);
      }

      return new ActionResult(EnumActionResult.SUCCESS, player.getHeldItem(hand));
   }
}
