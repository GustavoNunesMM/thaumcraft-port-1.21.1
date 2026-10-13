package thaumcraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.ICaster;
import thaumcraft.client.fx.FocusFX;
import thaumcraft.client.gui.FocalManipulatorScreen;
import thaumcraft.client.gui.FocusPouchScreen;
import thaumcraft.client.gui.FocusRadialScreen;
import thaumcraft.client.renderers.CasterRenderers;
import thaumcraft.common.items.casters.CasterItem;
import thaumcraft.common.items.casters.FocusItem;
import thaumcraft.common.lib.ClientBridge;
import thaumcraft.common.network.CasterPayloads;
import thaumcraft.common.registers.ModCasters;
import thaumcraft.common.registers.ModItems;

/** Client side of milestone 9: renderers, screens, colours, the focus overlay property, keys (F/G), HUD and plan preview. */
public final class CastersClient {
   public static final KeyMapping KEY_F = new KeyMapping("key.thaumcraft.change_focus", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
      GLFW.GLFW_KEY_F, "key.categories.thaumcraft");
   public static final KeyMapping KEY_G = new KeyMapping("key.thaumcraft.misc_toggle", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
      GLFW.GLFW_KEY_G, "key.categories.thaumcraft");
   private static boolean keyPressedF;
   private static boolean keyPressedG;
   /** The F key was used up (a focus was chosen or removed) until it is released. */
   public static boolean radialLock;

   private CastersClient() {
   }

   @EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
   public static final class ModBus {
      private ModBus() {
      }

      @SubscribeEvent
      public static void setup(FMLClientSetupEvent event) {
         ClientBridge.focusFX = FocusFX::handle;
         event.enqueueWork(() -> ItemProperties.register(ModItems.CASTER_BASIC.get(), ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "focus"),
            (stack, level, entity, seed) -> stack.getItem() instanceof CasterItem c && c.getFocus(stack) != null ? 1.0F : 0.0F));
      }

      @SubscribeEvent
      public static void keys(RegisterKeyMappingsEvent event) {
         event.register(KEY_F);
         event.register(KEY_G);
      }

      @SubscribeEvent
      public static void screens(RegisterMenuScreensEvent event) {
         event.register(ModCasters.FOCAL_MANIPULATOR_MENU.get(), FocalManipulatorScreen::new);
         event.register(ModCasters.FOCUS_POUCH_MENU.get(), FocusPouchScreen::new);
      }

      @SubscribeEvent
      public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
         event.registerEntityRenderer(ModCasters.FOCUS_PROJECTILE.get(), CasterRenderers.Projectile::new);
         event.registerEntityRenderer(ModCasters.FOCUS_CLOUD.get(), NoopRenderer::new);
         event.registerEntityRenderer(ModCasters.FOCUS_MINE.get(), CasterRenderers.Mine::new);
         event.registerEntityRenderer(ModCasters.SPELL_BAT.get(), CasterRenderers.SpellBat::new);
         event.registerBlockEntityRenderer(ModCasters.FOCAL_MANIPULATOR_BE.get(), CasterRenderers.Manipulator::new);
      }

      @SubscribeEvent
      public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
         event.registerLayerDefinition(CasterRenderers.MINE_LAYER, CasterRenderers::mineLayer);
         event.registerLayerDefinition(CasterRenderers.BAT_LAYER, CasterRenderers::batLayer);
      }

      /** ColorHandler: the gauntlet's focus (tint 1) and the foci take the colour of the package's effects. */
      @SubscribeEvent
      public static void itemColors(RegisterColorHandlersEvent.Item event) {
         event.register((stack, tint) -> {
            if (tint > 0 && stack.getItem() instanceof CasterItem c) {
               ItemStack f = c.getFocusStack(stack);
               if (f != null) return 0xFF000000 | FocusItem.getFocusColor(f);
            }
            return 0xFFFFFFFF;
         }, ModItems.CASTER_BASIC.get());
         event.register((stack, tint) -> 0xFF000000 | FocusItem.getFocusColor(stack), ModCasters.FOCUS_1.get(), ModCasters.FOCUS_2.get(),
            ModCasters.FOCUS_3.get());
      }

      @SubscribeEvent
      public static void guiLayers(RegisterGuiLayersEvent event) {
         event.registerAboveAll(CasterHud.ID, CasterHud::render);
      }
   }

   @EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
   public static final class GameBus {
      private GameBus() {
      }

      @SubscribeEvent
      public static void clientTick(ClientTickEvent.Post event) {
         FocusFX.tick();
         keys();
      }

      @SubscribeEvent
      public static void renderLevel(RenderLevelStageEvent event) {
         if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) ArchitectOverlay.render(event);
      }
   }

   private static boolean holdsCaster(Player p) {
      return p.getMainHandItem().getItem() instanceof ICaster || p.getOffhandItem().getItem() instanceof ICaster;
   }

   /** KeyHandler.playerTick: F opens the focus selector while held (sneak + F removes the focus); G is the misc toggle. */
   private static void keys() {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null) return;
      Screen screen = mc.screen;
      if (KEY_F.isDown()) {
         if (screen == null) {
            if (!keyPressedF) radialLock = false;
            if (!radialLock && holdsCaster(player)) {
               if (player.isShiftKeyDown()) {
                  if (!keyPressedF) CasterPayloads.send(new CasterPayloads.FocusChange("REMOVE"));
                  radialLock = true;
               } else {
                  FocusRadialScreen.open();
               }
            }
            keyPressedF = true;
         }
      } else if (!(screen instanceof FocusRadialScreen)) {
         keyPressedF = false;
      }
      if (KEY_G.isDown()) {
         if (screen == null && !keyPressedG) {
            int mod = Screen.hasControlDown() ? 1 : Screen.hasShiftDown() ? 2 : 0;
            CasterPayloads.send(new CasterPayloads.ItemKey(1, mod));
         }
         keyPressedG = true;
      } else {
         keyPressedG = false;
      }
   }
}
