package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.registers.ModBlockEntities;

/** TileNitor: spawns the flame particles. */
public class NitorBlockEntity extends BlockEntity {
   int count;

   public NitorBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.NITOR.get(), pos, state);
   }

   public static void clientTick(Level level, BlockPos pos, BlockState state, NitorBlockEntity be) {
      Client.tick(level, pos, state, be);
   }

   static final class Client {
      static void tick(Level level, BlockPos pos, BlockState state, NitorBlockEntity be) {
         var rand = level.random;
         int color = state.getBlock() instanceof NitorBlock n ? n.dye.getMapColor().col : 0xFFFFFF;
         thaumcraft.client.fx.FXDispatcher.drawNitorFlames(pos.getX() + 0.5F + rand.nextGaussian() * 0.025,
            pos.getY() + 0.45F + rand.nextGaussian() * 0.025, pos.getZ() + 0.5F + rand.nextGaussian() * 0.025,
            rand.nextGaussian() * 0.0025, rand.nextFloat() * 0.06, rand.nextGaussian() * 0.0025, color);
         if (be.count++ % 10 == 0) thaumcraft.client.fx.FXDispatcher.drawNitorCore(pos.getX() + 0.5F, pos.getY() + 0.49F, pos.getZ() + 0.5F, 0, 0, 0);
      }
   }
}
