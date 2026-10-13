package thaumcraft.common.blocks.crafting;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import thaumcraft.common.entities.SpecialItemEntity;
import thaumcraft.common.registers.ModBlockEntities;

/** BlockCrucible: made by sprinkling salis mundus on a cauldron. */
public class CrucibleBlock extends BaseEntityBlock {
   public static final MapCodec<CrucibleBlock> CODEC = simpleCodec(CrucibleBlock::new);
   private static final VoxelShape COLLISION = Shapes.or(box(0, 0, 0, 16, 5, 16), box(0, 0, 0, 2, 16, 16), box(14, 0, 0, 16, 16, 16),
      box(0, 0, 0, 16, 16, 2), box(0, 0, 14, 16, 16, 16));
   private int delay;

   public CrucibleBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return Shapes.block();
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return COLLISION;
   }

   @Override
   protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
      return Shapes.block();
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new CrucibleBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModBlockEntities.CRUCIBLE.get(),
         level.isClientSide ? CrucibleBlockEntity::clientTick : CrucibleBlockEntity::serverTick);
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrucibleBlockEntity tile) {
         if (entity instanceof ItemEntity item && !(entity instanceof SpecialItemEntity) && tile.isBoiling()) {
            tile.attemptSmelt(item);
         } else if (++delay >= 10) {
            delay = 0;
            if (entity instanceof LivingEntity && tile.isBoiling()) {
               entity.hurt(level.damageSources().inFire(), 1.0F);
               level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 2.0F + level.random.nextFloat() * 0.4F);
            }
         }
      }
      super.entityInside(state, level, pos, entity);
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit) {
      if (!(level.getBlockEntity(pos) instanceof CrucibleBlockEntity tile)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      FluidStack fs = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
      if (!fs.isEmpty() && fs.getFluid().isSame(Fluids.WATER)) {
         if (tile.tank.getFluidAmount() < tile.tank.getCapacity()) {
            if (!level.isClientSide && FluidUtil.interactWithFluidHandler(player, hand, tile.tank)) {
               level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.33F,
                  1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.3F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
         }
      } else if (!player.isShiftKeyDown() && hit.getDirection() == Direction.UP && !stack.isEmpty()) {
         // (casters are excluded in the original; they come with the casters milestone)
         if (level.isClientSide) return tile.isBoiling() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         if (tile.isBoiling() && tile.attemptSmelt(stack.copyWithCount(1), player).isEmpty()) {
            stack.shrink(1);
            return ItemInteractionResult.SUCCESS;
         }
      }
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof CrucibleBlockEntity tile) {
         if (!level.isClientSide) tile.spillRemnants();
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      return InteractionResult.PASS;
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CrucibleBlockEntity tile) tile.spillRemnants();
      super.onRemove(state, level, pos, newState, moved);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof CrucibleBlockEntity tile ? tile.comparatorSignal() : 0;
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
      if (r.nextInt(10) == 0 && level.getBlockEntity(pos) instanceof CrucibleBlockEntity tile && tile.isBoiling()) {
         level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.1F + r.nextFloat() * 0.1F,
            1.2F + r.nextFloat() * 0.2F, false);
      }
   }
}
