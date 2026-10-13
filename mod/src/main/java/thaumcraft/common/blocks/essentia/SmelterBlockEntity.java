package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.blocks.devices.BellowsBlockEntity;
import thaumcraft.common.menu.SmelterMenu;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TileSmelter: burns fuel to melt items down into their essentia (up to 256), losing some as flux (80/90/95% kept
 * for the basic/thaumium/void smeltery; auxiliary vents catch a third of the flux). The essentia is pushed up into
 * the alembics stacked on top (and on auxiliary pumps at the sides). Bellows speed it up, alumentum too.
 */
public class SmelterBlockEntity extends TCBlockEntity implements WorldlyContainer, MenuProvider {
   private static final int[] SLOTS_BOTTOM = {1};
   private static final int[] SLOTS_TOP = {};
   private static final int[] SLOTS_SIDES = {0};
   public static final int MAX_VIS = 256;
   private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
   public AspectList aspects = new AspectList();
   public int vis;
   public int smeltTime = 100;
   boolean speedBoost;
   public int furnaceBurnTime;
   public int currentItemBurnTime;
   public int furnaceCookTime;
   int count;
   int bellows = -1;

   public final ContainerData data = new ContainerData() {
      @Override
      public int get(int i) {
         return switch (i) {
            case 0 -> furnaceCookTime;
            case 1 -> furnaceBurnTime;
            case 2 -> currentItemBurnTime;
            case 3 -> vis;
            default -> smeltTime;
         };
      }

      @Override
      public void set(int i, int v) {
         switch (i) {
            case 0 -> furnaceCookTime = v;
            case 1 -> furnaceBurnTime = v;
            case 2 -> currentItemBurnTime = v;
            case 3 -> vis = v;
            default -> smeltTime = v;
         }
      }

      @Override
      public int getCount() {
         return 5;
      }
   };

   public SmelterBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.SMELTER.get(), pos, state);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      furnaceBurnTime = tag.getShort("BurnTime");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putShort("BurnTime", (short) furnaceBurnTime);
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      items.clear();
      ContainerHelper.loadAllItems(tag, items, registries);
      speedBoost = tag.getBoolean("speedBoost");
      furnaceCookTime = tag.getShort("CookTime");
      currentItemBurnTime = items.get(1).getBurnTime(RecipeType.SMELTING);
      aspects = AspectList.CODEC.parse(NbtOps.INSTANCE, tag.get("Aspects")).result().orElseGet(AspectList::new);
      vis = aspects.visSize();
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      ContainerHelper.saveAllItems(tag, items, registries);
      tag.putBoolean("speedBoost", speedBoost);
      tag.putShort("CookTime", (short) furnaceCookTime);
      tag.put("Aspects", AspectList.CODEC.encodeStart(NbtOps.INSTANCE, aspects).getOrThrow());
   }

   private int getKind() {
      BlockState s = getBlockState();
      return s.is(ModBlocks.SMELTER_BASIC.get()) ? 0 : s.is(ModBlocks.SMELTER_THAUMIUM.get()) ? 1 : 2;
   }

   private float getEfficiency() {
      return switch (getKind()) {
         case 1 -> 0.9F;
         case 2 -> 0.95F;
         default -> 0.8F;
      };
   }

   private int getSpeed() {
      return 20 - (getKind() == 1 ? 10 : 5);
   }

   private Direction facing() {
      return DeviceBlock.getFacing(getBlockState());
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, SmelterBlockEntity be) {
      be.update();
   }

   private void update() {
      boolean wasBurning = furnaceBurnTime > 0;
      boolean changed = false;
      count++;
      if (furnaceBurnTime > 0) furnaceBurnTime--;
      if (bellows < 0) checkNeighbours();
      int speed = getSpeed();
      if (speedBoost) speed = (int) (speed * 0.8);
      if (count % speed == 0 && aspects.size() > 0) {
         for (Aspect aspect : aspects.getAspects()) {
            if (aspects.getAmount(aspect) > 0 && AlembicBlockEntity.processAlembics(level, worldPosition, aspect)) {
               takeFromContainer(aspect, 1);
               break;
            }
         }
         for (Direction face : Direction.Plane.HORIZONTAL) {
            if (facing() == face) continue;
            BlockState aux = level.getBlockState(worldPosition.relative(face));
            if (aux.is(ModBlocks.SMELTER_AUX.get()) && DeviceBlock.getFacing(aux) == face.getOpposite()) {
               for (Aspect aspect : aspects.getAspects()) {
                  if (aspects.getAmount(aspect) > 0 && AlembicBlockEntity.processAlembics(level, worldPosition.relative(face), aspect)) {
                     takeFromContainer(aspect, 1);
                     break;
                  }
               }
            }
         }
      }
      if (furnaceBurnTime == 0) {
         if (canSmelt()) {
            currentItemBurnTime = furnaceBurnTime = items.get(1).getBurnTime(RecipeType.SMELTING);
            if (furnaceBurnTime > 0) {
               SmelterBlock.setFurnaceState(level, worldPosition, true);
               changed = true;
               speedBoost = false;
               ItemStack fuel = items.get(1);
               if (!fuel.isEmpty()) {
                  if (fuel.is(ModItems.ALUMENTUM.get())) speedBoost = true;
                  ItemStack remainder = fuel.getCraftingRemainingItem();
                  fuel.shrink(1);
                  if (fuel.isEmpty()) items.set(1, remainder);
               }
            } else {
               SmelterBlock.setFurnaceState(level, worldPosition, false);
            }
         } else {
            SmelterBlock.setFurnaceState(level, worldPosition, false);
         }
      }
      if (DeviceBlock.isEnabled(getBlockState()) && canSmelt()) {
         furnaceCookTime++;
         if (furnaceCookTime >= smeltTime) {
            furnaceCookTime = 0;
            smeltItem();
            changed = true;
         }
      } else {
         furnaceCookTime = 0;
      }
      if (wasBurning != furnaceBurnTime > 0) changed = true;
      if (changed) setChanged();
   }

   private boolean canSmelt() {
      if (items.get(0).isEmpty()) return false;
      AspectList al = AspectHelper.getObjectAspects(level, items.get(0));
      if (al == null || al.size() == 0) return false;
      int vs = al.visSize();
      if (vs > MAX_VIS - vis) return false;
      smeltTime = (int) (vs * 2 * (1.0F - 0.125F * bellows));
      return true;
   }

   public void checkNeighbours() {
      Direction f = facing();
      bellows = BellowsBlockEntity.getBellows(level, worldPosition,
         Direction.Plane.HORIZONTAL.stream().filter(d -> d != f).toArray(Direction[]::new));
   }

   private void smeltItem() {
      if (!canSmelt()) return;
      int flux = 0;
      AspectList al = AspectHelper.getObjectAspects(level, items.get(0)).copy();
      for (Aspect a : al.getAspects()) {
         int qq = al.getAmount(a);
         for (int q = 0; q < qq; q++) {
            if (level.random.nextFloat() > (a == Aspect.FLUX ? getEfficiency() * 0.66F : getEfficiency())) {
               al.reduce(a, 1);
               flux++;
            }
         }
         aspects.add(a, al.getAmount(a));
      }
      if (flux > 0) {
         int pp = 0;
         outer:
         for (int c = 0; c < flux; c++) {
            for (Direction face : Direction.Plane.HORIZONTAL) {
               if (facing() == face) continue;
               BlockState vent = level.getBlockState(worldPosition.relative(face));
               if (vent.is(ModBlocks.SMELTER_VENT.get()) && DeviceBlock.getFacing(vent) == face.getOpposite() && level.random.nextFloat() < 0.333) {
                  level.blockEvent(worldPosition, getBlockState().getBlock(), 1, face.getOpposite().get3DDataValue());
                  continue outer;
               }
            }
            pp++;
         }
         AuraHandler.addFlux(level, worldPosition, pp);
      }
      vis = aspects.visSize();
      items.get(0).shrink(1);
   }

   public boolean takeFromContainer(Aspect tag, int amount) {
      if (aspects.getAmount(tag) >= amount) {
         aspects.remove(tag, amount);
         vis = aspects.visSize();
         setChanged();
         return true;
      }
      return false;
   }

   @Override
   public boolean triggerEvent(int id, int param) {
      if (id == 1) {
         if (level.isClientSide) thaumcraft.client.fx.EssentiaFX.smelterVent(this, Direction.from3DDataValue(param));
         return true;
      }
      return super.triggerEvent(id, param);
   }

   // ---------------------------------------------------------------- inventory

   public static boolean isItemFuel(ItemStack stack) {
      return stack.getBurnTime(RecipeType.SMELTING) > 0;
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      if (slot == 0) {
         AspectList al = AspectHelper.getObjectAspects(level, stack);
         return al != null && al.size() > 0;
      }
      return slot == 1 && isItemFuel(stack);
   }

   @Override
   public int[] getSlotsForFace(Direction side) {
      return side == Direction.DOWN ? SLOTS_BOTTOM : side == Direction.UP ? SLOTS_TOP : SLOTS_SIDES;
   }

   @Override
   public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
      return side != Direction.UP && canPlaceItem(slot, stack);
   }

   @Override
   public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
      return side != Direction.UP || slot != 1 || stack.is(Items.BUCKET);
   }

   @Override
   public int getContainerSize() {
      return items.size();
   }

   @Override
   public boolean isEmpty() {
      return items.stream().allMatch(ItemStack::isEmpty);
   }

   @Override
   public ItemStack getItem(int slot) {
      return items.get(slot);
   }

   @Override
   public ItemStack removeItem(int slot, int amount) {
      ItemStack r = ContainerHelper.removeItem(items, slot, amount);
      if (!r.isEmpty()) setChanged();
      return r;
   }

   @Override
   public ItemStack removeItemNoUpdate(int slot) {
      return ContainerHelper.takeItem(items, slot);
   }

   @Override
   public void setItem(int slot, ItemStack stack) {
      items.set(slot, stack);
      stack.limitSize(getMaxStackSize(stack));
      setChanged();
   }

   @Override
   public boolean stillValid(Player player) {
      return net.minecraft.world.Container.stillValidBlockEntity(this, player);
   }

   @Override
   public void clearContent() {
      items.clear();
   }

   public NonNullList<ItemStack> getItems() {
      return items;
   }

   @Override
   public Component getDisplayName() {
      return getBlockState().getBlock().getName();
   }

   @Override
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new SmelterMenu(id, inv, this, data);
   }

   public int comparatorSignal() {
      return AbstractContainerMenu.getRedstoneSignalFromContainer(this);
   }

   static void playVent(Level level, BlockPos pos) {
      level.playLocalSound(pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.25F, 2.6F, true);
   }
}
