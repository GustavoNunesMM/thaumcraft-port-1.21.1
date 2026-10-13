package thaumcraft.common.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.world.taint.TaintBlock;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModSounds;

/** EntityFallingTaint: a lump of crusted taint falling like sand; it settles where it lands (into goo or fibre too). */
public class FallingTaintEntity extends Entity {
   private static final EntityDataAccessor<BlockState> TILE = SynchedEntityData.defineId(FallingTaintEntity.class, EntityDataSerializers.BLOCK_STATE);
   private BlockPos oldPos = BlockPos.ZERO;
   public int fallTime;

   public FallingTaintEntity(EntityType<? extends FallingTaintEntity> type, Level level) {
      super(type, level);
   }

   public FallingTaintEntity(Level level, double x, double y, double z, BlockState state, BlockPos old) {
      this(ModFlux.FALLING_TAINT.get(), level);
      entityData.set(TILE, state);
      blocksBuilding = true;
      setPos(x, y, z);
      setDeltaMovement(Vec3.ZERO);
      xo = x;
      yo = y;
      zo = z;
      oldPos = old;
   }

   @Override
   protected void defineSynchedData(SynchedEntityData.Builder builder) {
      builder.define(TILE, Blocks.AIR.defaultBlockState());
   }

   public BlockState getBlock() {
      return entityData.get(TILE);
   }

   @Override
   public boolean isPickable() {
      return isAlive();
   }

   @Override
   public void tick() {
      BlockState tile = getBlock();
      if (tile.isAir()) {
         discard();
         return;
      }
      xo = getX();
      yo = getY();
      zo = getZ();
      fallTime++;
      setDeltaMovement(getDeltaMovement().add(0, -0.04F, 0));
      move(MoverType.SELF, getDeltaMovement());
      setDeltaMovement(getDeltaMovement().scale(0.98F));
      BlockPos bp = blockPosition();
      if (!level().isClientSide) {
         if (fallTime == 1) {
            if (level().getBlockState(oldPos) != tile) {
               discard();
               return;
            }
            level().removeBlock(oldPos, false);
         }
         if (!onGround() && !level().getBlockState(bp.below()).is(ModFlux.FLUX_GOO.get())) {
            if (fallTime > 100 && (bp.getY() < level().getMinBuildHeight() + 1 || bp.getY() > level().getMaxBuildHeight()) || fallTime > 600) discard();
         } else {
            setDeltaMovement(getDeltaMovement().multiply(0.7, -0.5, 0.7));
            if (!level().getBlockState(bp).is(Blocks.MOVING_PISTON) && !level().getBlockState(bp).is(Blocks.PISTON_HEAD)) {
               level().playSound(null, getX(), getY(), getZ(), ModSounds.GORE.get(), SoundSource.BLOCKS, 0.5F,
                  ((random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F) * 0.8F);
               discard();
               if (canPlace(bp) && !TaintBlock.canFallBelow(level(), bp.below())) level().setBlockAndUpdate(bp, tile);
            }
         }
      } else if (onGround() || fallTime == 1) {
         for (int j = 0; j < 10; j++) thaumcraft.client.fx.TaintFX.taintLand(this);
      }
   }

   private boolean canPlace(BlockPos pos) {
      BlockState bs = level().getBlockState(pos);
      return bs.is(ModFlux.TAINT_FIBRE.get()) || bs.is(ModFlux.FLUX_GOO.get()) || bs.canBeReplaced() && getBlock().canSurvive(level(), pos);
   }

   @Override
   public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
      return false;
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.put("BlockState", NbtUtils.writeBlockState(getBlock()));
      tag.putInt("Time", fallTime);
      tag.putLong("Old", oldPos.asLong());
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      entityData.set(TILE, NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState")));
      fallTime = tag.getInt("Time");
      oldPos = BlockPos.of(tag.getLong("Old"));
      if (getBlock().isAir()) entityData.set(TILE, ModFlux.TAINT_CRUST.get().defaultBlockState());
   }

   @Override
   public boolean displayFireAnimation() {
      return false;
   }

   @Override
   public SoundSource getSoundSource() {
      return SoundSource.BLOCKS;
   }

   @Override
   public Direction getMotionDirection() {
      return Direction.DOWN;
   }
}
