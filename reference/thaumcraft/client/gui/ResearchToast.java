package thaumcraft.client.gui;

import net.minecraft.client.gui.toasts.GuiToast;
import net.minecraft.client.gui.toasts.IToast;
import net.minecraft.client.gui.toasts.IToast.Visibility;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import thaumcraft.api.research.ResearchEntry;

public class ResearchToast implements IToast {
   ResearchEntry entry;
   private long firstDrawTime;
   private boolean newDisplay;
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/hud.png");

   public ResearchToast(ResearchEntry entry) {
      this.entry = entry;
   }

   public Visibility draw(GuiToast toastGui, long delta) {
      if (this.newDisplay) {
         this.firstDrawTime = delta;
         this.newDisplay = false;
      }

      toastGui.getMinecraft().getTextureManager().bindTexture(this.tex);
      GlStateManager.color(1.0F, 1.0F, 1.0F);
      toastGui.drawTexturedModalRect(0, 0, 0, 224, 160, 32);
      GuiResearchBrowser.drawResearchIcon(this.entry, 6, 8, 0.0F, false);
      toastGui.getMinecraft().fontRenderer.drawString(I18n.translateToLocal("research.complete"), 30, 7, 10631665);
      String s = this.entry.getLocalizedName();
      float w = toastGui.getMinecraft().fontRenderer.getStringWidth(s);
      if (w > 124.0F) {
         w = 124.0F / w;
         GlStateManager.pushMatrix();
         GlStateManager.translate(30.0F, 18.0F, 0.0F);
         GlStateManager.scale(w, w, w);
         toastGui.getMinecraft().fontRenderer.drawString(s, 0, 0, 16755465);
         GlStateManager.popMatrix();
      } else {
         toastGui.getMinecraft().fontRenderer.drawString(s, 30, 18, 16755465);
      }

      return delta - this.firstDrawTime < 5000L ? Visibility.SHOW : Visibility.HIDE;
   }
}
