package thaumcraft.common.lib;

import thaumcraft.common.network.BamfPayload;
import thaumcraft.common.network.ModNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import thaumcraft.common.registers.ModSounds;

/**
 * Server-side stand-ins for the FXDispatcher effects that are triggered from the server, until the custom
 * particle engine is ported: they use vanilla particles sent to nearby players.
 */
public final class FXHelper {
   private FXHelper() {
   }

   /** PacketFXBlockBamf: a puff of smoke with sparkles and a "poof". */
   public static void bamf(ServerLevel level, BlockPos pos) {
      bamf(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, -9999, true, true, null);
   }

   public static void bamf(ServerLevel level, double x, double y, double z, int color, boolean sound, boolean flair, @javax.annotation.Nullable net.minecraft.core.Direction side) {
      ModNetwork.sendNear(level, x, y, z, 32.0, new BamfPayload(x, y, z, color, sound, flair, side == null ? -1 : side.get3DDataValue()));
   }
}
