package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModDevices;

/**
 * BlockPavingStone: the paving stone of travel gives speed and jump boost to whoever walks on it; the barrier stone
 * raises an invisible two-block wall that only stops mobs (not players), unless powered by redstone.
 */
public class PavingStoneBlock extends BaseEntityBlock {
   private static final VoxelShape SHAPE = box(0, 0, 0, 16, 15, 16);
   public final boolean barrier;

   public PavingStoneBlock(boolean barrier, Properties properties) {
      super(properties);
      this.barrier = barrier;
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return simpleCodec(p -> new PavingStoneBlock(barrier, p));
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   public void stepOn(Level level, BlockPos pos, BlockState state, Entity e) {
      if (!level.isClientSide && !barrier && e instanceof LivingEntity le) {
         le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
         le.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, false, false));
      }
      super.stepOn(level, pos, state, e);
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (!barrier) return;
      if (level.getBestNeighborSignal(pos) > 0) {
         for (int a = 0; a < 4; a++) {
            thaumcraft.client.fx.DeviceFX.blockRunes(pos.getX(), pos.getY() + 0.7F, pos.getZ(), 0.2F + random.nextFloat() * 0.4F,
               random.nextFloat() * 0.3F, 0.8F + random.nextFloat() * 0.2F, 20, -0.02F);
         }
      } else if (level.getBlockState(pos.above(1)).is(ModDevices.BARRIER.get()) && BarrierBlock.isPassable(level, pos.above(1))
         || level.getBlockState(pos.above(2)).is(ModDevices.BARRIER.get()) && BarrierBlock.isPassable(level, pos.above(2))) {
         for (int a = 0; a < 6; a++) {
            thaumcraft.client.fx.DeviceFX.blockRunes(pos.getX(), pos.getY() + 0.7F, pos.getZ(), 0.9F + random.nextFloat() * 0.1F,
               random.nextFloat() * 0.3F, random.nextFloat() * 0.3F, 24, -0.02F);
         }
      } else {
         List<Entity> list = level.getEntities((Entity) null, new AABB(pos).inflate(1.0));
         for (Entity entity : list) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
               thaumcraft.client.fx.DeviceFX.blockRunes(pos.getX(), pos.getY() + 0.6F + random.nextFloat() * Math.max(0.8F, entity.getEyeHeight()),
                  pos.getZ(), 0.6F + random.nextFloat() * 0.4F, 0.0F, 0.3F + random.nextFloat() * 0.7F, 20, 0.0F);
               break;
            }
         }
      }
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return barrier ? new BarrierStoneBlockEntity(pos, state) : null;
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return barrier && !level.isClientSide ? createTickerHelper(type, ModDevices.BARRIER_STONE_BE.get(), BarrierStoneBlockEntity::serverTick) : null;
   }
}
