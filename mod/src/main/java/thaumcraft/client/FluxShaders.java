package thaumcraft.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.compat.ShaderCompat;
import thaumcraft.common.registers.ModFlux;

/**
 * ShaderHandler.checkShaders: the warp potions tint the screen with a post-processing shader (death gaze desaturates,
 * blurred vision blurs, unnatural hunger reddens, sun scorned blooms).
 *
 * <p>Port note: 1.12 stacked one ShaderGroup per effect; the 1.21 GameRenderer holds a single post effect, so only the
 * first active one of the list below is shown. The shaders are the original ones moved to GLSL 150
 * (assets/thaumcraft/shaders, written by tools/gen_marco12.py). The death gaze vignette (RenderEventHandler) is not drawn.
 *
 * <p>Shader packs (Iris/Oculus): a vanilla PostChain fights the pack's own composite passes, so while a pack is active the
 * post effect is never loaded (and ours is shut down if a pack gets enabled) and a flat full-screen HUD tint that
 * approximates the effect is drawn instead (see {@link #renderFallback}).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class FluxShaders {
   private static ResourceLocation active;
   /** The effect to approximate with a HUD tint because a shader pack is active (null otherwise). */
   private static ResourceLocation fallback;
   private static final ResourceLocation FALLBACK_LAYER = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "flux_shader_fallback");
   /** Shaders that failed to load (no shader support, broken pack): not retried every tick. */
   private static final java.util.Set<ResourceLocation> FAILED = new java.util.HashSet<>();

   private FluxShaders() {
   }

   private static ResourceLocation shader(String name) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "shaders/post/" + name + ".json");
   }

   private static ResourceLocation wanted(Player player) {
      Object[][] list = {{ModFlux.DEATH_GAZE, "desaturatetc"}, {ModFlux.BLURRED_VISION, "blurtc"}, {ModFlux.UNNATURAL_HUNGER, "hunger"},
         {ModFlux.SUN_SCORNED, "sunscorned"}};
      for (Object[] e : list) {
         @SuppressWarnings("unchecked")
         Holder<MobEffect> effect = (Holder<MobEffect>) e[0];
         if (player.hasEffect(effect)) return shader((String) e[1]);
      }
      return null;
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      GameRenderer gr = mc.gameRenderer;
      if (mc.player == null || mc.level == null) {
         active = null;
         return;
      }
      ResourceLocation want = wanted(mc.player);
      PostChain current = gr.currentEffect();
      boolean ours = active != null && current != null && current.getName().equals(active.toString());
      fallback = null;
      if (want != null && ShaderCompat.isShaderPackInUse()) {
         // The shader pack owns post-processing: drop our chain and use the HUD tint instead.
         if (ours) gr.shutdownEffect();
         active = null;
         fallback = want;
         return;
      }
      if (want == null) {
         if (ours) gr.shutdownEffect();
         active = null;
      } else if (!ours || !want.equals(active)) {
         // Don't fight the vanilla spectator effects (creeper/spider/enderman views).
         if (current != null && !ours || FAILED.contains(want)) return;
         gr.loadEffect(want);
         if (gr.currentEffect() == null) {
            FAILED.add(want);
            active = null;
         } else {
            active = want;
         }
      }
   }

   /**
    * Shader-pack fallback: a full-screen tint per effect. Unnatural hunger multiplies by the same (1, 0.8, 0.8)
    * ColorScale as its post shader; the others (desaturation, blur, bloom) can't be done without the frame buffer and
    * become a grey, a hazy or a warm wash.
    */
   static void renderFallback(GuiGraphics g, DeltaTracker delta) {
      ResourceLocation fx = fallback;
      if (fx == null || Minecraft.getInstance().player == null) return;
      String name = fx.getPath();
      int w = g.guiWidth(), h = g.guiHeight();
      g.flush();
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      if (name.endsWith("hunger.json")) {
         RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);
         fill(g, w, h, 1.0F, 0.8F, 0.8F, 1.0F);
      } else if (name.endsWith("desaturatetc.json")) {
         RenderSystem.defaultBlendFunc();
         fill(g, w, h, 0.5F, 0.5F, 0.5F, 0.2F);
      } else if (name.endsWith("blurtc.json")) {
         RenderSystem.defaultBlendFunc();
         fill(g, w, h, 0.75F, 0.75F, 0.78F, 0.35F);
      } else if (name.endsWith("sunscorned.json")) {
         RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
         fill(g, w, h, 1.0F, 0.85F, 0.6F, 0.15F);
      }
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   }

   private static void fill(GuiGraphics g, int w, int h, float r, float gr, float b, float a) {
      Matrix4f m = g.pose().last().pose();
      BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
      bb.addVertex(m, 0, h, 0).setColor(r, gr, b, a);
      bb.addVertex(m, w, h, 0).setColor(r, gr, b, a);
      bb.addVertex(m, w, 0, 0).setColor(r, gr, b, a);
      bb.addVertex(m, 0, 0, 0).setColor(r, gr, b, a);
      BufferUploader.drawWithShader(bb.buildOrThrow());
   }

   @EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
   public static final class ModBus {
      private ModBus() {
      }

      @SubscribeEvent
      public static void guiLayers(RegisterGuiLayersEvent event) {
         event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, FALLBACK_LAYER, FluxShaders::renderFallback);
      }
   }
}
