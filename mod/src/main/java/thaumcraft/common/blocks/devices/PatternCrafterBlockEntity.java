package thaumcraft.common.blocks.devices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TilePatternCrafter: once a second (while enabled) finds a stack in the inventory above with enough items for its
 * pattern, crafts it and puts the result (and the leftovers) in the inventory below. Each craft costs 1 of the vis it
 * drains 5 at a time.
 */
public class PatternCrafterBlockEntity extends TCBlockEntity {
   /** How many items each pattern uses. */
   public static final int[] AMOUNTS = {9, 1, 2, 2, 4, 3, 3, 6, 6, 8};
   public byte type = 0;
   public int count = new java.util.Random(System.currentTimeMillis()).nextInt(20);
   private float power = 0.0F;
   public float rot;
   public float rp;
   public int rotTicks = 0;
   private ItemStack outStack = ItemStack.EMPTY;
   private final NonNullList<ItemStack> matrix = NonNullList.withSize(9, ItemStack.EMPTY);

   public PatternCrafterBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.PATTERN_CRAFTER_BE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, PatternCrafterBlockEntity be) {
      if (level.isClientSide) {
         if (be.rotTicks > 0) {
            be.rotTicks--;
            if (be.rotTicks % Math.floor(Math.max(1.0F, be.rp)) == 0.0) {
               level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.CLACK.get(), SoundSource.BLOCKS, 0.2F, 1.7F, false);
            }
            be.rp++;
         } else {
            be.rp *= 0.8F;
         }
         be.rot += be.rp;
         return;
      }
      if (be.count++ % 20 != 0 || !DeviceBlock.isEnabled(state)) return;
      if (be.power <= 0.0F) be.power += AuraHandler.drainVis(level, pos, 5.0F, false);
      int amt = AMOUNTS[Math.max(0, Math.min(9, be.type))];
      IItemHandler above = DeviceUtils.getItemHandlerAt(level, pos.above(), Direction.DOWN);
      IItemHandler below = DeviceUtils.getItemHandlerAt(level, pos.below(), Direction.UP);
      if (above == null || below == null) return;
      for (int a = 0; a < above.getSlots(); a++) {
         ItemStack testStack = above.getStackInSlot(a).copy();
         if (testStack.isEmpty()) continue;
         testStack.setCount(amt);
         if (removeStackFrom(above, testStack, true) == amt && be.craft(level, testStack) && be.power >= 1.0F
            && ItemHandlerHelper.insertItem(below, be.outStack.copy(), true).isEmpty()) {
            boolean b = true;
            for (ItemStack rest : be.matrix) {
               if (!rest.isEmpty() && !ItemHandlerHelper.insertItem(below, rest.copy(), true).isEmpty()) {
                  b = false;
                  break;
               }
            }
            if (b) {
               ItemHandlerHelper.insertItem(below, be.outStack.copy(), false);
               for (ItemStack rest : be.matrix) {
                  if (!rest.isEmpty()) ItemHandlerHelper.insertItem(below, rest.copy(), false);
               }
               removeStackFrom(above, testStack, false);
               level.blockEvent(pos, state.getBlock(), 1, 0);
               be.power--;
               be.setChanged();
               break;
            }
         }
      }
   }

   /** InventoryUtils.removeStackFrom: takes up to stack.count of that item; returns how many (were / would be) taken. */
   private static int removeStackFrom(IItemHandler inv, ItemStack stack, boolean simulate) {
      int amount = stack.getCount();
      int removed = 0;
      for (int a = 0; a < inv.getSlots() && removed < amount; a++) {
         if (ItemStack.isSameItemSameComponents(stack, inv.getStackInSlot(a))) {
            int s = Math.min(amount - removed, inv.getStackInSlot(a).getCount());
            ItemStack es = inv.extractItem(a, s, simulate);
            removed += es.getCount();
         }
      }
      return removed;
   }

   /** The slots each pattern fills. */
   private static int[] slots(int type) {
      return switch (type) {
         case 1 -> new int[]{0};
         case 2 -> new int[]{0, 1};
         case 3 -> new int[]{0, 3};
         case 4 -> new int[]{0, 1, 3, 4};
         case 5 -> new int[]{0, 1, 2};
         case 6 -> new int[]{0, 3, 6};
         case 7 -> new int[]{0, 1, 2, 3, 4, 5};
         case 8 -> new int[]{0, 1, 3, 4, 6, 7};
         case 9 -> new int[]{0, 1, 2, 3, 5, 6, 7, 8};
         default -> new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
      };
   }

   /** Fills the grid with the pattern, finds the recipe; leaves the result in outStack and the leftovers in the grid. */
   private boolean craft(Level level, ItemStack inStack) {
      outStack = ItemStack.EMPTY;
      for (int i = 0; i < 9; i++) matrix.set(i, ItemStack.EMPTY);
      for (int s : slots(type)) matrix.set(s, inStack.copyWithCount(1));
      CraftingInput input = CraftingInput.of(3, 3, new ArrayList<>(matrix));
      Optional<RecipeHolder<CraftingRecipe>> r = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
      if (r.isEmpty()) return false;
      outStack = r.get().value().assemble(input, level.registryAccess());
      List<ItemStack> remaining = r.get().value().getRemainingItems(input);
      // CraftingInput.of trims empty rows/columns, so the remainders follow its positions; only non-empty ones matter.
      for (int i = 0; i < 9; i++) matrix.set(i, ItemStack.EMPTY);
      int k = 0;
      for (ItemStack rest : remaining) {
         if (!rest.isEmpty() && k < 9) matrix.set(k++, rest.copy());
      }
      return !outStack.isEmpty();
   }

   public void cycle() {
      type++;
      if (type > 9) type = 0;
      sync();
   }

   @Override
   public boolean triggerEvent(int id, int param) {
      if (id == 1) {
         if (level.isClientSide) rotTicks = 10;
         return true;
      }
      return super.triggerEvent(id, param);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      type = tag.getByte("type");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putByte("type", type);
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      power = tag.getFloat("power");
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putFloat("power", power);
   }
}
