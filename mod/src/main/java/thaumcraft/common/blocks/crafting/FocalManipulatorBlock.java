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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModCasters;

/** BlockFocalManipulator ("wand_workbench"): imprints focus packages into foci. Shaped like the arcane workbench. */
public class FocalManipulatorBlock extends BaseEntityBlock {
   public static final MapCodec<FocalManipulatorBlock> CODEC = simpleCodec(FocalManipulatorBlock::new);
   private static final VoxelShape SHAPE = Shapes.or(box(0, 8, 0, 16, 16, 16), box(0, 0, 0, 16, 4, 16), box(1, 4, 1, 5, 8, 5),
      box(11, 4, 1, 15, 8, 5), box(1, 4, 11, 5, 8, 15), box(11, 4, 11, 15, 8, 15));

   public FocalManipulatorBlock(Properties properties) {
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
      return new FocalManipulatorBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModCasters.FOCAL_MANIPULATOR_BE.get(), FocalManipulatorBlockEntity::tick);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof FocalManipulatorBlockEntity be) {
         sp.openMenu(be, pos);
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
      BlockEntity be = level.getBlockEntity(pos);
      return be != null && be.triggerEvent(id, param);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof FocalManipulatorBlockEntity be) {
         Containers.dropContents(level, pos, be.inventory);
      }
      super.onRemove(state, level, pos, newState, moved);
   }
}
