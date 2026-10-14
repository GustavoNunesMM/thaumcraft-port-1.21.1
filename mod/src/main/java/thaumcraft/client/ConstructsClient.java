package thaumcraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.gui.ConstructScreens;
import thaumcraft.client.renderers.ConstructRenderers;
import thaumcraft.common.registers.ModMobs;

/** Client side of the constructs (milestone 13): turret and bore renderers, the golem dart, and their screens. */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ConstructsClient {
   private ConstructsClient() {
   }

   @SubscribeEvent
   public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ModMobs.TURRET_CROSSBOW.get(), ConstructRenderers.TurretCrossbow::new);
      event.registerEntityRenderer(ModMobs.TURRET_CROSSBOW_ADVANCED.get(), ConstructRenderers.TurretCrossbowAdvanced::new);
      event.registerEntityRenderer(ModMobs.ARCANE_BORE.get(), ConstructRenderers.ArcaneBore::new);
      event.registerEntityRenderer(ModMobs.GOLEM_DART.get(), ConstructRenderers.Dart::new);
   }

   @SubscribeEvent
   public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
      event.registerLayerDefinition(ConstructRenderers.CROSSBOW_LAYER, ConstructRenderers::crossbowLayer);
      event.registerLayerDefinition(ConstructRenderers.BORE_LAYER, ConstructRenderers::boreLayer);
   }

   @SubscribeEvent
   public static void screens(RegisterMenuScreensEvent event) {
      event.register(ModMobs.TURRET_BASIC_MENU.get(), ConstructScreens.TurretBasic::new);
      event.register(ModMobs.TURRET_ADVANCED_MENU.get(), ConstructScreens.TurretAdvanced::new);
      event.register(ModMobs.ARCANE_BORE_MENU.get(), ConstructScreens.ArcaneBore::new);
   }
}
