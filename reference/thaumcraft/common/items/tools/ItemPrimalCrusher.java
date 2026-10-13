package thaumcraft.common.items.tools;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.IThaumcraftItems;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;

public class ItemPrimalCrusher extends ItemTool implements IWarpingGear, IThaumcraftItems {
   public static ToolMaterial material = EnumHelper.addToolMaterial("PRIMALVOID", 5, 500, 8.0F, 4.0F, 20).setRepairItem(new ItemStack(ItemsTC.ingots, 1, 1));
   private static final Set isEffective = Sets.newHashSet(
      new Block[]{
         Blocks.COBBLESTONE,
         Blocks.DOUBLE_STONE_SLAB,
         Blocks.STONE_SLAB,
         Blocks.STONE,
         Blocks.SANDSTONE,
         Blocks.MOSSY_COBBLESTONE,
         Blocks.IRON_ORE,
         Blocks.IRON_BLOCK,
         Blocks.COAL_ORE,
         Blocks.GOLD_BLOCK,
         Blocks.GOLD_ORE,
         Blocks.DIAMOND_ORE,
         Blocks.DIAMOND_BLOCK,
         Blocks.ICE,
         Blocks.NETHERRACK,
         Blocks.LAPIS_ORE,
         Blocks.LAPIS_BLOCK,
         Blocks.REDSTONE_ORE,
         Blocks.LIT_REDSTONE_ORE,
         Blocks.RAIL,
         Blocks.DETECTOR_RAIL,
         Blocks.GOLDEN_RAIL,
         Blocks.ACTIVATOR_RAIL,
         Blocks.GRASS,
         Blocks.DIRT,
         Blocks.SAND,
         Blocks.GRAVEL,
         Blocks.SNOW_LAYER,
         Blocks.SNOW,
         Blocks.CLAY,
         Blocks.FARMLAND,
         Blocks.SOUL_SAND,
         Blocks.MYCELIUM,
         BlocksTC.taintCrust,
         BlocksTC.taintRock,
         BlocksTC.taintSoil,
         BlocksTC.taintFeature,
         BlocksTC.taintLog,
         BlocksTC.taintFibre,
         Blocks.OBSIDIAN
      }
   );

   public ItemPrimalCrusher() {
      super(3.5F, -2.8F, material, isEffective);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setRegistryName("primal_crusher");
      this.setTranslationKey("primal_crusher");
      ConfigItems.ITEM_VARIANT_HOLDERS.add(this);
   }

   @Override
   public Item getItem() {
      return this;
   }

   @Override
   public String[] getVariantNames() {
      return new String[]{"normal"};
   }

   @Override
   public int[] getVariantMeta() {
      return new int[]{0};
   }

   @Override
   public ItemMeshDefinition getCustomMesh() {
      return null;
   }

   @Override
   public ModelResourceLocation getCustomModelResourceLocation(String variant) {
      return new ModelResourceLocation("thaumcraft:" + variant);
   }

   public boolean canHarvestBlock(IBlockState blockIn) {
      return blockIn.getMaterial() != Material.WOOD
         && blockIn.getMaterial() != Material.LEAVES
         && blockIn.getMaterial() != Material.PLANTS;
   }

   public float getDestroySpeed(ItemStack stack, IBlockState state) {
      return state.getMaterial() != Material.IRON
            && state.getMaterial() != Material.ANVIL
            && state.getMaterial() != Material.ROCK
         ? super.getDestroySpeed(stack, state)
         : this.efficiency;
   }

   public Set<String> getToolClasses(ItemStack stack) {
      return ImmutableSet.of("shovel", "pickaxe");
   }

   private boolean isEffectiveAgainst(Block block) {
      for (Object b : isEffective) {
         if (b == block) {
            return true;
         }
      }

      return false;
   }

   public int getItemEnchantability() {
      return 20;
   }

   @Override
   public int getWarp(ItemStack itemstack, EntityPlayer player) {
      return 2;
   }

   public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
      super.onUpdate(stack, world, entity, itemSlot, isSelected);
      if (stack.isItemDamaged() && entity != null && entity.ticksExisted % 20 == 0 && entity instanceof EntityLivingBase) {
         stack.damageItem(-1, (EntityLivingBase)entity);
      }
   }

   public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
      if (tab == ConfigItems.TABTC || tab == CreativeTabs.SEARCH) {
         ItemStack w1 = new ItemStack(this);
         EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.DESTRUCTIVE, 1);
         EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.REFINING, 1);
         items.add(w1);
      }
   }
}
