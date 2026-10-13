package thaumcraft.common.blocks.infusion;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.crafting.IInfusionStabiliser;
import thaumcraft.common.registers.ModBlocks;

/** BlockPedestal: arcane, ancient or eldritch. Carries the inlay charge so stabilizers can protect its item. */
public class PedestalBlock extends BaseEntityBlock implements IInfusionStabiliser {
   public static final MapCodec<PedestalBlock> CODEC = simpleCodec(PedestalBlock::new);
   private static final VoxelShape SHAPE = Shapes.or(box(4, 0, 4, 12, 3, 12), box(5, 3, 5, 11, 11, 11), box(4, 11, 4, 12, 12, 12));

   public PedestalBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(InlayBlock.CHARGE, 0));
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(InlayBlock.CHARGE);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new PedestalBlockEntity(pos, state);
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit) {
      if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity ped)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      if (ped.getItem().isEmpty() && !stack.isEmpty()) {
         if (!level.isClientSide) {
            ped.setItem(0, stack.copyWithCount(1));
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
               ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 1.6F);
         }
         return ItemInteractionResult.sidedSuccess(level.isClientSide);
      }
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (level.getBlockEntity(pos) instanceof PedestalBlockEntity ped && !ped.getItem().isEmpty()) {
         if (!level.isClientSide) {
            ItemStack out = ped.removeItemNoUpdate(0);
            ped.sync();
            if (!player.getInventory().add(out)) player.drop(out, false);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2F,
               ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 1.5F);
         }
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      return InteractionResult.PASS;
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
      super.onPlace(state, level, pos, old, moving);
      if (!old.is(state.getBlock())) InlayBlock.updateCharge(level, pos);
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
      InlayBlock.updateCharge(level, pos);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PedestalBlockEntity ped) Containers.dropContents(level, pos, ped);
      super.onRemove(state, level, pos, newState, moved);
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      if (state.getValue(InlayBlock.CHARGE) > 0) thaumcraft.client.fx.InfusionFX.blockRunes(pos.getX(), pos.getY() - 0.375, pos.getZ(), 1.0F, 0.0F, 0.0F, 10);
   }

   @Override
   public boolean canStabaliseInfusion(Level level, BlockPos pos) {
      return true;
   }

   @Override
   public float getStabilizationAmount(Level level, BlockPos pos) {
      return level.getBlockState(pos).is(ModBlocks.PEDESTAL_ELDRITCH.get()) ? 0.1F : 0.0F;
   }

   /** An item on one pedestal and nothing on its mirror image unbalances the infusion. */
   @Override
   public boolean hasSymmetryPenalty(Level level, BlockPos pos1, BlockPos pos2) {
      return level.getBlockEntity(pos1) instanceof PedestalBlockEntity p1 && level.getBlockEntity(pos2) instanceof PedestalBlockEntity p2
         && p1.getItem().isEmpty() != p2.getItem().isEmpty();
   }

   @Override
   public float getSymmetryPenalty(Level level, BlockPos pos) {
      return 0.1F;
   }
}
