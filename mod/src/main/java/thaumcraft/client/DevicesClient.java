package thaumcraft.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.gui.DeviceScreens;
import thaumcraft.client.renderers.tile.DeviceRenderers;
import thaumcraft.common.blocks.devices.BannerBlock;
import thaumcraft.common.registers.ModDevices;

/** Client registrations of the devices milestone. */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class DevicesClient {
   private DevicesClient() {
   }

   @SubscribeEvent
   public static void setup(FMLClientSetupEvent event) {
      event.enqueueWork(() -> {
         // BlockMirrorItem / ItemHandMirror: the "linked" look (damage 1 in 1.12).
         ResourceLocation linked = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "linked");
         for (Item i : new Item[]{ModDevices.MIRROR_ITEM.get(), ModDevices.MIRROR_ESSENTIA_ITEM.get()}) {
            ItemProperties.register(i, linked, (stack, level, entity, seed) -> stack.has(ModDevices.MIRROR_LINK.get()) ? 1.0F : 0.0F);
         }
      });
   }

   @SubscribeEvent
   public static void screens(RegisterMenuScreensEvent event) {
      event.register(ModDevices.SPA_MENU.get(), DeviceScreens.Spa::new);
      event.register(ModDevices.THAUMATORIUM_MENU.get(), DeviceScreens.Thaumatorium::new);
      event.register(ModDevices.POTION_SPRAYER_MENU.get(), DeviceScreens.PotionSprayer::new);
      event.register(ModDevices.HAND_MIRROR_MENU.get(), DeviceScreens.HandMirror::new);
   }

   @SubscribeEvent
   public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerBlockEntityRenderer(ModDevices.HUNGRY_CHEST_BE.get(), DeviceRenderers.HungryChest::new);
      event.registerBlockEntityRenderer(ModDevices.DIOPTRA_BE.get(), DeviceRenderers.Dioptra::new);
      event.registerBlockEntityRenderer(ModDevices.RECHARGE_PEDESTAL_BE.get(), DeviceRenderers.RechargePedestal::new);
      event.registerBlockEntityRenderer(ModDevices.THAUMATORIUM_BE.get(), DeviceRenderers.Thaumatorium::new);
      event.registerBlockEntityRenderer(ModDevices.PATTERN_CRAFTER_BE.get(), DeviceRenderers.PatternCrafter::new);
      event.registerBlockEntityRenderer(ModDevices.MIRROR_BE.get(), DeviceRenderers.Mirror::new);
      event.registerBlockEntityRenderer(ModDevices.MIRROR_ESSENTIA_BE.get(), DeviceRenderers.Mirror::new);
      event.registerBlockEntityRenderer(ModDevices.JAR_BRAIN_BE.get(), DeviceRenderers.JarBrain::new);
      event.registerBlockEntityRenderer(ModDevices.BANNER_BE.get(), DeviceRenderers.Banner::new);
   }

   @SubscribeEvent
   public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
      event.registerLayerDefinition(DeviceRenderers.CHEST, DeviceRenderers::chestLayer);
      event.registerLayerDefinition(DeviceRenderers.BRAIN, DeviceRenderers::brainLayer);
      event.registerLayerDefinition(DeviceRenderers.BANNER, DeviceRenderers::bannerLayer);
   }

   /** Banner items: the cloth (layer 1) and its symbol take the dye colour. */
   @SubscribeEvent
   public static void itemColors(RegisterColorHandlersEvent.Item event) {
      for (BannerBlock b : ModDevices.allBanners()) {
         if (b.dye == null) continue;
         int color = 0xFF000000 | b.dye.getTextureDiffuseColor();
         event.register((stack, tint) -> tint == 1 ? color : 0xFFFFFFFF, b.asItem());
      }
   }
}
