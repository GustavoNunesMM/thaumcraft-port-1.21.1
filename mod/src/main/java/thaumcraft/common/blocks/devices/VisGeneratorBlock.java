package thaumcraft.common.blocks.devices;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.registers.ModDevices;

/** BlockVisGenerator: turns vis from the aura into Forge Energy, pushed into the block it faces. */
public class VisGeneratorBlock extends DeviceBlock {
   public static final MapCodec<VisGeneratorBlock> CODEC = simpleCodec(VisGeneratorBlock::new);
   private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

   static {
      for (Direction d : Direction.values()) {
         double[] min = {0.25, 0.25, 0.25}, max = {0.75, 0.75, 0.75};
         int axis = d.getAxis().ordinal();
         if (d.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            min[axis] = 0.125;
            max[axis] = 1.0;
         } else {
            min[axis] = 0.0;
            max[axis] = 0.875;
         }
         SHAPES.put(d, Shapes.box(min[0], min[1], min[2], max[0], max[1], max[2]));
      }
   }

   public VisGeneratorBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected MapCodec<? extends BaseEntityBlock> codec() {
      return CODEC;
   }

   @Override
   protected Facing facingType() {
      return Facing.ALL;
   }

   @Override
   protected boolean hasEnabled() {
      return true;
   }

   @Override
   protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
      return SHAPES.get(getFacing(state));
   }

   /** Faces the first neighbour that accepts energy, if any. */
   @Nullable
   @Override
   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      for (Direction face : Direction.values()) {
         IEnergyStorage cap = ctx.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, ctx.getClickedPos().relative(face), face.getOpposite());
         if (cap != null && cap.canReceive()) return defaultBlockState().setValue(FACING, face).setValue(ENABLED, true);
      }
      return super.getStateForPlacement(ctx);
   }

   @Override
   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
      if (level.getBlockEntity(pos) instanceof VisGeneratorBlockEntity te && te.getEnergyStored() > 0) {
         Direction face = getFacing(state);
         double x = face.getStepX() == 0 ? rand.nextGaussian() * 0.1 : face.getStepX() * 0.1;
         double y = face.getStepY() == 0 ? rand.nextGaussian() * 0.1 : face.getStepY() * 0.1;
         double z = face.getStepZ() == 0 ? rand.nextGaussian() * 0.1 : face.getStepZ() * 0.1;
         thaumcraft.client.fx.DeviceFX.spark(pos.getX() + 0.5 + x, pos.getY() + 0.5 + y, pos.getZ() + 0.5 + z, 0.66F + rand.nextFloat(),
            0.65F + rand.nextFloat() * 0.1F, 1.0F, 1.0F, 0.8F);
      }
   }

   @Nullable
   @Override
   public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
      return new VisGeneratorBlockEntity(pos, state);
   }

   @Nullable
   @Override
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return level.isClientSide ? null : createTickerHelper(type, ModDevices.VIS_GENERATOR_BE.get(), VisGeneratorBlockEntity::serverTick);
   }
}
