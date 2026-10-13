package thaumcraft.common.golems.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import thaumcraft.api.golems.seals.ISealEntity;

public class GuiGolemLockButton extends GuiButton {
   ISealEntity seal;
   static ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_base.png");

   public GuiGolemLockButton(int buttonId, int x, int y, int width, int height, ISealEntity seal) {
      super(buttonId, x, y, width, height, "");
      this.seal = seal;
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
         if (this.seal.isLocked()) {
            this.drawTexturedModalRect(this.x, this.y, 32, 136, 16, 16);
         } else {
            this.drawTexturedModalRect(this.x, this.y, 48, 136, 16, 16);
         }

         if (k == 2) {
            String s = this.seal.isLocked() ? I18n.translateToLocal("golem.prop.lock") : I18n.translateToLocal("golem.prop.unlock");
            this.drawCenteredString(fontrenderer, s, this.x + 8, this.y + 17, 16777215);
         }

         this.mouseDragged(mc, xx, yy);
      }
   }
}
