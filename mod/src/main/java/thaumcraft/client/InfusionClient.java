package thaumcraft.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.InfusionFX;
import thaumcraft.client.renderers.InfusionRenderers;
import thaumcraft.common.blocks.devices.StabilizerBlockEntity;
import thaumcraft.common.blocks.essentia.TubeFilterBlockEntity;
import thaumcraft.common.blocks.infusion.InlayBlock;
import thaumcraft.common.lib.ClientBridge;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModBlocks;

/** Client registrations of milestone 8 (infusion). */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class InfusionClient {
   private InfusionClient() {
   }

   @SubscribeEvent
   public static void setup(FMLClientSetupEvent event) {
      ClientBridge.infusionFX = InfusionFX::handle;
      ClientBridge.scanSource = InfusionFX::scan;
   }

   @SubscribeEvent
   public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerBlockEntityRenderer(ModBlockEntities.PEDESTAL.get(), InfusionRenderers.Pedestal::new);
      event.registerBlockEntityRenderer(ModBlockEntities.INFUSION_MATRIX.get(), InfusionRenderers.Matrix::new);
   }

   @SubscribeEvent
   public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
      event.registerLayerDefinition(InfusionRenderers.CUBE, () -> InfusionRenderers.cubeLayer(0));
      event.registerLayerDefinition(InfusionRenderers.CUBE_OVER, () -> InfusionRenderers.cubeLayer(32));
   }

   /** ColorHandler: inlay and stabilizer glow with their charge, candles take their dye, filter tubes their aspect. */
   @SubscribeEvent
   public static void blockColors(RegisterColorHandlersEvent.Block event) {
      event.register((state, level, pos, tint) -> tint == 0 ? InlayBlock.color(state.getValue(InlayBlock.CHARGE)) : 0xFFFFFFFF, ModBlocks.INLAY.get());
      event.register((state, level, pos, tint) -> {
         int energy = level != null && pos != null && level.getBlockEntity(pos) instanceof StabilizerBlockEntity s ? s.getEnergy() : 0;
         return tint == 0 ? StabilizerBlockEntity.color(energy) : 0xFFFFFFFF;
      }, ModBlocks.STABILIZER.get());
      for (var candle : ModBlocks.CANDLES.values()) {
         event.register((state, level, pos, tint) -> tint == 0 ? 0xFF000000 | candle.get().dye.getTextureDiffuseColor() : 0xFFFFFFFF, candle.get());
      }
      event.register((state, level, pos, tint) -> level != null && pos != null && level.getBlockEntity(pos) instanceof TubeFilterBlockEntity f
         && f.aspectFilter != null ? 0xFF000000 | f.aspectFilter.getColor() : 0xFFFFFFFF, ModBlocks.TUBE_FILTER.get());
   }

   @SubscribeEvent
   public static void itemColors(RegisterColorHandlersEvent.Item event) {
      for (var candle : ModBlocks.CANDLES.values()) {
         event.register((stack, tint) -> tint == 0 ? 0xFF000000 | candle.get().dye.getTextureDiffuseColor() : 0xFFFFFFFF, candle.get());
      }
   }
}
