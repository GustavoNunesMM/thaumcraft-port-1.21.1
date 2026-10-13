package thaumcraft.common.items.consumables;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import thaumcraft.common.entities.projectile.EntityAlumentum;
import thaumcraft.common.items.ItemTCBase;

public class ItemAlumentum extends ItemTCBase {
   public ItemAlumentum() {
      super("alumentum");
      this.setMaxStackSize(64);
      this.setHasSubtypes(true);
      this.setMaxDamage(0);
   }

   public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
      if (!player.capabilities.isCreativeMode) {
         player.getHeldItem(hand).shrink(1);
      }

      player.playSound(SoundEvents.ENTITY_EGG_THROW, 0.3F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
      if (!world.isRemote) {
         EntityAlumentum alumentum = new EntityAlumentum(world, player);
         alumentum.shoot(player, player.rotationPitch, player.rotationYaw, -5.0F, 0.4F, 2.0F);
         world.spawnEntity(alumentum);
      }

      return new ActionResult(EnumActionResult.SUCCESS, player.getHeldItem(hand));
   }
}
