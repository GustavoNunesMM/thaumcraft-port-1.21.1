package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModDevices;

/**
 * BlockLamp: arcane lamp (lights the area with glimmers), lamp of growth (speeds up crops, fed herba) and lamp of
 * fertility (makes animals breed, fed desiderium). Hangs from the block it was placed against; redstone turns it off.
 */
public class LampBlock extends DeviceBlock {
   public enum Kind {
      ARCANE, GROWTH, FERTILITY
   }

   private static final VoxelShape SHAPE = box(4, 2, 4, 12, 14, 12);
   private final Kind kind;

   public LampBlock(Kind kind, Properties properties) {
      super(properties);
      this.kind = kind;
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return simpleCodec(p -> new LampBlock(kind, p));
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
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return defaultBlockState().setValue(FACING, ctx.getClickedFace().getOpposite()).setValue(ENABLED, false);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ArcaneLampBlockEntity lamp) lamp.removeLights();
      super.onRemove(state, level, pos, newState, moving);
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      if (level.isEmptyBlock(pos.relative(getFacing(state)))) {
         level.destroyBlock(pos, true);
         return;
      }
      BlockEntity te = level.getBlockEntity(pos);
      if (te instanceof ArcaneLampBlockEntity lamp && isEnabled(state) && level.hasNeighborSignal(pos)) lamp.removeLights();
      boolean checkUpdate = true;
      if (te instanceof GrowthLampBlockEntity g && g.charges <= 0) checkUpdate = false;
      if (te instanceof FertilityLampBlockEntity f && f.charges <= 0) checkUpdate = false;
      if (checkUpdate) super.neighborChanged(state, level, pos, block, fromPos, moving);
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
      // BlockTCDevice.onBlockAdded: the lamps light up unless powered.
      super.onPlace(state, level, pos, oldState, moving);
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return switch (kind) {
         case ARCANE -> new ArcaneLampBlockEntity(pos, state);
         case GROWTH -> new GrowthLampBlockEntity(pos, state);
         case FERTILITY -> new FertilityLampBlockEntity(pos, state);
      };
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      if (level.isClientSide) return null;
      return switch (kind) {
         case ARCANE -> createTickerHelper(type, ModDevices.LAMP_ARCANE_BE.get(), ArcaneLampBlockEntity::serverTick);
         case GROWTH -> createTickerHelper(type, ModDevices.LAMP_GROWTH_BE.get(), GrowthLampBlockEntity::serverTick);
         case FERTILITY -> createTickerHelper(type, ModDevices.LAMP_FERTILITY_BE.get(), FertilityLampBlockEntity::serverTick);
      };
   }
}
