package thaumcraft.common.items.curios;

import javax.annotation.Nullable;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.items.ItemTCBase;

public class ItemPrimordialPearl extends ItemTCBase {
   public ItemPrimordialPearl() {
      super("primordial_pearl");
      this.maxStackSize = 1;
      this.setMaxDamage(8);
      this.addPropertyOverride(new ResourceLocation("type"), new IItemPropertyGetter() {
         @SideOnly(Side.CLIENT)
         public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
            if (stack.getItemDamage() < 3) {
               return 0.0F;
            } else {
               return stack.getItemDamage() < 6 ? 1.0F : 2.0F;
            }
         }
      });
      this.setNoRepair();
   }

   public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
      return false;
   }

   public boolean isRepairable() {
      return false;
   }

   @Override
   public String getTranslationKey(ItemStack stack) {
      if (stack.getItemDamage() < 3) {
         return super.getTranslationKey() + ".pearl";
      } else {
         return stack.getItemDamage() < 6 ? super.getTranslationKey() + ".nodule" : super.getTranslationKey() + ".mote";
      }
   }

   public ItemStack getContainerItem(ItemStack itemStack) {
      return !this.hasContainerItem(itemStack)
         ? ItemStack.EMPTY
         : new ItemStack(itemStack.getItem(), itemStack.getCount(), itemStack.getItemDamage() + 1);
   }

   public boolean hasContainerItem(ItemStack stack) {
      return stack.getItemDamage() < 7;
   }

   public EnumRarity getRarity(ItemStack stack) {
      return EnumRarity.UNCOMMON;
   }

   public boolean isEnchantable(ItemStack stack) {
      return false;
   }

   public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
      return false;
   }
}
