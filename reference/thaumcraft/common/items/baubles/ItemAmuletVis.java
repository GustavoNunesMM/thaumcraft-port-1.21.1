package thaumcraft.common.items.baubles;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.items.ItemTCBase;

public class ItemAmuletVis extends ItemTCBase implements IBauble {
   public ItemAmuletVis() {
      super("amulet_vis", "found", "crafted");
      this.maxStackSize = 1;
      this.setMaxDamage(0);
      this.setHasSubtypes(true);
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return itemstack.getItemDamage() == 0 ? EnumRarity.UNCOMMON : EnumRarity.RARE;
   }

   public BaubleType getBaubleType(ItemStack itemstack) {
      return BaubleType.AMULET;
   }

   public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
      if (player instanceof EntityPlayer && !player.world.isRemote && player.ticksExisted % (itemstack.getItemDamage() == 0 ? 40 : 5) == 0) {
         NonNullList<ItemStack> inv = ((EntityPlayer)player).inventory.mainInventory;

         for (int a = 0; a < InventoryPlayer.getHotbarSize(); a++) {
            if (RechargeHelper.rechargeItem(player.world, (ItemStack)inv.get(a), player.getPosition(), (EntityPlayer)player, 1) > 0.0F) {
               return;
            }
         }

         IBaublesItemHandler baubles = BaublesApi.getBaublesHandler((EntityPlayer)player);

         for (int ax = 0; ax < baubles.getSlots(); ax++) {
            if (RechargeHelper.rechargeItem(player.world, baubles.getStackInSlot(ax), player.getPosition(), (EntityPlayer)player, 1) > 0.0F) {
               return;
            }
         }

         inv = ((EntityPlayer)player).inventory.armorInventory;

         for (int axx = 0; axx < inv.size(); axx++) {
            if (RechargeHelper.rechargeItem(player.world, (ItemStack)inv.get(axx), player.getPosition(), (EntityPlayer)player, 1) > 0.0F) {
               return;
            }
         }
      }
   }

   @SideOnly(Side.CLIENT)
   public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
      tooltip.add(TextFormatting.AQUA + I18n.translateToLocal("item.amulet_vis.text"));
   }
}
