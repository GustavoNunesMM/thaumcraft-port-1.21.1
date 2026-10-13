package thaumcraft.api.research.theorycraft;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

public class CardNotation extends TheorycraftCard {
   private String cat1;
   private String cat2;

   @Override
   public NBTTagCompound serialize() {
      NBTTagCompound nbt = super.serialize();
      nbt.setString("cat1", this.cat1);
      nbt.setString("cat2", this.cat2);
      return nbt;
   }

   @Override
   public void deserialize(NBTTagCompound nbt) {
      super.deserialize(nbt);
      this.cat1 = nbt.getString("cat1");
      this.cat2 = nbt.getString("cat2");
   }

   @Override
   public boolean isAidOnly() {
      return true;
   }

   @Override
   public int getInspirationCost() {
      return 1;
   }

   @Override
   public String getLocalizedName() {
      return new TextComponentTranslation("card.notation.name", new Object[0]).getUnformattedText();
   }

   @Override
   public String getLocalizedText() {
      return new TextComponentTranslation(
            "card.notation.text",
            new Object[]{
               TextFormatting.BOLD + new TextComponentTranslation("tc.research_category." + this.cat1, new Object[0]).getFormattedText() + TextFormatting.RESET,
               TextFormatting.BOLD + new TextComponentTranslation("tc.research_category." + this.cat2, new Object[0]).getFormattedText() + TextFormatting.RESET
            }
         )
         .getUnformattedText();
   }

   @Override
   public boolean initialize(EntityPlayer player, ResearchTableData data) {
      if (data.categoryTotals.size() < 2) {
         return false;
      } else {
         int lVal = Integer.MAX_VALUE;
         String lKey = "";
         int hVal = 0;
         String hKey = "";

         for (String category : data.categoryTotals.keySet()) {
            int q = data.getTotal(category);
            if (q < lVal) {
               lVal = q;
               lKey = category;
            }

            if (q > hVal) {
               hVal = q;
               hKey = category;
            }
         }

         if (!hKey.equals(lKey) && lVal > 0) {
            this.cat1 = lKey;
            this.cat2 = hKey;
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean activate(EntityPlayer player, ResearchTableData data) {
      if (this.cat1 != null && this.cat2 != null) {
         int lVal = data.getTotal(this.cat1);
         data.addTotal(this.cat1, -lVal);
         data.addTotal(this.cat2, lVal / 2 + MathHelper.getInt(player.getRNG(), 0, lVal / 2));
         return true;
      } else {
         return false;
      }
   }
}
