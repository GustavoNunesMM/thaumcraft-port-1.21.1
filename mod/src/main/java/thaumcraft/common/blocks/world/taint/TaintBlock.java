package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.entities.FallingTaintEntity;
import thaumcraft.common.entities.monster.tainted.TaintSwarmEntity;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * BlockTaint: crusted taint (falls like sand when nothing holds it), tainted soil, tainted rock (spreads fibres) and
 * the taint geyser (spawns swarms, pollutes the aura). They decay without a nearby taint seed. Drops come from the loot
 * tables (silk touch only; tainted rock may drop a vitium crystal).
 *
 * <p>Port note: 1.12 tainted rock reverted to porous stone, which isn't ported yet (eldritch milestone): it reverts to
 * cobblestone for now.
 */
public class TaintBlock extends Block implements ITaintBlock {
   public TaintBlock(Properties properties) {
      super(properties);
   }

   @Override
   public void die(Level level, BlockPos pos, BlockState state) {
      if (state.is(ModFlux.TAINT_ROCK.get())) {
         level.setBlockAndUpdate(pos, Blocks.COBBLESTONE.defaultBlockState()); // stonePorous (not ported yet)
      } else if (state.is(ModFlux.TAINT_SOIL.get())) {
         level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
      } else if (state.is(ModFlux.TAINT_CRUST.get()) || state.is(ModFlux.TAINT_GEYSER.get())) {
         level.setBlockAndUpdate(pos, ModFlux.FLUX_GOO.get().defaultBlockState());
      } else {
         level.removeBlock(pos, false);
      }
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!TaintHelper.isNearTaintSeed(level, pos) && random.nextInt(10) == 0) {
         die(level, pos, state);
         return;
      }
      if (state.is(ModFlux.TAINT_ROCK.get())) TaintHelper.spreadFibres(level, pos);
      if (state.is(ModFlux.TAINT_CRUST.get())) {
         if (tryToFall(level, pos, pos)) return;
         if (level.isEmptyBlock(pos.above())) {
            boolean doIt = true;
            Direction dir = ModFlux.HORIZONTALS[random.nextInt(4)];
            for (int a = 1; a < 4; a++) {
               if (!level.isEmptyBlock(pos.relative(dir).below(a)) || !level.getBlockState(pos.below(a)).is(this)) {
                  doIt = false;
                  break;
               }
            }
            if (doIt) tryToFall(level, pos, pos.relative(dir));
         }
      } else if (state.is(ModFlux.TAINT_GEYSER.get())) {
         if (level.random.nextFloat() < 0.2F && level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32.0)
            && level.getEntitiesOfClass(TaintSwarmEntity.class, new AABB(pos).inflate(32.0)).isEmpty()) {
            TaintSwarmEntity e = new TaintSwarmEntity(ModFlux.TAINT_SWARM.get(), level);
            e.moveTo(pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5, level.random.nextInt(360), 0.0F);
            level.addFreshEntity(e);
         } else if (AuraHandler.getFlux(level, pos) < 2.0F) {
            FluxEffects.pollute(level, pos, 0.25F, true);
         }
      }
   }

   @Override
   public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
      if (!level.isClientSide && entity instanceof LivingEntity le && !le.getType().is(EntityTypeTags.UNDEAD) && level.random.nextInt(250) == 0) {
         le.addEffect(new MobEffectInstance(ModFlux.FLUX_TAINT, 200, 0, false, true));
      }
      super.stepOn(level, pos, state, entity);
   }

   @Override
   protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
      if (id == 1) {
         if (level.isClientSide) {
            level.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 0.1F, 0.9F + level.random.nextFloat() * 0.2F, false);
         }
         return true;
      }
      return super.triggerEvent(state, level, pos, id, param);
   }

   public static boolean canFallBelow(Level level, BlockPos pos) {
      BlockState bs = level.getBlockState(pos);
      for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
         if (TaintHelper.isWoodLog(level, p)) return false;
      }
      if (bs.isAir()) return true;
      if (bs.is(ModFlux.FLUX_GOO.get()) && bs.getValue(FluxGooBlock.LEVEL) >= 4) return false;
      if (bs.is(Blocks.FIRE) || bs.is(ModFlux.TAINT_FIBRE.get())) return true;
      if (bs.canBeReplaced()) return true;
      return !bs.getFluidState().isEmpty();
   }

   private boolean tryToFall(Level level, BlockPos pos, BlockPos pos2) {
      if (!TaintHelper.isOnlyAdjacentToTaint(level, pos)) return false;
      if (canFallBelow(level, pos2.below()) && pos2.getY() >= level.getMinBuildHeight()) {
         if (level.hasChunksAt(pos2.offset(-32, -32, -32), pos2.offset(32, 32, 32))) {
            level.addFreshEntity(new FallingTaintEntity(level, pos2.getX() + 0.5, pos2.getY() + 0.5, pos2.getZ() + 0.5, level.getBlockState(pos), pos));
            return true;
         }
         level.removeBlock(pos, false);
         BlockPos p2 = pos2;
         while (canFallBelow(level, p2.below()) && p2.getY() > level.getMinBuildHeight()) p2 = p2.below();
         if (p2.getY() > level.getMinBuildHeight()) level.setBlockAndUpdate(p2, ModFlux.TAINT_CRUST.get().defaultBlockState());
      }
      return false;
   }

}
