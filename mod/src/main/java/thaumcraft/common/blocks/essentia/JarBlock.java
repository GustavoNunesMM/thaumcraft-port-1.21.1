package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.blocks.ILabelable;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.world.aura.AuraHandler;

/** BlockJar: warded jar and void jar. */
public class JarBlock extends BaseEntityBlock implements ILabelable {
   public static final MapCodec<JarBlock> CODEC = simpleCodec(JarBlock::new);
   private static final VoxelShape SHAPE = box(3, 0, 3, 13, 12, 13);

   public JarBlock(Properties properties) {
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
      return new JarBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.JAR.get(), JarBlockEntity::serverTick);
   }

   /** The label faces the player who placed (or labelled) the jar. */
   private static void face(JarBlockEntity jar, @Nullable LivingEntity placer) {
      if (placer != null) jar.facing = placer.getDirection().getOpposite();
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (level.getBlockEntity(pos) instanceof JarBlockEntity jar) {
         face(jar, placer);
         jar.sync();
      }
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit) {
      if (level.getBlockEntity(pos) instanceof JarBlockEntity jar && !jar.blocked && stack.is(ModItems.JAR_BRACE.get())) {
         if (!level.isClientSide) {
            jar.blocked = true;
            stack.consume(1, player);
            jar.sync();
            level.playSound(null, pos, ModSounds.KEY.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
         }
         return ItemInteractionResult.sidedSuccess(level.isClientSide);
      }
      if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof JarBlockEntity jar && jar.aspectFilter != null
         && hit.getDirection() == jar.facing) {
         return removeLabel(level, pos, jar, hit.getDirection()) ? ItemInteractionResult.sidedSuccess(level.isClientSide)
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof JarBlockEntity jar)) return InteractionResult.PASS;
      if (jar.aspectFilter != null && hit.getDirection() == jar.facing) {
         removeLabel(level, pos, jar, hit.getDirection());
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      // Empties the jar into the aura.
      if (!level.isClientSide) {
         if (jar.aspectFilter == null) jar.aspect = null;
         AuraHandler.addFlux(level, pos, jar.amount);
         jar.amount = 0;
         jar.sync();
         level.playSound(null, pos, ModSounds.JAR.get(), SoundSource.BLOCKS, 0.4F, 1.0F);
         level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.5F, 1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.3F);
      }
      return InteractionResult.sidedSuccess(level.isClientSide);
   }

   private static boolean removeLabel(Level level, BlockPos pos, JarBlockEntity jar, Direction side) {
      if (!level.isClientSide) {
         jar.aspectFilter = null;
         jar.sync();
         level.playSound(null, pos, ModSounds.PAGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
         level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5 + side.getStepX() / 3.0, pos.getY() + 0.5,
            pos.getZ() + 0.5 + side.getStepZ() / 3.0, new ItemStack(ModItems.LABEL.get())));
      }
      return true;
   }

   @Override
   public boolean applyLabel(Player player, BlockPos pos, Direction side, ItemStack label) {
      if (!(player.level().getBlockEntity(pos) instanceof JarBlockEntity jar) || jar.aspectFilter != null) return false;
      var labelAspect = label.get(ModDataComponents.ASPECT_FILTER.get());
      if (jar.amount == 0 && labelAspect == null) return false;
      if (jar.amount == 0) jar.aspect = labelAspect;
      face(jar, player);
      jar.aspectFilter = jar.aspect;
      jar.sync();
      player.level().playSound(null, pos, ModSounds.JAR.get(), SoundSource.BLOCKS, 0.4F, 1.0F);
      return true;
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof JarBlockEntity jar && jar.blocked) {
         Block.popResource(level, pos, new ItemStack(ModItems.JAR_BRACE.get()));
      }
      super.onRemove(state, level, pos, newState, moved);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof JarBlockEntity jar ? jar.comparatorSignal() : 0;
   }
}
