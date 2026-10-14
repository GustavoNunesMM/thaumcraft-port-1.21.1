package thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.EldritchFX;
import thaumcraft.client.renderers.EldritchRenderers;
import thaumcraft.client.renderers.TaintRenderers;
import thaumcraft.client.renderers.models.EldritchModelLayers;
import thaumcraft.common.network.EldritchPayload;
import thaumcraft.common.registers.ModEldritch;
import thaumcraft.common.registers.ModSounds;

/**
 * Client side of milestone 14: renderers and model layers, the PacketMiscEvent effects (warp heartbeat, the creeping
 * warp mist - RenderEventHandler.fogFiddled) and the screech / void shield particles.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class EldritchClient {
   /** RenderEventHandler fogFiddled / fogTarget / fogDuration. */
   static boolean fogFiddled = false;
   static float fogTarget = 0.0F;
   static int fogDuration = 0;

   private EldritchClient() {
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   @SubscribeEvent
   public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ModEldritch.ELDRITCH_GUARDIAN.get(), ctx -> new EldritchRenderers.GuardianRenderer<>(ctx, 0.5F));
      event.registerEntityRenderer(ModEldritch.ELDRITCH_WARDEN.get(), ctx -> new EldritchRenderers.GuardianRenderer<>(ctx, 0.6F));
      event.registerEntityRenderer(ModEldritch.ELDRITCH_GOLEM.get(), EldritchRenderers.GolemRenderer::new);
      event.registerEntityRenderer(ModEldritch.ELDRITCH_CRAB.get(), EldritchRenderers.CrabRenderer::new);
      event.registerEntityRenderer(ModEldritch.INHABITED_ZOMBIE.get(), EldritchRenderers.InhabitedZombieRenderer::new);
      event.registerEntityRenderer(ModEldritch.TAINTACLE_GIANT.get(), (EntityRendererProvider) ctx -> new TaintRenderers.TaintacleRenderer(ctx, 1.0F, 14));
      event.registerEntityRenderer(ModEldritch.ELDRITCH_ORB.get(), EldritchRenderers.OrbRenderer::new);
      event.registerBlockEntityRenderer(ModEldritch.HOLE_BE.get(), EldritchRenderers.HoleRenderer::new);
      event.registerBlockEntityRenderer(ModEldritch.ELDRITCH_PORTAL_BE.get(), EldritchRenderers.PortalRenderer::new);
   }

   @SubscribeEvent
   public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
      event.registerLayerDefinition(EldritchRenderers.GUARDIAN, EldritchModelLayers::guardian);
      event.registerLayerDefinition(EldritchRenderers.GOLEM, EldritchModelLayers::golem);
      event.registerLayerDefinition(EldritchRenderers.CRAB, EldritchModelLayers::crab);
   }

   /** PacketMiscEvent / PacketFXSonic / PacketFXShield. */
   public static void handle(EldritchPayload p) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null || mc.level == null) return;
      switch (p.kind()) {
         case EldritchPayload.HEARTBEAT -> mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(), ModSounds.HEARTBEAT.get(),
            SoundSource.AMBIENT, 1.0F, 1.0F, false);
         case EldritchPayload.MIST -> {
            fogFiddled = true;
            fogDuration = 2400;
         }
         case EldritchPayload.GUARDIAN_FOG -> {
            fogFiddled = true;
            if (fogDuration < 200) fogDuration = 200;
         }
         case EldritchPayload.SONIC -> {
            Entity e = mc.level.getEntity(p.value());
            if (e != null) EldritchFX.sonic(e);
         }
         case EldritchPayload.SHIELD -> {
            Entity e = mc.level.getEntity(p.value());
            if (e != null) EldritchFX.shield(e);
         }
         default -> {
         }
      }
   }

   /** The game bus half (fog and its countdown). */
   @EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
   public static final class GameEvents {
      private GameEvents() {
      }

      @SubscribeEvent
      public static void tick(ClientTickEvent.Pre event) {
         EldritchClient.tick(event);
      }

      @SubscribeEvent
      public static void fog(ViewportEvent.RenderFog event) {
         EldritchClient.fog(event);
      }
   }

   /** RenderEventHandler.playerTick: the mist thickens to its full density, then thins out over the last 100 ticks. */
   private static void tick(ClientTickEvent.Pre event) {
      if (Minecraft.getInstance().player == null || !fogFiddled) return;
      if (fogDuration < 100) {
         fogTarget = 0.1F * (fogDuration / 100.0F);
      } else if (fogTarget < 0.1F) {
         fogTarget += 0.001F;
      }
      fogDuration--;
      if (fogDuration < 0) {
         fogFiddled = false;
         fogTarget = 0.0F;
      }
   }

   /**
    * RenderEventHandler.fogDensityEvent: exponential fog of density fogTarget (GL_EXP). 1.21 fog is linear, so the far
    * plane is put where an exponential fog would be nearly opaque (about 3 / density).
    */
   private static void fog(ViewportEvent.RenderFog event) {
      if (!fogFiddled || fogTarget <= 0.0F) return;
      float far = Math.min(event.getFarPlaneDistance(), 3.0F / fogTarget);
      event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), far * 0.05F));
      event.setFarPlaneDistance(far);
      event.setCanceled(true);
   }
}
