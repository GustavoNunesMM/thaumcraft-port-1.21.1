package thaumcraft.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.client.fx.EssentiaFX;
import thaumcraft.client.gui.SmelterScreen;
import thaumcraft.client.renderers.EssentiaRenderers;
import thaumcraft.common.blocks.essentia.JarBlockEntity;
import thaumcraft.common.items.JarItem;
import thaumcraft.common.lib.ClientBridge;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModMenus;

/** Client registrations of milestone 7 (essentia). */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class EssentiaClient {
   private EssentiaClient() {
   }

   @SubscribeEvent
   public static void setup(FMLClientSetupEvent event) {
      ClientBridge.essentiaSource = EssentiaFX::essentiaStream;
      event.enqueueWork(() -> {
         // BlockJarItem "fill": 0 empty, 1..4 for up to a quarter, half, three quarters, full.
         ResourceLocation fill = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fill");
         for (var jar : new net.minecraft.world.item.Item[]{ModItems.JAR_NORMAL.get(), ModItems.JAR_VOID.get()}) {
            ItemProperties.register(jar, fill, (stack, level, entity, seed) -> {
               int a = JarItem.amount(stack);
               if (a <= 0) return 0.0F;
               float r = a / (float) JarBlockEntity.CAPACITY;
               return r <= 0.25F ? 1.0F : r <= 0.5F ? 2.0F : r <= 0.75F ? 3.0F : 4.0F;
            });
         }
      });
   }

   @SubscribeEvent
   public static void screens(RegisterMenuScreensEvent event) {
      event.register(ModMenus.SMELTER.get(), SmelterScreen::new);
   }

   @SubscribeEvent
   public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerBlockEntityRenderer(ModBlockEntities.JAR.get(), EssentiaRenderers.Jar::new);
      event.registerBlockEntityRenderer(ModBlockEntities.TUBE_VALVE.get(), EssentiaRenderers.Valve::new);
      event.registerBlockEntityRenderer(ModBlockEntities.TUBE_ONEWAY.get(), EssentiaRenderers.Oneway::new);
      event.registerBlockEntityRenderer(ModBlockEntities.TUBE_BUFFER.get(), EssentiaRenderers.Buffer::new);
      event.registerBlockEntityRenderer(ModBlockEntities.CENTRIFUGE.get(), EssentiaRenderers.Centrifuge::new);
      event.registerBlockEntityRenderer(ModBlockEntities.ALEMBIC.get(), EssentiaRenderers.Alembic::new);
      event.registerBlockEntityRenderer(ModBlockEntities.BELLOWS.get(), EssentiaRenderers.Bellows::new);
   }

   @SubscribeEvent
   public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
      event.registerLayerDefinition(EssentiaRenderers.JAR, EssentiaRenderers::jarLayer);
      event.registerLayerDefinition(EssentiaRenderers.VALVE, EssentiaRenderers::valveLayer);
      event.registerLayerDefinition(EssentiaRenderers.CENTRIFUGE, EssentiaRenderers::centrifugeLayer);
      event.registerLayerDefinition(EssentiaRenderers.BELLOWS, EssentiaRenderers::bellowsLayer);
      event.registerLayerDefinition(EssentiaRenderers.BORE_NOZZLE, EssentiaRenderers::nozzleLayer);
   }

   private static int essentiaColor(net.minecraft.world.item.ItemStack stack) {
      AspectList al = stack.get(ModDataComponents.ESSENTIA.get());
      return al == null || al.isEmpty() ? 0xFFFFFFFF : 0xFF000000 | al.getAspects()[0].getColor();
   }

   @SubscribeEvent
   public static void itemColors(RegisterColorHandlersEvent.Item event) {
      event.register((stack, tint) -> tint == 1 ? essentiaColor(stack) : 0xFFFFFFFF, ModItems.PHIAL_FILLED.get(), ModItems.JAR_NORMAL.get(),
         ModItems.JAR_VOID.get());
      event.register((stack, tint) -> {
         Aspect a = stack.get(ModDataComponents.ASPECT_FILTER.get());
         return tint == 1 && a != null ? 0xFF000000 | a.getColor() : 0xFFFFFFFF;
      }, ModItems.LABEL_FILLED.get());
   }
}
