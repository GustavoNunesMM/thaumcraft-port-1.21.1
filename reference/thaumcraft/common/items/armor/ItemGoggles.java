package thaumcraft.common.items.armor;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.render.IRenderBauble;
import baubles.api.render.IRenderBauble.Helper;
import baubles.api.render.IRenderBauble.RenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;

public class ItemGoggles extends ItemArmor implements IVisDiscountGear, IRevealer, IGoggles, IThaumcraftItems, IBauble, IRenderBauble {
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/items/goggles_bauble.png");

   public ItemGoggles() {
      super(ThaumcraftMaterials.ARMORMAT_SPECIAL, 4, EntityEquipmentSlot.HEAD);
      this.setMaxDamage(350);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setRegistryName("goggles");
      this.setTranslationKey("goggles");
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
      return "thaumcraft:textures/entity/armor/goggles.png";
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.RARE;
   }

   public boolean getIsRepairable(ItemStack stack1, ItemStack stack2) {
      return stack2.isItemEqual(new ItemStack(ItemsTC.ingots, 1, 2)) ? true : super.getIsRepairable(stack1, stack2);
   }

   @Override
   public int getVisDiscount(ItemStack stack, EntityPlayer player) {
      return 5;
   }

   @Override
   public boolean showNodes(ItemStack itemstack, EntityLivingBase player) {
      return true;
   }

   @Override
   public boolean showIngamePopups(ItemStack itemstack, EntityLivingBase player) {
      return true;
   }

   public BaubleType getBaubleType(ItemStack arg0) {
      return BaubleType.HEAD;
   }

   @SideOnly(Side.CLIENT)
   public void onPlayerBaubleRender(ItemStack stack, EntityPlayer player, RenderType type, float ticks) {
      if (type == RenderType.HEAD) {
         boolean armor = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD) != null;
         Minecraft.getMinecraft().renderEngine.bindTexture(this.tex);
         Helper.translateToHeadLevel(player);
         Helper.translateToFace();
         Helper.defaultTransforms();
         GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
         GlStateManager.translate(-0.5, -0.5, armor ? 0.12F : 0.0);
         UtilsFX.renderTextureIn3D(0.0F, 0.0F, 1.0F, 1.0F, 16, 26, 0.1F);
      }
   }
}
