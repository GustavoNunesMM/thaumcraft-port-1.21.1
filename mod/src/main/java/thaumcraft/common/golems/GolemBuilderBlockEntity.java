package thaumcraft.common.golems;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModGolems;
import thaumcraft.common.registers.ModSounds;

/**
 * TileGolemBuilder: once a design is started (its components taken from the inventories around or the player), the
 * press draws one machina essentia per cost point through tubes and then presses the golem into its output slot.
 */
public class GolemBuilderBlockEntity extends TCBlockEntity implements IEssentiaTransport, Container {
   public long golem = -1L;
   public int cost = 0;
   public int maxCost = 0;
   /** Client: which components the inventories around hold (sent on request by the screen). */
   @Nullable
   public boolean[] hasStuff = null;
   boolean bufferedEssentia = false;
   int ticks = 0;
   public int press = 0;
   private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

   public GolemBuilderBlockEntity(BlockPos pos, BlockState state) {
      super(ModGolems.GOLEM_BUILDER_BE.get(), pos, state);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      golem = tag.getLong("golem");
      cost = tag.getInt("cost");
      maxCost = tag.getInt("mcost");
      items.set(0, ItemStack.EMPTY);
      ContainerHelper.loadAllItems(tag, items, registries);
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putLong("golem", golem);
      tag.putInt("cost", cost);
      tag.putInt("mcost", maxCost);
      ContainerHelper.saveAllItems(tag, items, true, registries);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, GolemBuilderBlockEntity be) {
      be.ticks++;
      if (be.ticks % 5 != 0 || be.cost <= 0 || be.golem < 0L) return;
      if (be.bufferedEssentia || be.drawEssentia()) {
         be.bufferedEssentia = false;
         be.cost--;
         be.setChanged();
      }
      if (be.cost <= 0) {
         ItemStack placer = GolemPlacerItem.of(GolemProperties.fromLong(be.golem));
         ItemStack out = be.items.get(0);
         if (out.isEmpty() || out.getCount() < out.getMaxStackSize() && ItemStack.isSameItemSameComponents(out, placer)) {
            if (!out.isEmpty()) out.grow(1);
            else be.items.set(0, placer.copy());
            level.playSound(null, pos, ModSounds.WAND.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            be.cost = 0;
            be.golem = -1L;
            be.sync();
         }
      }
   }

   public static void clientTick(Level level, BlockPos pos, BlockState state, GolemBuilderBlockEntity be) {
      if (be.press < 90 && be.cost > 0 && be.golem > 0L) {
         be.press += 6;
         if (be.press >= 60) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.66F,
               1.0F + level.random.nextFloat() * 0.1F, false);
            GolemClientHooks.pressVent.accept(pos, 16);
         }
      }
      if (be.press >= 90 && level.random.nextInt(8) == 0) {
         GolemClientHooks.pressVent.accept(pos, 1);
         level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.1F,
            1.0F + level.random.nextFloat() * 0.1F, false);
      }
      if (be.press > 0 && (be.cost <= 0 || be.golem == -1L)) {
         if (be.press >= 90) GolemClientHooks.pressVent.accept(pos, 10);
         be.press -= 3;
      }
   }

   /** Which of the design's components the inventories around the press hold. */
   public boolean[] checkCraft(long id) {
      ItemStack[] cc = GolemProperties.fromLong(id).generateComponents();
      boolean[] ret = new boolean[cc.length];
      for (int a = 0; a < cc.length; a++) ret[a] = GolemInvHelper.checkAdjacentChests(level, worldPosition, cc[a]);
      return ret;
   }

   public boolean startCraft(long id, @Nullable Player p) {
      ItemStack placer = GolemPlacerItem.of(GolemProperties.fromLong(id));
      ItemStack out = items.get(0);
      if (!(out.isEmpty() || out.getCount() < out.getMaxStackSize() && ItemStack.isSameItemSameComponents(out, placer))) {
         cost = 0;
         golem = -1L;
         return false;
      }
      IGolemProperties props = GolemProperties.fromLong(id);
      ItemStack[] components = props.generateComponents();
      if (!GolemInvHelper.consumeItemsFromAdjacentInventoryOrPlayer(level, worldPosition, p, true, components)) {
         cost = 0;
         golem = -1L;
         return false;
      }
      golem = id;
      cost = props.getTraits().size() * 2;
      for (ItemStack stack : components) cost += stack.getCount();
      GolemInvHelper.consumeItemsFromAdjacentInventoryOrPlayer(level, worldPosition, p, false, components);
      maxCost = cost;
      sync();
      level.playSound(null, worldPosition, ModSounds.WAND.get(), SoundSource.BLOCKS, 0.25F, 1.0F);
      return true;
   }

   boolean drawEssentia() {
      for (Direction face : Direction.values()) {
         IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition, face);
         if (ic != null) {
            if (!ic.canOutputTo(face.getOpposite())) return false;
            if (ic.getSuctionAmount(face.getOpposite()) < getSuctionAmount(face) && ic.takeEssentia(Aspect.MECHANISM, 1, face.getOpposite()) == 1) {
               return true;
            }
         }
      }
      return false;
   }

   // ------------------------------------------------------------------ essentia

   @Override
   public boolean isConnectable(Direction face) {
      return face.getAxis().isHorizontal() || face == Direction.DOWN;
   }

   @Override
   public boolean canInputFrom(Direction face) {
      return isConnectable(face);
   }

   @Override
   public boolean canOutputTo(Direction face) {
      return false;
   }

   @Override
   public void setSuction(@Nullable Aspect aspect, int amount) {
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   @Override
   public Aspect getSuctionType(@Nullable Direction face) {
      return Aspect.MECHANISM;
   }

   @Override
   public int getSuctionAmount(@Nullable Direction face) {
      return cost > 0 && golem >= 0L ? 128 : 0;
   }

   @Override
   public Aspect getEssentiaType(@Nullable Direction loc) {
      return null;
   }

   @Override
   public int getEssentiaAmount(@Nullable Direction loc) {
      return 0;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, @Nullable Direction facing) {
      return 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, @Nullable Direction facing) {
      if (!bufferedEssentia && cost > 0 && golem >= 0L && aspect == Aspect.MECHANISM) {
         bufferedEssentia = true;
         return 1;
      }
      return 0;
   }

   // ------------------------------------------------------------------ the output slot

   @Override
   public int getContainerSize() {
      return 1;
   }

   @Override
   public boolean isEmpty() {
      return items.get(0).isEmpty();
   }

   @Override
   public ItemStack getItem(int slot) {
      return items.get(slot);
   }

   @Override
   public ItemStack removeItem(int slot, int amount) {
      ItemStack s = ContainerHelper.removeItem(items, slot, amount);
      if (!s.isEmpty()) sync();
      return s;
   }

   @Override
   public ItemStack removeItemNoUpdate(int slot) {
      return ContainerHelper.takeItem(items, slot);
   }

   @Override
   public void setItem(int slot, ItemStack stack) {
      items.set(slot, stack);
      sync();
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      return stack.getItem() instanceof GolemPlacerItem;
   }

   @Override
   public boolean stillValid(Player player) {
      return Container.stillValidBlockEntity(this, player);
   }

   @Override
   public void clearContent() {
      items.set(0, ItemStack.EMPTY);
   }
}
