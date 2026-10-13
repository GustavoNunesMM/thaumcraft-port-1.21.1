package thaumcraft.client;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.items.ItemGenericEssentiaContainer;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.blocks.devices.BlockInlay;
import thaumcraft.common.blocks.devices.BlockStabilizer;
import thaumcraft.common.blocks.essentia.BlockJarItem;
import thaumcraft.common.blocks.essentia.BlockTube;
import thaumcraft.common.blocks.world.ore.BlockCrystal;
import thaumcraft.common.golems.GolemProperties;
import thaumcraft.common.items.casters.ItemCaster;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.tiles.devices.TileStabilizer;
import thaumcraft.common.tiles.essentia.TileTubeFilter;

@SideOnly(Side.CLIENT)
public class ColorHandler {
   public static void registerColourHandlers() {
      BlockColors blockColors = Minecraft.getMinecraft().getBlockColors();
      ItemColors itemColors = Minecraft.getMinecraft().getItemColors();
      registerBlockColourHandlers(blockColors);
      registerItemColourHandlers(blockColors, itemColors);
   }

   private static void registerBlockColourHandlers(BlockColors blockColors) {
      IBlockColor basicColourHandler = (state, blockAccess, pos, tintIndex) -> state.getBlock().getMapColor(state, blockAccess, pos).colorValue;
      Block[] basicBlocks = new Block[BlocksTC.candles.size() + BlocksTC.banners.size() + BlocksTC.nitor.size()];
      int i = 0;

      for (Block b : BlocksTC.candles.values()) {
         basicBlocks[i] = b;
         i++;
      }

      for (Block b : BlocksTC.banners.values()) {
         basicBlocks[i] = b;
         i++;
      }

      for (Block b : BlocksTC.nitor.values()) {
         basicBlocks[i] = b;
         i++;
      }

      blockColors.registerBlockColorHandler(basicColourHandler, basicBlocks);
      IBlockColor grassColourHandler = (state, blockAccess, pos, tintIndex) -> blockAccess != null && pos != null
         ? BiomeColorHelper.getGrassColorAtPos(blockAccess, pos)
         : ColorizerGrass.getGrassColor(0.5, 1.0);
      blockColors.registerBlockColorHandler(grassColourHandler, new Block[]{BlocksTC.grassAmbient});
      IBlockColor leafColourHandler = (state, blockAccess, pos, tintIndex) -> {
         if (state.getBlock() == BlocksTC.leafSilverwood) {
            return 16777215;
         } else {
            return blockAccess != null && pos != null ? BiomeColorHelper.getFoliageColorAtPos(blockAccess, pos) : ColorizerFoliage.getFoliageColorBasic();
         }
      };
      blockColors.registerBlockColorHandler(leafColourHandler, new Block[]{BlocksTC.leafGreatwood, BlocksTC.leafSilverwood});
      IBlockColor crystalColourHandler = (state, blockAccess, pos, tintIndex) -> state.getBlock() instanceof BlockCrystal
         ? ((BlockCrystal)state.getBlock()).aspect.getColor()
         : 16777215;
      blockColors.registerBlockColorHandler(
         crystalColourHandler,
         new Block[]{
            BlocksTC.crystalAir,
            BlocksTC.crystalEarth,
            BlocksTC.crystalFire,
            BlocksTC.crystalWater,
            BlocksTC.crystalEntropy,
            BlocksTC.crystalOrder,
            BlocksTC.crystalTaint
         }
      );
      IBlockColor tubeFilterColourHandler = (state, blockAccess, pos, tintIndex) -> {
         if (state.getBlock() instanceof BlockTube && tintIndex == 1) {
            TileEntity te = blockAccess.getTileEntity(pos);
            if (te != null && te instanceof TileTubeFilter && ((TileTubeFilter)te).aspectFilter != null) {
               return ((TileTubeFilter)te).aspectFilter.getColor();
            }
         }

         return 16777215;
      };
      blockColors.registerBlockColorHandler(tubeFilterColourHandler, new Block[]{BlocksTC.tubeFilter});
      IBlockColor inlayColourHandler = (state, blockAccess, pos, tintIndex) -> {
         if (state.getBlock() instanceof BlockInlay && tintIndex == 0) {
            BlockInlay var10000 = (BlockInlay)state.getBlock();
            return BlockInlay.colorMultiplier(state.getBlock().getMetaFromState(state));
         } else {
            return 16777215;
         }
      };
      blockColors.registerBlockColorHandler(inlayColourHandler, new Block[]{BlocksTC.inlay});
      IBlockColor stabilizerColourHandler = (state, blockAccess, pos, tintIndex) -> {
         if (state.getBlock() instanceof BlockStabilizer && tintIndex == 0) {
            int charge = 0;
            TileEntity te = blockAccess.getTileEntity(pos);
            if (te != null && te instanceof TileStabilizer) {
               charge = ((TileStabilizer)te).getEnergy();
            }

            BlockStabilizer var10000 = (BlockStabilizer)state.getBlock();
            return BlockStabilizer.colorMultiplier(charge);
         } else {
            return 16777215;
         }
      };
      blockColors.registerBlockColorHandler(stabilizerColourHandler, new Block[]{BlocksTC.stabilizer});
   }

   private static void registerItemColourHandlers(BlockColors blockColors, ItemColors itemColors) {
      IItemColor itemBlockColourHandler = (stack, tintIndex) -> {
         IBlockState state = ((ItemBlock)stack.getItem()).getBlock().getStateFromMeta(stack.getMetadata());
         return blockColors.colorMultiplier(state, null, null, tintIndex);
      };
      Block[] basicBlocks = new Block[BlocksTC.candles.size() + BlocksTC.nitor.size() + 3];
      int i = 0;

      for (Block b : BlocksTC.candles.values()) {
         basicBlocks[i] = b;
         i++;
      }

      for (Block b : BlocksTC.nitor.values()) {
         basicBlocks[i] = b;
         i++;
      }

      basicBlocks[i] = BlocksTC.leafGreatwood;
      basicBlocks[++i] = BlocksTC.leafSilverwood;
      basicBlocks[++i] = BlocksTC.grassAmbient;
      i++;
      itemColors.registerItemColorHandler(itemBlockColourHandler, basicBlocks);
      IItemColor itemEssentiaColourHandler = (stack, tintIndex) -> {
         ItemGenericEssentiaContainer item = (ItemGenericEssentiaContainer)stack.getItem();

         try {
            if (item != null && item.getAspects(stack) != null) {
               return item.getAspects(stack).getAspects()[0].getColor();
            }
         } catch (Exception var4x) {
         }

         return 16777215;
      };
      itemColors.registerItemColorHandler(itemEssentiaColourHandler, new Item[]{ItemsTC.crystalEssence});
      IItemColor itemJarColourHandler = (stack, tintIndex) -> {
         BlockJarItem item = (BlockJarItem)stack.getItem();

         try {
            if (item.getAspects(stack) != null && tintIndex == 1) {
               return item.getAspects(stack).getAspects()[0].getColor();
            }
         } catch (Exception var4x) {
         }

         return 16777215;
      };
      itemColors.registerItemColorHandler(itemJarColourHandler, new Block[]{BlocksTC.jarNormal});
      itemColors.registerItemColorHandler(itemJarColourHandler, new Block[]{BlocksTC.jarVoid});
      IItemColor itemCrystalPlanterColourHandler = (stack, tintIndex) -> {
         Item item = stack.getItem();
         return item instanceof ItemBlock && ((ItemBlock)item).getBlock() instanceof BlockCrystal
            ? ((BlockCrystal)((ItemBlock)item).getBlock()).aspect.getColor()
            : 16777215;
      };
      itemColors.registerItemColorHandler(itemCrystalPlanterColourHandler, new Block[]{BlocksTC.crystalAir});
      itemColors.registerItemColorHandler(itemCrystalPlanterColourHandler, new Block[]{BlocksTC.crystalEarth});
      itemColors.registerItemColorHandler(itemCrystalPlanterColourHandler, new Block[]{BlocksTC.crystalFire});
      itemColors.registerItemColorHandler(itemCrystalPlanterColourHandler, new Block[]{BlocksTC.crystalWater});
      itemColors.registerItemColorHandler(itemCrystalPlanterColourHandler, new Block[]{BlocksTC.crystalEntropy});
      itemColors.registerItemColorHandler(itemCrystalPlanterColourHandler, new Block[]{BlocksTC.crystalOrder});
      itemColors.registerItemColorHandler(itemCrystalPlanterColourHandler, new Block[]{BlocksTC.crystalTaint});
      IItemColor itemEssentiaAltColourHandler = (stack, tintIndex) -> {
         ItemGenericEssentiaContainer item = (ItemGenericEssentiaContainer)stack.getItem();
         return stack.getItemDamage() == 1 && item.getAspects(stack) != null && tintIndex == 1 ? item.getAspects(stack).getAspects()[0].getColor() : 16777215;
      };
      itemColors.registerItemColorHandler(itemEssentiaAltColourHandler, new Item[]{ItemsTC.phial, ItemsTC.label});
      IItemColor itemArmorColourHandler = (stack, tintIndex) -> {
         ItemArmor item = (ItemArmor)stack.getItem();
         return tintIndex > 0 ? -1 : item.getColor(stack);
      };
      itemColors.registerItemColorHandler(
         itemArmorColourHandler,
         new Item[]{ItemsTC.voidRobeChest, ItemsTC.voidRobeHelm, ItemsTC.voidRobeLegs, ItemsTC.clothChest, ItemsTC.clothLegs, ItemsTC.clothBoots}
      );
      IItemColor itemCasterColourHandler = (stack, tintIndex) -> {
         ItemCaster item = (ItemCaster)stack.getItem();
         ItemFocus focus = item.getFocus(stack);
         return tintIndex > 0 && focus != null ? focus.getFocusColor(item.getFocusStack(stack)) : -1;
      };
      itemColors.registerItemColorHandler(itemCasterColourHandler, new Item[]{ItemsTC.casterBasic});
      IItemColor itemFocusColourHandler = (stack, tintIndex) -> {
         ItemFocus item = (ItemFocus)stack.getItem();
         return item.getFocusColor(stack);
      };
      itemColors.registerItemColorHandler(itemFocusColourHandler, new Item[]{ItemsTC.focus1});
      itemColors.registerItemColorHandler(itemFocusColourHandler, new Item[]{ItemsTC.focus2});
      itemColors.registerItemColorHandler(itemFocusColourHandler, new Item[]{ItemsTC.focus3});
      IItemColor itemGolemColourHandler = (stack, tintIndex) -> {
         if (stack.hasTagCompound() && stack.getTagCompound().hasKey("props")) {
            IGolemProperties props = GolemProperties.fromLong(stack.getTagCompound().getLong("props"));
            return props.getMaterial().itemColor;
         } else {
            return 16777215;
         }
      };
      itemColors.registerItemColorHandler(itemGolemColourHandler, new Item[]{ItemsTC.golemPlacer});
      IItemColor itemBannerColourHandler = (stack, tintIndex) -> {
         if (tintIndex == 1) {
            IBlockState state = ((ItemBlock)stack.getItem()).getBlock().getStateFromMeta(stack.getMetadata());
            return blockColors.colorMultiplier(state, null, null, tintIndex);
         } else if (tintIndex == 2) {
            if (stack.hasTagCompound() && stack.getTagCompound().hasKey("aspect") && stack.getTagCompound().getString("aspect") != null) {
               return Aspect.getAspect(stack.getTagCompound().getString("aspect")).getColor();
            } else {
               IBlockState state = ((ItemBlock)stack.getItem()).getBlock().getStateFromMeta(stack.getMetadata());
               return blockColors.colorMultiplier(state, null, null, tintIndex);
            }
         } else {
            return 16777215;
         }
      };
      Block[] bannerBlocks = new Block[BlocksTC.banners.size()];
      i = 0;

      for (Block b : BlocksTC.banners.values()) {
         bannerBlocks[i] = b;
         i++;
      }

      itemColors.registerItemColorHandler(itemBannerColourHandler, bannerBlocks);
   }
}
