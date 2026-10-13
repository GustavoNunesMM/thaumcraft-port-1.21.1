package thaumcraft.common.lib.research.theorycraft;

import java.util.Random;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.common.items.consumables.ItemPhial;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;

public class CardInfuse extends TheorycraftCard {
   Aspect aspect;
   ItemStack stack = ItemStack.EMPTY;
   static ItemStack[] options = new ItemStack[]{
      new ItemStack(ItemsTC.alumentum),
      new ItemStack(BlocksTC.nitor.get(EnumDyeColor.YELLOW)),
      new ItemStack(ItemsTC.amber),
      new ItemStack(ItemsTC.brain),
      new ItemStack(ItemsTC.fabric),
      new ItemStack(ItemsTC.salisMundus),
      new ItemStack(ItemsTC.ingots, 1, 0),
      new ItemStack(ItemsTC.ingots, 1, 2),
      new ItemStack(ItemsTC.quicksilver),
      new ItemStack(Items.GOLD_INGOT),
      new ItemStack(Items.IRON_INGOT),
      new ItemStack(Items.DIAMOND),
      new ItemStack(Items.EMERALD),
      new ItemStack(Items.BLAZE_ROD),
      new ItemStack(Items.LEATHER),
      new ItemStack(Blocks.WOOL),
      new ItemStack(Items.BRICK),
      new ItemStack(Items.ARROW),
      new ItemStack(Items.EGG),
      new ItemStack(Items.FEATHER),
      new ItemStack(Items.GLOWSTONE_DUST),
      new ItemStack(Items.REDSTONE),
      new ItemStack(Items.GHAST_TEAR),
      new ItemStack(Items.GUNPOWDER),
      new ItemStack(Items.BOW),
      new ItemStack(Items.GOLDEN_SWORD),
      new ItemStack(Items.IRON_SWORD),
      new ItemStack(Items.IRON_PICKAXE),
      new ItemStack(Items.GOLDEN_PICKAXE),
      new ItemStack(Items.QUARTZ),
      new ItemStack(Items.APPLE)
   };

   @Override
   public NBTTagCompound serialize() {
      NBTTagCompound nbt = super.serialize();
      nbt.setString("aspect", this.aspect.getTag());
      nbt.setTag("stack", this.stack.serializeNBT());
      return nbt;
   }

   @Override
   public void deserialize(NBTTagCompound nbt) {
      super.deserialize(nbt);
      this.aspect = Aspect.getAspect(nbt.getString("aspect"));
      this.stack = new ItemStack(nbt.getCompoundTag("stack"));
   }

   @Override
   public boolean initialize(EntityPlayer player, ResearchTableData data) {
      Random r = new Random(this.getSeed());
      int num = r.nextInt(Aspect.getCompoundAspects().size());
      this.aspect = Aspect.getCompoundAspects().get(num);
      this.stack = options[r.nextInt(options.length)].copy();
      return this.aspect != null && this.stack != null;
   }

   @Override
   public int getInspirationCost() {
      return 1;
   }

   @Override
   public String getResearchCategory() {
      return "INFUSION";
   }

   @Override
   public String getLocalizedName() {
      return new TextComponentTranslation("card.infuse.name", new Object[0]).getFormattedText();
   }

   @Override
   public String getLocalizedText() {
      return new TextComponentTranslation(
            "card.infuse.text", new Object[]{TextFormatting.BOLD + this.aspect.getName() + TextFormatting.RESET, this.stack.getDisplayName(), this.getVal()}
         )
         .getFormattedText();
   }

   private int getVal() {
      int q = 10;

      try {
         q = (int)(q + Math.sqrt(ThaumcraftCraftingManager.getObjectTags(this.stack).visSize()) * 1.5);
      } catch (Exception var3) {
      }

      return q;
   }

   @Override
   public ItemStack[] getRequiredItems() {
      return new ItemStack[]{this.stack, ItemPhial.makeFilledPhial(this.aspect)};
   }

   @Override
   public boolean[] getRequiredItemsConsumed() {
      return new boolean[]{true, true};
   }

   @Override
   public boolean activate(EntityPlayer player, ResearchTableData data) {
      data.addTotal(this.getResearchCategory(), this.getVal());
      return true;
   }
}
