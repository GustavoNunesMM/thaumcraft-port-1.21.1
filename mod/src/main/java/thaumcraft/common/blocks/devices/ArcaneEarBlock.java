package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModDevices;

/**
 * BlockArcaneEar: placed on a block's side, it listens for note blocks playing its tone (the instrument of the block it
 * hangs on) and note within 64 blocks, and gives a short redstone pulse (or toggles, for the toggle ear).
 */
public class ArcaneEarBlock extends DeviceBlock {
   private static final VoxelShape[] SHAPES = {
      box(2, 10, 2, 14, 16, 14), box(2, 0, 2, 14, 6, 14), box(2, 2, 10, 14, 14, 16),
      box(2, 2, 0, 14, 14, 6), box(10, 2, 2, 16, 14, 14), box(0, 2, 2, 6, 14, 14)};
   public final boolean toggle;

   public ArcaneEarBlock(boolean toggle, Properties properties) {
      super(properties);
      this.toggle = toggle;
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return simpleCodec(p -> new ArcaneEarBlock(toggle, p));
   }

   @Override
   protected Facing facingType() {
      return Facing.ALL;
   }

   @Override
   protected boolean hasEnabled() {
      return true;
   }

   @Override
   protected boolean redstoneDisables() {
      return false;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPES[getFacing(state).get3DDataValue()];
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockState bs = defaultBlockState().setValue(FACING, ctx.getClickedFace()).setValue(ENABLED, false);
      return canSurvive(bs, ctx.getLevel(), ctx.getClickedPos()) ? bs : null;
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      Direction f = getFacing(state);
      BlockPos behind = pos.relative(f.getOpposite());
      return level.getBlockState(behind).isFaceSturdy(level, behind, f);
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
      return dir == getFacing(state).getOpposite() && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
         : super.updateShape(state, dir, other, level, pos, otherPos);
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
      super.onPlace(state, level, pos, oldState, moving);
      if (level.getBlockEntity(pos) instanceof ArcaneEarBlockEntity tile) tile.updateTone();
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      if (level.getBlockEntity(pos) instanceof ArcaneEarBlockEntity tile) tile.updateTone();
      super.neighborChanged(state, level, pos, block, fromPos, moving);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof ArcaneEarBlockEntity tile) {
         tile.changePitch();
         tile.triggerNote(level, pos, true);
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   protected boolean isSignalSource(BlockState state) {
      return true;
   }

   @Override
   protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
      return isEnabled(state) ? 15 : 0;
   }

   @Override
   protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
      return isEnabled(state) ? 15 : 0;
   }

   /** eventReceived: plays the note (id = instrument, param = note). */
   @Override
   protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
      NoteBlockInstrument[] all = NoteBlockInstrument.values();
      NoteBlockInstrument inst = id >= 0 && id < all.length ? all[id] : NoteBlockInstrument.HARP;
      float pitch = (float) Math.pow(2.0, (param - 12) / 12.0);
      if (!level.isClientSide) {
         level.playSound(null, pos, inst.getSoundEvent().value(), SoundSource.BLOCKS, 3.0F, pitch);
      } else {
         level.addParticle(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, param / 24.0, 0.0, 0.0);
      }
      return true;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new ArcaneEarBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, ModDevices.ARCANE_EAR_BE.get(), ArcaneEarBlockEntity::serverTick);
   }
}
