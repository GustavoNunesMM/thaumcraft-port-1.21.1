package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModSounds;

/**
 * BlockJar with a TileJarBrain: a brain in a jar that pulls in and stores experience orbs (up to 2000 xp); click it to
 * get some back. Counts as 5 bookshelves for enchanting.
 */
public class JarBrainBlock extends BaseEntityBlock {
   public static final MapCodec<JarBrainBlock> CODEC = simpleCodec(JarBrainBlock::new);
   private static final VoxelShape SHAPE = box(3, 0, 3, 13, 12, 13);

   public JarBrainBlock(Properties properties) {
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

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!(level.getBlockEntity(pos) instanceof JarBrainBlockEntity te)) return InteractionResult.PASS;
      te.eatDelay = 40;
      if (!level.isClientSide) {
         int amount = level.random.nextInt(Math.min(te.xp + 1, 64));
         if (amount > 0) {
            te.xp -= amount;
            int xp = amount;
            while (xp > 0) {
               int v = ExperienceOrb.getExperienceValue(xp);
               xp -= v;
               level.addFreshEntity(new ExperienceOrb(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, v));
            }
            te.sync();
         }
      } else {
         level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.JAR.get(), SoundSource.BLOCKS, 0.2F, 1.0F, false);
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
      return 5.0F;
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      if (level.getBlockEntity(pos) instanceof JarBrainBlockEntity te && te.xp >= JarBrainBlockEntity.XP_MAX) {
         thaumcraft.client.fx.DeviceFX.spark(pos.getX() + 0.5F, pos.getY() + 0.8F, pos.getZ() + 0.5F, 3.0F, 0.2F + rand.nextFloat() * 0.2F, 1.0F,
            0.3F + rand.nextFloat() * 0.2F, 0.5F);
      }
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof JarBrainBlockEntity te) {
         float r = (float) te.xp / JarBrainBlockEntity.XP_MAX;
         return Mth.floor(r * 14.0F) + (te.xp > 0 ? 1 : 0);
      }
      return 0;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new JarBrainBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return createTickerHelper(type, ModDevices.JAR_BRAIN_BE.get(), JarBrainBlockEntity::tick);
   }
}
