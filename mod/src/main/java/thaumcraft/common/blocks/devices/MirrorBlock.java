package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModDevices;

/**
 * BlockMirror: the magic mirror (items thrown in come out of the linked mirror) and the essentia mirror (machines near
 * one can use the jars near the other). Linked by using a mirror item on a placed mirror, then placing the linked item.
 */
public class MirrorBlock extends DeviceBlock {
   private static final VoxelShape[] SHAPES = {
      box(0, 14, 0, 16, 16, 16), box(0, 0, 0, 16, 2, 16), box(0, 0, 14, 16, 16, 16),
      box(0, 0, 0, 16, 16, 2), box(14, 0, 0, 16, 16, 16), box(0, 0, 0, 2, 16, 16)};
   public final boolean essentia;

   public MirrorBlock(boolean essentia, Properties properties) {
      super(properties);
      this.essentia = essentia;
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return simpleCodec(p -> new MirrorBlock(essentia, p));
   }

   @Override
   protected Facing facingType() {
      return Facing.ALL;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPES[getFacing(state).get3DDataValue()];
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockState bs = defaultBlockState().setValue(FACING, ctx.getClickedFace());
      return canSurvive(bs, ctx.getLevel(), ctx.getClickedPos()) ? bs : null;
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      Direction d = getFacing(state);
      BlockPos behind = pos.relative(d.getOpposite());
      return level.getBlockState(behind).isFaceSturdy(level, behind, d);
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
      return dir == getFacing(state).getOpposite() && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, dir, other, level, pos, otherPos);
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof MirrorBlockEntity m) m.restoreLink();
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof MirrorBlockEntity m && m.linked) {
         m.invalidateLink();
      }
      super.onRemove(state, level, pos, newState, moving);
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      if (!level.isClientSide && !essentia && entity instanceof ItemEntity item && item.isAlive() && item.getPortalCooldown() == 0
         && level.getBlockEntity(pos) instanceof MirrorBlockEntity m) {
         m.transport(item);
      }
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return essentia ? new EssentiaMirrorBlockEntity(pos, state) : new MirrorBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      if (level.isClientSide) return null;
      return essentia ? createTickerHelper(type, ModDevices.MIRROR_ESSENTIA_BE.get(), MirrorBlockEntity::serverTick)
         : createTickerHelper(type, ModDevices.MIRROR_BE.get(), MirrorBlockEntity::serverTick);
   }
}
