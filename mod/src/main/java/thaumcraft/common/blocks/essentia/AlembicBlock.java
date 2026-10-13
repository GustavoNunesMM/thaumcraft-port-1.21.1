package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.ILabelable;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.world.aura.AuraHandler;

/** BlockAlembic: stacked on an essentia smeltery. Labels go on its sides. */
public class AlembicBlock extends BaseEntityBlock implements ILabelable {
   public static final MapCodec<AlembicBlock> CODEC = simpleCodec(AlembicBlock::new);
   private static final VoxelShape SHAPE = box(2, 0, 2, 14, 16, 14);

   public AlembicBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
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
      return new AlembicBlockEntity(pos, state);
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof AlembicBlockEntity te)) return InteractionResult.PASS;
      Direction side = hit.getDirection();
      if (!level.isClientSide) {
         if (te.aspectFilter != null && side == te.facing) {
            te.aspectFilter = null;
            te.facing = Direction.DOWN;
            te.sync();
            level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5 + side.getStepX() / 3.0, pos.getY() + 0.5,
               pos.getZ() + 0.5 + side.getStepZ() / 3.0, new ItemStack(ModItems.LABEL.get())));
         } else {
            te.aspect = null;
            AuraHandler.addFlux(level, pos, te.amount);
            te.amount = 0;
            te.sync();
            level.playSound(null, pos, ModSounds.JAR.get(), SoundSource.BLOCKS, 0.4F, 1.0F);
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.5F, 1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.3F);
         }
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   @Override
   public boolean applyLabel(Player player, BlockPos pos, Direction side, ItemStack label) {
      if (!(player.level().getBlockEntity(pos) instanceof AlembicBlockEntity te) || !side.getAxis().isHorizontal() || te.aspectFilter != null) {
         return false;
      }
      Aspect la = label.get(ModDataComponents.ASPECT_FILTER.get());
      Aspect aspect = te.amount > 0 ? te.aspect : la;
      if (aspect == null) return false;
      te.aspectFilter = aspect;
      te.facing = side;
      te.sync();
      player.level().playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
      return true;
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AlembicBlockEntity te && te.amount > 0) {
         AuraHandler.addFlux(level, pos, te.amount);
      }
      super.onRemove(state, level, pos, newState, moved);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof AlembicBlockEntity te ? te.comparatorSignal() : 0;
   }
}
