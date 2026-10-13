package thaumcraft.api;

import java.util.List;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.VanillaInventoryCodeHooks;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.tuple.Pair;
import thaumcraft.common.lib.utils.InventoryUtils;

public class ThaumcraftInvHelper {
   public static IItemHandler getItemHandlerAt(World world, BlockPos pos, EnumFacing side) {
      Pair<IItemHandler, Object> dest = VanillaInventoryCodeHooks.getItemHandler(world, pos.getX(), pos.getY(), pos.getZ(), side);
      if (dest != null && dest.getLeft() != null) {
         return (IItemHandler)dest.getLeft();
      } else {
         TileEntity tileentity = world.getTileEntity(pos);
         return tileentity != null && tileentity instanceof IInventory ? wrapInventory((IInventory)tileentity, side) : null;
      }
   }

   public static IItemHandler wrapInventory(IInventory inventory, EnumFacing side) {
      return (IItemHandler)(inventory instanceof ISidedInventory ? new SidedInvWrapper((ISidedInventory)inventory, side) : new InvWrapper(inventory));
   }

   public static boolean areItemStackTagsEqualRelaxed(ItemStack prime, ItemStack other) {
      if (prime.isEmpty() && other.isEmpty()) {
         return true;
      } else {
         return !prime.isEmpty() && !other.isEmpty()
            ? prime.getTagCompound() == null || compareTagsRelaxed(prime.getTagCompound(), other.getTagCompound())
            : false;
      }
   }

   public static boolean compareTagsRelaxed(NBTTagCompound prime, NBTTagCompound other) {
      for (String key : prime.getKeySet()) {
         if (!other.hasKey(key) || !prime.getTag(key).equals(other.getTag(key))) {
            return false;
         }
      }

      return true;
   }

   public static boolean areItemStacksEqualForCrafting(ItemStack stack0, Object in) {
      if (stack0 == null && in != null) {
         return false;
      } else if (stack0 != null && in == null) {
         return false;
      } else if (stack0 == null && in == null) {
         return true;
      } else if (in instanceof Object[]) {
         return true;
      } else if (in instanceof String) {
         List<ItemStack> l = OreDictionary.getOres((String)in, false);
         return containsMatch(false, new ItemStack[]{stack0}, l);
      } else if (!(in instanceof ItemStack)) {
         return false;
      } else {
         boolean t1 = !stack0.hasTagCompound() || areItemStackTagsEqualForCrafting(stack0, (ItemStack)in);
         return !t1 ? false : OreDictionary.itemMatches((ItemStack)in, stack0, false);
      }
   }

   public static boolean containsMatch(boolean strict, ItemStack[] inputs, List<ItemStack> targets) {
      for (ItemStack input : inputs) {
         for (ItemStack target : targets) {
            if (OreDictionary.itemMatches(target, input, strict) && ItemStack.areItemStackTagsEqual(target, input)) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean areItemsEqual(ItemStack s1, ItemStack s2) {
      return s1.isItemStackDamageable() && s2.isItemStackDamageable()
         ? s1.getItem() == s2.getItem()
         : s1.getItem() == s2.getItem() && s1.getItemDamage() == s2.getItemDamage();
   }

   public static boolean areItemStackTagsEqualForCrafting(ItemStack slotItem, ItemStack recipeItem) {
      if (recipeItem != null && slotItem != null) {
         if (recipeItem.getTagCompound() != null && slotItem.getTagCompound() == null) {
            return false;
         } else if (recipeItem.getTagCompound() == null) {
            return true;
         } else {
            for (String s : recipeItem.getTagCompound().getKeySet()) {
               if (!slotItem.getTagCompound().hasKey(s)) {
                  return false;
               }

               if (!slotItem.getTagCompound().getTag(s).toString().equals(recipeItem.getTagCompound().getTag(s).toString())) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static ItemStack insertStackAt(World world, BlockPos pos, EnumFacing side, ItemStack stack, boolean simulate) {
      IItemHandler inventory = getItemHandlerAt(world, pos, side);
      return inventory != null ? ItemHandlerHelper.insertItemStacked(inventory, stack, simulate) : stack;
   }

   public static ItemStack hasRoomFor(World world, BlockPos pos, EnumFacing side, ItemStack stack) {
      ItemStack testStack = insertStackAt(world, pos, side, stack.copy(), true);
      if (testStack.isEmpty()) {
         return stack.copy();
      } else {
         testStack.setCount(stack.getCount() - testStack.getCount());
         return testStack;
      }
   }

   public static boolean hasRoomForSome(World world, BlockPos pos, EnumFacing side, ItemStack stack) {
      ItemStack testStack = insertStackAt(world, pos, side, stack.copy(), true);
      return stack.getCount() == 0 || testStack.getCount() != stack.getCount();
   }

   public static boolean hasRoomForAll(World world, BlockPos pos, EnumFacing side, ItemStack stack) {
      return insertStackAt(world, pos, side, stack.copy(), true).isEmpty();
   }

   public static int countTotalItemsIn(IItemHandler inventory, ItemStack stack, ThaumcraftInvHelper.InvFilter filter) {
      int count = 0;
      if (inventory != null) {
         for (int a = 0; a < inventory.getSlots(); a++) {
            if (InventoryUtils.areItemStacksEqual(stack, inventory.getStackInSlot(a), filter)) {
               count += inventory.getStackInSlot(a).getCount();
            }
         }
      }

      return count;
   }

   public static int countTotalItemsIn(World world, BlockPos pos, EnumFacing side, ItemStack stack, ThaumcraftInvHelper.InvFilter filter) {
      return countTotalItemsIn(getItemHandlerAt(world, pos, side), stack, filter);
   }

   public static class InvFilter {
      public boolean igDmg;
      public boolean igNBT;
      public boolean useOre;
      public boolean useMod;
      public boolean relaxedNBT = false;
      public static final ThaumcraftInvHelper.InvFilter STRICT = new ThaumcraftInvHelper.InvFilter(false, false, false, false);
      public static final ThaumcraftInvHelper.InvFilter BASEORE = new ThaumcraftInvHelper.InvFilter(false, false, true, false);

      public InvFilter(boolean ignoreDamage, boolean ignoreNBT, boolean useOre, boolean useMod) {
         this.igDmg = ignoreDamage;
         this.igNBT = ignoreNBT;
         this.useOre = useOre;
         this.useMod = useMod;
      }

      public ThaumcraftInvHelper.InvFilter setRelaxedNBT() {
         this.relaxedNBT = true;
         return this;
      }
   }
}
