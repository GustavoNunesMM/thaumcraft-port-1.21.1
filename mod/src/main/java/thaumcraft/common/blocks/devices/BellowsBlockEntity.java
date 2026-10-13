package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModBlockEntities;

/** TileBellows: speeds up the smelter (or a furnace) it blows into, while not powered by redstone. */
public class BellowsBlockEntity extends TCBlockEntity {
   public float inflation = 1.0F;
   boolean direction;
   boolean firstrun = true;
   int delay;

   public BellowsBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.BELLOWS.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, BellowsBlockEntity be) {
      if (!DeviceBlock.isEnabled(state)) return;
      if (level.isClientSide) {
         if (be.firstrun) be.inflation = 0.35F + level.random.nextFloat() * 0.55F;
         be.firstrun = false;
         if (be.inflation > 0.35F && !be.direction) be.inflation -= 0.075F;
         if (be.inflation <= 0.35F && !be.direction) be.direction = true;
         if (be.inflation < 1.0F && be.direction) be.inflation += 0.025F;
         if (be.inflation >= 1.0F && be.direction) {
            be.direction = false;
            level.playLocalSound(pos, SoundEvents.GHAST_SHOOT, SoundSource.BLOCKS, 0.01F,
               0.5F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F, false);
         }
      } else if (++be.delay >= 2) {
         be.delay = 0;
         if (level.getBlockEntity(pos.relative(DeviceBlock.getFacing(state))) instanceof AbstractFurnaceBlockEntity f
            && f.cookingProgress > 0 && f.cookingProgress < f.cookingTotalTime - 1) {
            f.cookingProgress++;
         }
      }
   }

   /** How many enabled bellows around pos (on the given sides) blow into it. */
   public static int getBellows(Level level, BlockPos pos, Direction... directions) {
      int bellows = 0;
      for (Direction dir : directions) {
         BlockEntity tile = level.getBlockEntity(pos.relative(dir));
         if (tile instanceof BellowsBlockEntity && DeviceBlock.getFacing(tile.getBlockState()) == dir.getOpposite()
            && DeviceBlock.isEnabled(tile.getBlockState())) {
            bellows++;
         }
      }
      return bellows;
   }
}
