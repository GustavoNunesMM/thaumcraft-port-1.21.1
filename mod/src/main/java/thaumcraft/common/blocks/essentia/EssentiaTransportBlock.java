package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModBlockEntities;

/** BlockEssentiaTransport: the essentia transfusers; they face away from the tube they are attached to. */
public class EssentiaTransportBlock extends DeviceBlock {
   public static final MapCodec<EssentiaTransportBlock> CODEC = simpleCodec(p -> new EssentiaTransportBlock(true, p));
   final boolean input;

   public EssentiaTransportBlock(boolean input, Properties properties) {
      super(properties);
      this.input = input;
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected Facing facingType() {
      return Facing.ALL;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return defaultBlockState().setValue(FACING, ctx.getClickedFace());
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return switch (getFacing(state)) {
         case UP -> box(4, 0, 4, 12, 8, 12);
         case NORTH -> box(4, 4, 8, 12, 12, 16);
         case SOUTH -> box(4, 4, 0, 12, 12, 8);
         case WEST -> box(8, 4, 4, 16, 12, 12);
         case EAST -> box(0, 4, 4, 8, 12, 12);
         default -> box(4, 8, 4, 12, 16, 12);
      };
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new EssentiaTransportBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ESSENTIA_TRANSPORT.get(), EssentiaTransportBlockEntity::tick);
   }
}
