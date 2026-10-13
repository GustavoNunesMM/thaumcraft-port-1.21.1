package thaumcraft.api;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.IEssentiaTransport;

public final class ThaumcraftApiHelper {
   private ThaumcraftApiHelper() {
   }

   /** The essentia transport next to pos on that face, if it connects back towards pos. */
   @Nullable
   public static IEssentiaTransport getConnectableTile(BlockGetter level, BlockPos pos, Direction face) {
      BlockEntity te = level.getBlockEntity(pos.relative(face));
      return te instanceof IEssentiaTransport t && t.isConnectable(face.getOpposite()) ? t : null;
   }
}
