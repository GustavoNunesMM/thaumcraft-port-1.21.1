package thaumcraft.common.items;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.common.registers.ModItems;

/** ItemCelestialNotes: notes of the sun, of a star field (4) or of the moon in a phase (8), made by scanning the sky. */
public class CelestialNotesItem extends Item {
   public static final String[] VARIANTS = {"sun", "stars_1", "stars_2", "stars_3", "stars_4", "moon_1", "moon_2", "moon_3", "moon_4", "moon_5",
      "moon_6", "moon_7", "moon_8"};
   public final int index;

   public CelestialNotesItem(int index, Properties properties) {
      super(properties);
      this.index = index;
   }

   /** The notes of a 1.12 metadata value (0 = sun, 1-4 stars, 5-12 moon phases). */
   public static ItemStack of(int index) {
      return new ItemStack(ModItems.CELESTIAL_NOTES.get(Math.floorMod(index, VARIANTS.length)).get());
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("item.celestial_notes." + VARIANTS[index] + ".text").withStyle(ChatFormatting.AQUA));
   }
}
