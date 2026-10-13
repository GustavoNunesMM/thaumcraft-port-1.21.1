package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.registers.ModBlockEntities;

/** TileTubeOneway: suction only comes from the side it faces, and essentia only from the opposite side. */
public class TubeOnewayBlockEntity extends TubeBlockEntity {
   public TubeOnewayBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.TUBE_ONEWAY.get(), pos, state);
   }

   @Override
   void calculateSuction(Aspect filter, boolean restrict, boolean directional) {
      super.calculateSuction(filter, restrict, true);
   }

   @Override
   void equalizeWithNeighbours(boolean directional) {
      super.equalizeWithNeighbours(true);
   }
}
