package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModDevices;

/**
 * BlockInfernalFurnace: the lava core of the 3x3x3 nether brick / obsidian furnace made with salis mundus. Items
 * thrown in are smelted (faster with vis and bellows) and come out of the front, with the odd bonus nugget.
 */
public class InfernalFurnaceBlock extends DeviceBlock {
   public static final MapCodec<InfernalFurnaceBlock> CODEC = simpleCodec(InfernalFurnaceBlock::new);
   private static final VoxelShape SHAPE = box(0, 0, 0, 16, 8, 16);
   /** Set while the furnace takes itself apart, so the placeholders don't recurse. */
   public static boolean ignore = false;

   public InfernalFurnaceBlock(Properties properties) {
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
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return defaultBlockState().setValue(HORIZONTAL_FACING, ctx.getHorizontalDirection().getOpposite());
   }

   @Override
   protected BlockState rotate(BlockState state, Rotation rot) {
      return state;
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
   }

   /** Puts the structure back the way it was: bricks, obsidian, the iron bars in front and the lava core. */
   public static void destroyFurnace(Level level, BlockPos pos, BlockState state, BlockPos startpos) {
      destroyFurnace(level, pos, state, startpos, false);
   }

   /** deferCore: the core block itself is being replaced right now, so the lava goes back on the next tick. */
   public static void destroyFurnace(Level level, BlockPos pos, BlockState state, BlockPos startpos, boolean deferCore) {
      if (ignore || level.isClientSide) return;
      ignore = true;
      try {
         for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (p.equals(startpos)) continue;
            BlockState bs = level.getBlockState(p);
            if (bs.is(ModDevices.PLACEHOLDER_BRICK.get())) level.setBlockAndUpdate(p, Blocks.NETHER_BRICKS.defaultBlockState());
            if (bs.is(ModDevices.PLACEHOLDER_OBSIDIAN.get())) level.setBlockAndUpdate(p, Blocks.OBSIDIAN.defaultBlockState());
         }
         BlockPos front = pos.relative(getFacing(state).getOpposite());
         if (level.isEmptyBlock(front)) level.setBlockAndUpdate(front, Blocks.IRON_BARS.defaultBlockState());
         if (deferCore) {
            BlockPos core = pos.immutable();
            thaumcraft.common.lib.ServerTasks.schedule(0, () -> {
               if (level.getBlockState(core).canBeReplaced()) level.setBlockAndUpdate(core, Blocks.LAVA.defaultBlockState());
            });
         } else {
            level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
         }
      } finally {
         ignore = false;
      }
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
      boolean replaced = !state.is(newState.getBlock());
      super.onRemove(state, level, pos, newState, moving);
      if (replaced && !newState.is(Blocks.LAVA)) destroyFurnace(level, pos, state, pos, true);
   }

   @Override
   protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      Vec3 m = entity.getDeltaMovement();
      double mx = m.x, mz = m.z;
      if (entity.getX() < pos.getX() + 0.3F) mx += 1.0E-4F;
      if (entity.getX() > pos.getX() + 0.7F) mx -= 1.0E-4F;
      if (entity.getZ() < pos.getZ() + 0.3F) mz += 1.0E-4F;
      if (entity.getZ() > pos.getZ() + 0.7F) mz -= 1.0E-4F;
      entity.setDeltaMovement(mx, m.y, mz);
      if (level.isClientSide || entity.tickCount % 10 != 0) return;
      if (entity instanceof ItemEntity item) {
         item.setDeltaMovement(item.getDeltaMovement().x, 0.025F, item.getDeltaMovement().z);
         if (item.onGround() && level.getBlockEntity(pos) instanceof InfernalFurnaceBlockEntity taf) {
            item.setItem(taf.addItemsToInventory(item.getItem()));
            if (item.getItem().isEmpty()) item.discard();
         }
      } else if (entity instanceof LivingEntity && !entity.fireImmune()) {
         entity.hurt(level.damageSources().lava(), 3.0F);
         entity.igniteForSeconds(10.0F);
      }
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new InfernalFurnaceBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModDevices.INFERNAL_FURNACE_BE.get(), InfernalFurnaceBlockEntity::tick);
   }
}
