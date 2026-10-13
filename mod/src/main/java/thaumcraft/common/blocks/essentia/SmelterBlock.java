package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.world.aura.AuraHandler;

/** BlockSmelter: "enabled" means lit here; it faces the player who placed it. */
public class SmelterBlock extends DeviceBlock {
   public static final MapCodec<SmelterBlock> CODEC = simpleCodec(SmelterBlock::new);

   public SmelterBlock(Properties properties) {
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

   @Override
   protected boolean redstoneDisables() {
      return false;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
      return defaultBlockState().setValue(HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite()).setValue(ENABLED, false);
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new SmelterBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SMELTER.get(), SmelterBlockEntity::serverTick);
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      if (level.getBlockEntity(pos) instanceof SmelterBlockEntity te) te.checkNeighbours();
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!level.isClientSide && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof SmelterBlockEntity te) {
         ((ServerPlayer) player).openMenu(te, pos);
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   public static void setFurnaceState(Level level, BlockPos pos, boolean lit) {
      BlockState state = level.getBlockState(pos);
      if (state.hasProperty(ENABLED) && state.getValue(ENABLED) != lit) level.setBlock(pos, state.setValue(ENABLED, lit), 3);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SmelterBlockEntity te) {
         if (te.vis > 0) AuraHandler.addFlux(level, pos, te.vis);
         Containers.dropContents(level, pos, te);
      }
      super.onRemove(state, level, pos, newState, moved);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof SmelterBlockEntity te ? te.comparatorSignal() : 0;
   }

   @Override
   public void animateTick(BlockState state, Level w, BlockPos pos, RandomSource r) {
      if (!isEnabled(state)) return;
      float f = pos.getX() + 0.5F;
      float f1 = pos.getY() + 0.2F + r.nextFloat() * 5.0F / 16.0F;
      float f2 = pos.getZ() + 0.5F;
      float f3 = 0.52F;
      float f4 = r.nextFloat() * 0.5F - 0.25F;
      var d = getFacing(state);
      double x = f + d.getStepX() * f3 + (d.getStepX() == 0 ? f4 : 0);
      double z = f2 + d.getStepZ() * f3 + (d.getStepZ() == 0 ? f4 : 0);
      w.addParticle(ParticleTypes.SMOKE, x, f1, z, 0, 0, 0);
      w.addParticle(ParticleTypes.FLAME, x, f1, z, 0, 0, 0);
   }
}
