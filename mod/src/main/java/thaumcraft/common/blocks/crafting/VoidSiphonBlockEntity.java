package thaumcraft.common.blocks.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.entities.FluxRiftEntity;
import thaumcraft.common.menu.VoidSiphonMenu;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModItems;

/**
 * TileVoidSiphon: leeches the rifts it can see within 8 blocks (making them less stable and slowly smaller) and turns
 * the energy into void seeds (one per 2000 progress, sqrt(rift size) per second per rift).
 */
public class VoidSiphonBlockEntity extends TCBlockEntity implements MenuProvider {
   public static final int PROGREQ = 2000;
   private final SimpleContainer inventory = new SimpleContainer(1) {
      @Override
      public boolean canPlaceItem(int slot, ItemStack stack) {
         return stack.is(ModItems.VOID_SEED.get());
      }

      @Override
      public void setChanged() {
         super.setChanged();
         VoidSiphonBlockEntity.this.setChanged();
      }
   };
   private int counter;
   public int progress;
   public final ContainerData data = new ContainerData() {
      @Override
      public int get(int index) {
         return progress;
      }

      @Override
      public void set(int index, int value) {
         progress = value;
      }

      @Override
      public int getCount() {
         return 1;
      }
   };

   public VoidSiphonBlockEntity(BlockPos pos, BlockState state) {
      super(ModFlux.VOID_SIPHON_BE.get(), pos, state);
   }

   public SimpleContainer getContainer() {
      return inventory;
   }

   /** Automation may only take seeds out (canInsertItem false). */
   public IItemHandler itemHandler() {
      return new InvWrapper(inventory) {
         @Override
         public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
         }
      };
   }

   private boolean hasRoom() {
      ItemStack s = inventory.getItem(0);
      return s.isEmpty() || s.is(ModItems.VOID_SEED.get()) && s.getCount() < s.getMaxStackSize();
   }

   public static void tick(Level level, BlockPos pos, BlockState state, VoidSiphonBlockEntity be) {
      be.counter++;
      if (level.isClientSide || !DeviceBlock.isEnabled(state) || be.counter % 20 != 0 || be.progress >= PROGREQ || !be.hasRoom()) return;
      boolean b = false;
      for (FluxRiftEntity fr : be.getValidRifts()) {
         double d = Math.sqrt(fr.getRiftSize());
         be.progress = (int) (be.progress + d);
         fr.setRiftStability((float) (fr.getRiftStability() - d / 15.0));
         if (level.random.nextInt(33) == 0) fr.setRiftSize(fr.getRiftSize() - 1);
         b = d >= 1.0;
      }
      if (b && be.counter % 40 == 0) level.blockEvent(pos, state.getBlock(), 5, be.counter);
      b = false;
      while (be.progress >= PROGREQ && be.hasRoom()) {
         be.progress -= PROGREQ;
         ItemStack s = be.inventory.getItem(0);
         if (s.isEmpty()) be.inventory.setItem(0, new ItemStack(ModItems.VOID_SEED.get()));
         else s.grow(1);
         b = true;
      }
      if (b) be.sync();
      else be.setChanged();
   }

   public List<FluxRiftEntity> getValidRifts() {
      List<FluxRiftEntity> ret = new ArrayList<>();
      for (FluxRiftEntity fr : level.getEntitiesOfClass(FluxRiftEntity.class, new AABB(worldPosition).inflate(8.0))) {
         if (!fr.isAlive() || fr.getRiftSize() < 2) continue;
         Vec3 v1 = new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5);
         Vec3 v2 = fr.position();
         v1 = v1.add(v2.subtract(v1).normalize());
         if (level.clip(new ClipContext(v1, v2, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, fr)).getType() == HitResult.Type.MISS) ret.add(fr);
      }
      return ret;
   }

   /** receiveClientEvent 5: void streaks from the rifts being drained. */
   @Override
   public boolean triggerEvent(int id, int param) {
      if (id != 5) return super.triggerEvent(id, param);
      if (level != null && level.isClientSide) {
         for (FluxRiftEntity fr : getValidRifts()) {
            thaumcraft.client.fx.TaintFX.voidStreak(fr.getX(), fr.getY(), fr.getZ(), worldPosition.getX() + 0.5, worldPosition.getY() + 0.5625,
               worldPosition.getZ() + 0.5, param, 0.04F);
         }
      }
      return true;
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      progress = tag.getShort("progress");
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putShort("progress", (short) progress);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      inventory.setItem(0, ItemStack.EMPTY);
      var list = net.minecraft.core.NonNullList.withSize(1, ItemStack.EMPTY);
      ContainerHelper.loadAllItems(tag, list, registries);
      inventory.setItem(0, list.get(0));
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      ContainerHelper.saveAllItems(tag, net.minecraft.core.NonNullList.of(ItemStack.EMPTY, inventory.getItem(0)), registries);
   }

   @Override
   public Component getDisplayName() {
      return Component.translatable("block.thaumcraft.void_siphon");
   }

   @Override
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new VoidSiphonMenu(id, inv, inventory, data, this);
   }
}
