package thaumcraft.common.golems.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public class GuiGolemCraftButton extends GuiButton {
   static ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_golembuilder.png");

   public GuiGolemCraftButton(int buttonId, int x, int y) {
      super(buttonId, x, y, 24, 16, "");
   }

   public void drawButton(Minecraft mc, int xx, int yy, float partialTicks) {
      if (this.visible) {
         FontRenderer fontrenderer = mc.fontRenderer;
         mc.getTextureManager().bindTexture(tex);
         GlStateManager.color(0.9F, 0.9F, 0.9F, 0.9F);
         this.hovered = xx >= this.x
            && yy >= this.y
            && xx < this.x + this.width
            && yy < this.y + this.height;
         int k = this.getHoverState(this.hovered);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.blendFunc(770, 771);
         if (this.enabled && k == 2) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         }

         this.drawTexturedModalRect(this.x, this.y, 216, 64, 24, 16);
         if (!this.enabled) {
            this.drawTexturedModalRect(this.x, this.y, 216, 40, 24, 16);
         }

         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.mouseDragged(mc, xx, yy);
      }
   }
}
