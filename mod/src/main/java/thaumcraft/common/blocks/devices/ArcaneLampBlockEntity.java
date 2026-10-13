package thaumcraft.common.blocks.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModDevices;

/** TileLampArcane: every 5 ticks puts an invisible glimmer light on a random dark spot it can see, within 15 blocks. */
public class ArcaneLampBlockEntity extends TCBlockEntity {
   public ArcaneLampBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.LAMP_ARCANE_BE.get(), pos, state);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, ArcaneLampBlockEntity be) {
      if (level.getGameTime() % 5L != 0L || be.gettingPower()) return;
      var r = level.random;
      BlockPos bp = pos.offset(r.nextInt(16) - r.nextInt(16), r.nextInt(16) - r.nextInt(16), r.nextInt(16) - r.nextInt(16));
      BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, bp);
      if (bp.getY() > top.getY() + 4) bp = top.above(4);
      if (bp.getY() < level.getMinBuildHeight() + 5) bp = new BlockPos(bp.getX(), level.getMinBuildHeight() + 5, bp.getZ());
      if (level.isEmptyBlock(bp) && !level.getBlockState(bp).is(ModBlocks.EFFECT_GLIMMER.get())
         && level.getBrightness(LightLayer.BLOCK, bp) < 11 && DeviceUtils.hasLOS(level, pos, bp)) {
         level.setBlock(bp, ModBlocks.EFFECT_GLIMMER.get().defaultBlockState(), 3);
      }
   }

   public void removeLights() {
      if (level == null) return;
      for (BlockPos bp : BlockPos.betweenClosed(worldPosition.offset(-15, -15, -15), worldPosition.offset(15, 15, 15))) {
         if (level.getBlockState(bp).is(ModBlocks.EFFECT_GLIMMER.get())) level.removeBlock(bp, false);
      }
   }
}
