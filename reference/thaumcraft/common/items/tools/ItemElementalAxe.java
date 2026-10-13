package thaumcraft.common.items.tools;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.utils.EntityUtils;

public class ItemElementalAxe extends ItemAxe implements IThaumcraftItems {
   public ItemElementalAxe(ToolMaterial enumtoolmaterial) {
      super(enumtoolmaterial, 8.0F, -3.0F);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setRegistryName("elemental_axe");
      this.setTranslationKey("elemental_axe");
      ConfigItems.ITEM_VARIANT_HOLDERS.add(this);
   }

   @Override
   public Item getItem() {
      return this;
   }

   @Override
   public String[] getVariantNames() {
      return new String[]{"normal"};
   }

   @Override
   public int[] getVariantMeta() {
      return new int[]{0};
   }

   @Override
   public ItemMeshDefinition getCustomMesh() {
      return null;
   }

   @Override
   public ModelResourceLocation getCustomModelResourceLocation(String variant) {
      return new ModelResourceLocation("thaumcraft:" + variant);
   }

   public Set<String> getToolClasses(ItemStack stack) {
      return ImmutableSet.of("axe");
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.RARE;
   }

   public boolean getIsRepairable(ItemStack stack1, ItemStack stack2) {
      return stack2.isItemEqual(new ItemStack(ItemsTC.ingots, 1, 0)) ? true : super.getIsRepairable(stack1, stack2);
   }

   public EnumAction getItemUseAction(ItemStack itemstack) {
      return EnumAction.BOW;
   }

   public int getMaxItemUseDuration(ItemStack stack) {
      return 72000;
   }

   public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand) {
      playerIn.setActiveHand(hand);
      return new ActionResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(hand));
   }

   public void onUsingTick(ItemStack stack, EntityLivingBase player, int count) {
      List<EntityItem> stuff = EntityUtils.getEntitiesInRange(
         player.world, player.posX, player.posY, player.posZ, player, EntityItem.class, 10.0
      );
      if (stuff != null && stuff.size() > 0) {
         for (EntityItem e : stuff) {
            if (!e.isDead) {
               double d6 = e.posX - player.posX;
               double d8 = e.posY - player.posY + player.height / 2.0F;
               double d10 = e.posZ - player.posZ;
               double d11 = MathHelper.sqrt(d6 * d6 + d8 * d8 + d10 * d10);
               d6 /= d11;
               d8 /= d11;
               d10 /= d11;
               double d13 = 0.3;
               e.motionX -= d6 * d13;
               e.motionY -= d8 * d13 - 0.1;
               e.motionZ -= d10 * d13;
               if (e.motionX > 0.25) {
                  e.motionX = 0.25;
               }

               if (e.motionX < -0.25) {
                  e.motionX = -0.25;
               }

               if (e.motionY > 0.25) {
                  e.motionY = 0.25;
               }

               if (e.motionY < -0.25) {
                  e.motionY = -0.25;
               }

               if (e.motionZ > 0.25) {
                  e.motionZ = 0.25;
               }

               if (e.motionZ < -0.25) {
                  e.motionZ = -0.25;
               }

               if (player.world.isRemote) {
                  FXDispatcher.INSTANCE
                     .crucibleBubble(
                        (float)e.posX + (player.world.rand.nextFloat() - player.world.rand.nextFloat()) * 0.2F,
                        (float)e.posY
                           + e.height
                           + (player.world.rand.nextFloat() - player.world.rand.nextFloat()) * 0.2F,
                        (float)e.posZ + (player.world.rand.nextFloat() - player.world.rand.nextFloat()) * 0.2F,
                        0.33F,
                        0.33F,
                        1.0F
                     );
               }
            }
         }
      }
   }

   public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
      if (tab != ConfigItems.TABTC && tab != CreativeTabs.SEARCH) {
         super.getSubItems(tab, items);
      } else {
         ItemStack w1 = new ItemStack(this);
         EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.BURROWING, 1);
         EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.COLLECTOR, 1);
         items.add(w1);
      }
   }
}
