package thaumcraft.common.golems;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

/**
 * The inventory helpers the seals use (ThaumcraftInvHelper and parts of InventoryUtils in 1.12). Damage and NBT
 * matching map to the damage and the other data components; the ore dictionary maps to shared "c:" item tags.
 */
public final class GolemInvHelper {
   private GolemInvHelper() {
   }

   public static class InvFilter {
      public boolean igDmg;
      public boolean igNBT;
      public boolean useOre;
      public boolean useMod;
      public boolean relaxedNBT = false;
      public static final InvFilter STRICT = new InvFilter(false, false, false, false);
      public static final InvFilter BASEORE = new InvFilter(false, false, true, false);

      public InvFilter(boolean ignoreDamage, boolean ignoreNBT, boolean useOre, boolean useMod) {
         this.igDmg = ignoreDamage;
         this.igNBT = ignoreNBT;
         this.useOre = useOre;
         this.useMod = useMod;
      }

      public InvFilter setRelaxedNBT() {
         this.relaxedNBT = true;
         return this;
      }
   }

   @Nullable
   public static IItemHandler getItemHandlerAt(Level level, BlockPos pos, @Nullable Direction side) {
      IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
      if (h != null) return h;
      BlockEntity be = level.getBlockEntity(pos);
      if (be instanceof WorldlyContainer wc && side != null) return new SidedInvWrapper(wc, side);
      if (be instanceof Container c) return new InvWrapper(c);
      return null;
   }

   public static ItemStack insertStackAt(Level level, BlockPos pos, Direction side, ItemStack stack, boolean simulate) {
      IItemHandler inv = getItemHandlerAt(level, pos, side);
      return inv != null ? ItemHandlerHelper.insertItemStacked(inv, stack, simulate) : stack;
   }

   /** How much of the stack would fit (the part that fits, as a stack). */
   public static ItemStack hasRoomFor(Level level, BlockPos pos, Direction side, ItemStack stack) {
      ItemStack test = insertStackAt(level, pos, side, stack.copy(), true);
      if (test.isEmpty()) return stack.copy();
      ItemStack out = stack.copy();
      out.setCount(stack.getCount() - test.getCount());
      return out;
   }

   public static boolean hasRoomForSome(Level level, BlockPos pos, Direction side, ItemStack stack) {
      ItemStack test = insertStackAt(level, pos, side, stack.copy(), true);
      return stack.getCount() == 0 || test.getCount() != stack.getCount();
   }

   public static int countTotalItemsIn(@Nullable IItemHandler inv, ItemStack stack, InvFilter filter) {
      int count = 0;
      if (inv != null) {
         for (int a = 0; a < inv.getSlots(); a++) {
            if (areItemStacksEqual(stack, inv.getStackInSlot(a), filter)) count += inv.getStackInSlot(a).getCount();
         }
      }
      return count;
   }

   public static int countTotalItemsIn(Level level, BlockPos pos, Direction side, ItemStack stack, InvFilter filter) {
      return countTotalItemsIn(getItemHandlerAt(level, pos, side), stack, filter);
   }

   public static ItemStack copyLimitedStack(ItemStack stack, int limit) {
      if (stack.isEmpty()) return ItemStack.EMPTY;
      ItemStack s = stack.copy();
      if (s.getCount() > limit) s.setCount(limit);
      return s;
   }

   public static ItemStack removeStackFrom(Level level, BlockPos pos, Direction side, ItemStack stack, InvFilter filter, boolean simulate) {
      return removeStackFrom(getItemHandlerAt(level, pos, side), stack, filter, simulate);
   }

   public static ItemStack removeStackFrom(@Nullable IItemHandler inv, ItemStack stack, InvFilter filter, boolean simulate) {
      int amount = stack.getCount();
      int removed = 0;
      if (inv != null) {
         for (int a = 0; a < inv.getSlots() && removed < amount; a++) {
            if (areItemStacksEqual(stack, inv.getStackInSlot(a), filter)) {
               int s = Math.min(amount - removed, inv.getStackInSlot(a).getCount());
               ItemStack es = inv.extractItem(a, s, simulate);
               if (!es.isEmpty()) removed += es.getCount();
            }
         }
      }
      if (removed == 0) return ItemStack.EMPTY;
      ItemStack s = stack.copy();
      s.setCount(removed);
      return s;
   }

   /** InventoryUtils.ejectStackAt: into the inventory there, else (or with smart off) dropped in front of it. */
   public static ItemStack ejectStackAt(Level level, BlockPos pos, Direction side, ItemStack out, boolean smart) {
      out = insertStackAt(level, pos.relative(side), side.getOpposite(), out, false);
      if (smart && getItemHandlerAt(level, pos.relative(side), side.getOpposite()) != null) return out;
      if (!out.isEmpty()) {
         if (level.getBlockState(pos.relative(side)).isCollisionShapeFullBlock(level, pos.relative(side))) pos = pos.relative(side.getOpposite());
         ItemEntity e = new ItemEntity(level, pos.getX() + 0.5 + side.getStepX(), pos.getY() + side.getStepY(), pos.getZ() + 0.5 + side.getStepZ(), out);
         e.setDeltaMovement(0.3 * side.getStepX(), 0.3 * side.getStepY(), 0.3 * side.getStepZ());
         level.addFreshEntity(e);
      }
      return ItemStack.EMPTY;
   }

   public static void ejectStackAt(Level level, BlockPos pos, Direction side, ItemStack out) {
      ejectStackAt(level, pos, side, out, false);
   }

   /** Items lying within range of the block (InventoryUtils.countStackInWorld; 1.12 only counted empty stacks, a bug). */
   public static int countStackInWorld(Level level, BlockPos pos, ItemStack stack, double range, InvFilter filter) {
      int count = 0;
      Vec3 c = Vec3.atCenterOf(pos);
      for (ItemEntity ei : level.getEntitiesOfClass(ItemEntity.class, new AABB(c, c).inflate(range))) {
         if (!ei.getItem().isEmpty() && areItemStacksEqual(stack, ei.getItem(), filter)) count += ei.getItem().getCount();
      }
      return count;
   }

   public static void dropItemAtEntity(Level level, ItemStack item, Entity entity) {
      if (!level.isClientSide && !item.isEmpty()) {
         level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY() + entity.getEyeHeight() / 2.0F, entity.getZ(), item.copy()));
      }
   }

   /** The first of the inventory's stacks that passes the filter list (InventoryUtils.findFirstMatchFromFilter). */
   public static ItemStack findFirstMatchFromFilter(NonNullList<ItemStack> filterStacks, boolean blacklist, @Nullable IItemHandler inv, InvFilter filter,
                                                    boolean leaveOne) {
      if (inv == null) return ItemStack.EMPTY;
      for (int a = 0; a < inv.getSlots(); a++) {
         ItemStack is = inv.getStackInSlot(a);
         if (is.isEmpty() || leaveOne && countTotalItemsIn(inv, is, filter) < 2) continue;
         if (matchesFilters(filterStacks, blacklist, is, filter)) return is;
      }
      return ItemStack.EMPTY;
   }

   public static boolean matchesFilters(NonNullList<ItemStack> filterStacks, boolean blacklist, ItemStack is, InvFilter filter) {
      if (is.isEmpty()) return false;
      boolean allow = false;
      boolean allEmpty = true;
      for (ItemStack fs : filterStacks) {
         if (fs.isEmpty()) continue;
         allEmpty = false;
         boolean r = areItemStacksEqual(fs.copy(), is.copy(), filter);
         if (blacklist) {
            if (r) return false;
            allow = true;
         } else if (r) {
            return true;
         }
      }
      return blacklist && (allow || allEmpty);
   }

   public static ItemStack findFirstMatchFromFilter(NonNullList<ItemStack> filterStacks, NonNullList<Integer> sizes, boolean blacklist,
                                                    NonNullList<ItemStack> stacks, InvFilter filter) {
      return findFirstMatchFromFilterTuple(filterStacks, sizes, blacklist, stacks, filter).getA();
   }

   /** The first stack passing the filter, with the size limit of the filter slot it matched. */
   public static Tuple<ItemStack, Integer> findFirstMatchFromFilterTuple(NonNullList<ItemStack> filterStacks, NonNullList<Integer> sizes, boolean blacklist,
                                                                         NonNullList<ItemStack> stacks, InvFilter filter) {
      outer:
      for (ItemStack is : stacks) {
         if (is.isEmpty()) continue;
         boolean allow = false;
         boolean allEmpty = true;
         for (int idx = 0; idx < filterStacks.size(); idx++) {
            ItemStack fs = filterStacks.get(idx);
            if (fs.isEmpty()) continue;
            allEmpty = false;
            boolean r = areItemStacksEqual(fs.copy(), is.copy(), filter);
            if (blacklist) {
               if (r) continue outer;
               allow = true;
            } else if (r) {
               return new Tuple<>(is, sizes.get(idx));
            }
         }
         if (blacklist && (allow || allEmpty)) return new Tuple<>(is, 0);
      }
      return new Tuple<>(ItemStack.EMPTY, 0);
   }

   public static boolean areItemStacksEqual(ItemStack stack0, ItemStack stack1, InvFilter filter) {
      if (stack0.isEmpty() || stack1.isEmpty()) return stack0.isEmpty() && stack1.isEmpty();
      if (filter.useMod) {
         return Objects.equals(BuiltInRegistries.ITEM.getKey(stack0.getItem()).getNamespace(),
            BuiltInRegistries.ITEM.getKey(stack1.getItem()).getNamespace());
      }
      if (filter.useOre && sharesCommonTag(stack0.getItem(), stack1.getItem())) return true;
      if (stack0.getItem() != stack1.getItem()) return false;
      if (!filter.igDmg && stack0.isDamageableItem() && stack0.getDamageValue() != stack1.getDamageValue()) return false;
      if (filter.igNBT) return true;
      DataComponentPatch p0 = withoutDamage(stack0), p1 = withoutDamage(stack1);
      if (filter.relaxedNBT) {
         for (var e : p0.entrySet()) {
            if (!p1.entrySet().contains(e)) return false;
         }
         return true;
      }
      return p0.equals(p1);
   }

   private static DataComponentPatch withoutDamage(ItemStack s) {
      return s.getComponentsPatch().forget(t -> t == DataComponents.DAMAGE);
   }

   /** Ore dictionary names were specific ("plateIron"): only specific "c:" tags ("c:plates/iron") count. */
   private static boolean sharesCommonTag(Item a, Item b) {
      return a.builtInRegistryHolder().tags().filter(t -> t.location().getNamespace().equals("c") && t.location().getPath().contains("/"))
         .anyMatch((TagKey<Item> t) -> b.builtInRegistryHolder().is(t));
   }

   // ------------------------------------------------------------------ golem press components

   public static boolean checkAdjacentChests(Level level, BlockPos pos, ItemStack stack) {
      int c = stack.getCount();
      for (Direction face : Direction.values()) {
         if (face == Direction.UP) continue;
         c -= countTotalItemsIn(level, pos.relative(face), face.getOpposite(), stack.copy(), InvFilter.BASEORE);
         if (c <= 0) return true;
      }
      return false;
   }

   public static boolean consumeFromAdjacentChests(Level level, BlockPos pos, ItemStack stack) {
      for (Direction face : Direction.values()) {
         if (face == Direction.UP || stack.isEmpty()) continue;
         ItemStack os = removeStackFrom(level, pos.relative(face), face.getOpposite(), stack, InvFilter.BASEORE, false);
         stack.setCount(stack.getCount() - os.getCount());
      }
      return stack.isEmpty();
   }

   private static InvFilter playerFilter(ItemStack stack, boolean ore) {
      return new InvFilter(false, stack.getComponentsPatch().isEmpty(), ore, false).setRelaxedNBT();
   }

   public static boolean isPlayerCarryingAmount(net.minecraft.world.entity.player.Player player, ItemStack stack, boolean ore) {
      if (stack.isEmpty()) return false;
      int count = stack.getCount();
      for (ItemStack s : player.getInventory().items) {
         if (areItemStacksEqual(s, stack, playerFilter(stack, ore))) {
            count -= s.getCount();
            if (count <= 0) return true;
         }
      }
      return false;
   }

   public static boolean consumePlayerItem(net.minecraft.world.entity.player.Player player, ItemStack item, boolean ore) {
      int count = item.getCount();
      var items = player.getInventory().items;
      for (int i = 0; i < items.size(); i++) {
         ItemStack s = items.get(i);
         if (!areItemStacksEqual(s, item, playerFilter(item, ore))) continue;
         if (s.getCount() > count) {
            s.shrink(count);
            count = 0;
         } else {
            count -= s.getCount();
            items.set(i, ItemStack.EMPTY);
         }
         if (count <= 0) return true;
      }
      return false;
   }

   /** Each stack from the inventories around (not above) the block, or else from the player. */
   public static boolean consumeItemsFromAdjacentInventoryOrPlayer(Level level, BlockPos pos, @Nullable net.minecraft.world.entity.player.Player player,
                                                                   boolean sim, ItemStack... items) {
      for (ItemStack stack : items) {
         if (!checkAdjacentChests(level, pos, stack) && (player == null || !isPlayerCarryingAmount(player, stack, true))) return false;
      }
      if (!sim) {
         for (ItemStack stack : items) {
            if (!consumeFromAdjacentChests(level, pos, stack.copy()) && player != null) consumePlayerItem(player, stack, true);
         }
      }
      return true;
   }
}
