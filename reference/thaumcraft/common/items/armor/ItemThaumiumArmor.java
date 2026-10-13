package thaumcraft.common.items.armor;

import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;

public class ItemThaumiumArmor extends ItemArmor implements IThaumcraftItems {
   public ItemThaumiumArmor(String name, ArmorMaterial enumarmormaterial, int j, EntityEquipmentSlot k) {
      super(enumarmormaterial, j, k);
      this.setRegistryName(name);
      this.setTranslationKey(name);
      ConfigItems.ITEM_VARIANT_HOLDERS.add(this);
      this.setCreativeTab(ConfigItems.TABTC);
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

   @Override
   public ModelResourceLocation getCustomModelResourceLocation(String variant) {
      return new ModelResourceLocation("thaumcraft:" + variant);
   }

   public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
      if (stack.getItem() == ItemsTC.thaumiumHelm || stack.getItem() == ItemsTC.thaumiumChest || stack.getItem() == ItemsTC.thaumiumBoots) {
         return "thaumcraft:textures/entity/armor/thaumium_1.png";
      } else {
         return stack.getItem() == ItemsTC.thaumiumLegs
            ? "thaumcraft:textures/entity/armor/thaumium_2.png"
            : "thaumcraft:textures/entity/armor/thaumium_1.png";
      }
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.UNCOMMON;
   }

   public boolean getIsRepairable(ItemStack stack1, ItemStack stack2) {
      return stack2.isItemEqual(new ItemStack(ItemsTC.ingots, 1, 0)) ? true : super.getIsRepairable(stack1, stack2);
   }
}
