package thaumcraft.api.research.theorycraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import thaumcraft.api.research.ResearchCategories;

public class CardExperimentation extends TheorycraftCard {
   @Override
   public int getInspirationCost() {
      return 2;
   }

   @Override
   public String getLocalizedName() {
      return new TextComponentTranslation("card.experimentation.name", new Object[0]).getUnformattedText();
   }

   @Override
   public String getLocalizedText() {
      return new TextComponentTranslation("card.experimentation.text", new Object[0]).getUnformattedText();
   }

   @Override
   public boolean activate(EntityPlayer player, ResearchTableData data) {
      try {
         String[] s = ResearchCategories.researchCategories.keySet().toArray(new String[0]);
         String cat = s[player.getRNG().nextInt(s.length)];
         data.addTotal(cat, MathHelper.getInt(player.getRNG(), 15, 30));
         data.addTotal("BASICS", MathHelper.getInt(player.getRNG(), 1, 10));
         return true;
      } catch (Exception var5) {
         return false;
      }
   }
}
