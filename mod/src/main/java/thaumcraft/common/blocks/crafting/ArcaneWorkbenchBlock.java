package thaumcraft.common.blocks.crafting;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** BlockArcaneWorkbench: made by sprinkling salis mundus on a crafting table. */
public class ArcaneWorkbenchBlock extends BaseEntityBlock {
   public static final MapCodec<ArcaneWorkbenchBlock> CODEC = simpleCodec(ArcaneWorkbenchBlock::new);
   private static final VoxelShape SHAPE = Shapes.or(box(0, 8, 0, 16, 16, 16), box(0, 0, 0, 16, 4, 16), box(1, 4, 1, 5, 8, 5),
      box(11, 4, 1, 15, 8, 5), box(1, 4, 11, 5, 8, 15), box(11, 4, 11, 15, 8, 15));

   public ArcaneWorkbenchBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
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
      return new ArcaneWorkbenchBlockEntity(pos, state);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      return open(level, pos, player);
   }

   static InteractionResult open(Level level, BlockPos pos, Player player) {
      if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity be) {
         sp.openMenu(be, pos);
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ArcaneWorkbenchBlockEntity be) {
         Containers.dropContents(level, pos, be.inventory);
      }
      super.onRemove(state, level, pos, newState, moved);
   }
}
