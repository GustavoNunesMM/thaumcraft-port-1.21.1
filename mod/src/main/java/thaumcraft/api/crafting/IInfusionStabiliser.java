package thaumcraft.api.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Blocks that steady an infusion when placed symmetrically around the matrix (candles, stabilizers, pedestals...). */
public interface IInfusionStabiliser {
   boolean canStabaliseInfusion(Level level, BlockPos pos);

   /** How much stability a symmetric pair adds per cycle (0.1 for blocks that don't say). */
   default float getStabilizationAmount(Level level, BlockPos pos) {
      return 0.1F;
   }

   default boolean hasSymmetryPenalty(Level level, BlockPos pos1, BlockPos pos2) {
      return false;
   }

   default float getSymmetryPenalty(Level level, BlockPos pos) {
      return 0.0F;
   }
}
