package thaumcraft.common.items.consumables;

import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;

public class ItemChunksEdible extends ItemFood implements IThaumcraftItems {
   public final int itemUseDuration;
   private String[] variants = new String[]{"beef", "chicken", "pork", "fish", "rabbit", "mutton"};

   public ItemChunksEdible() {
      super(1, 0.3F, false);
      this.itemUseDuration = 10;
      this.setMaxStackSize(64);
      this.setHasSubtypes(true);
      this.setMaxDamage(0);
      this.setRegistryName("chunk");
      this.setTranslationKey("chunk");
      this.setCreativeTab(ConfigItems.TABTC);
      ConfigItems.ITEM_VARIANT_HOLDERS.add(this);
   }

   public int getMaxItemUseDuration(ItemStack stack1) {
      return this.itemUseDuration;
   }

   public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
      if (tab == ConfigItems.TABTC || tab == CreativeTabs.SEARCH) {
         for (int a = 0; a < this.variants.length; a++) {
            items.add(new ItemStack(this, 1, a));
         }
      }
   }

   public String getTranslationKey(ItemStack itemStack) {
      return this.hasSubtypes && itemStack.getMetadata() < this.variants.length && this.variants[itemStack.getMetadata()] != "chunk"
         ? String.format(super.getTranslationKey() + ".%s", this.variants[itemStack.getMetadata()])
         : super.getTranslationKey(itemStack);
   }

   @Override
   public String[] getVariantNames() {
      return this.variants;
   }

   @Override
   public int[] getVariantMeta() {
      return new int[]{0, 1, 2, 3, 4, 5};
   }

   @Override
   public Item getItem() {
      return this;
   }

   @Override
   public ItemMeshDefinition getCustomMesh() {
      return null;
   }

   @Override
   public ModelResourceLocation getCustomModelResourceLocation(String variant) {
      return variant.equals("chunk") ? new ModelResourceLocation("thaumcraft:chunk") : new ModelResourceLocation("thaumcraft:chunk", variant);
   }
}
