package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModDevices;

/** BlockHungryChest: a chest that never pairs up and swallows the items that land on it. */
public class HungryChestBlock extends ChestBlock {
   public static final MapCodec<HungryChestBlock> CODEC = simpleCodec(HungryChestBlock::new);
   private static final VoxelShape SHAPE = box(1, 0, 1, 15, 14, 15);

   public HungryChestBlock(Properties properties) {
      super(properties, () -> ModDevices.HUNGRY_CHEST_BE.get());
   }

   @Override
   public MapCodec<? extends ChestBlock> codec() {
      return CODEC;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(TYPE, ChestType.SINGLE)
         .setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER);
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos facingPos) {
      if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      return state;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new HungryChestBlockEntity(pos, state);
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      if (level.isClientSide || !(entity instanceof ItemEntity item) || !item.isAlive()) return;
      if (!(level.getBlockEntity(pos) instanceof HungryChestBlockEntity chest)) return;
      ItemStack stack = item.getItem();
      ItemStack leftovers = DeviceUtils.insertStackAt(level, pos, Direction.UP, stack.copy(), false);
      if (leftovers.isEmpty() || leftovers.getCount() != stack.getCount()) {
         entity.playSound(SoundEvents.GENERIC_EAT, 0.25F, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
      }
      if (!leftovers.isEmpty()) item.setItem(leftovers);
      else item.discard();
      chest.setChanged();
   }
}
