package thaumcraft.common.items;

import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import thaumcraft.common.config.ConfigItems;

public class ItemTCBase extends Item implements IThaumcraftItems {
   protected final String BASE_NAME;
   protected String[] VARIANTS;
   protected int[] VARIANTS_META;

   public ItemTCBase(String name, String... variants) {
      this.setRegistryName(name);
      this.setTranslationKey(name);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setNoRepair();
      this.setHasSubtypes(variants.length > 1);
      this.BASE_NAME = name;
      if (variants.length == 0) {
         this.VARIANTS = new String[]{name};
      } else {
         this.VARIANTS = variants;
      }

      this.VARIANTS_META = new int[this.VARIANTS.length];
      int m = 0;

      while (m < this.VARIANTS.length) {
         this.VARIANTS_META[m] = m++;
      }

      ConfigItems.ITEM_VARIANT_HOLDERS.add(this);
   }

   public String getTranslationKey(ItemStack itemStack) {
      return this.hasSubtypes && itemStack.getMetadata() < this.VARIANTS.length && this.VARIANTS[itemStack.getMetadata()] != this.BASE_NAME
         ? String.format(super.getTranslationKey() + ".%s", this.VARIANTS[itemStack.getMetadata()])
         : super.getTranslationKey(itemStack);
   }

   public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
      if (tab == ConfigItems.TABTC || tab == CreativeTabs.SEARCH) {
         if (!this.getHasSubtypes()) {
            super.getSubItems(tab, items);
         } else {
            for (int meta = 0; meta < this.VARIANTS.length; meta++) {
               items.add(new ItemStack(this, 1, meta));
            }
         }
      }
   }

   @Override
   public Item getItem() {
      return this;
   }

   @Override
   public String[] getVariantNames() {
      return this.VARIANTS;
   }

   @Override
   public int[] getVariantMeta() {
      return this.VARIANTS_META;
   }

   @Override
   public ItemMeshDefinition getCustomMesh() {
      return null;
   }

   @Override
   public ModelResourceLocation getCustomModelResourceLocation(String variant) {
      return variant.equals(this.BASE_NAME)
         ? new ModelResourceLocation("thaumcraft:" + this.BASE_NAME)
         : new ModelResourceLocation("thaumcraft:" + this.BASE_NAME, variant);
   }
}
