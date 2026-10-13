package thaumcraft.common.blocks.infusion;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModBlockEntities;

/** BlockInfusionMatrix: drawn by its block entity renderer (eight floating cubes). */
public class InfusionMatrixBlock extends BaseEntityBlock {
   public static final MapCodec<InfusionMatrixBlock> CODEC = simpleCodec(InfusionMatrixBlock::new);
   private static final VoxelShape SHAPE = box(1, 1, 1, 15, 15, 15);

   public InfusionMatrixBlock(Properties properties) {
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
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new InfusionMatrixBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModBlockEntities.INFUSION_MATRIX.get(), InfusionMatrixBlockEntity::tick);
   }
}
