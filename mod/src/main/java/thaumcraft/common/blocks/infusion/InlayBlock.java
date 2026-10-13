package thaumcraft.common.blocks.infusion;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import thaumcraft.api.crafting.IInfusionStabiliser;
import thaumcraft.common.blocks.devices.StabilizerBlockEntity;
import thaumcraft.common.registers.ModBlocks;

/**
 * BlockInlay (redstone inlay): carries the charge of an infusion stabilizer to pedestals, like redstone dust,
 * losing one per block. A charged pedestal lets the stabilizer stop its item being thrown off.
 */
public class InlayBlock extends Block implements IInfusionStabiliser {
   public enum Attach implements StringRepresentable {
      NONE, SIDE, EXT;

      @Override
      public String getSerializedName() {
         return name().toLowerCase();
      }
   }

   public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 15);
   public static final EnumProperty<Attach> NORTH = EnumProperty.create("north", Attach.class);
   public static final EnumProperty<Attach> EAST = EnumProperty.create("east", Attach.class);
   public static final EnumProperty<Attach> SOUTH = EnumProperty.create("south", Attach.class);
   public static final EnumProperty<Attach> WEST = EnumProperty.create("west", Attach.class);
   public static final Map<Direction, EnumProperty<Attach>> BY_DIR = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST);
   private static final VoxelShape SHAPE = box(0, 0, 0, 16, 1, 16);

   public InlayBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(CHARGE, 0).setValue(NORTH, Attach.NONE).setValue(EAST, Attach.NONE)
         .setValue(SOUTH, Attach.NONE).setValue(WEST, Attach.NONE));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(CHARGE, NORTH, EAST, SOUTH, WEST);
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPE;
   }

   @Override
   protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return Shapes.empty();
   }

   @Override
   protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
   }

   static boolean conducts(BlockState s) {
      return s.getBlock() instanceof InlayBlock || s.getBlock() instanceof PedestalBlock;
   }

   public static boolean isSourceBlock(BlockGetter level, BlockPos pos) {
      return level.getBlockState(pos).is(ModBlocks.STABILIZER.get());
   }

   public static int getSourceStrengthAt(BlockGetter level, BlockPos pos) {
      return isSourceBlock(level, pos) && level.getBlockEntity(pos) instanceof StabilizerBlockEntity s ? s.getEnergy() : 0;
   }

   private static Attach attach(BlockGetter level, BlockPos pos, Direction dir) {
      BlockPos p = pos.relative(dir);
      if (conducts(level.getBlockState(p))) return Attach.SIDE;
      return isSourceBlock(level, p) ? Attach.EXT : Attach.NONE;
   }

   private static BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
      for (var e : BY_DIR.entrySet()) state = state.setValue(e.getValue(), attach(level, pos, e.getKey()));
      return state;
   }

   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
   }

   @Override
   protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
      if (dir == Direction.DOWN && !canSurvive(state, level, pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
      return dir.getAxis().isHorizontal() ? state.setValue(BY_DIR.get(dir), attach(level, pos, dir)) : state;
   }

   @Override
   protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
      if (!old.is(state.getBlock())) updateCharge(level, pos);
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
      updateCharge(level, pos);
   }

   @Override
   protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
      super.onRemove(state, level, pos, newState, moved);
      if (!state.is(newState.getBlock()) && !level.isClientSide) {
         for (Direction d : Direction.Plane.HORIZONTAL) updateCharge(level, pos.relative(d));
      }
   }

   /** calculateChanges: charge = max(adjacent stabilizer energy, strongest conducting neighbour - 1). */
   public static void updateCharge(Level level, BlockPos pos) {
      if (level.isClientSide) return;
      BlockState state = level.getBlockState(pos);
      if (!conducts(state)) return;
      int max = 0;
      for (Direction d : Direction.Plane.HORIZONTAL) {
         BlockPos p = pos.relative(d);
         max = Math.max(max, getSourceStrengthAt(level, p));
         BlockState n = level.getBlockState(p);
         if (conducts(n)) max = Math.max(max, n.getValue(CHARGE) - 1);
      }
      if (max != state.getValue(CHARGE)) {
         level.setBlock(pos, state.setValue(CHARGE, max), 2);
         for (Direction d : Direction.Plane.HORIZONTAL) {
            if (conducts(level.getBlockState(pos.relative(d)))) updateCharge(level, pos.relative(d));
         }
      }
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      int charge = state.getValue(CHARGE);
      if (charge > 0 && rand.nextInt(20 - charge) == 0) thaumcraft.client.fx.InfusionFX.inlaySparkle(level, pos, state, rand);
   }

   @Override
   public boolean canStabaliseInfusion(Level level, BlockPos pos) {
      return true;
   }

   @Override
   public float getStabilizationAmount(Level level, BlockPos pos) {
      return 0.025F;
   }

   /** BlockInlay.colorMultiplier: brighter with more charge. */
   public static int color(int charge) {
      float f1 = charge == 0 ? 0.3F : charge / 15.0F * 0.5F + 0.5F;
      int c = Math.clamp((int) (f1 * 255.0F), 0, 255);
      return 0xFF000000 | c << 16 | c << 8 | c;
   }
}
