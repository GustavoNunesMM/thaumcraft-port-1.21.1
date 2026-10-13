package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModSounds;

/** BlockLevitator: pushes entities away from its face; clicking the little knob at its back cycles the range. */
public class LevitatorBlock extends DeviceBlock {
   public static final MapCodec<LevitatorBlock> CODEC = simpleCodec(LevitatorBlock::new);
   private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

   static {
      for (Direction facing : Direction.values()) {
         double f = 0.125;
         VoxelShape body = Shapes.box(facing.getStepX() > 0 ? f : 0, facing.getStepY() > 0 ? f : 0, facing.getStepZ() > 0 ? f : 0,
            1 - (facing.getStepX() < 0 ? f : 0), 1 - (facing.getStepY() < 0 ? f : 0), 1 - (facing.getStepZ() < 0 ? f : 0));
         SHAPES.put(facing, Shapes.or(body, Shapes.create(knob(facing))));
      }
   }

   public LevitatorBlock(Properties properties) {
      super(properties);
   }

   /** TileLevitator.getCuboidByFacing: the range knob, block-relative. */
   public static AABB knob(Direction facing) {
      return switch (facing) {
         case DOWN -> new AABB(0.375, 0.875, 0.375, 0.625, 0.9375, 0.625);
         case EAST -> new AABB(0.0625, 0.375, 0.375, 0.125, 0.625, 0.625);
         case WEST -> new AABB(0.875, 0.375, 0.375, 0.9375, 0.625, 0.625);
         case SOUTH -> new AABB(0.375, 0.375, 0.0625, 0.625, 0.625, 0.125);
         case NORTH -> new AABB(0.375, 0.375, 0.875, 0.625, 0.625, 0.9375);
         default -> new AABB(0.375, 0.0625, 0.375, 0.625, 0.125, 0.625);
      };
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
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
      return SHAPES.get(getFacing(state));
   }

   /** Whether the hit point lies on the given block-relative box. */
   public static boolean hits(BlockHitResult hit, BlockPos pos, AABB box) {
      return box.inflate(0.002).contains(hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()));
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (hits(hit, pos, knob(getFacing(state))) && level.getBlockEntity(pos) instanceof LevitatorBlockEntity tile) {
         tile.increaseRange(player);
         level.playSound(null, pos, ModSounds.KEY.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      return super.useWithoutItem(state, level, pos, player, hit);
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new LevitatorBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModDevices.LEVITATOR_BE.get(), LevitatorBlockEntity::tick);
   }
}
