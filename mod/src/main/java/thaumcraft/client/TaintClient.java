package thaumcraft.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.gui.VoidSiphonScreen;
import thaumcraft.client.renderers.TaintRenderers;
import thaumcraft.common.registers.ModFlux;

/** Client registrations of milestone 12 (flux and taint). */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class TaintClient {
   private TaintClient() {
   }

   @SubscribeEvent
   public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ModFlux.FLUX_RIFT.get(), TaintRenderers.FluxRiftRenderer::new);
      event.registerEntityRenderer(ModFlux.FALLING_TAINT.get(), TaintRenderers.FallingTaintRenderer::new);
      event.registerEntityRenderer(ModFlux.BOTTLE_TAINT_ENTITY.get(), ThrownItemRenderer::new);
      event.registerEntityRenderer(ModFlux.CAUSALITY_COLLAPSER_ENTITY.get(), ThrownItemRenderer::new);
      event.registerEntityRenderer(ModFlux.TAINT_CRAWLER.get(), TaintRenderers.CrawlerRenderer::new);
      event.registerEntityRenderer(ModFlux.TAINTACLE.get(), ctx -> new TaintRenderers.TaintacleRenderer(ctx, 0.6F, 10));
      event.registerEntityRenderer(ModFlux.TAINTACLE_TINY.get(), ctx -> new TaintRenderers.TaintacleRenderer(ctx, 0.2F, 6));
      event.registerEntityRenderer(ModFlux.TAINT_SEED.get(), ctx -> new TaintRenderers.TaintSeedRenderer(ctx, 0.4F));
      event.registerEntityRenderer(ModFlux.TAINT_SEED_PRIME.get(), ctx -> new TaintRenderers.TaintSeedRenderer(ctx, 0.6F));
      event.registerEntityRenderer(ModFlux.TAINT_SWARM.get(), TaintRenderers.SwarmRenderer::new);
      event.registerBlockEntityRenderer(ModFlux.VOID_SIPHON_BE.get(), TaintRenderers.VoidSiphonRenderer::new);
   }

   @SubscribeEvent
   public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
      event.registerLayerDefinition(TaintRenderers.SEGMENTS, TaintRenderers::segmentsLayer);
   }

   @SubscribeEvent
   public static void screens(RegisterMenuScreensEvent event) {
      event.register(ModFlux.VOID_SIPHON_MENU.get(), VoidSiphonScreen::new);
   }

   private static IClientFluidTypeExtensions fluid(String still, String flow, int tint) {
      ResourceLocation s = ResourceLocation.parse(still);
      ResourceLocation f = ResourceLocation.parse(flow);
      return new IClientFluidTypeExtensions() {
         @Override
         public ResourceLocation getStillTexture() {
            return s;
         }

         @Override
         public ResourceLocation getFlowingTexture() {
            return f;
         }

         @Override
         public int getTintColor() {
            return tint;
         }
      };
   }

   @SubscribeEvent
   public static void fluids(RegisterClientExtensionsEvent event) {
      event.registerFluidType(fluid("thaumcraft:blocks/flux_goo", "thaumcraft:blocks/flux_goo", 0xFFFFFFFF), ModFlux.FLUX_GOO_TYPE.get());
      event.registerFluidType(fluid("thaumcraft:blocks/animatedglow", "thaumcraft:blocks/animatedglow", 0xF0440099), ModFlux.LIQUID_DEATH_TYPE.get());
      event.registerFluidType(fluid("minecraft:block/water_still", "minecraft:block/water_flow", 0x77FFCCAA), ModFlux.PURIFYING_FLUID_TYPE.get());
   }
}
