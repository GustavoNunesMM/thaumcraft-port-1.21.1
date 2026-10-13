package thaumcraft.common.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModItems;

/** Port of ItemCrystalEssence: a vis crystal holding one point of a single aspect. */
public class VisCrystalItem extends Item {
   public VisCrystalItem(Properties properties) {
      super(properties);
   }

   /** ThaumcraftApiHelper.makeCrystal */
   public static ItemStack of(Aspect aspect, int count) {
      ItemStack is = new ItemStack(ModItems.CRYSTAL_ESSENCE.get(), count);
      is.set(ModDataComponents.ESSENTIA.get(), new AspectList().add(aspect, 1));
      return is;
   }

   public static Aspect getAspect(ItemStack stack) {
      AspectList a = stack.get(ModDataComponents.ESSENTIA.get());
      return a == null || a.isEmpty() ? null : a.getAspects()[0];
   }

   @Override
   public Component getName(ItemStack stack) {
      Aspect a = getAspect(stack);
      return a == null ? Component.translatable(getDescriptionId(stack), "") : Component.translatable(getDescriptionId(stack), a.getName());
   }
}
