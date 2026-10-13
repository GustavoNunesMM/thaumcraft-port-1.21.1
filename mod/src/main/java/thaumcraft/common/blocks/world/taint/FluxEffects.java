package thaumcraft.common.blocks.world.taint;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import thaumcraft.common.network.FluxFXPayload;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.world.aura.AuraHandler;

/** AuraHelper.polluteAura: adds flux to the aura and (showEffect) puffs pink pollution particles (PacketFXPollute). */
public final class FluxEffects {
   private FluxEffects() {
   }

   public static void pollute(Level level, BlockPos pos, float amount, boolean showEffect) {
      if (level.isClientSide) return;
      AuraHandler.addFlux(level, pos, amount);
      if (showEffect && amount > 0.0F && level instanceof ServerLevel sl) {
         ModNetwork.sendNear(sl, pos.getX(), pos.getY(), pos.getZ(), 32.0, FluxFXPayload.pollute(pos, amount));
      }
   }
}
