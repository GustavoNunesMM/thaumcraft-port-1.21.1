package thaumcraft.common.blocks.devices;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;

/** TileRechargePedestal: every 10 ticks pours up to 5 vis from the aura into the rechargeable item on it. */
public class RechargePedestalBlockEntity extends TCBlockEntity implements WorldlyContainer, IAspectContainer {
   private static final int[] SLOTS = {0};
   private ItemStack item = ItemStack.EMPTY;
   private int counter = 0;

   public RechargePedestalBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.RECHARGE_PEDESTAL_BE.get(), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, RechargePedestalBlockEntity be) {
      if (be.counter++ % 10 == 0 && !be.item.isEmpty() && RechargeHelper.rechargeItem(level, be.item, pos, null, 5) > 0.0F) {
         be.sync();
         List<Aspect> al = Aspect.getPrimalAspects();
         level.blockEvent(pos, state.getBlock(), 5, al.get(level.random.nextInt(al.size())).getColor());
      }
   }

   @Override
   public boolean triggerEvent(int id, int param) {
      if (id == 5) {
         if (level.isClientSide) {
            var r = level.random;
            thaumcraft.client.fx.DeviceFX.visSparkle(worldPosition.getX() + r.nextInt(3) - r.nextInt(3), worldPosition.above().getY() + r.nextInt(3),
               worldPosition.getZ() + r.nextInt(3) - r.nextInt(3), worldPosition.getX(), worldPosition.above().getY(), worldPosition.getZ(), param);
         }
         return true;
      }
      return super.triggerEvent(id, param);
   }

   public ItemStack getItem() {
      return item;
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      item = tag.contains("Item") ? ItemStack.parseOptional(registries, tag.getCompound("Item")) : ItemStack.EMPTY;
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      if (!item.isEmpty()) tag.put("Item", item.save(registries));
   }

   // ---------------------------------------------------------------- container

   @Override
   public int getContainerSize() {
      return 1;
   }

   @Override
   public boolean isEmpty() {
      return item.isEmpty();
   }

   @Override
   public ItemStack getItem(int slot) {
      return item;
   }

   @Override
   public ItemStack removeItem(int slot, int amount) {
      ItemStack out = item.split(amount);
      sync();
      return out;
   }

   @Override
   public ItemStack removeItemNoUpdate(int slot) {
      ItemStack out = item;
      item = ItemStack.EMPTY;
      return out;
   }

   @Override
   public void setItem(int slot, ItemStack stack) {
      item = stack;
      sync();
   }

   @Override
   public int getMaxStackSize() {
      return 1;
   }

   @Override
   public boolean stillValid(Player player) {
      return Container.stillValidBlockEntity(this, player);
   }

   @Override
   public void clearContent() {
      item = ItemStack.EMPTY;
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      return stack.getItem() instanceof IRechargable;
   }

   @Override
   public int[] getSlotsForFace(Direction side) {
      return SLOTS;
   }

   @Override
   public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
      return stack.getItem() instanceof IRechargable;
   }

   @Override
   public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
      return true;
   }

   // ---------------------------------------------------------------- aspects: the charge shows as potentia

   @Override
   public AspectList getAspects() {
      if (item.getItem() instanceof IRechargable) return new AspectList().add(Aspect.ENERGY, RechargeHelper.getCharge(item));
      return null;
   }

   @Override
   public void setAspects(AspectList aspects) {
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   @Override
   public int addToContainer(Aspect tag, int amount) {
      return 0;
   }

   @Override
   public boolean takeFromContainer(Aspect tag, int amount) {
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tag, int amount) {
      return false;
   }

   @Override
   public int containerContains(Aspect tag) {
      return 0;
   }
}
