package thaumcraft.common.lib.research.theorycraft;

import java.util.Random;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;

public class CardSynthesis extends TheorycraftCard {
   Aspect aspect1;
   Aspect aspect2;
   Aspect aspect3;

   @Override
   public NBTTagCompound serialize() {
      NBTTagCompound nbt = super.serialize();
      nbt.setString("aspect1", this.aspect1.getTag());
      nbt.setString("aspect2", this.aspect2.getTag());
      nbt.setString("aspect3", this.aspect3.getTag());
      return nbt;
   }

   @Override
   public void deserialize(NBTTagCompound nbt) {
      super.deserialize(nbt);
      this.aspect1 = Aspect.getAspect(nbt.getString("aspect1"));
      this.aspect2 = Aspect.getAspect(nbt.getString("aspect2"));
      this.aspect3 = Aspect.getAspect(nbt.getString("aspect3"));
   }

   @Override
   public boolean initialize(EntityPlayer player, ResearchTableData data) {
      Random r = new Random(this.getSeed());
      int num = MathHelper.getInt(r, 0, Aspect.getCompoundAspects().size() - 1);
      this.aspect3 = Aspect.getCompoundAspects().get(num);
      this.aspect1 = this.aspect3.getComponents()[0];
      this.aspect2 = this.aspect3.getComponents()[1];
      return true;
   }

   @Override
   public int getInspirationCost() {
      return 1;
   }

   @Override
   public String getResearchCategory() {
      return "ALCHEMY";
   }

   @Override
   public String getLocalizedName() {
      return new TextComponentTranslation("card.synthesis.name", new Object[0]).getUnformattedText();
   }

   @Override
   public String getLocalizedText() {
      return new TextComponentTranslation(
            "card.synthesis.text",
            new Object[]{
               TextFormatting.BOLD + this.aspect1.getName() + TextFormatting.RESET, TextFormatting.BOLD + this.aspect2.getName() + TextFormatting.RESET
            }
         )
         .getUnformattedText();
   }

   @Override
   public ItemStack[] getRequiredItems() {
      return new ItemStack[]{ThaumcraftApiHelper.makeCrystal(this.aspect1), ThaumcraftApiHelper.makeCrystal(this.aspect2)};
   }

   @Override
   public boolean[] getRequiredItemsConsumed() {
      return new boolean[]{true, true};
   }

   @Override
   public boolean activate(EntityPlayer player, ResearchTableData data) {
      ItemStack res = ThaumcraftApiHelper.makeCrystal(this.aspect3);
      data.addTotal(this.getResearchCategory(), 40);
      if (player.getRNG().nextFloat() < 0.33) {
         data.addInspiration(1);
      }

      if (!player.inventory.addItemStackToInventory(res)) {
         player.dropItem(res, true);
      }

      return true;
   }
}
