package thaumcraft.common.blocks.eldritch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModEldritch;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.world.eldritch.MazeGenerator;

/**
 * Ancient locking mechanism (tile.eldritch.lock): set into the ancient doorway that seals the boss chamber of an Outer
 * Lands maze. A runed tablet unlocks it: after two seconds the doorway crumbles and whatever guards the chamber wakes.
 * {@code facing} points into the sealed chamber. (Thaumcraft 4 design, see EldritchAltarBlock.)
 */
public class EldritchLockBlock extends HorizontalDirectionalBlock {
   public static final com.mojang.serialization.MapCodec<EldritchLockBlock> CODEC = simpleCodec(EldritchLockBlock::new);
   public static final BooleanProperty OPENING = BooleanProperty.create("opening");

   public EldritchLockBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPENING, false));
   }

   @Override
   protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(FACING, OPENING);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
      BlockHitResult hit) {
      if (!stack.is(ModEldritch.RUNED_TABLET.get()) || state.getValue(OPENING)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      if (!level.isClientSide) {
         if (!player.getAbilities().instabuild) stack.shrink(1);
         level.setBlockAndUpdate(pos, state.setValue(OPENING, true));
         level.scheduleTick(pos, this, 40);
         level.playSound(null, pos, ModSounds.CRAFTSTART.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
      }
      return ItemInteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (state.getValue(OPENING)) open(level, pos, state.getValue(FACING));
   }

   /** Crumbles the doorway (every ancient doorway block within 3 blocks) and wakes the chamber's guardian. */
   public static void open(ServerLevel level, BlockPos pos, Direction facing) {
      for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -3, -3), pos.offset(3, 3, 3))) {
         BlockState s = level.getBlockState(p);
         if (s.is(ModBlocks.STONE_ANCIENT_DOORWAY.get()) || s.is(ModEldritch.ELDRITCH_LOCK.get())) {
            level.removeBlock(p, false);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
         }
      }
      level.playSound(null, pos, ModSounds.CRAFTFAIL.get(), SoundSource.BLOCKS, 1.0F, 0.6F);
      MazeGenerator.wakeBoss(level, pos.relative(facing, 2));
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
      if (state.getValue(OPENING)) {
         for (int a = 0; a < 3; a++) {
            thaumcraft.client.fx.EldritchFX.sparkle(pos.getX() + r.nextFloat(), pos.getY() + r.nextFloat(), pos.getZ() + r.nextFloat(), 0.6F, 0.1F, 0.9F);
         }
      }
   }
}
