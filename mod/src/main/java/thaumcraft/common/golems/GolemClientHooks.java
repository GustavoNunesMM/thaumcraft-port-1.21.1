package thaumcraft.common.golems;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/** Client effects the common golem code triggers; the golem client fills them in at startup. */
public final class GolemClientHooks {
   /** FXDispatcher.drawGolemFlyParticles under a levitating golem. */
   public static Consumer<LivingEntity> flyParticles = e -> {
   };
   /** The emote particles of handleStatusUpdate (5 to 9). */
   public static java.util.function.BiConsumer<LivingEntity, Byte> emote = (e, b) -> {
   };
   /** The golem press venting steam. */
   public static java.util.function.BiConsumer<BlockPos, Integer> pressVent = (p, n) -> {
   };

   private GolemClientHooks() {
   }
}
