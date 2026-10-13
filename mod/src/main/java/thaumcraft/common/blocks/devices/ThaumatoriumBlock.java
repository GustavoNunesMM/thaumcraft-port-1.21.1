package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModDevices;

/**
 * BlockThaumatorium: the two alchemical constructs on top of a heated crucible, made with salis mundus. The lower one
 * holds the catalyst and the chosen recipes, the top one only passes essentia and clicks down to it.
 */
public class ThaumatoriumBlock extends DeviceBlock {
   private final boolean top;

   public ThaumatoriumBlock(boolean top, Properties properties) {
      super(properties);
      this.top = top;
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return simpleCodec(p -> new ThaumatoriumBlock(top, p));
   }

   @Override
   protected Facing facingType() {
      return Facing.HORIZONTAL;
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return top ? RenderShape.INVISIBLE : RenderShape.MODEL;
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide && !player.isShiftKeyDown()) {
         BlockPos p = top ? pos.below() : pos;
         if (level.getBlockEntity(p) instanceof ThaumatoriumBlockEntity te) ((ServerPlayer) player).openMenu(te, p);
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      if (!state.is(newState.getBlock())) {
         if (!top && level.getBlockEntity(pos) instanceof ThaumatoriumBlockEntity te) net.minecraft.world.Containers.dropContents(level, pos, te);
         super.onRemove(state, level, pos, newState, moving);
         if (top && level.getBlockState(pos.below()).is(ModDevices.THAUMATORIUM.get())) {
            level.setBlockAndUpdate(pos.below(), ModDevices.METAL_ALCHEMICAL.get().defaultBlockState());
         }
         if (!top && level.getBlockState(pos.above()).is(ModDevices.THAUMATORIUM_TOP.get())) {
            level.setBlockAndUpdate(pos.above(), ModDevices.METAL_ALCHEMICAL.get().defaultBlockState());
         }
         return;
      }
      super.onRemove(state, level, pos, newState, moving);
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      if (!top && !level.getBlockState(pos.below()).is(ModBlocks.CRUCIBLE.get())) {
         level.setBlockAndUpdate(pos, ModDevices.METAL_ALCHEMICAL.get().defaultBlockState());
      }
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState state) {
      return !top;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof ThaumatoriumBlockEntity te ? AbstractContainerMenu.getRedstoneSignalFromContainer(te) : 0;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return top ? new ThaumatoriumTopBlockEntity(pos, state) : new ThaumatoriumBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return top ? null : createTickerHelper(type, ModDevices.THAUMATORIUM_BE.get(), ThaumatoriumBlockEntity::tick);
   }
}
