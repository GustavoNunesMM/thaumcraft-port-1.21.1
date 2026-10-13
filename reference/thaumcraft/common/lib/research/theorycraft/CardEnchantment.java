package thaumcraft.common.lib.research.theorycraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

public class CardEnchantment extends TheorycraftCard {
   @Override
   public int getInspirationCost() {
      return 1;
   }

   @Override
   public boolean isAidOnly() {
      return true;
   }

   @Override
   public String getLocalizedName() {
      return new TextComponentTranslation("card.enchantment.name", new Object[0]).getFormattedText();
   }

   @Override
   public String getLocalizedText() {
      return new TextComponentTranslation("card.enchantment.text", new Object[0]).getFormattedText();
   }

   @Override
   public boolean activate(EntityPlayer player, ResearchTableData data) {
      if (player.experienceLevel >= 5) {
         player.addExperienceLevel(-5);
         data.addTotal("INFUSION", MathHelper.getInt(player.getRNG(), 15, 20));
         data.addTotal("AUROMANCY", MathHelper.getInt(player.getRNG(), 15, 20));
         return true;
      } else {
         return false;
      }
   }
}
