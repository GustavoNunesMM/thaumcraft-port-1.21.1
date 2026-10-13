package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * BlockVisBattery: soaks up vis (up to 10) while the aura is above 90% of its base, gives it back when the aura drops
 * below 75%, and empties itself into the aura while powered by redstone.
 */
public class VisBatteryBlock extends Block {
   public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 10);

   public VisBatteryBlock(Properties properties) {
      super(properties);
      registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
   }

   @Override
   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
      builder.add(CHARGE);
   }

   @Override
   protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      tick(state, level, pos, random);
   }

   @Override
   protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
      int charge = state.getValue(CHARGE);
      if (level.hasNeighborSignal(pos)) {
         if (charge > 0) {
            AuraHandler.addVis(level, pos, 1.0F);
            level.setBlockAndUpdate(pos, state.setValue(CHARGE, charge - 1));
            level.scheduleTick(pos, this, 5);
         }
      } else {
         float aura = AuraHandler.getVis(level, pos);
         int base = AuraHandler.getAuraBase(level, pos);
         if (charge < 10 && aura > base * 0.9 && aura > 1.0F) {
            AuraHandler.drainVis(level, pos, 1.0F, false);
            level.setBlockAndUpdate(pos, state.setValue(CHARGE, charge + 1));
            level.scheduleTick(pos, this, 100 + rand.nextInt(100));
         } else if (charge > 0 && aura < base * 0.75) {
            AuraHandler.addVis(level, pos, 1.0F);
            level.setBlockAndUpdate(pos, state.setValue(CHARGE, charge - 1));
            level.scheduleTick(pos, this, 20 + rand.nextInt(20));
         }
      }
   }

   @Override
   protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
      if (level.hasNeighborSignal(pos)) level.scheduleTick(pos, this, 1);
   }

   @Override
   protected boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   @Override
   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      return state.getValue(CHARGE);
   }
}
