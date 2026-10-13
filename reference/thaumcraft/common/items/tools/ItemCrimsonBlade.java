package thaumcraft.common.items.tools;

import java.util.List;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;

public class ItemCrimsonBlade extends ItemSword implements IWarpingGear, IThaumcraftItems {
   public static ToolMaterial toolMatCrimsonVoid = EnumHelper.addToolMaterial("CVOID", 4, 200, 8.0F, 3.5F, 20)
      .setRepairItem(new ItemStack(ItemsTC.ingots, 1, 1));

   public ItemCrimsonBlade() {
      super(toolMatCrimsonVoid);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setRegistryName("crimson_blade");
      this.setTranslationKey("crimson_blade");
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

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.EPIC;
   }

   public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
      super.onUpdate(stack, world, entity, itemSlot, isSelected);
      if (stack.isItemDamaged() && entity != null && entity.ticksExisted % 20 == 0 && entity instanceof EntityLivingBase) {
         stack.damageItem(-1, (EntityLivingBase)entity);
      }
   }

   public boolean hitEntity(ItemStack is, EntityLivingBase target, EntityLivingBase hitter) {
      if (!target.world.isRemote
         && (!(target instanceof EntityPlayer) || !(hitter instanceof EntityPlayer) || FMLCommonHandler.instance().getMinecraftServerInstance().isPVPEnabled())
         )
       {
         try {
            target.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60));
            target.addPotionEffect(new PotionEffect(MobEffects.HUNGER, 120));
         } catch (Exception var5) {
         }
      }

      return super.hitEntity(is, target, hitter);
   }

   @Override
   public int getWarp(ItemStack itemstack, EntityPlayer player) {
      return 2;
   }

   @SideOnly(Side.CLIENT)
   public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
      tooltip.add(TextFormatting.GOLD + I18n.translateToLocal("enchantment.special.sapgreat"));
      super.addInformation(stack, worldIn, tooltip, flagIn);
   }
}
