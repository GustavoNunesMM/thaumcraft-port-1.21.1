package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.crafting.IInfusionStabiliser;

/** BlockCandle: tallow candles, 16 colours; light and steady infusions. */
public class CandleBlock extends Block implements IInfusionStabiliser {
   private static final VoxelShape SHAPE = box(6, 0, 6, 10, 8, 10);
   public final DyeColor dye;

   public CandleBlock(DyeColor dye, Properties properties) {
      super(properties);
      this.dye = dye;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return net.minecraft.world.phys.shapes.Shapes.empty();
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 0, 0, 0);
      level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 0, 0, 0);
   }

   @Override
   public boolean canStabaliseInfusion(Level level, BlockPos pos) {
      return true;
   }

   @Override
   public float getStabilizationAmount(Level level, BlockPos pos) {
      return 0.1F;
   }
}
