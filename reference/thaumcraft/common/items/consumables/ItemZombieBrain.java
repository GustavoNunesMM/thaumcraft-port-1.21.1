package thaumcraft.common.items.consumables;

import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;

public class ItemZombieBrain extends ItemFood implements IThaumcraftItems {
   public ItemZombieBrain() {
      super(4, 0.2F, true);
      this.setPotionEffect(new PotionEffect(MobEffects.HUNGER, 30, 0), 0.8F);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setRegistryName("brain");
      this.setTranslationKey("brain");
      ConfigItems.ITEM_VARIANT_HOLDERS.add(this);
   }

   public void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
      if (!world.isRemote) {
         if (world.rand.nextFloat() < 0.1F) {
            ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1, IPlayerWarp.EnumWarpType.NORMAL);
         } else {
            ThaumcraftApi.internalMethods.addWarpToPlayer(player, 1 + world.rand.nextInt(3), IPlayerWarp.EnumWarpType.TEMPORARY);
         }
      }
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
}
