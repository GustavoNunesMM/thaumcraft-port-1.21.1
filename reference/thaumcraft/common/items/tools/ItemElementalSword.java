package thaumcraft.common.items.tools;

import java.util.List;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.utils.EntityUtils;

public class ItemElementalSword extends ItemSword implements IThaumcraftItems {
   public ItemElementalSword(ToolMaterial enumtoolmaterial) {
      super(enumtoolmaterial);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setRegistryName("elemental_sword");
      this.setTranslationKey("elemental_sword");
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

   public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
      if (tab != ConfigItems.TABTC && tab != CreativeTabs.SEARCH) {
         super.getSubItems(tab, items);
      } else {
         ItemStack w1 = new ItemStack(this);
         EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.ARCING, 2);
         items.add(w1);
      }
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.RARE;
   }

   public boolean getIsRepairable(ItemStack stack1, ItemStack stack2) {
      return stack2.isItemEqual(new ItemStack(ItemsTC.ingots, 1, 0)) ? true : super.getIsRepairable(stack1, stack2);
   }

   public EnumAction getItemUseAction(ItemStack stack) {
      return EnumAction.NONE;
   }

   public int getMaxItemUseDuration(ItemStack stack) {
      return 72000;
   }

   public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand) {
      playerIn.setActiveHand(hand);
      return new ActionResult(EnumActionResult.SUCCESS, playerIn.getHeldItem(hand));
   }

   public void onUsingTick(ItemStack stack, EntityLivingBase player, int count) {
      super.onUsingTick(stack, player, count);
      int ticks = this.getMaxItemUseDuration(stack) - count;
      if (player.motionY < 0.0) {
         player.motionY /= 1.2F;
         player.fallDistance /= 1.2F;
      }

      player.motionY += 0.08F;
      if (player.motionY > 0.5) {
         player.motionY = 0.2F;
      }

      if (player instanceof EntityPlayerMP) {
         EntityUtils.resetFloatCounter((EntityPlayerMP)player);
      }

      List targets = player.world.getEntitiesWithinAABBExcludingEntity(player, player.getEntityBoundingBox().grow(2.5, 2.5, 2.5));
      if (targets.size() > 0) {
         for (int var9 = 0; var9 < targets.size(); var9++) {
            Entity entity = (Entity)targets.get(var9);
            if (!(entity instanceof EntityPlayer)
               && entity instanceof EntityLivingBase
               && !entity.isDead
               && (player.getRidingEntity() == null || player.getRidingEntity() != entity)) {
               Vec3d p = new Vec3d(player.posX, player.posY, player.posZ);
               Vec3d t = new Vec3d(entity.posX, entity.posY, entity.posZ);
               double distance = p.distanceTo(t) + 0.1;
               Vec3d r = new Vec3d(t.x - p.x, t.y - p.y, t.z - p.z);
               entity.motionX = entity.motionX + r.x / 2.5 / distance;
               entity.motionY = entity.motionY + r.y / 2.5 / distance;
               entity.motionZ = entity.motionZ + r.z / 2.5 / distance;
            }
         }
      }

      if (player.world.isRemote) {
         int miny = (int)(player.getEntityBoundingBox().minY - 2.0);
         if (player.onGround) {
            miny = MathHelper.floor(player.getEntityBoundingBox().minY);
         }

         for (int a = 0; a < 5; a++) {
            FXDispatcher.INSTANCE
               .smokeSpiral(
                  player.posX,
                  player.getEntityBoundingBox().minY + player.height / 2.0F,
                  player.posZ,
                  1.5F,
                  player.world.rand.nextInt(360),
                  miny,
                  14540253
               );
         }

         if (player.onGround) {
            float r1 = player.world.rand.nextFloat() * 360.0F;
            float mx = -MathHelper.sin(r1 / 180.0F * (float) Math.PI) / 5.0F;
            float mz = MathHelper.cos(r1 / 180.0F * (float) Math.PI) / 5.0F;
            player.world
               .spawnParticle(
                  EnumParticleTypes.SMOKE_NORMAL,
                  player.posX,
                  player.getEntityBoundingBox().minY + 0.1F,
                  player.posZ,
                  mx,
                  0.0,
                  mz,
                  new int[0]
               );
         }
      } else if (ticks == 0 || ticks % 20 == 0) {
         player.playSound(SoundsTC.wind, 0.5F, 0.9F + player.world.rand.nextFloat() * 0.2F);
      }

      if (ticks % 20 == 0) {
         stack.damageItem(1, player);
      }
   }
}
