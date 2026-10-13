package thaumcraft.gametest;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import thaumcraft.Thaumcraft;

/**
 * GameTestHelper.makeMockServerPlayerInLevel connects the player through an EmbeddedChannel that never negotiated any
 * mod channel, and Curios syncs its slots on login (OnDatapackSyncEvent) without checking, which throws and fails the
 * test. For those mock connections only, every registered payload is declared as an ad-hoc channel first.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class MockPlayerChannels {
   private MockPlayerChannels() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void beforeSync(OnDatapackSyncEvent event) {
      ServerPlayer player = event.getPlayer();
      if (player == null || player.connection == null) return;
      var connection = player.connection.getConnection();
      if (!(connection.channel() instanceof io.netty.channel.embedded.EmbeddedChannel) || ChannelAttributes.getPayloadSetup(connection) != null) return;
      try {
         Field f = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
         f.setAccessible(true);
         Map<?, ?> byProtocol = (Map<?, ?>) f.get(null);
         var adhoc = ChannelAttributes.getOrCreateAdHocChannels(connection);
         for (Object m : byProtocol.values()) {
            for (Object id : ((Map<?, ?>) m).keySet()) adhoc.add((ResourceLocation) id);
         }
      } catch (ReflectiveOperationException | RuntimeException e) {
         Thaumcraft.LOGGER.warn("Could not open the payload channels of a mock player", e);
      }
   }
}
