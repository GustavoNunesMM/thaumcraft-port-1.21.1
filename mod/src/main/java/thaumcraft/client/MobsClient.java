package thaumcraft.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.MobFX;
import thaumcraft.client.gui.PechScreen;
import thaumcraft.client.renderers.MobRenderers;
import thaumcraft.common.lib.MobFXBridge;
import thaumcraft.common.registers.ModMobs;

/** Client side of milestone 13 (mobs): renderers, the pech screen, the primordial pearl's look and the mob effects. */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MobsClient {
   private MobsClient() {
   }

   @SubscribeEvent
   public static void setup(FMLClientSetupEvent event) {
      MobFXBridge.INSTANCE = MobFX.INSTANCE;
      event.enqueueWork(() -> ItemProperties.register(ModMobs.PRIMORDIAL_PEARL.get(), ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "type"),
         (stack, level, entity, seed) -> stack.getDamageValue() < 3 ? 0.0F : stack.getDamageValue() < 6 ? 1.0F : 2.0F));
   }

   @SubscribeEvent
   public static void screens(RegisterMenuScreensEvent event) {
      event.register(ModMobs.PECH_MENU.get(), PechScreen::new);
   }

   @SubscribeEvent
   public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
      event.registerLayerDefinition(MobRenderers.PECH_LAYER, MobRenderers::pechLayer);
      event.registerLayerDefinition(MobRenderers.CULTIST_LAYER, MobRenderers::cultistLayer);
      // ModelFireBat is CasterRenderers.BAT_LAYER, registered by CastersClient.
   }

   @SubscribeEvent
   public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ModMobs.WISP.get(), MobRenderers.Wisp::new);
      event.registerEntityRenderer(ModMobs.FIREBAT.get(), MobRenderers.FireBat::new);
      event.registerEntityRenderer(ModMobs.PECH.get(), MobRenderers.Pech::new);
      event.registerEntityRenderer(ModMobs.MIND_SPIDER.get(), MobRenderers.MindSpider::new);
      event.registerEntityRenderer(ModMobs.BRAINY_ZOMBIE.get(), MobRenderers.BrainyZombie::new);
      event.registerEntityRenderer(ModMobs.GIANT_BRAINY_ZOMBIE.get(), MobRenderers.BrainyZombie::new);
      event.registerEntityRenderer(ModMobs.THAUMIC_SLIME.get(), MobRenderers.ThaumicSlime::new);
      event.registerEntityRenderer(ModMobs.CULTIST_KNIGHT.get(), MobRenderers.Cultist.of(1.0F));
      event.registerEntityRenderer(ModMobs.CULTIST_CLERIC.get(), MobRenderers.Cultist.of(1.0F));
      event.registerEntityRenderer(ModMobs.CULTIST_LEADER.get(), MobRenderers.Cultist.of(1.15F));
      event.registerEntityRenderer(ModMobs.CULTIST_PORTAL_LESSER.get(), MobRenderers.PortalLesser::new);
      event.registerEntityRenderer(ModMobs.CULTIST_PORTAL_GREATER.get(), MobRenderers.PortalGreater::new);
      event.registerEntityRenderer(ModMobs.GOLEM_ORB.get(), MobRenderers.ElectricOrb::new);
   }
}
