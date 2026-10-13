package thaumcraft.client.gui.plugins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiPlusMinusButton extends GuiButton {
   boolean minus = false;
   static ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_base.png");

   public GuiPlusMinusButton(int buttonId, int x, int y, int width, int height, boolean left) {
      super(buttonId, x, y, width, height, "");
      this.minus = left;
   }

   public void drawButton(Minecraft mc, int xx, int yy, float pt) {
      if (this.visible) {
         FontRenderer fontrenderer = mc.fontRenderer;
         mc.getTextureManager().bindTexture(tex);
         GlStateManager.color(0.9F, 0.9F, 0.9F, 0.9F);
         this.hovered = xx >= this.x
            && yy >= this.y
            && xx < this.x + this.width
            && yy < this.y + this.height;
         int k = this.getHoverState(this.hovered);
         if (k == 2) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         }

         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.blendFunc(770, 771);
         this.drawTexturedModalRect(this.x, this.y, this.minus ? 0 : 10, 0, 10, 10);
         this.mouseDragged(mc, xx, yy);
      }
   }
}
