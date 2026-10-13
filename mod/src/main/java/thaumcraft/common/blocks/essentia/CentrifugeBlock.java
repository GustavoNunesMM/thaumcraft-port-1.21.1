package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.registers.ModBlockEntities;

/** BlockCentrifuge: drawn by its block entity renderer (the middle spins). */
public class CentrifugeBlock extends BaseEntityBlock {
   public static final MapCodec<CentrifugeBlock> CODEC = simpleCodec(CentrifugeBlock::new);

   public CentrifugeBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new CentrifugeBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModBlockEntities.CENTRIFUGE.get(), CentrifugeBlockEntity::tick);
   }
}
