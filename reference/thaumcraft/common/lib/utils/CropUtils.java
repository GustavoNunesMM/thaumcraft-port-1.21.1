package thaumcraft.common.lib.utils;

import java.util.ArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockStem;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CropUtils {
   public static ArrayList<String> clickableCrops = new ArrayList<>();
   public static ArrayList<String> standardCrops = new ArrayList<>();
   public static ArrayList<String> stackedCrops = new ArrayList<>();
   public static ArrayList<String> lampBlacklist = new ArrayList<>();

   public static void addStandardCrop(ItemStack stack, int grownMeta) {
      Block block = Block.getBlockFromItem(stack.getItem());
      if (block != null) {
         addStandardCrop(block, grownMeta);
      }
   }

   public static void addStandardCrop(Block block, int grownMeta) {
      if (grownMeta == 32767) {
         for (int a = 0; a < 16; a++) {
            standardCrops.add(block.getTranslationKey() + a);
         }
      } else {
         standardCrops.add(block.getTranslationKey() + grownMeta);
      }

      if (block instanceof BlockCrops && grownMeta != 7) {
         standardCrops.add(block.getTranslationKey() + "7");
      }
   }

   public static void addClickableCrop(ItemStack stack, int grownMeta) {
      if (Block.getBlockFromItem(stack.getItem()) != null) {
         if (grownMeta == 32767) {
            for (int a = 0; a < 16; a++) {
               clickableCrops.add(Block.getBlockFromItem(stack.getItem()).getTranslationKey() + a);
            }
         } else {
            clickableCrops.add(Block.getBlockFromItem(stack.getItem()).getTranslationKey() + grownMeta);
         }

         if (Block.getBlockFromItem(stack.getItem()) instanceof BlockCrops && grownMeta != 7) {
            clickableCrops.add(Block.getBlockFromItem(stack.getItem()).getTranslationKey() + "7");
         }
      }
   }

   public static void addStackedCrop(ItemStack stack, int grownMeta) {
      if (Block.getBlockFromItem(stack.getItem()) != null) {
         addStackedCrop(Block.getBlockFromItem(stack.getItem()), grownMeta);
      }
   }

   public static void addStackedCrop(Block block, int grownMeta) {
      if (grownMeta == 32767) {
         for (int a = 0; a < 16; a++) {
            stackedCrops.add(block.getTranslationKey() + a);
         }
      } else {
         stackedCrops.add(block.getTranslationKey() + grownMeta);
      }

      if (block instanceof BlockCrops && grownMeta != 7) {
         stackedCrops.add(block.getTranslationKey() + "7");
      }
   }

   public static boolean isGrownCrop(World world, BlockPos pos) {
      if (world.isAirBlock(pos)) {
         return false;
      } else {
         boolean found = false;
         IBlockState bs = world.getBlockState(pos);
         Block bi = bs.getBlock();
         int md = bi.getMetaFromState(bs);
         if (standardCrops.contains(bi.getTranslationKey() + md)
            || clickableCrops.contains(bi.getTranslationKey() + md)
            || stackedCrops.contains(bi.getTranslationKey() + md)) {
            found = true;
         }

         Block biB = world.getBlockState(pos.down()).getBlock();
         return bi instanceof IGrowable
               && !((IGrowable)bi).canGrow(world, pos, world.getBlockState(pos), world.isRemote)
               && !(bi instanceof BlockStem)
            || bi instanceof BlockCrops && md == 7 && !found
            || standardCrops.contains(bi.getTranslationKey() + md)
            || clickableCrops.contains(bi.getTranslationKey() + md)
            || stackedCrops.contains(bi.getTranslationKey() + md) && biB == bi;
      }
   }

   public static void blacklistLamp(ItemStack stack, int meta) {
      if (Block.getBlockFromItem(stack.getItem()) != null) {
         if (meta == 32767) {
            for (int a = 0; a < 16; a++) {
               lampBlacklist.add(Block.getBlockFromItem(stack.getItem()).getTranslationKey() + a);
            }
         } else {
            lampBlacklist.add(Block.getBlockFromItem(stack.getItem()).getTranslationKey() + meta);
         }
      }
   }

   public static boolean doesLampGrow(World world, BlockPos pos) {
      Block bi = world.getBlockState(pos).getBlock();
      int md = bi.getMetaFromState(world.getBlockState(pos));
      return !lampBlacklist.contains(bi.getTranslationKey() + md);
   }
}
