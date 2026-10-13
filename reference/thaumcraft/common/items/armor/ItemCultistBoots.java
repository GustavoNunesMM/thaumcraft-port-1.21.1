package thaumcraft.common.items.armor;

import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;

public class ItemCultistBoots extends ItemArmor implements IWarpingGear, IVisDiscountGear, IThaumcraftItems {
   public ItemCultistBoots() {
      super(ArmorMaterial.IRON, 2, EntityEquipmentSlot.FEET);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setRegistryName("crimson_boots");
      this.setTranslationKey("crimson_boots");
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

   @SideOnly(Side.CLIENT)
   @Override
   public ItemMeshDefinition getCustomMesh() {
      return null;
   }

   @SideOnly(Side.CLIENT)
   @Override
   public ModelResourceLocation getCustomModelResourceLocation(String variant) {
      return new ModelResourceLocation("thaumcraft:" + variant);
   }

   @SideOnly(Side.CLIENT)
   public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
      return "thaumcraft:textures/entity/armor/cultistboots.png";
   }

   public boolean getIsRepairable(ItemStack stack1, ItemStack stack2) {
      return stack2.isItemEqual(new ItemStack(Items.IRON_INGOT)) ? true : super.getIsRepairable(stack1, stack2);
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.UNCOMMON;
   }

   @Override
   public int getWarp(ItemStack itemstack, EntityPlayer player) {
      return 1;
   }

   @Override
   public int getVisDiscount(ItemStack stack, EntityPlayer player) {
      return 1;
   }
}
