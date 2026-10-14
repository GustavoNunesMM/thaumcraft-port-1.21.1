package thaumcraft.client.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Render types for the world-space overlays drawn from RenderLevelStageEvent (architect outlines, golem seals).
 *
 * <p>They used to be drawn with Tesselator + BufferUploader and hand-set GL state, which shader packs (Iris/Oculus)
 * can't capture. These types reproduce the same state through vanilla shards (vanilla position_tex_color shader,
 * vanilla transparency/depth/cull shards), so they can be fed through
 * {@code Minecraft.getInstance().renderBuffers().bufferSource()} and Iris redirects them to its own programs.
 */
public final class OverlayRenderTypes extends RenderType {
   private static final Map<String, RenderType> CACHE = new HashMap<>();
   private static final Map<ResourceLocation, RenderType> GLOW = new HashMap<>();
   /** Vanilla position_tex_color core shader (1.21.1 has no predefined shard for it). */
   private static final ShaderStateShard POSITION_TEX_COLOR = new ShaderStateShard(GameRenderer::getPositionTexColorShader);
   /**
    * Vanilla's NO_DEPTH_TEST shard assumes the depth test is already off, which is not the case during
    * RenderLevelStageEvent: this (layering slot, applied after the depth shard) really turns it off and back on.
    */
   private static final LayeringStateShard DEPTH_OFF = new LayeringStateShard("thaumcraft_depth_off", RenderSystem::disableDepthTest,
      RenderSystem::enableDepthTest);

   private OverlayRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort,
      Runnable setup, Runnable clear) {
      super(name, format, mode, size, crumbling, sort, setup, clear);
   }

   /**
    * A POSITION_TEX_COLOR quad type, never culled.
    *
    * @param additive   GL_SRC_ALPHA, GL_ONE blending instead of the default alpha blending
    * @param depthTest  false draws through walls
    * @param depthWrite whether the quads write depth
    */
   public static RenderType overlay(ResourceLocation tex, boolean additive, boolean depthTest, boolean depthWrite) {
      String key = tex + "|" + additive + depthTest + depthWrite;
      return CACHE.computeIfAbsent(key, k -> {
         boolean atlas = tex.equals(InventoryMenu.BLOCK_ATLAS);
         return create("thaumcraft_overlay", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, !additive,
            CompositeState.builder()
               .setShaderState(POSITION_TEX_COLOR)
               .setTextureState(new TextureStateShard(tex, false, atlas))
               .setTransparencyState(additive ? LIGHTNING_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
               .setDepthTestState(depthTest ? LEQUAL_DEPTH_TEST : NO_DEPTH_TEST)
               .setLayeringState(depthTest ? NO_LAYERING : DEPTH_OFF)
               .setCullState(NO_CULL)
               .setWriteMaskState(depthWrite ? COLOR_DEPTH_WRITE : COLOR_WRITE)
               .createCompositeState(false));
      });
   }

   /**
    * Like {@link RenderType#eyes} (vanilla eyes shader: full-bright, and shader packs treat it as emissive) but with
    * GL_SRC_ALPHA, GL_ONE blending instead of GL_ONE, GL_ONE, i.e. the glBlendFunc(770, 1) of the original. Needed for
    * the glow textures whose shape is in the alpha channel (white RGB everywhere): with eyes() the transparent texels
    * were added too and the quads showed up as solid white.
    */
   public static RenderType glow(ResourceLocation tex) {
      return GLOW.computeIfAbsent(tex, t -> create("thaumcraft_glow", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
         CompositeState.builder()
            .setShaderState(RENDERTYPE_EYES_SHADER)
            .setTextureState(new TextureStateShard(t, false, false))
            .setTransparencyState(LIGHTNING_TRANSPARENCY)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)));
   }
}
