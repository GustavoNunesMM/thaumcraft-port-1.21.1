package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModDevices;

/** BlockSpa (arcane spa): click with a fluid container to fill it, otherwise opens its GUI. */
public class SpaBlock extends DeviceBlock {
   public static final MapCodec<SpaBlock> CODEC = simpleCodec(SpaBlock::new);

   public SpaBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit) {
      if (level.isClientSide) return ItemInteractionResult.SUCCESS;
      if (!(level.getBlockEntity(pos) instanceof SpaBlockEntity tile) || player.isShiftKeyDown()) return ItemInteractionResult.SUCCESS;
      Optional<FluidStack> fs = FluidUtil.getFluidContained(stack);
      if (fs.isPresent()) {
         if (tile.tank.getFluidAmount() < tile.tank.getCapacity()
            && (tile.tank.getFluid().isEmpty() || FluidStack.isSameFluidSameComponents(tile.tank.getFluid(), fs.get()))
            && FluidUtil.interactWithFluidHandler(player, hand, tile.tank)) {
            tile.sync();
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.33F, 1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.3F);
         }
      } else {
         ((ServerPlayer) player).openMenu(tile, pos);
      }
      return ItemInteractionResult.SUCCESS;
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SpaBlockEntity te) Containers.dropContents(level, pos, te);
      super.onRemove(state, level, pos, newState, moving);
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SpaBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, ModDevices.SPA_BE.get(), SpaBlockEntity::serverTick);
   }
}
