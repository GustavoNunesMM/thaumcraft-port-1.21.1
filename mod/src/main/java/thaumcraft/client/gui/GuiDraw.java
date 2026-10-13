package thaumcraft.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

/** The UtilsFX drawing helpers used by the Thaumcraft screens. Texture coordinates are in pixels of a texW x texH texture. */
public final class GuiDraw {
   private static final Map<ResourceLocation, int[]> SIZES = new HashMap<>();

   private GuiDraw() {
   }

   /** A textured quad with float position/size and UVs; uses the current shader colour. */
   public static void quad(GuiGraphics g, ResourceLocation tex, float x, float y, float w, float h, float u, float v, float uw, float vh,
                           float texW, float texH) {
      RenderSystem.setShaderTexture(0, tex);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.enableBlend();
      Matrix4f m = g.pose().last().pose();
      BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      float u0 = u / texW, u1 = (u + uw) / texW, v0 = v / texH, v1 = (v + vh) / texH;
      b.addVertex(m, x, y + h, 0).setUv(u0, v1);
      b.addVertex(m, x + w, y + h, 0).setUv(u1, v1);
      b.addVertex(m, x + w, y, 0).setUv(u1, v0);
      b.addVertex(m, x, y, 0).setUv(u0, v0);
      BufferUploader.drawWithShader(b.buildOrThrow());
   }

   /** drawTexturedModalRect on a 256x256 texture. */
   public static void rect(GuiGraphics g, ResourceLocation tex, float x, float y, float u, float v, float w, float h) {
      quad(g, tex, x, y, w, h, u, v, w, h, 256, 256);
   }

   /** drawTexturedQuadFull: the whole texture as a 16x16 square. */
   public static void full(GuiGraphics g, ResourceLocation tex, float x, float y) {
      quad(g, tex, x, y, 16, 16, 0, 0, 1, 1, 1, 1);
   }

   public static void color(float r, float gr, float b, float a) {
      RenderSystem.setShaderColor(r, gr, b, a);
   }

   public static void resetColor() {
      RenderSystem.setShaderColor(1, 1, 1, 1);
   }

   /** Pixel size of a texture file (cached), for the animated research icons. */
   public static int[] textureSize(ResourceLocation tex) {
      return SIZES.computeIfAbsent(tex, t -> {
         try (InputStream in = Minecraft.getInstance().getResourceManager().open(t); NativeImage img = NativeImage.read(in)) {
            return new int[]{img.getWidth(), img.getHeight()};
         } catch (Exception e) {
            return new int[]{16, 16};
         }
      });
   }

   /** A research icon texture: animated vertically/horizontally if it is a strip of square frames. */
   public static void icon(GuiGraphics g, ResourceLocation tex, float x, float y) {
      int[] s = textureSize(tex);
      int w = s[0], h = s[1];
      if (h > w && h % w == 0) {
         int frames = h / w;
         float idx = (System.currentTimeMillis() / 150L % frames);
         quad(g, tex, x, y, 16, 16, 0, idx, 1, 1, 1, frames);
      } else if (w > h && w % h == 0) {
         int frames = w / h;
         float idx = (System.currentTimeMillis() / 150L % frames);
         quad(g, tex, x, y, 16, 16, idx, 0, 1, 1, frames, 1);
      } else {
         full(g, tex, x, y);
      }
   }

   /**
    * UtilsFX.drawCustomTooltip: a tooltip whose lines starting with "@@" are drawn at half size
    * (used for the stage/requirement lines under a research name).
    */
   public static void tooltip(GuiGraphics g, Font font, List<Component> lines, int x, int y) {
      if (lines.isEmpty()) return;
      int max = 240;
      List<FormattedCharSequence> rows = new ArrayList<>();
      List<Boolean> small = new ArrayList<>();
      for (Component c : lines) {
         boolean half = c.getString().startsWith("@@");
         Component text = half ? Component.literal(c.getString().substring(2)).withStyle(c.getStyle()) : c;
         for (FormattedCharSequence row : font.split(text, half ? max * 2 : max)) {
            rows.add(row);
            small.add(half);
         }
      }
      int width = 0;
      int height = -2;
      for (int i = 0; i < rows.size(); i++) {
         int lw = font.width(rows.get(i));
         width = Math.max(width, small.get(i) ? lw / 2 : lw);
         height += small.get(i) ? 5 : 10;
      }
      int sx = x + 12, sy = y - 12;
      Minecraft mc = Minecraft.getInstance();
      int sw = mc.getWindow().getGuiScaledWidth();
      if (sx + width + 4 > sw) sx = Math.max(4, x - 16 - width);
      g.pose().pushPose();
      g.pose().translate(0, 0, 400);
      TooltipRenderUtil.renderTooltipBackground(g, sx, sy, width, height, 0);
      int ty = sy;
      for (int i = 0; i < rows.size(); i++) {
         if (small.get(i)) {
            g.pose().pushPose();
            g.pose().translate(sx, ty, 0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            g.drawString(font, rows.get(i), 0, 0, 0xFFFFFF, true);
            g.pose().popPose();
            ty += 5;
         } else {
            g.drawString(font, rows.get(i), sx, ty, 0xFFFFFF, true);
            ty += 10;
         }
      }
      g.pose().popPose();
   }

   public static Component text(String s) {
      return Component.literal(s);
   }

   public static FormattedText plain(String s) {
      return FormattedText.of(s);
   }
}
