package thaumcraft.common.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.EnumResearchFlag;
import thaumcraft.api.research.ResearchData;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.blocks.crafting.ResearchTableBlockEntity;
import thaumcraft.common.lib.ClientBridge;
import thaumcraft.common.registers.ModAttachments;
import thaumcraft.common.research.ClientResearchEvents;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.ClientAura;

public final class ModNetwork {
   private ModNetwork() {
   }

   /** Sends only if the client negotiated our channel (a client without the mod would otherwise be kicked). */
   public static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
      if (player.connection != null && player.connection.hasChannel(payload)) {
         PacketDistributor.sendToPlayer(player, payload);
      }
   }

   /** Sends to every player within range of a point. */
   public static void sendNear(net.minecraft.server.level.ServerLevel level, double x, double y, double z, double range, CustomPacketPayload payload) {
      for (ServerPlayer p : level.players()) {
         if (p.distanceToSqr(x, y, z) <= range * range) sendTo(p, payload);
      }
   }

   public static void register(RegisterPayloadHandlersEvent event) {
      PayloadRegistrar registrar = event.registrar("1");
      CasterPayloads.register(registrar);
      CloudJumpPayload.register(registrar);
      DeviceNetwork.register(registrar);
      FluxFXPayload.register(registrar);
      registrar.playToClient(SyncAspectsPayload.TYPE, SyncAspectsPayload.STREAM_CODEC,
         (payload, ctx) -> {
            AspectManager.CLIENT.acceptSync(payload);
            Thaumcraft.LOGGER.info("Received aspect tables: {} items, {} entities", payload.items().size(), payload.entities().size());
         });
      registrar.playToClient(SyncKnowledgePayload.TYPE, SyncKnowledgePayload.STREAM_CODEC,
         // Copy: in singleplayer the payload object is shared with the server, not serialized.
         (payload, ctx) -> {
            ctx.player().setData(ModAttachments.KNOWLEDGE, payload.knowledge().copy());
            ClientBridge.knowledgeSynced.accept(ctx.player().getData(ModAttachments.KNOWLEDGE));
            Thaumcraft.LOGGER.debug("Received knowledge: {} research keys", payload.knowledge().getResearchList().size());
         });
      registrar.playToClient(SyncWarpPayload.TYPE, SyncWarpPayload.STREAM_CODEC,
         (payload, ctx) -> ctx.player().setData(ModAttachments.WARP, payload.warp().copy()));
      registrar.playToClient(KnowledgeGainPayload.TYPE, KnowledgeGainPayload.STREAM_CODEC,
         (payload, ctx) -> ClientResearchEvents.KNOWLEDGE_GAINS.add(new ClientResearchEvents.Gain(payload.knowledgeType(), payload.category())));
      registrar.playToClient(SyncResearchPayload.TYPE, SyncResearchPayload.STREAM_CODEC, (payload, ctx) -> {
         // In singleplayer the server already loaded the very same files into the shared registry.
         if (!payload.files().equals(ResearchData.rawFiles())) ResearchData.load(payload.files(), ctx.player().registryAccess());
      });
      registrar.playToServer(CompleteStagePayload.TYPE, CompleteStagePayload.STREAM_CODEC, (payload, ctx) -> {
         if (ctx.player() instanceof ServerPlayer sp && payload.first() != ResearchManager.knowsResearch(sp, payload.key())) {
            ResearchManager.tryCompleteStage(sp, payload.key(), payload.checks(), payload.noFlags());
         }
      });
      registrar.playToServer(ClearResearchFlagsPayload.TYPE, ClearResearchFlagsPayload.STREAM_CODEC, (payload, ctx) -> {
         var k = ResearchManager.knowledge(ctx.player());
         if (!payload.popupOnly()) {
            k.clearResearchFlag(payload.key(), EnumResearchFlag.RESEARCH);
            k.clearResearchFlag(payload.key(), EnumResearchFlag.PAGE);
         }
         k.clearResearchFlag(payload.key(), EnumResearchFlag.POPUP);
      });
      registrar.playToServer(StartTheoryPayload.TYPE, StartTheoryPayload.STREAM_CODEC, (payload, ctx) -> {
         var player = ctx.player();
         if (player.distanceToSqr(payload.pos().getCenter()) > 64.0) return;
         if (player.level().getBlockEntity(payload.pos()) instanceof ResearchTableBlockEntity be && be.data == null && be.hasInk() && be.hasPaper()) {
            var available = be.checkSurroundingAids();
            var aids = new java.util.LinkedHashSet<>(payload.aids());
            aids.retainAll(available);
            be.startNewTheory(player, aids);
         }
      });
      registrar.playToClient(BamfPayload.TYPE, BamfPayload.STREAM_CODEC, (payload, ctx) -> ClientBridge.bamf.accept(payload));
      registrar.playToClient(FocusFXPayload.TYPE, FocusFXPayload.STREAM_CODEC, (payload, ctx) -> ClientBridge.focusFX.accept(payload));
      registrar.playToClient(ScanSourcePayload.TYPE, ScanSourcePayload.STREAM_CODEC, (payload, ctx) -> ClientBridge.scanSource.accept(payload));
      registrar.playToClient(InfusionFXPayload.TYPE, InfusionFXPayload.STREAM_CODEC, (payload, ctx) -> ClientBridge.infusionFX.accept(payload));
      registrar.playToClient(EssentiaSourcePayload.TYPE, EssentiaSourcePayload.STREAM_CODEC, (payload, ctx) -> ClientBridge.essentiaSource.accept(payload));
      registrar.playToClient(SyncAuraPayload.TYPE, SyncAuraPayload.STREAM_CODEC, (payload, ctx) -> {
         ClientAura.current = new AuraChunk(payload.base(), payload.vis(), payload.flux());
         ClientAura.lastUpdate = ctx.player().level().getGameTime();
      });
   }
}
