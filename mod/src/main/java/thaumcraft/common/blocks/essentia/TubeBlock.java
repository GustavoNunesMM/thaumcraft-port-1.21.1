package thaumcraft.common.blocks.essentia;

import com.mojang.serialization.MapCodec;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.casters.ICaster;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.world.aura.AuraHandler;

/** BlockTube: the six essentia tube kinds. The connection properties follow neighbours that connect back. */
public class TubeBlock extends BaseEntityBlock {
   public enum Kind {
      NORMAL(TubeBlockEntity::new), VALVE(TubeValveBlockEntity::new), RESTRICT(TubeRestrictBlockEntity::new),
      ONEWAY(TubeOnewayBlockEntity::new), FILTER(TubeFilterBlockEntity::new), BUFFER(TubeBufferBlockEntity::new);

      final BiFunction<BlockPos, BlockState, ? extends TubeBlockEntity> factory;

      Kind(BiFunction<BlockPos, BlockState, ? extends TubeBlockEntity> factory) {
         this.factory = factory;
      }
   }

   public static final MapCodec<TubeBlock> CODEC = simpleCodec(p -> new TubeBlock(Kind.NORMAL, p));
   public final Kind kind;

   public TubeBlock(Kind kind, Properties properties) {
      super(properties);
      this.kind = kind;
      BlockState bs = stateDefinition.any();
      for (BooleanProperty p : PipeBlock.PROPERTY_BY_DIRECTION.values()) bs = bs.setValue(p, false);
      registerDefaultState(bs);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(PipeBlock.NORTH, PipeBlock.EAST, PipeBlock.SOUTH, PipeBlock.WEST, PipeBlock.UP, PipeBlock.DOWN);
   }

   @Override
   protected RenderShape getRenderShape(BlockState state) {
      return RenderShape.MODEL;
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return kind.factory.apply(pos, state);
   }

   @Nullable
   @Override
   @SuppressWarnings("unchecked")
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return (lvl, pos, st, be) -> {
         if (be instanceof TubeBlockEntity t) TubeBlockEntity.tick(lvl, pos, st, t);
      };
   }

   /** makeConnections: a side shows an arm when this tube and the neighbour connect to each other. */
   static BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
      BlockEntity t = level.getBlockEntity(pos);
      for (Direction face : Direction.values()) {
         boolean open = !(t instanceof IEssentiaTransport tube) || tube.isConnectable(face);
         state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(face), open && ThaumcraftApiHelper.getConnectableTile(level, pos, face) != null);
      }
      return state;
   }

   /** Recomputes the connections after a side was opened or closed. */
   public static void refresh(Level level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      if (state.getBlock() instanceof TubeBlock) {
         BlockState ns = withConnections(state, level, pos);
         if (ns != state) level.setBlock(pos, ns, 3);
      }
      // Neighbouring tubes see the change too (their arm towards this tube).
      for (Direction d : Direction.values()) {
         BlockPos np = pos.relative(d);
         BlockState s = level.getBlockState(np);
         if (s.getBlock() instanceof TubeBlock) {
            BlockState ns = withConnections(s, level, np);
            if (ns != s) level.setBlock(np, ns, 3);
         }
      }
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      return withConnections(state, level, pos);
   }

   @Override
   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(level, pos, state, placer, stack);
      if (placer != null && level.getBlockEntity(pos) instanceof TubeBlockEntity tube) {
         tube.facing = Direction.orderedByNearest(placer)[0].getOpposite();
         tube.sync();
         refresh(level, pos);
      }
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      double min = 5, max = 11;
      VoxelShape s = box(min, min, min, max, max, max);
      if (state.getValue(PipeBlock.DOWN)) s = Shapes.or(s, box(min, 0, min, max, min, max));
      if (state.getValue(PipeBlock.UP)) s = Shapes.or(s, box(min, max, min, max, 16, max));
      if (state.getValue(PipeBlock.NORTH)) s = Shapes.or(s, box(min, min, 0, max, max, min));
      if (state.getValue(PipeBlock.SOUTH)) s = Shapes.or(s, box(min, min, max, max, max, 16));
      if (state.getValue(PipeBlock.WEST)) s = Shapes.or(s, box(0, min, min, min, max, max));
      if (state.getValue(PipeBlock.EAST)) s = Shapes.or(s, box(max, min, min, 16, max, max));
      if (kind == Kind.BUFFER) s = Shapes.or(s, box(4, 4, 4, 12, 12, 12));
      return s;
   }

   @Override
   protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                             BlockHitResult hit) {
      if (stack.getItem() instanceof ICaster || stack.is(asItem())) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
      if (kind == Kind.FILTER && level.getBlockEntity(pos) instanceof TubeFilterBlockEntity te) {
         if (player.isShiftKeyDown() && te.aspectFilter != null) {
            if (!level.isClientSide) {
               te.aspectFilter = null;
               te.sync();
               level.playSound(null, pos, ModSounds.KEY.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
         }
         AspectList al = stack.get(ModDataComponents.ESSENTIA.get());
         if (te.aspectFilter == null && al != null && !al.isEmpty()) {
            if (!level.isClientSide) {
               te.aspectFilter = al.getAspects()[0];
               te.sync();
               level.playSound(null, pos, ModSounds.KEY.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
         }
      }
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      if (player.getMainHandItem().getItem() instanceof ICaster) return InteractionResult.PASS;
      if (kind == Kind.VALVE && level.getBlockEntity(pos) instanceof TubeValveBlockEntity te) {
         if (!level.isClientSide) te.setFlow(!te.allowFlow);
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      if (kind == Kind.FILTER && player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof TubeFilterBlockEntity te && te.aspectFilter != null) {
         if (!level.isClientSide) {
            te.aspectFilter = null;
            te.sync();
            level.playSound(null, pos, ModSounds.KEY.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
         }
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
      return InteractionResult.PASS;
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof TubeBlockEntity te && te.breakSpill() > 0) {
         AuraHandler.addFlux(level, pos, te.breakSpill());
      }
      super.onRemove(state, level, pos, newState, moved);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState state) {
      return kind == Kind.BUFFER;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof TubeBufferBlockEntity b ? b.comparatorSignal() : 0;
   }
}
