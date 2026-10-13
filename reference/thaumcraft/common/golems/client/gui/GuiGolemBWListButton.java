package thaumcraft.common.golems.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import thaumcraft.api.golems.seals.ISealConfigFilter;

public class GuiGolemBWListButton extends GuiButton {
   ISealConfigFilter filter;
   static ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_base.png");

   public GuiGolemBWListButton(int buttonId, int x, int y, int width, int height, ISealConfigFilter filter) {
      super(buttonId, x, y, width, height, "");
      this.filter = filter;
   }

   public void drawButton(Minecraft mc, int xx, int yy, float partialTicks) {
      if (this.visible) {
         FontRenderer fontrenderer = mc.fontRenderer;
         mc.getTextureManager().bindTexture(tex);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.hovered = xx >= this.x
            && yy >= this.y
            && xx < this.x + this.width
            && yy < this.y + this.height;
         int k = this.getHoverState(this.hovered);
         if (k == 2) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         } else {
            GlStateManager.color(0.9F, 0.9F, 0.9F, 0.9F);
         }

         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.blendFunc(770, 771);
         if (this.filter.isBlacklist()) {
            this.drawTexturedModalRect(this.x, this.y, 0, 136, 16, 16);
         } else {
            this.drawTexturedModalRect(this.x, this.y, 16, 136, 16, 16);
         }

         if (k == 2) {
            this.drawCenteredString(
               fontrenderer,
               I18n.translateToLocal(this.filter.isBlacklist() ? "button.bl" : "button.wl"),
               this.x + 8,
               this.y + 17,
               16777215
            );
         }

         this.mouseDragged(mc, xx, yy);
      }
   }
}
