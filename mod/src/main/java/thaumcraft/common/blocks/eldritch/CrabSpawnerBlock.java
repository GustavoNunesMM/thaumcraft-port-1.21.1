package thaumcraft.common.blocks.eldritch;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.entities.monster.eldritch.EldritchCrabEntity;
import thaumcraft.common.registers.ModEldritch;
import thaumcraft.common.registers.ModSounds;

/**
 * Crusted opening (tile.eldritch.crabspawner): a vent in the floor of Outer Lands corridors that lets out eldritch
 * crabs while a player is near, up to five around it. (Thaumcraft 4 design, see EldritchAltarBlock.)
 */
public class CrabSpawnerBlock extends BaseEntityBlock {
   public static final MapCodec<CrabSpawnerBlock> CODEC = simpleCodec(CrabSpawnerBlock::new);

   public CrabSpawnerBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new Tile(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, ModEldritch.CRAB_SPAWNER_BE.get(), Tile::serverTick);
   }

   public static class Tile extends BlockEntity {
      public int count = 150;

      public Tile(BlockPos pos, BlockState state) {
         super(ModEldritch.CRAB_SPAWNER_BE.get(), pos, state);
      }

      static void serverTick(Level level, BlockPos pos, BlockState state, Tile te) {
         if (level.getDifficulty() == Difficulty.PEACEFUL || --te.count > 0) return;
         te.count = 150 + level.random.nextInt(150);
         if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16.0, false) == null) return;
         if (level.getEntitiesOfClass(EldritchCrabEntity.class, new AABB(pos).inflate(16.0)).size() >= 5) return;
         BlockPos out = pos.above();
         if (!level.getBlockState(out).getCollisionShape(level, out).isEmpty()) return;
         EldritchCrabEntity crab = ModEldritch.ELDRITCH_CRAB.get().create(level);
         if (crab == null) return;
         crab.moveTo(out.getX() + 0.5, out.getY(), out.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
         crab.finalizeSpawn((ServerLevel) level, level.getCurrentDifficultyAt(out), MobSpawnType.SPAWNER, null);
         crab.setDeltaMovement(0.0, 0.4, 0.0);
         level.addFreshEntity(crab);
         level.playSound(null, out, ModSounds.CRABTALK.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
         ((ServerLevel) level).sendParticles(ParticleTypes.LARGE_SMOKE, out.getX() + 0.5, out.getY() + 0.1, out.getZ() + 0.5, 8, 0.3, 0.1, 0.3, 0.02);
      }
   }
}
