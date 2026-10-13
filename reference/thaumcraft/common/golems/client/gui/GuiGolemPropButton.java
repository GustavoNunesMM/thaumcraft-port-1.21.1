package thaumcraft.common.golems.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import thaumcraft.api.golems.seals.ISealConfigToggles;

public class GuiGolemPropButton extends GuiButton {
   ISealConfigToggles.SealToggle prop;
   static ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_base.png");

   public GuiGolemPropButton(int buttonId, int x, int y, int width, int height, String buttonText, ISealConfigToggles.SealToggle prop) {
      super(buttonId, x, y, width, height, buttonText);
      this.prop = prop;
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
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.blendFunc(770, 771);
         this.drawTexturedModalRect(this.x - 2, this.y - 2, 2, 18, 12, 12);
         if (this.prop.getValue()) {
            this.drawTexturedModalRect(this.x - 2, this.y - 2, 18, 18, 12, 12);
         }

         this.drawString(fontrenderer, I18n.translateToLocal(this.displayString), this.x + 12, this.y, 16777215);
         this.mouseDragged(mc, xx, yy);
      }
   }
}
