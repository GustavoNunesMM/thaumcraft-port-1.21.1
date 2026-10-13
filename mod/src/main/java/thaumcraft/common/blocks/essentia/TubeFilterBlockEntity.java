package thaumcraft.common.blocks.essentia;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.registers.ModBlockEntities;

/** TileTubeFilter: only passes on suction for one aspect (set with a phial, cleared with sneak + empty hand). */
public class TubeFilterBlockEntity extends TubeBlockEntity {
   @Nullable
   public Aspect aspectFilter;

   public TubeFilterBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.TUBE_FILTER.get(), pos, state);
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.readSync(tag, registries);
      aspectFilter = getAspect(tag, "AspectFilter");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.writeSync(tag, registries);
      putAspect(tag, "AspectFilter", aspectFilter);
   }

   @Override
   void calculateSuction(Aspect filter, boolean restrict, boolean dir) {
      super.calculateSuction(aspectFilter, restrict, dir);
   }
}
