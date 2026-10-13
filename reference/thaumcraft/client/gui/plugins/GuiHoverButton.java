package thaumcraft.client.gui.plugins;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.util.ITooltipFlag.TooltipFlags;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.UtilsFX;

@SideOnly(Side.CLIENT)
public class GuiHoverButton extends GuiButton {
   String description;
   GuiScreen screen;
   int color;
   Object tex = null;

   public GuiHoverButton(GuiScreen screen, int buttonId, int x, int y, int width, int height, String buttonText, String description, Object tex) {
      super(buttonId, x, y, width, height, buttonText);
      this.description = description;
      this.tex = tex;
      this.screen = screen;
      this.color = 16777215;
   }

   public GuiHoverButton(GuiScreen screen, int buttonId, int x, int y, int width, int height, String buttonText, String description, Object tex, int color) {
      super(buttonId, x, y, width, height, buttonText);
      this.description = description;
      this.tex = tex;
      this.screen = screen;
      this.color = color;
   }

   public void drawButton(Minecraft mc, int xx, int yy, float pt) {
      if (this.visible) {
         FontRenderer fontrenderer = mc.fontRenderer;
         Color c = new Color(this.color);
         GlStateManager.color(0.9F * (c.getRed() / 255.0F), 0.9F * (c.getGreen() / 255.0F), 0.9F * (c.getBlue() / 255.0F), 0.9F);
         this.hovered = xx >= this.x - this.width / 2
            && yy >= this.y - this.height / 2
            && xx < this.x - this.width / 2 + this.width
            && yy < this.y - this.height / 2 + this.height;
         int k = this.getHoverState(this.hovered);
         GlStateManager.enableBlend();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GlStateManager.blendFunc(770, 771);
         if (k == 2) {
            GlStateManager.color(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F);
         }

         if (this.tex instanceof Aspect) {
            mc.getTextureManager().bindTexture(((Aspect)this.tex).getImage());
            Color c2 = new Color(((Aspect)this.tex).getColor());
            if (k != 2) {
               GlStateManager.color(c2.getRed() / 290.0F, c2.getGreen() / 290.0F, c2.getBlue() / 290.0F, 0.9F);
            } else {
               GlStateManager.color(c2.getRed() / 255.0F, c2.getGreen() / 255.0F, c2.getBlue() / 255.0F, 1.0F);
            }

            drawModalRectWithCustomSizedTexture(this.x - this.width / 2, this.y - this.height / 2, 0.0F, 0.0F, 16, 16, 16.0F, 16.0F);
         }

         if (this.tex instanceof ResourceLocation) {
            mc.getTextureManager().bindTexture((ResourceLocation)this.tex);
            drawModalRectWithCustomSizedTexture(this.x - this.width / 2, this.y - this.height / 2, 0.0F, 0.0F, 16, 16, 16.0F, 16.0F);
         }

         if (this.tex instanceof TextureAtlasSprite) {
            this.drawTexturedModalRect(
               this.x - this.width / 2, this.y - this.height / 2, (TextureAtlasSprite)this.tex, 16, 16
            );
         }

         if (this.tex instanceof ItemStack) {
            this.zLevel -= 90.0F;
            UtilsFX.renderItemStackShaded(
               mc,
               (ItemStack)this.tex,
               this.x - this.width / 2,
               this.y - this.height / 2 - (k == 2 ? 1 : 0),
               null,
               1.0F
            );
            this.zLevel += 90.0F;
         }

         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.mouseDragged(mc, xx, yy);
      }
   }

   public void drawButtonForegroundLayer(int xx, int yy) {
      FontRenderer fontrenderer = Minecraft.getMinecraft().fontRenderer;
      this.zLevel += 90.0F;
      List<String> text = new ArrayList<>();
      if (this.tex instanceof ItemStack) {
         text = ((ItemStack)this.tex)
            .getTooltip(
               Minecraft.getMinecraft().player, Minecraft.getMinecraft().gameSettings.advancedItemTooltips ? TooltipFlags.ADVANCED : TooltipFlags.NORMAL
            );
         int qq = 0;

         for (String s : text) {
            if (s.endsWith(" " + TextFormatting.RESET)) {
               text = text.subList(0, qq);
               break;
            }

            qq++;
         }
      } else {
         text.add(this.displayString);
      }

      int m = 8;
      if (this.description != null) {
         m = 0;
         text.add("§o§9" + this.description);
      }

      UtilsFX.drawCustomTooltip(this.screen, fontrenderer, text, xx + 4, yy + m, -99);
      this.zLevel -= 90.0F;
      RenderHelper.disableStandardItemLighting();
      GlStateManager.disableLighting();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
      return false;
   }
}
