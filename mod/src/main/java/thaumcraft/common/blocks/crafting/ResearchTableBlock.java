package thaumcraft.common.blocks.crafting;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** BlockResearchTable: where theories are written (opens the research table screen). */
public class ResearchTableBlock extends BaseEntityBlock {
   public static final MapCodec<ResearchTableBlock> CODEC = simpleCodec(ResearchTableBlock::new);
   private static final VoxelShape SHAPE = Shapes.or(box(0, 10, 0, 16, 16, 16), box(1, 0, 1, 5, 10, 5), box(11, 0, 1, 15, 10, 5),
      box(1, 0, 11, 5, 10, 15), box(11, 0, 11, 15, 10, 15));

   public ResearchTableBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.NORTH));
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(HorizontalDirectionalBlock.FACING);
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, ctx.getHorizontalDirection());
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ResearchTableBlockEntity(pos, state);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof ResearchTableBlockEntity be) {
         sp.openMenu(be, pos);
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ResearchTableBlockEntity be) {
         for (int i = 0; i < be.getItems().getSlots(); i++) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), be.getItems().getStackInSlot(i));
      }
      super.onRemove(state, level, pos, newState, moved);
   }

   /** Motes drifting down onto the table while a theory is being written (FXGeneric in the original). */
   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      if (rand.nextInt(5) == 0 && level.getBlockEntity(pos) instanceof ResearchTableBlockEntity be && be.data != null) {
         double xx = rand.nextGaussian() / 2.0, zz = rand.nextGaussian() / 2.0, yy = 1.5 + rand.nextFloat();
         int a = 40 + rand.nextInt(20);
         Vector3f c = new Vector3f(0.5F + rand.nextFloat() * 0.5F, 0.5F + rand.nextFloat() * 0.5F, 0.5F + rand.nextFloat() * 0.5F);
         level.addParticle(new DustParticleOptions(c, 0.6F), pos.getX() + 0.5 + xx, pos.getY() + yy, pos.getZ() + 0.5 + zz,
            -xx / a * 20, -(yy - 0.85) / a * 20, -zz / a * 20);
      }
   }
}
