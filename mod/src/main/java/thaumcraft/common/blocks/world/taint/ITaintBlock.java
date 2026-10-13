package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A taint block: dies (reverts or decays into goo) once no taint seed is near. */
public interface ITaintBlock {
   void die(Level level, BlockPos pos, BlockState state);
}
