package thaumcraft.common.items.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.common.registers.ModGear;

/**
 * ItemFortressArmor: thaumium fortress armor (ModelFortressArmor). The helm can be infused with goggles
 * (thaumcraft:goggles) and one of three masks (thaumcraft:mask); the ISpecialArmor set bonus and its resistances to magic,
 * fire and explosions are applied in GearEvents.
 */
public class FortressArmorItem extends TCArmorItem implements IGoggles, IRevealer {
   public FortressArmorItem(Holder<ArmorMaterial> material, Type type, Properties p) {
      super(material, type, p, "fortress_armor", null);
   }

   public static boolean hasGoggles(ItemStack stack) {
      return stack.has(ModGear.GOGGLES.get());
   }

   /** -1 without a mask. */
   public static int getMask(ItemStack stack) {
      return stack.getOrDefault(ModGear.MASK.get(), -1);
   }

   /** Fortress pieces worn on legs, chest and head (the set count of ModelFortressArmor/getProperties). */
   public static int countSet(LivingEntity entity) {
      int set = 0;
      for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
         if (entity.getItemBySlot(s).getItem() instanceof FortressArmorItem) set++;
      }
      return set;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      if (hasGoggles(stack)) tooltip.add(Component.translatable("item.goggles.name").withStyle(ChatFormatting.DARK_PURPLE));
      if (getMask(stack) >= 0) tooltip.add(Component.translatable("item.fortress_helm.mask." + getMask(stack)).withStyle(ChatFormatting.GOLD));
      super.appendHoverText(stack, ctx, tooltip, flag);
   }

   @Override
   public boolean showNodes(ItemStack stack, LivingEntity wearer) {
      return hasGoggles(stack);
   }

   @Override
   public boolean showIngamePopups(ItemStack stack, LivingEntity wearer) {
      return hasGoggles(stack);
   }
}
