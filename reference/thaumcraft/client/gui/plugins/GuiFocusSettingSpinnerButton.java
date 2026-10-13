package thaumcraft.client.gui.plugins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.casters.NodeSetting;

public class GuiFocusSettingSpinnerButton extends GuiButton {
   private NodeSetting setting;
   static ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_base.png");

   public GuiFocusSettingSpinnerButton(int buttonId, int x, int y, int width, NodeSetting ns) {
      super(buttonId, x, y, width, 10, "");
      this.setting = ns;
   }

   public void drawButton(Minecraft mc, int xx, int yy, float pt) {
      if (this.visible) {
         FontRenderer fontrenderer = mc.fontRenderer;
         mc.getTextureManager().bindTexture(tex);
         GlStateManager.color(0.9F, 0.9F, 0.9F, 0.9F);
         this.hovered = xx >= this.x
            && yy >= this.y
            && xx < this.x + this.width + 10
            && yy < this.y + this.height;
         int k = this.getHoverState(this.hovered);
         if (k == 2) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         }

         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.blendFunc(770, 771);
         this.drawTexturedModalRect(this.x, this.y, 20, 0, 10, 10);
         this.drawTexturedModalRect(this.x + this.width, this.y, 30, 0, 10, 10);
         String s = this.setting.getValueText();
         fontrenderer.drawStringWithShadow(
            s, this.x + (this.width + 10) / 2 - fontrenderer.getStringWidth(s) / 2, this.y + 1, 16777215
         );
         this.mouseDragged(mc, xx, yy);
      }
   }

   public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
      if (this.enabled && this.visible) {
         if (mouseX >= this.x
            && mouseY >= this.y
            && mouseX < this.x + 10
            && mouseY < this.y + this.height) {
            this.setting.decrement();
            return true;
         }

         if (mouseX >= this.x + this.width
            && mouseY >= this.y
            && mouseX < this.x + this.width + 10
            && mouseY < this.y + this.height) {
            this.setting.increment();
            return true;
         }
      }

      return false;
   }
}
