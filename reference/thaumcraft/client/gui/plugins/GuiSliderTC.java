package thaumcraft.client.gui.plugins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GuiSliderTC extends GuiButton {
   private float sliderPosition = 1.0F;
   public boolean isMouseDown;
   private final String name;
   private final float min;
   private float max;
   private final boolean vertical;
   static ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_base.png");

   public GuiSliderTC(int idIn, int x, int y, int w, int h, String name, float min, float max, float defaultValue, boolean vertical) {
      super(idIn, x, y, w, h, "");
      this.name = name;
      this.min = min;
      this.max = max;
      this.sliderPosition = (defaultValue - min) / (max - min);
      this.vertical = vertical;
   }

   public float getMax() {
      return this.max;
   }

   public float getMin() {
      return this.min;
   }

   public void setMax(float max) {
      this.max = max;
      this.sliderPosition = 0.0F;
   }

   public float getSliderValue() {
      return this.min + (this.max - this.min) * this.sliderPosition;
   }

   public void setSliderValue(float value, boolean notifyResponder) {
      this.sliderPosition = (value - this.min) / (this.max - this.min);
   }

   public float getSliderPosition() {
      return this.sliderPosition;
   }

   protected int getHoverState(boolean mouseOver) {
      return 0;
   }

   protected void mouseDragged(Minecraft mc, int mouseX, int mouseY) {
      if (this.visible) {
         if (this.isMouseDown) {
            if (this.vertical) {
               this.sliderPosition = (float)(mouseY - (this.y + 4)) / (this.height - 8);
            } else {
               this.sliderPosition = (float)(mouseX - (this.x + 4)) / (this.width - 8);
            }

            if (this.sliderPosition < 0.0F) {
               this.sliderPosition = 0.0F;
            }

            if (this.sliderPosition > 1.0F) {
               this.sliderPosition = 1.0F;
            }
         }

         mc.getTextureManager().bindTexture(tex);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         if (this.vertical) {
            this.drawTexturedModalRect(this.x, this.y + (int)(this.sliderPosition * (this.height - 8)), 20, 20, 8, 8);
         } else {
            this.drawTexturedModalRect(this.x + (int)(this.sliderPosition * (this.width - 8)), this.y, 20, 20, 8, 8);
         }
      }
   }

   public void setSliderPosition(float position) {
      this.sliderPosition = position;
   }

   public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
      if (super.mousePressed(mc, mouseX, mouseY)) {
         if (this.vertical) {
            this.sliderPosition = (float)(mouseY - (this.y + 4)) / (this.height - 8);
         } else {
            this.sliderPosition = (float)(mouseX - (this.x + 4)) / (this.width - 8);
         }

         if (this.sliderPosition < 0.0F) {
            this.sliderPosition = 0.0F;
         }

         if (this.sliderPosition > 1.0F) {
            this.sliderPosition = 1.0F;
         }

         this.isMouseDown = true;
         return true;
      } else {
         return false;
      }
   }

   public void mouseReleased(int mouseX, int mouseY) {
      this.isMouseDown = false;
   }

   public void drawButton(Minecraft mc, int mouseX, int mouseY, float pt) {
      if (this.visible) {
         mc.getTextureManager().bindTexture(tex);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.hovered = mouseX >= this.x
            && mouseY >= this.y
            && mouseX < this.x + this.width
            && mouseY < this.y + this.height;
         int i = this.getHoverState(this.hovered);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
         GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
         GlStateManager.pushMatrix();
         if (this.vertical) {
            GlStateManager.translate(this.x + 2, this.y, 0.0F);
            GlStateManager.scale(1.0F, this.height / 32.0F, 1.0F);
            this.drawTexturedModalRect(0, 0, 240, 176, 4, 32);
         } else {
            GlStateManager.translate(this.x, this.y + 2, 0.0F);
            GlStateManager.scale(this.width / 32.0F, 1.0F, 1.0F);
            this.drawTexturedModalRect(0, 0, 208, 176, 32, 4);
         }

         GlStateManager.popMatrix();
         this.mouseDragged(mc, mouseX, mouseY);
      }
   }

   @SideOnly(Side.CLIENT)
   public interface FormatHelper {
      String getText(int var1, String var2, float var3);
   }
}
