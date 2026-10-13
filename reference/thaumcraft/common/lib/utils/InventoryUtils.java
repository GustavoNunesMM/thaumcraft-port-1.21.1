package thaumcraft.common.lib.utils;

import java.util.List;
import java.util.Map;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.IngredientNBT;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.ThaumcraftInvHelper;
import thaumcraft.api.crafting.IngredientNBTTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.entities.EntityFollowingItem;

public class InventoryUtils {
   public static ItemStack copyMaxedStack(ItemStack stack) {
      return copyLimitedStack(stack, stack.getMaxStackSize());
   }

   public static ItemStack copyLimitedStack(ItemStack stack, int limit) {
      if (stack == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack s = stack.copy();
         if (s.getCount() > limit) {
            s.setCount(limit);
         }

         return s;
      }
   }

   public static boolean consumeItemsFromAdjacentInventoryOrPlayer(World world, BlockPos pos, EntityPlayer player, boolean sim, ItemStack... items) {
      for (ItemStack stack : items) {
         boolean b = checkAdjacentChests(world, pos, stack);
         if (!b) {
            b = isPlayerCarryingAmount(player, stack, true);
         }

         if (!b) {
            return false;
         }
      }

      if (!sim) {
         for (ItemStack stack : items) {
            if (!consumeFromAdjacentChests(world, pos, stack.copy())) {
               consumePlayerItem(player, stack, true, true);
            }
         }
      }

      return true;
   }

   public static boolean checkAdjacentChests(World world, BlockPos pos, ItemStack itemStack) {
      int c = itemStack.getCount();

      for (EnumFacing face : EnumFacing.VALUES) {
         if (face != EnumFacing.UP) {
            c -= ThaumcraftInvHelper.countTotalItemsIn(
               world, pos.offset(face), face.getOpposite(), itemStack.copy(), ThaumcraftInvHelper.InvFilter.BASEORE
            );
            if (c <= 0) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean consumeFromAdjacentChests(World world, BlockPos pos, ItemStack itemStack) {
      for (EnumFacing face : EnumFacing.VALUES) {
         if (face != EnumFacing.UP && !itemStack.isEmpty()) {
            ItemStack os = removeStackFrom(world, pos.offset(face), face.getOpposite(), itemStack, ThaumcraftInvHelper.InvFilter.BASEORE, false);
            itemStack.setCount(itemStack.getCount() - os.getCount());
            if (itemStack.isEmpty()) {
               break;
            }
         }
      }

      return itemStack.isEmpty();
   }

   /** @deprecated */
   public static ItemStack insertStackAt(World world, BlockPos pos, EnumFacing side, ItemStack stack, boolean simulate) {
      return ThaumcraftInvHelper.insertStackAt(world, pos, side, stack, simulate);
   }

   public static void ejectStackAt(World world, BlockPos pos, EnumFacing side, ItemStack out) {
      ejectStackAt(world, pos, side, out, false);
   }

   public static ItemStack ejectStackAt(World world, BlockPos pos, EnumFacing side, ItemStack out, boolean smart) {
      out = ThaumcraftInvHelper.insertStackAt(world, pos.offset(side), side.getOpposite(), out, false);
      if (smart && ThaumcraftInvHelper.getItemHandlerAt(world, pos.offset(side), side.getOpposite()) != null) {
         return out;
      } else {
         if (!out.isEmpty()) {
            if (world.isBlockFullCube(pos.offset(side))) {
               pos = pos.offset(side.getOpposite());
            }

            EntityItem entityitem2 = new EntityItem(
               world,
               pos.getX() + 0.5 + 1 * side.getXOffset(),
               (float)pos.getY() + 1 * side.getYOffset(),
               pos.getZ() + 0.5 + 1 * side.getZOffset(),
               out
            );
            entityitem2.motionX = 0.3 * side.getXOffset();
            entityitem2.motionY = 0.3 * side.getYOffset();
            entityitem2.motionZ = 0.3 * side.getZOffset();
            world.spawnEntity(entityitem2);
         }

         return ItemStack.EMPTY;
      }
   }

   public static ItemStack removeStackFrom(World world, BlockPos pos, EnumFacing side, ItemStack stack, ThaumcraftInvHelper.InvFilter filter, boolean simulate) {
      return removeStackFrom(ThaumcraftInvHelper.getItemHandlerAt(world, pos, side), stack, filter, simulate);
   }

   public static ItemStack removeStackFrom(IItemHandler inventory, ItemStack stack, ThaumcraftInvHelper.InvFilter filter, boolean simulate) {
      int amount = stack.getCount();
      int removed = 0;
      if (inventory != null) {
         for (int a = 0; a < inventory.getSlots(); a++) {
            if (areItemStacksEqual(stack, inventory.getStackInSlot(a), filter)) {
               int s = Math.min(amount - removed, inventory.getStackInSlot(a).getCount());
               ItemStack es = inventory.extractItem(a, s, simulate);
               if (es != null && !es.isEmpty()) {
                  removed += es.getCount();
               }
            }

            if (removed >= amount) {
               break;
            }
         }
      }

      if (removed == 0) {
         return ItemStack.EMPTY;
      } else {
         ItemStack s = stack.copy();
         s.setCount(removed);
         return s;
      }
   }

   public static int countStackInWorld(World world, BlockPos pos, ItemStack stack, double range, ThaumcraftInvHelper.InvFilter filter) {
      int count = 0;

      for (EntityItem ei : EntityUtils.getEntitiesInRange(world, pos, null, EntityItem.class, range)) {
         if (ei.getItem() != null && ei.getItem().isEmpty() && areItemStacksEqual(stack, ei.getItem(), filter)) {
            count += ei.getItem().getCount();
         }
      }

      return count;
   }

   public static void dropItems(World world, BlockPos pos) {
      TileEntity tileEntity = world.getTileEntity(pos);
      if (tileEntity instanceof IInventory) {
         IInventory inventory = (IInventory)tileEntity;
         InventoryHelper.dropInventoryItems(world, pos, inventory);
      }
   }

   public static boolean consumePlayerItem(EntityPlayer player, ItemStack item, boolean nocheck, boolean ore) {
      if (!nocheck && !isPlayerCarryingAmount(player, item, ore)) {
         return false;
      } else {
         int count = item.getCount();

         for (int var2x = 0; var2x < player.inventory.mainInventory.size(); var2x++) {
            if (checkEnchantedPlaceholder(item, (ItemStack)player.inventory.mainInventory.get(var2x))
               || areItemStacksEqual(
                  (ItemStack)player.inventory.mainInventory.get(var2x),
                  item,
                  new ThaumcraftInvHelper.InvFilter(false, !item.hasTagCompound(), ore, false).setRelaxedNBT()
               )) {
               if (((ItemStack)player.inventory.mainInventory.get(var2x)).getCount() > count) {
                  ((ItemStack)player.inventory.mainInventory.get(var2x)).shrink(count);
                  count = 0;
               } else {
                  count -= ((ItemStack)player.inventory.mainInventory.get(var2x)).getCount();
                  player.inventory.mainInventory.set(var2x, ItemStack.EMPTY);
               }

               if (count <= 0) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public static boolean consumePlayerItem(EntityPlayer player, Item item, int md, int amt) {
      if (!isPlayerCarryingAmount(player, new ItemStack(item, amt, md), false)) {
         return false;
      } else {
         int count = amt;

         for (int var2x = 0; var2x < player.inventory.mainInventory.size(); var2x++) {
            if (((ItemStack)player.inventory.mainInventory.get(var2x)).getItem() == item
               && ((ItemStack)player.inventory.mainInventory.get(var2x)).getItemDamage() == md) {
               if (((ItemStack)player.inventory.mainInventory.get(var2x)).getCount() > count) {
                  ((ItemStack)player.inventory.mainInventory.get(var2x)).shrink(count);
                  count = 0;
               } else {
                  count -= ((ItemStack)player.inventory.mainInventory.get(var2x)).getCount();
                  player.inventory.mainInventory.set(var2x, ItemStack.EMPTY);
               }

               if (count <= 0) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   public static boolean consumePlayerItem(EntityPlayer player, Item item, int md) {
      for (int var2x = 0; var2x < player.inventory.mainInventory.size(); var2x++) {
         if (((ItemStack)player.inventory.mainInventory.get(var2x)).getItem() == item
            && ((ItemStack)player.inventory.mainInventory.get(var2x)).getItemDamage() == md) {
            ((ItemStack)player.inventory.mainInventory.get(var2x)).shrink(1);
            if (((ItemStack)player.inventory.mainInventory.get(var2x)).getCount() <= 0) {
               player.inventory.mainInventory.set(var2x, ItemStack.EMPTY);
            }

            return true;
         }
      }

      return false;
   }

   public static boolean isPlayerCarryingAmount(EntityPlayer player, ItemStack stack, boolean ore) {
      if (stack != null && !stack.isEmpty()) {
         int count = stack.getCount();

         for (int var2x = 0; var2x < player.inventory.mainInventory.size(); var2x++) {
            if (checkEnchantedPlaceholder(stack, (ItemStack)player.inventory.mainInventory.get(var2x))
               || areItemStacksEqual(
                  (ItemStack)player.inventory.mainInventory.get(var2x),
                  stack,
                  new ThaumcraftInvHelper.InvFilter(false, !stack.hasTagCompound(), ore, false).setRelaxedNBT()
               )) {
               count -= ((ItemStack)player.inventory.mainInventory.get(var2x)).getCount();
               if (count <= 0) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean checkEnchantedPlaceholder(ItemStack stack, ItemStack stack2) {
      if (stack.getItem() != ItemsTC.enchantedPlaceholder) {
         return false;
      } else {
         Map<Enchantment, Integer> en = EnchantmentHelper.getEnchantments(stack);
         boolean b = !en.isEmpty();

         for (Enchantment e : en.keySet()) {
            Map<Enchantment, Integer> en2 = EnchantmentHelper.getEnchantments(stack2);
            if (en2.isEmpty()) {
               return false;
            }

            b = false;

            for (Enchantment e2 : en2.keySet()) {
               if (e2.equals(e)) {
                  b = true;
                  if (en2.get(e2) < en.get(e)) {
                     b = false;
                     return b;
                  }
               }
            }
         }

         return b;
      }
   }

   public static EntityEquipmentSlot isHoldingItem(EntityPlayer player, Item item) {
      if (player != null && item != null) {
         if (player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem() == item) {
            return EntityEquipmentSlot.MAINHAND;
         } else {
            return player.getHeldItemOffhand() != null && player.getHeldItemOffhand().getItem() == item ? EntityEquipmentSlot.OFFHAND : null;
         }
      } else {
         return null;
      }
   }

   public static EntityEquipmentSlot isHoldingItem(EntityPlayer player, Class item) {
      if (player == null || item == null) {
         return null;
      } else if (player.getHeldItemMainhand() != null && item.isAssignableFrom(player.getHeldItemMainhand().getItem().getClass())) {
         return EntityEquipmentSlot.MAINHAND;
      } else {
         return player.getHeldItemOffhand() != null && item.isAssignableFrom(player.getHeldItemOffhand().getItem().getClass())
            ? EntityEquipmentSlot.OFFHAND
            : null;
      }
   }

   public static int getPlayerSlotFor(EntityPlayer player, ItemStack stack) {
      for (int i = 0; i < player.inventory.mainInventory.size(); i++) {
         if (!((ItemStack)player.inventory.mainInventory.get(i)).isEmpty()
            && stackEqualExact(stack, (ItemStack)player.inventory.mainInventory.get(i))) {
            return i;
         }
      }

      return -1;
   }

   public static boolean stackEqualExact(ItemStack stack1, ItemStack stack2) {
      return stack1.getItem() == stack2.getItem()
         && (!stack1.getHasSubtypes() || stack1.getMetadata() == stack2.getMetadata())
         && ItemStack.areItemStackTagsEqual(stack1, stack2);
   }

   public static boolean areItemStacksEqualStrict(ItemStack stack0, ItemStack stack1) {
      return areItemStacksEqual(stack0, stack1, ThaumcraftInvHelper.InvFilter.STRICT);
   }

   public static ItemStack findFirstMatchFromFilter(
      NonNullList<ItemStack> filterStacks, boolean blacklist, IItemHandler inv, EnumFacing face, ThaumcraftInvHelper.InvFilter filter
   ) {
      return findFirstMatchFromFilter(filterStacks, blacklist, inv, face, filter, false);
   }

   public static ItemStack findFirstMatchFromFilter(
      NonNullList<ItemStack> filterStacks, boolean blacklist, IItemHandler inv, EnumFacing face, ThaumcraftInvHelper.InvFilter filter, boolean leaveOne
   ) {
      label56:
      for (int a = 0; a < inv.getSlots(); a++) {
         ItemStack is = inv.getStackInSlot(a);
         if (is != null && !is.isEmpty() && is.getCount() > 0 && (!leaveOne || ThaumcraftInvHelper.countTotalItemsIn(inv, is, filter) >= 2)) {
            boolean allow = false;
            boolean allEmpty = true;

            for (ItemStack fs : filterStacks) {
               if (fs != null && !fs.isEmpty()) {
                  allEmpty = false;
                  boolean r = areItemStacksEqual(fs.copy(), is.copy(), filter);
                  if (blacklist) {
                     if (r) {
                        continue label56;
                     }

                     allow = true;
                  } else if (r) {
                     return is;
                  }
               }
            }

            if (blacklist && (allow || allEmpty)) {
               return is;
            }
         }
      }

      return ItemStack.EMPTY;
   }

   public static boolean matchesFilters(NonNullList<ItemStack> nonNullList, boolean blacklist, ItemStack is, ThaumcraftInvHelper.InvFilter filter) {
      if (is != null && !is.isEmpty() && is.getCount() > 0) {
         boolean allow = false;
         boolean allEmpty = true;

         for (ItemStack fs : nonNullList) {
            if (fs != null && !fs.isEmpty()) {
               allEmpty = false;
               boolean r = areItemStacksEqual(fs.copy(), is.copy(), filter);
               if (blacklist) {
                  if (r) {
                     return false;
                  }

                  allow = true;
               } else if (r) {
                  return true;
               }
            }
         }

         return blacklist && (allow || allEmpty);
      } else {
         return false;
      }
   }

   public static ItemStack findFirstMatchFromFilter(
      NonNullList<ItemStack> filterStacks,
      NonNullList<Integer> filterStacksSizes,
      boolean blacklist,
      NonNullList<ItemStack> itemStacks,
      ThaumcraftInvHelper.InvFilter filter
   ) {
      return (ItemStack)findFirstMatchFromFilterTuple(filterStacks, filterStacksSizes, blacklist, itemStacks, filter).getFirst();
   }

   public static Tuple<ItemStack, Integer> findFirstMatchFromFilterTuple(
      NonNullList<ItemStack> filterStacks,
      NonNullList<Integer> filterStacksSizes,
      boolean blacklist,
      NonNullList<ItemStack> stacks,
      ThaumcraftInvHelper.InvFilter filter
   ) {
      label63:
      for (ItemStack is : stacks) {
         if (is != null && !is.isEmpty() && is.getCount() > 0) {
            boolean allow = false;
            boolean allEmpty = true;

            for (int idx = 0; idx < filterStacks.size(); idx++) {
               ItemStack fs = (ItemStack)filterStacks.get(idx);
               if (fs != null && !fs.isEmpty()) {
                  allEmpty = false;
                  boolean r = areItemStacksEqual(fs.copy(), is.copy(), filter);
                  if (blacklist) {
                     if (r) {
                        continue label63;
                     }

                     allow = true;
                  } else if (r) {
                     return new Tuple(is, filterStacksSizes.get(idx));
                  }
               }
            }

            if (blacklist && (allow || allEmpty)) {
               return new Tuple(is, 0);
            }
         }
      }

      return new Tuple(ItemStack.EMPTY, 0);
   }

   public static boolean areItemStacksEqual(ItemStack stack0, ItemStack stack1, ThaumcraftInvHelper.InvFilter filter) {
      if (stack0 == null && stack1 != null) {
         return false;
      } else if (stack0 != null && stack1 == null) {
         return false;
      } else if (stack0 == null && stack1 == null) {
         return true;
      } else if (stack0.isEmpty() && !stack1.isEmpty()) {
         return false;
      } else if (!stack0.isEmpty() && stack1.isEmpty()) {
         return false;
      } else if (stack0.isEmpty() && stack1.isEmpty()) {
         return true;
      } else if (filter.useMod) {
         String m1 = "A";
         String m2 = "B";
         String a = stack0.getItem().getRegistryName().getNamespace();
         if (a != null) {
            m1 = a;
         }

         String b = stack1.getItem().getRegistryName().getNamespace();
         if (b != null) {
            m2 = b;
         }

         return m1.equals(m2);
      } else {
         if (filter.useOre && !stack0.isEmpty()) {
            int[] od = OreDictionary.getOreIDs(stack0);

            for (int i : od) {
               if (ThaumcraftInvHelper.containsMatch(false, new ItemStack[]{stack1}, OreDictionary.getOres(OreDictionary.getOreName(i), false))) {
                  return true;
               }
            }
         }

         boolean t1 = true;
         if (!filter.igNBT) {
            t1 = filter.relaxedNBT ? ThaumcraftInvHelper.areItemStackTagsEqualRelaxed(stack0, stack1) : ItemStack.areItemStackTagsEqual(stack0, stack1);
         }

         if (stack0.getItemDamage() == 32767 || stack1.getItemDamage() == 32767) {
            filter.igDmg = true;
         }

         boolean t2 = !filter.igDmg && stack0.getItemDamage() != stack1.getItemDamage();
         return stack0.getItem() != stack1.getItem() ? false : (t2 ? false : t1);
      }
   }

   public static void dropHarvestsAtPos(World worldIn, BlockPos pos, List<ItemStack> list) {
      dropHarvestsAtPos(worldIn, pos, list, false, 0, null);
   }

   public static void dropHarvestsAtPos(World worldIn, BlockPos pos, List<ItemStack> list, boolean followItem, int color, Entity target) {
      for (ItemStack item : list) {
         if (!worldIn.isRemote && worldIn.getGameRules().getBoolean("doTileDrops") && !worldIn.restoringBlockSnapshots) {
            float f = 0.5F;
            double d0 = worldIn.rand.nextFloat() * f + (1.0F - f) * 0.5;
            double d1 = worldIn.rand.nextFloat() * f + (1.0F - f) * 0.5;
            double d2 = worldIn.rand.nextFloat() * f + (1.0F - f) * 0.5;
            EntityItem entityitem = null;
            if (followItem) {
               entityitem = new EntityFollowingItem(worldIn, pos.getX() + d0, pos.getY() + d1, pos.getZ() + d2, item, target, color);
            } else {
               entityitem = new EntityItem(worldIn, pos.getX() + d0, pos.getY() + d1, pos.getZ() + d2, item);
            }

            entityitem.setDefaultPickupDelay();
            worldIn.spawnEntity(entityitem);
         }
      }
   }

   public static void dropItemAtPos(World world, ItemStack item, BlockPos pos) {
      if (!world.isRemote && item != null && !item.isEmpty() && item.getCount() > 0) {
         EntityItem entityItem = new EntityItem(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item.copy());
         world.spawnEntity(entityItem);
      }
   }

   public static void dropItemAtEntity(World world, ItemStack item, Entity entity) {
      if (!world.isRemote && item != null && !item.isEmpty() && item.getCount() > 0) {
         EntityItem entityItem = new EntityItem(
            world, entity.posX, entity.posY + entity.getEyeHeight() / 2.0F, entity.posZ, item.copy()
         );
         world.spawnEntity(entityItem);
      }
   }

   public static void dropItemsAtEntity(World world, BlockPos pos, Entity entity) {
      TileEntity tileEntity = world.getTileEntity(pos);
      if (tileEntity instanceof IInventory && !world.isRemote) {
         IInventory inventory = (IInventory)tileEntity;

         for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack item = inventory.getStackInSlot(i);
            if (!item.isEmpty() && item.getCount() > 0) {
               EntityItem entityItem = new EntityItem(
                  world, entity.posX, entity.posY + entity.getEyeHeight() / 2.0F, entity.posZ, item.copy()
               );
               world.spawnEntity(entityItem);
               inventory.setInventorySlotContents(i, ItemStack.EMPTY);
            }
         }
      }
   }

   public static ItemStack cycleItemStack(Object input) {
      return cycleItemStack(input, 0);
   }

   public static ItemStack cycleItemStack(Object input, int counter) {
      ItemStack it = ItemStack.EMPTY;
      if (input instanceof Ingredient) {
         boolean b = !((Ingredient)input).isSimple() && !(input instanceof IngredientNBTTC) && !(input instanceof IngredientNBT);
         input = ((Ingredient)input).getMatchingStacks();
         if (b) {
            ItemStack[] q = (ItemStack[])input;
            ItemStack[] r = new ItemStack[q.length];

            for (int a = 0; a < q.length; a++) {
               r[a] = q[a].copy();
               r[a].setItemDamage(32767);
            }

            input = r;
         }
      }

      if (input instanceof ItemStack[]) {
         ItemStack[] q = (ItemStack[])input;
         if (q != null && q.length > 0) {
            int idx = (int)((counter + System.currentTimeMillis() / 1000L) % q.length);
            it = cycleItemStack(q[idx], counter++);
         }
      } else if (input instanceof ItemStack) {
         it = (ItemStack)input;
         if (it != null && !it.isEmpty() && it.getItem() != null && it.isItemStackDamageable() && it.getItemDamage() == 32767) {
            int q = 5000 / it.getMaxDamage();
            int md = (int)((counter + System.currentTimeMillis() / q) % it.getMaxDamage());
            ItemStack it2 = new ItemStack(it.getItem(), 1, md);
            it2.setTagCompound(it.getTagCompound());
            it = it2;
         }
      } else if (input instanceof List) {
         List<ItemStack> q = (List<ItemStack>)input;
         if (q != null && q.size() > 0) {
            int idx = (int)((counter + System.currentTimeMillis() / 1000L) % q.size());
            it = cycleItemStack(q.get(idx), counter++);
         }
      } else if (input instanceof String) {
         List<ItemStack> q = OreDictionary.getOres((String)input, false);
         if (q != null && q.size() > 0) {
            int idx = (int)((counter + System.currentTimeMillis() / 1000L) % q.size());
            it = cycleItemStack(q.get(idx), counter++);
         }
      }

      return it;
   }
}
