package thaumcraft.common.lib;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;

/** Client hooks for the gear (filled in by GearClient), so common item code needs no client classes. */
public final class GearClientBridge {
   /** Whether the player is the one this client controls. */
   public static Predicate<Player> isLocalPlayer = p -> false;
   /** KeyBinding.isPressed of the jump key in 1.12: a fresh press since the last call. */
   public static BooleanSupplier jumpPressed = () -> false;

   private GearClientBridge() {
   }
}
