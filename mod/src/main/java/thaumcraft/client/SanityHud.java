package thaumcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import thaumcraft.Thaumcraft;
import thaumcraft.common.items.ThaumometerItem;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;

/** HudHandler.renderSanityHud: the warp meter shown while a sanity checker is held (below the thaumometer gauge). */
public final class SanityHud {
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "sanity_meter");
   private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/hud.png");

   private SanityHud() {
   }

   public static void render(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null || mc.options.hideGui) return;
      boolean main = player.getMainHandItem().is(ModGear.SANITY_CHECKER.get());
      if (!main && !player.getOffhandItem().is(ModGear.SANITY_CHECKER.get())) return;
      boolean thaumometer = player.getMainHandItem().getItem() instanceof ThaumometerItem || player.getOffhandItem().getItem() instanceof ThaumometerItem;
      int shifty = thaumometer ? 80 : 0;
      PlayerWarp w = ResearchManager.warp(player);
      int p = w.get(PlayerWarp.Type.PERMANENT);
      int s = w.get(PlayerWarp.Type.NORMAL);
      int t = w.get(PlayerWarp.Type.TEMPORARY);
      float tw = p + s + t;
      float mod = 1.0F;
      if (tw > 100.0F) {
         mod = 100.0F / tw;
         tw = 100.0F;
      }
      int gap = (int) ((100.0F - tw) / 100.0F * 48.0F);
      int wt = (int) (t / 100.0F * 48.0F * mod);
      int ws = (int) (s / 100.0F * 48.0F * mod);
      var pose = g.pose();
      pose.pushPose();
      pose.translate(0.0F, shifty, 0.0F);
      RenderSystem.enableBlend();
      g.blit(HUD, 1, 1, 152, 0, 20, 76);
      // UtilsFX.drawTexturedQuad(x, y, u, v, w, h): the bars are drawn exactly as the 1.12 calls did.
      if (t > 0) {
         g.setColor(1.0F, 0.5F, 1.0F, 1.0F);
         g.blit(HUD, 7, 21 + gap, 200, gap, 8, wt + gap);
      }
      if (s > 0) {
         g.setColor(0.75F, 0.0F, 0.75F, 1.0F);
         g.blit(HUD, 7, 21 + wt + gap, 200, wt + gap, 8, wt + ws + gap);
      }
      if (p > 0) {
         g.setColor(0.5F, 0.0F, 0.5F, 1.0F);
         g.blit(HUD, 7, 21 + wt + ws + gap, 200, wt + ws + gap, 8, 48);
      }
      g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      g.blit(HUD, 1, 1, 176, 0, 20, 76);
      if (tw >= 100.0F) {
         pose.pushPose();
         pose.scale(0.75F, 0.75F, 1.0F);
         pose.translate(player.getRandom().nextInt(2), player.getRandom().nextInt(2), 0.0F);
         g.blit(HUD, 3, 3, 216, 0, 20, 16);
         pose.popPose();
      }
      pose.popPose();
      RenderSystem.disableBlend();
   }
}
