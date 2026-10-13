package thaumcraft.common.items.armor;

import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;
import thaumcraft.common.lib.events.PlayerEvents;

public class ItemBootsTraveller extends ItemArmor implements IThaumcraftItems, IRechargable {
   public ItemBootsTraveller() {
      super(ThaumcraftMaterials.ARMORMAT_SPECIAL, 4, EntityEquipmentSlot.FEET);
      this.setMaxDamage(350);
      this.setRegistryName("traveller_boots");
      this.setTranslationKey("traveller_boots");
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
      return "thaumcraft:textures/entity/armor/bootstraveler.png";
   }

   public boolean getIsRepairable(ItemStack stack1, ItemStack stack2) {
      return stack2.isItemEqual(new ItemStack(Items.LEATHER)) ? true : super.getIsRepairable(stack1, stack2);
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.RARE;
   }

   public void onArmorTick(World world, EntityPlayer player, ItemStack itemStack) {
      boolean hasCharge = RechargeHelper.getCharge(itemStack) > 0;
      if (!world.isRemote && player.ticksExisted % 20 == 0) {
         int e = 0;
         if (itemStack.hasTagCompound()) {
            e = itemStack.getTagCompound().getInteger("energy");
         }

         if (e > 0) {
            e--;
         } else if (e <= 0 && RechargeHelper.consumeCharge(itemStack, player, 1)) {
            e = 60;
         }

         itemStack.setTagInfo("energy", new NBTTagInt(e));
      }

      if (hasCharge && !player.capabilities.isFlying && player.moveForward > 0.0F) {
         if (player.world.isRemote && !player.isSneaking()) {
            if (!PlayerEvents.prevStep.containsKey(player.getEntityId())) {
               PlayerEvents.prevStep.put(player.getEntityId(), player.stepHeight);
            }

            player.stepHeight = 1.0F;
         }

         if (player.onGround) {
            float bonus = 0.05F;
            if (player.isInWater()) {
               bonus /= 4.0F;
            }

            player.moveRelative(0.0F, 0.0F, bonus, 1.0F);
         } else {
            if (player.isInWater()) {
               player.moveRelative(0.0F, 0.0F, 0.025F, 1.0F);
            }

            player.jumpMovementFactor = 0.05F;
         }
      }
   }

   @Override
   public int getMaxCharge(ItemStack stack, EntityLivingBase player) {
      return 240;
   }

   @Override
   public IRechargable.EnumChargeDisplay showInHud(ItemStack stack, EntityLivingBase player) {
      return IRechargable.EnumChargeDisplay.PERIODIC;
   }
}
