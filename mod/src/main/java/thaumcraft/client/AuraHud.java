package thaumcraft.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.text.DecimalFormat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.ThaumometerItem;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.ClientAura;

/** HudHandler.renderThaumometerHud: the vis/flux gauge in the top-left corner while a thaumometer is held. */
public final class AuraHud {
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "aura_gauge");
   private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/hud.png");
   private static final float VISCON = 525.0F;
   private static final DecimalFormat FORMAT = new DecimalFormat("#######.#");

   private AuraHud() {
   }

   private static boolean holdsThaumometer(Player p) {
      return p.getMainHandItem().getItem() instanceof ThaumometerItem || p.getOffhandItem().getItem() instanceof ThaumometerItem;
   }

   /** A textured quad of w x h (float height allowed) from the 256x256 hud texture. */
   private static void quad(GuiGraphics g, float x, float y, float u, float v, float w, float h, float uw, float vh) {
      var pose = g.pose().last().pose();
      BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      float s = 1.0F / 256.0F;
      buf.addVertex(pose, x, y + h, 0).setUv(u * s, (v + vh) * s);
      buf.addVertex(pose, x + w, y + h, 0).setUv((u + uw) * s, (v + vh) * s);
      buf.addVertex(pose, x + w, y, 0).setUv((u + uw) * s, v * s);
      buf.addVertex(pose, x, y, 0).setUv(u * s, v * s);
      BufferUploader.drawWithShader(buf.buildOrThrow());
   }

   public static void render(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null || mc.options.hideGui || !holdsThaumometer(player)) return;
      AuraChunk aura = ClientAura.current;
      float partial = delta.getGameTimeDeltaPartialTick(false);
      float base = Mth.clamp(aura.getBase() / VISCON, 0.0F, 1.0F);
      float vis = Mth.clamp(aura.getVis() / VISCON, 0.0F, 1.0F);
      float flux = Mth.clamp(aura.getFlux() / VISCON, 0.0F, 1.0F);
      float count = player.tickCount + partial;
      float count2 = player.tickCount / 3.0F + partial;
      if (flux + vis > 1.0F) {
         float m = 1.0F / (flux + vis);
         base *= m;
         vis *= m;
         flux *= m;
      }

      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, HUD);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      g.pose().pushPose();
      g.pose().translate(2.0F, 0.0F, 0.0F);
      float start = 10.0F + (1.0F - vis) * 64.0F;
      if (vis > 0.0F) {
         RenderSystem.setShaderColor(0.7F, 0.4F, 0.9F, 1.0F);
         quad(g, 5, start, 88, 56, 8, vis * 64, 8, 64);
         RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
         quad(g, 5, start, 96, 56 + count % 64.0F, 8, vis * 64, 8, vis * 64);
         RenderSystem.defaultBlendFunc();
      }
      if (flux > 0.0F) {
         float fs = 10.0F + (1.0F - flux - vis) * 64.0F;
         RenderSystem.setShaderColor(0.25F, 0.1F, 0.3F, 1.0F);
         quad(g, 5, fs, 88, 56, 8, flux * 64, 8, 64);
         RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
         RenderSystem.setShaderColor(0.7F, 0.4F, 1.0F, 0.5F);
         quad(g, 5, fs, 104, 120 - count2 % 64.0F, 8, flux * 64, 8, flux * 64);
         RenderSystem.defaultBlendFunc();
      }
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      quad(g, 1, 1, 72, 48, 16, 80, 16, 80);
      quad(g, 2, 8.0F + (1.0F - base) * 64.0F, 117, 61, 14, 5, 14, 5);
      RenderSystem.disableBlend();

      if (player.isShiftKeyDown()) {
         g.pose().pushPose();
         g.pose().scale(0.5F, 0.5F, 1.0F);
         if (vis > 0) g.drawString(mc.font, FORMAT.format(aura.getVis()), 32, (int) (start * 2), 0xEEAAFF);
         if (flux > 0) g.drawString(mc.font, FORMAT.format(aura.getFlux()), 32, (int) ((10.0F + (1.0F - flux - vis) * 64.0F - 4) * 2), 0xAA11BB);
         g.pose().popPose();
      }
      g.pose().popPose();
   }
}
