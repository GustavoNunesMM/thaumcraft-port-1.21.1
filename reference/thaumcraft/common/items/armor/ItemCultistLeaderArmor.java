package thaumcraft.common.items.armor;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.client.renderers.models.gear.ModelLeaderArmor;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;

public class ItemCultistLeaderArmor extends ItemArmor implements IThaumcraftItems {
   ModelBiped model1 = null;
   ModelBiped model2 = null;
   ModelBiped model = null;

   public ItemCultistLeaderArmor(String name, int j, EntityEquipmentSlot k) {
      super(ThaumcraftMaterials.ARMORMAT_CULTIST_LEADER, j, k);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setRegistryName(name);
      this.setTranslationKey(name);
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

   @Override
   public ModelResourceLocation getCustomModelResourceLocation(String variant) {
      return new ModelResourceLocation("thaumcraft:" + variant);
   }

   public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
      return "thaumcraft:textures/entity/armor/cultist_leader_armor.png";
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.RARE;
   }

   public boolean getIsRepairable(ItemStack stack1, ItemStack stack2) {
      return stack2.isItemEqual(new ItemStack(Items.IRON_INGOT)) ? true : super.getIsRepairable(stack1, stack2);
   }

   @SideOnly(Side.CLIENT)
   public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, EntityEquipmentSlot armorSlot, ModelBiped _default) {
      if (this.model1 == null) {
         this.model1 = new ModelLeaderArmor(1.0F);
      }

      if (this.model2 == null) {
         this.model2 = new ModelLeaderArmor(0.5F);
      }

      this.model = CustomArmorHelper.getCustomArmorModel(entityLiving, itemStack, armorSlot, this.model, this.model1, this.model2);
      return this.model;
   }
}
