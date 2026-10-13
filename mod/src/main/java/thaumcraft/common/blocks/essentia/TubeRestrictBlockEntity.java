package thaumcraft.common.blocks.essentia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.registers.ModBlockEntities;

/** TileTubeRestrict: passes on only half the suction. */
public class TubeRestrictBlockEntity extends TubeBlockEntity {
   public TubeRestrictBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.TUBE_RESTRICT.get(), pos, state);
   }

   @Override
   void calculateSuction(Aspect filter, boolean restrict, boolean dir) {
      super.calculateSuction(filter, true, dir);
   }
}
