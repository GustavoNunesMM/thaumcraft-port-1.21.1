package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidType;
import thaumcraft.common.registers.ModFlux;

/**
 * The flux goo fluid (FluidFluxGoo), for fluid containers and tanks. In the world flux goo is the finite FluxGooBlock,
 * which has no fluid state of its own; this fluid never flows (its legacy block is a full puddle of goo).
 */
public class FluxGooFluid extends Fluid {
   @Override
   public FluidType getFluidType() {
      return ModFlux.FLUX_GOO_TYPE.get();
   }

   @Override
   public Item getBucket() {
      return ModFlux.BUCKET_GOO.get();
   }

   @Override
   protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
      return false;
   }

   @Override
   protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState state) {
      return Vec3.ZERO;
   }

   @Override
   public int getTickDelay(LevelReader level) {
      return 30;
   }

   @Override
   protected float getExplosionResistance() {
      return 100.0F;
   }

   @Override
   public float getHeight(FluidState state, BlockGetter level, BlockPos pos) {
      return 1.0F;
   }

   @Override
   public float getOwnHeight(FluidState state) {
      return 1.0F;
   }

   @Override
   protected BlockState createLegacyBlock(FluidState state) {
      return ModFlux.FLUX_GOO.get().defaultBlockState();
   }

   @Override
   public boolean isSource(FluidState state) {
      return true;
   }

   @Override
   public int getAmount(FluidState state) {
      return 8;
   }

   @Override
   public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
      return Shapes.block();
   }
}
