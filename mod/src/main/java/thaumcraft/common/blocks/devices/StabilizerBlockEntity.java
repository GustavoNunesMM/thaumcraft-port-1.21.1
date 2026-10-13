package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.blocks.infusion.InlayBlock;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TileStabilizer: builds up to 15 energy (one a second, at the cost of a little flux) and spends it, through the
 * redstone inlay, to keep items on infusion pedestals. It also calms flux rifts nearby.
 */
public class StabilizerBlockEntity extends TCBlockEntity {
   public static final int CAPACITY = 15;
   private int ticks;
   protected int energy;
   private int lastEnergy = -1;

   public StabilizerBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.STABILIZER.get(), pos, state);
   }

   public int getEnergy() {
      return energy;
   }

   public static void tick(Level level, BlockPos pos, BlockState state, StabilizerBlockEntity be) {
      if (!level.isClientSide) {
         be.ticks++;
         if (be.energy < CAPACITY && be.ticks % 20 == 0) {
            be.energy++;
            AuraHandler.addFlux(level, pos, 0.25F);
            be.sync();
            be.notifyInlay();
         }
         // Milestone 12: calm the flux rifts within 8 blocks (TileStabilizer.tryAddStability).
         if (be.energy > 0 && be.delay <= 0 && be.ticks % 5 == 0) {
            int q = be.energy;
            be.tryAddStability();
            if (q != be.energy) be.sync();
         }
         if (be.delay > 0) be.delay--;
      } else if (be.energy != be.lastEnergy) {
         be.lastEnergy = be.energy;
         level.sendBlockUpdated(pos, state, state, 8);
      }
   }

   private int delay;

   private void tryAddStability() {
      for (thaumcraft.common.entities.FluxRiftEntity e : level.getEntitiesOfClass(thaumcraft.common.entities.FluxRiftEntity.class,
         new net.minecraft.world.phys.AABB(worldPosition).inflate(8.0))) {
         if (e.isAlive() && e.getStability() != thaumcraft.common.entities.FluxRiftEntity.Stability.VERY_STABLE && mitigate(1)) {
            e.addStability();
            delay += 5;
            if (energy <= 0) return;
         }
      }
   }

   private void notifyInlay() {
      for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) InlayBlock.updateCharge(level, worldPosition.relative(d));
   }

   public boolean mitigate(int e) {
      if (energy >= e) {
         energy -= e;
         sync();
         notifyInlay();
         return true;
      }
      return false;
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      energy = Math.min(tag.getInt("energy"), CAPACITY);
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putInt("energy", energy);
   }

   /** BlockStabilizer.colorMultiplier. */
   public static int color(int energy) {
      return InlayBlock.color(energy);
   }
}
