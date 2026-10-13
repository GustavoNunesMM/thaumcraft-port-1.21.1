package thaumcraft.gametest;

import io.netty.channel.embedded.EmbeddedChannel;
import java.util.HashSet;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import thaumcraft.Thaumcraft;

/**
 * GameTestHelper.makeMockServerPlayerInLevel gives players an embedded (never negotiated) connection; Curios sends its
 * payloads to every joining player and NeoForge refuses them on such a connection, failing every test that makes a mock
 * player. For those connections only, Curios channels are declared as ad-hoc channels before Curios syncs.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class MockPlayerCuriosShim {
   private MockPlayerCuriosShim() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void datapackSync(OnDatapackSyncEvent event) {
      ServerPlayer player = event.getPlayer();
      if (player != null) allowCurios(player.connection.getConnection());
   }

   private static void allowCurios(Connection connection) {
      if (!(connection.channel() instanceof EmbeddedChannel)) return;
      var attr = connection.channel().attr(ChannelAttributes.ADHOC_CHANNELS);
      HashSet<ResourceLocation> channels = new HashSet<>() {
         @Override
         public boolean contains(Object o) {
            return o instanceof ResourceLocation rl && "curios".equals(rl.getNamespace()) || super.contains(o);
         }
      };
      if (attr.get() != null) channels.addAll(attr.get());
      attr.set(channels);
   }
}
