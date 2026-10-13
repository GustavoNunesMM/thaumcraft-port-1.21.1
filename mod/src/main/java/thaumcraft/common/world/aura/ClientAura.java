package thaumcraft.common.world.aura;

/** HudHandler.currentAura: the last aura the server sent this client. Plain data, safe to load on a server. */
public final class ClientAura {
   public static volatile AuraChunk current = new AuraChunk((short) 0, 0, 0);
   /** Game time of the last update; the HUD hides stale values. */
   public static volatile long lastUpdate;

   private ClientAura() {
   }
}
