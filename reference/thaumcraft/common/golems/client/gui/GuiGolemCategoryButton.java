package thaumcraft.common.golems.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

class GuiGolemCategoryButton extends GuiButton {
   int icon;
   boolean active;
   static ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_base.png");

   public GuiGolemCategoryButton(int buttonId, int x, int y, int width, int height, String buttonText, int i, boolean act) {
      super(buttonId, x, y, width, height, buttonText);
      this.icon = i;
      this.active = act;
   }

   public void drawButton(Minecraft mc, int xx, int yy, float partialTicks) {
      if (this.visible) {
         FontRenderer fontrenderer = mc.fontRenderer;
         mc.getTextureManager().bindTexture(tex);
         GlStateManager.color(0.9F, 0.9F, 0.9F, 0.9F);
         this.hovered = xx >= this.x - 8
            && yy >= this.y - 8
            && xx < this.x - 8 + this.width
            && yy < this.y - 8 + this.height;
         int k = this.getHoverState(this.hovered);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.blendFunc(770, 771);
         if (this.active) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         } else if (k != 2) {
            GlStateManager.color(0.7F, 0.7F, 0.7F, 0.7F);
         }

         this.drawTexturedModalRect(this.x - 8, this.y - 8, this.icon * 16, 120, 16, 16);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         if (k == 2) {
            this.zLevel += 90.0F;
            String s = I18n.translateToLocal(this.displayString);
            this.drawString(fontrenderer, s, this.x - 10 - fontrenderer.getStringWidth(s), this.y - 4, 16777215);
            this.zLevel -= 90.0F;
         }

         this.mouseDragged(mc, xx, yy);
      }
   }

   public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
      return this.enabled
         && this.visible
         && mouseX >= this.x - 8
         && mouseY >= this.y - 8
         && mouseX < this.x - 8 + this.width
         && mouseY < this.y - 8 + this.height;
   }
}
