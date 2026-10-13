package thaumcraft.common.blocks.crafting;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.menu.ArcaneWorkbenchMenu;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

/** TileArcaneWorkbench: keeps the 3x3 grid + 6 crystal slots and reads/spends the vis of the aura. */
public class ArcaneWorkbenchBlockEntity extends BlockEntity implements MenuProvider {
   /** Slots 0-8: crafting grid, 9-14: crystals (aer, ignis, aqua, terra, ordo, perditio). */
   public final SimpleContainer inventory = new SimpleContainer(15) {
      @Override
      public void setChanged() {
         super.setChanged();
         ArcaneWorkbenchBlockEntity.this.setChanged();
      }
   };
   public int auraVisServer;

   public ArcaneWorkbenchBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.ARCANE_WORKBENCH.get(), pos, state);
   }

   public boolean hasCharger() {
      return level != null && level.getBlockState(worldPosition.above()).is(ModBlocks.ARCANE_WORKBENCH_CHARGER.get());
   }

   /** Vis available: the chunk's, or with a charger on top the sum of the 3x3 chunks around. */
   public int getAura() {
      if (level == null || level.isClientSide) return auraVisServer;
      int t = 0;
      if (!hasCharger()) {
         t = (int) AuraHandler.getVis(level, worldPosition);
      } else {
         int sx = worldPosition.getX() >> 4, sz = worldPosition.getZ() >> 4;
         for (int xx = -1; xx <= 1; xx++) {
            for (int zz = -1; zz <= 1; zz++) {
               AuraChunk ac = AuraHandler.getAuraChunk(level, sx + xx, sz + zz);
               if (ac != null) t += (int) ac.getVis();
            }
         }
      }
      return auraVisServer = t;
   }

   public void spendAura(int vis) {
      if (level == null || level.isClientSide) return;
      if (hasCharger()) {
         int q = vis;
         int z = Math.max(1, vis / 9);
         int attempts = 0;
         while (q > 0) {
            attempts++;
            for (int xx = -1; xx <= 1; xx++) {
               for (int zz = -1; zz <= 1; zz++) {
                  if (z > q) z = q;
                  q -= (int) AuraHandler.drainVis(level, worldPosition.offset(xx * 16, 0, zz * 16), z, false);
                  if (q <= 0 || attempts > 1000) return;
               }
            }
         }
      } else {
         AuraHandler.drainVis(level, worldPosition, vis, false);
      }
   }

   @Override
   protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      NonNullList<ItemStack> stacks = NonNullList.withSize(inventory.getContainerSize(), ItemStack.EMPTY);
      ContainerHelper.loadAllItems(tag, stacks, registries);
      for (int i = 0; i < stacks.size(); i++) inventory.setItem(i, stacks.get(i));
   }

   @Override
   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      ContainerHelper.saveAllItems(tag, inventory.getItems(), registries);
   }

   @Override
   public Component getDisplayName() {
      return Component.translatable("block.thaumcraft.arcane_workbench");
   }

   @Nullable
   @Override
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new ArcaneWorkbenchMenu(id, inv, this);
   }
}
