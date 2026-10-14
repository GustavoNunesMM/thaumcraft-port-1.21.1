package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModSounds;

/**
 * BlockPatternCrafter: takes items from the inventory above, crafts them in one of ten fixed patterns (shown on its
 * face, cycled by clicking the little lever) and puts the result into the inventory below. Costs vis.
 */
public class PatternCrafterBlock extends DeviceBlock {
   public static final MapCodec<PatternCrafterBlock> CODEC = simpleCodec(PatternCrafterBlock::new);

   public PatternCrafterBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected Facing facingType() {
      return Facing.HORIZONTAL;
   }

   @Override
   protected boolean hasEnabled() {
      return true;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return super.getStateForPlacement(ctx).setValue(HORIZONTAL_FACING, ctx.getHorizontalDirection());
   }

   /** TilePatternCrafter.getCuboidByFacing, stretched out to the block's side so a click on that face finds it. */
   public static AABB knob(Direction facing) {
      return switch (facing) {
         case EAST -> new AABB(0.0, 0.125, 0.375, 0.25, 0.375, 0.625);
         case NORTH -> new AABB(0.375, 0.125, 0.75, 0.625, 0.375, 1.0);
         case SOUTH -> new AABB(0.375, 0.125, 0.0, 0.625, 0.375, 0.25);
         default -> new AABB(0.75, 0.125, 0.375, 1.0, 0.375, 0.625);
      };
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (LevitatorBlock.hits(hit, pos, knob(getFacing(state))) && level.getBlockEntity(pos) instanceof PatternCrafterBlockEntity tile) {
         if (!level.isClientSide) {
            tile.cycle();
            level.playSound(null, pos, ModSounds.KEY.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
         }
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      return InteractionResult.PASS;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new PatternCrafterBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModDevices.PATTERN_CRAFTER_BE.get(), PatternCrafterBlockEntity::tick);
   }
}
