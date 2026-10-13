package thaumcraft.common.events;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.ResearchData;
import thaumcraft.common.aspects.AspectData;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.command.ThaumcraftCommand;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.network.SyncResearchPayload;
import thaumcraft.common.network.SyncWarpPayload;
import thaumcraft.common.research.ResearchManager;

@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class CommonEvents {
   private CommonEvents() {
   }

   @SubscribeEvent
   public static void addReloadListeners(AddReloadListenerEvent event) {
      event.addListener(AspectData.INSTANCE);
      event.addListener(new ResearchData(event.getRegistryAccess()));
   }

   @SubscribeEvent
   public static void serverStarted(ServerStartedEvent event) {
      ensureComputed(event.getServer());
   }

   /** Fires for each joining player, and for everyone after /reload (tags and recipes are ready by then). */
   @SubscribeEvent
   public static void datapackSync(OnDatapackSyncEvent event) {
      ensureComputed(event.getPlayerList().getServer());
      event.getRelevantPlayers().forEach(p -> {
         ModNetwork.sendTo(p, AspectManager.SERVER.toPayload());
         ModNetwork.sendTo(p, new SyncResearchPayload(ResearchData.rawFiles()));
      });
   }

   @SubscribeEvent
   public static void login(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer sp) {
         ResearchManager.addAutoUnlockResearch(sp);
         syncPlayer(sp);
      }
   }

   @SubscribeEvent
   public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
      if (event.getEntity() instanceof ServerPlayer sp) syncPlayer(sp);
   }

   @SubscribeEvent
   public static void dimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
      if (event.getEntity() instanceof ServerPlayer sp) syncPlayer(sp);
   }

   private static void syncPlayer(ServerPlayer sp) {
      ResearchManager.sync(sp);
      ModNetwork.sendTo(sp, new SyncWarpPayload(ResearchManager.warp(sp).copy()));
   }

   @SubscribeEvent
   public static void serverTick(ServerTickEvent.Post event) {
      ResearchManager.flushDirty();
   }

   @SubscribeEvent
   public static void registerCommands(RegisterCommandsEvent event) {
      ThaumcraftCommand.register(event.getDispatcher(), event.getBuildContext());
   }

   private static void ensureComputed(MinecraftServer server) {
      if (AspectManager.SERVER.isDirty()) AspectManager.SERVER.compute(server);
   }
}
