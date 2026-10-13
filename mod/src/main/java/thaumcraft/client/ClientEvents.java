package thaumcraft.client;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.EnumResearchFlag;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.client.gui.ResearchBrowserScreen;
import thaumcraft.client.gui.ResearchToast;
import thaumcraft.common.lib.ClientBridge;
import thaumcraft.common.network.ClearResearchFlagsPayload;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.blocks.world.CrystalBlock;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;

@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ClientEvents {
   private ClientEvents() {
   }

   private static boolean shiftDown() {
      return Screen.hasShiftDown() || ClientSmokeTest.forceShift;
   }

   private static AspectList tooltipAspects(ItemStack stack) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || !(mc.screen instanceof AbstractContainerScreen<?>)) return null;
      return AspectManager.CLIENT.getObjectAspects(mc.level.registryAccess(), stack);
   }

   /** Hint line so players know the aspects are there; the icons themselves need Shift, as in the original. */
   @SubscribeEvent
   public static void tooltipText(ItemTooltipEvent event) {
      if (!shiftDown() && tooltipAspects(event.getItemStack()) != null) {
         event.getToolTip().add(Component.translatable("tc.port.shiftaspects").withStyle(ChatFormatting.DARK_PURPLE));
      }
   }

   @SubscribeEvent
   public static void tooltipIcons(RenderTooltipEvent.GatherComponents event) {
      if (!shiftDown()) return;
      AspectList aspects = tooltipAspects(event.getItemStack());
      if (aspects != null) event.getTooltipElements().add(Either.right(new AspectTooltip(aspects)));
   }

   @SubscribeEvent
   public static void clientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
      KnowledgeGainHud.tick();
   }

   @SubscribeEvent
   public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
      AspectManager.CLIENT.clear();
   }

   @EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
   public static final class ModBus {
      private ModBus() {
      }

      @SubscribeEvent
      public static void tooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
         event.register(AspectTooltip.class, AspectTooltip.Client::new);
      }

      @SubscribeEvent
      public static void guiLayers(RegisterGuiLayersEvent event) {
         event.registerAboveAll(AuraHud.ID, AuraHud::render);
         event.registerAboveAll(KnowledgeGainHud.ID, KnowledgeGainHud::render);
      }

      @SubscribeEvent
      public static void menuScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
         event.register(thaumcraft.common.registers.ModMenus.RESEARCH_TABLE.get(), thaumcraft.client.gui.ResearchTableScreen::new);
         event.register(thaumcraft.common.registers.ModMenus.ARCANE_WORKBENCH.get(), thaumcraft.client.gui.ArcaneWorkbenchScreen::new);
      }

      @SubscribeEvent
      public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
         event.registerBlockEntityRenderer(thaumcraft.common.registers.ModBlockEntities.CRUCIBLE.get(), thaumcraft.client.renderers.CrucibleRenderer::new);
         event.registerEntityRenderer(thaumcraft.common.registers.ModEntities.SPECIAL_ITEM.get(), net.minecraft.client.renderer.entity.ItemEntityRenderer::new);
         event.registerEntityRenderer(thaumcraft.common.registers.ModEntities.ALUMENTUM.get(), net.minecraft.client.renderer.entity.ThrownItemRenderer::new);
         event.registerBlockEntityRenderer(thaumcraft.common.registers.ModBlockEntities.RESEARCH_TABLE.get(),
            thaumcraft.client.renderers.ResearchTableRenderer::new);
      }

      @SubscribeEvent
      public static void layers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
         event.registerLayerDefinition(thaumcraft.client.renderers.ResearchTableRenderer.LAYER, thaumcraft.client.renderers.ResearchTableRenderer::createLayer);
      }

      @SubscribeEvent
      public static void clientSetup(FMLClientSetupEvent event) {
         ClientBridge.openThaumonomicon = () -> Minecraft.getInstance().setScreen(new ResearchBrowserScreen());
         ClientBridge.bamf = p -> {
            net.minecraft.core.Direction side = p.side() < 0 ? null : net.minecraft.core.Direction.from3DDataValue(p.side());
            if (p.color() != -9999) thaumcraft.client.fx.FXDispatcher.drawBamf(p.x(), p.y(), p.z(), p.color(), p.sound(), p.flair(), side);
            else thaumcraft.client.fx.FXDispatcher.drawBamf(p.x(), p.y(), p.z(), p.sound(), p.flair(), side);
         };
         ClientBridge.knowledgeSynced = k -> {
            for (String key : java.util.List.copyOf(k.getResearchList())) {
               if (k.hasResearchFlag(key, EnumResearchFlag.POPUP)) {
                  ResearchEntry ri = ResearchCategories.getResearch(key);
                  if (ri != null) Minecraft.getInstance().getToasts().addToast(new ResearchToast(ri));
                  k.clearResearchFlag(key, EnumResearchFlag.POPUP);
                  PacketDistributor.sendToServer(new ClearResearchFlagsPayload(key, true));
               }
            }
         };
      }

      /** ColorHandler: crystals (block and item) take the colour of their aspect; greatwood leaves the biome's foliage colour. */
      @SubscribeEvent
      public static void blockColors(RegisterColorHandlersEvent.Block event) {
         event.register((state, level, pos, tint) -> level != null && pos != null
            ? BiomeColors.getAverageFoliageColor(level, pos) : FoliageColor.getDefaultColor(), ModBlocks.LEAVES_GREATWOOD.get());
         event.register((state, level, pos, tint) -> state.getBlock() instanceof CrystalBlock c ? c.aspect.getColor() : 0xFFFFFF,
            ModBlocks.crystals().toArray(new Block[0]));
      }

      @SubscribeEvent
      public static void itemColors(RegisterColorHandlersEvent.Item event) {
         for (var nitor : ModBlocks.NITOR.values()) {
            event.register((stack, tint) -> tint == 0 ? 0xFF000000 | nitor.get().dye.getMapColor().col : 0xFFFFFFFF, nitor.get());
         }
         event.register((stack, tint) -> 0xFF000000 | FoliageColor.getDefaultColor(), ModBlocks.LEAVES_GREATWOOD.get());
         for (CrystalBlock c : ModBlocks.crystals()) {
            event.register((stack, tint) -> 0xFF000000 | c.aspect.getColor(), c);
         }
         event.register((stack, tint) -> {
            Aspect a = VisCrystalItem.getAspect(stack);
            return 0xFF000000 | (a == null ? 0xFFFFFF : a.getColor());
         }, ModItems.CRYSTAL_ESSENCE.get());
      }
   }
}
