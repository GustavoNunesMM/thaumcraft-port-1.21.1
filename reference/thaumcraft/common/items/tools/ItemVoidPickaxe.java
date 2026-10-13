package thaumcraft.common.items.tools;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;

public class ItemVoidPickaxe extends ItemPickaxe implements IWarpingGear, IThaumcraftItems {
   public ItemVoidPickaxe(ToolMaterial enumtoolmaterial) {
      super(enumtoolmaterial);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setRegistryName("void_pick");
      this.setTranslationKey("void_pick");
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
      return ImmutableSet.of("pickaxe");
   }

   public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
      super.onUpdate(stack, world, entity, itemSlot, isSelected);
      if (stack.isItemDamaged() && entity != null && entity.ticksExisted % 20 == 0 && entity instanceof EntityLivingBase) {
         stack.damageItem(-1, (EntityLivingBase)entity);
      }
   }

   public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
      if (!player.world.isRemote
         && entity instanceof EntityLivingBase
         && (!(entity instanceof EntityPlayer) || FMLCommonHandler.instance().getMinecraftServerInstance().isPVPEnabled())) {
         ((EntityLivingBase)entity).addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 80));
      }

      return super.onLeftClickEntity(stack, player, entity);
   }

   @Override
   public int getWarp(ItemStack itemstack, EntityPlayer player) {
      return 1;
   }
}
