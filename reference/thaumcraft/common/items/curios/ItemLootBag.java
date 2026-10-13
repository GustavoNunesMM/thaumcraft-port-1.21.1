package thaumcraft.common.items.curios;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.items.ItemTCBase;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.Utils;

public class ItemLootBag extends ItemTCBase {
   public ItemLootBag() {
      super("loot_bag", "common", "uncommon", "rare");
      this.setMaxStackSize(16);
   }

   public EnumRarity getRarity(ItemStack stack) {
      switch (stack.getItemDamage()) {
         case 1:
            return EnumRarity.UNCOMMON;
         case 2:
            return EnumRarity.RARE;
         default:
            return EnumRarity.COMMON;
      }
   }

   @SideOnly(Side.CLIENT)
   public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
      super.addInformation(stack, worldIn, tooltip, flagIn);
      tooltip.add(I18n.translateToLocal("tc.lootbag"));
   }

   public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
      if (!world.isRemote) {
         int q = 8 + world.rand.nextInt(5);

         for (int a = 0; a < q; a++) {
            ItemStack is = Utils.generateLoot(player.getHeldItem(hand).getItemDamage(), world.rand);
            if (is != null && !is.isEmpty()) {
               world.spawnEntity(new EntityItem(world, player.posX, player.posY, player.posZ, is.copy()));
            }
         }

         player.playSound(SoundsTC.coins, 0.75F, 1.0F);
      }

      player.getHeldItem(hand).shrink(1);
      return new ActionResult(EnumActionResult.SUCCESS, player.getHeldItem(hand));
   }
}
