package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModDevices;

/** BlockDioptra: shows the vis (enabled) or flux (disabled) of the 13x13 chunks around it; click to switch. */
public class DioptraBlock extends DeviceBlock {
   public static final MapCodec<DioptraBlock> CODEC = simpleCodec(DioptraBlock::new);

   public DioptraBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected boolean hasEnabled() {
      return true;
   }

   @Override
   protected boolean redstoneDisables() {
      return false;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return defaultBlockState();
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      level.setBlock(pos, state.setValue(ENABLED, !state.getValue(ENABLED)), 3);
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof DioptraBlockEntity tile) {
         float r = tile.gridAmt[84] / 64.0F;
         return Mth.floor(r * 14.0F) + (r > 0.0F ? 1 : 0);
      }
      return 0;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new DioptraBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModDevices.DIOPTRA_BE.get(), DioptraBlockEntity::tick);
   }
}
