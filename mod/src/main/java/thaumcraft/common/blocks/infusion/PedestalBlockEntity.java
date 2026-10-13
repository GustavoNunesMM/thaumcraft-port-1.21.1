package thaumcraft.common.blocks.infusion;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModBlockEntities;

/** TilePedestal: holds one item for infusion. */
public class PedestalBlockEntity extends TCBlockEntity implements Container {
   private ItemStack item = ItemStack.EMPTY;

   public PedestalBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.PEDESTAL.get(), pos, state);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      item = tag.contains("Item") ? ItemStack.parseOptional(registries, tag.getCompound("Item")) : ItemStack.EMPTY;
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      if (!item.isEmpty()) tag.put("Item", item.save(registries));
   }

   public ItemStack getItem() {
      return item;
   }

   public void setItemFromInfusion(ItemStack stack) {
      setItem(0, stack);
   }

   /** findInstabilityMitigator: a charged stabilizer reached through the redstone inlay from this pedestal. */
   @Nullable
   public BlockPos findInstabilityMitigator() {
      int charge = getBlockState().getValue(InlayBlock.CHARGE);
      return charge > 0 ? seekSourceRecursive(worldPosition, charge) : null;
   }

   @Nullable
   private BlockPos seekSourceRecursive(BlockPos pos, int lastCharge) {
      for (Direction face : Direction.Plane.HORIZONTAL) {
         BlockPos pp = pos.relative(face);
         if (InlayBlock.getSourceStrengthAt(level, pp) >= 5) return pp;
         BlockState bs = level.getBlockState(pp);
         if (bs.getBlock() instanceof InlayBlock && bs.getValue(InlayBlock.CHARGE) > lastCharge) {
            BlockPos ob = seekSourceRecursive(pp, bs.getValue(InlayBlock.CHARGE));
            if (ob != null) return ob;
         }
      }
      return null;
   }

   @Override
   public boolean triggerEvent(int id, int param) {
      if (id == 11 || id == 12 || id == 5) {
         if (level.isClientSide) thaumcraft.client.fx.InfusionFX.pedestalEvent(this, id);
         return true;
      }
      return super.triggerEvent(id, param);
   }

   // ---------------------------------------------------------------- Container

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
   public boolean canPlaceItem(int slot, ItemStack stack) {
      return item.isEmpty();
   }

   @Override
   public boolean stillValid(Player player) {
      return Container.stillValidBlockEntity(this, player);
   }

   @Override
   public void clearContent() {
      item = ItemStack.EMPTY;
   }
}
