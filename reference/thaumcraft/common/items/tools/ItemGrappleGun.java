package thaumcraft.common.items.tools;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.entities.projectile.EntityGrapple;
import thaumcraft.common.items.ItemTCBase;
import thaumcraft.common.lib.SoundsTC;

public class ItemGrappleGun extends ItemTCBase implements IRechargable {
   public ItemGrappleGun() {
      super("grapple_gun");
      this.setMaxStackSize(1);
      this.addPropertyOverride(new ResourceLocation("type"), new IItemPropertyGetter() {
         @SideOnly(Side.CLIENT)
         public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
            return stack.hasTagCompound() && stack.getTagCompound().getByte("loaded") == 1 ? 1.0F : 0.0F;
         }
      });
   }

   @Override
   public int getMaxCharge(ItemStack stack, EntityLivingBase player) {
      return 100;
   }

   @Override
   public IRechargable.EnumChargeDisplay showInHud(ItemStack stack, EntityLivingBase player) {
      return IRechargable.EnumChargeDisplay.NORMAL;
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.UNCOMMON;
   }

   public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
      if (!EntityGrapple.grapples.containsKey(entityIn.getEntityId()) && stack.hasTagCompound() && stack.getTagCompound().getByte("loaded") == 1) {
         stack.setTagInfo("loaded", new NBTTagByte((byte)0));
      }
   }

   public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
      player.playSound(SoundsTC.ice, 3.0F, 0.8F + world.rand.nextFloat() * 0.1F);
      if (!world.isRemote && RechargeHelper.getCharge(player.getHeldItem(hand)) > 0) {
         EntityGrapple grapple = new EntityGrapple(world, player, hand);
         grapple.shoot(player, player.rotationPitch, player.rotationYaw, -5.0F, 1.5F, 0.0F);
         double px = -MathHelper.cos((player.rotationYaw - 0.5F) / 180.0F * 3.141593F) * 0.2F * (grapple.hand == EnumHand.MAIN_HAND ? 1 : -1);
         double pz = -MathHelper.sin((player.rotationYaw - 0.5F) / 180.0F * 3.141593F) * 0.3F * (grapple.hand == EnumHand.MAIN_HAND ? 1 : -1);
         Vec3d vl = player.getLookVec();
         grapple.posX = grapple.posX + (px + vl.x);
         grapple.posZ = grapple.posZ + (pz + vl.y);
         if (world.spawnEntity(grapple)) {
            RechargeHelper.consumeCharge(player.getHeldItem(hand), player, 1);
            player.getHeldItem(hand).setTagInfo("loaded", new NBTTagByte((byte)1));
         }
      }

      return new ActionResult(EnumActionResult.SUCCESS, player.getHeldItem(hand));
   }

   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      if (oldStack.getItem() != null
         && oldStack.getItem() == this
         && newStack.getItem() != null
         && newStack.getItem() == this
         && !ItemStack.areItemStackTagsEqual(oldStack, newStack)) {
         boolean b1 = !oldStack.hasTagCompound() || oldStack.getTagCompound().getByte("loaded") == 0;
         boolean b2 = !newStack.hasTagCompound() || newStack.getTagCompound().getByte("loaded") == 0;
         return b1 != b2;
      } else {
         return newStack.getItem() != oldStack.getItem();
      }
   }
}
