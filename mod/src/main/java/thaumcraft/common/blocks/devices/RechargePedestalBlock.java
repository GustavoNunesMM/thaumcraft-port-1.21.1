package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.items.IRechargable;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModDevices;

/** BlockRechargePedestal: put a rechargeable item on it (click), take it back with another click. */
public class RechargePedestalBlock extends DeviceBlock {
   public static final MapCodec<RechargePedestalBlock> CODEC = simpleCodec(RechargePedestalBlock::new);

   public RechargePedestalBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit) {
      if (!(level.getBlockEntity(pos) instanceof RechargePedestalBlockEntity ped)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      if (level.isClientSide) return ItemInteractionResult.SUCCESS;
      if (ped.getItem().isEmpty() && stack.getItem() instanceof IRechargable) {
         ped.setItem(0, stack.copyWithCount(1));
         stack.shrink(1);
         level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
            ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 1.6F);
         return ItemInteractionResult.SUCCESS;
      }
      if (!ped.getItem().isEmpty()) {
         dropAtPlayer(level, ped, player);
         level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
            ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 1.5F);
         return ItemInteractionResult.SUCCESS;
      }
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   /** InventoryUtils.dropItemsAtEntity. */
   private static void dropAtPlayer(Level level, RechargePedestalBlockEntity ped, Player player) {
      ItemStack out = ped.removeItemNoUpdate(0);
      ped.sync();
      ItemEntity e = new ItemEntity(level, player.getX(), player.getY() + player.getEyeHeight() / 2.0F, player.getZ(), out);
      e.setNoPickUpDelay();
      level.addFreshEntity(e);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RechargePedestalBlockEntity ped) {
         Containers.dropContents(level, pos, ped);
      }
      super.onRemove(state, level, pos, newState, moving);
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new RechargePedestalBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, ModDevices.RECHARGE_PEDESTAL_BE.get(), RechargePedestalBlockEntity::serverTick);
   }
}
