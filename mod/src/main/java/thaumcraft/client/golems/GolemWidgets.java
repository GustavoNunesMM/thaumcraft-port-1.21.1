package thaumcraft.client.golems;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiDraw;

/**
 * The little 1.12 GuiButtons of the golem screens (GuiPlusMinusButton, GuiScrollButton, GuiHoverButton, GuiSliderTC,
 * GuiImageButton and the seal buttons), drawn from gui_base.png as before. The screens keep them in a list and turn
 * clicks into actionPerformed(id).
 */
public final class GolemWidgets {
   public static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
   public static final ResourceLocation BUILDER = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_golembuilder.png");

   private GolemWidgets() {
   }

   static void color(float r, float g, float b, float a) {
      GuiDraw.color(r, g, b, a);
   }

   static void rect(GuiGraphics g, ResourceLocation tex, float x, float y, float u, float v, float w, float h) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      GuiDraw.rect(g, tex, x, y, u, v, w, h);
   }

   /** A GuiButton: top-left at x, y unless centred. */
   public abstract static class Btn {
      public final int id;
      public int x;
      public int y;
      public final int w;
      public final int h;
      public boolean visible = true;
      public boolean enabled = true;
      protected boolean centred = false;

      protected Btn(int id, int x, int y, int w, int h) {
         this.id = id;
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
      }

      public boolean isOver(double mx, double my) {
         int x0 = centred ? x - w / 2 : x;
         int y0 = centred ? y - h / 2 : y;
         return visible && mx >= x0 && my >= y0 && mx < x0 + w && my < y0 + h;
      }

      /** mousePressed: whether a click here fires actionPerformed. */
      public boolean clicks(double mx, double my) {
         return enabled && isOver(mx, my);
      }

      public abstract void render(GuiGraphics g, Font font, int mx, int my);

      /** drawButtonForegroundLayer: tooltips, drawn above everything. */
      public void foreground(GuiGraphics g, Font font, int mx, int my) {
      }

      public void mouseReleased() {
      }

      public void mouseDragged(double mx, double my) {
      }
   }

   public static class PlusMinus extends Btn {
      private final boolean minus;

      public PlusMinus(int id, int x, int y, int w, int h, boolean minus) {
         super(id, x, y, w, h);
         this.minus = minus;
      }

      @Override
      public void render(GuiGraphics g, Font font, int mx, int my) {
         if (!visible) return;
         if (isOver(mx, my)) color(1, 1, 1, 1);
         else color(0.9F, 0.9F, 0.9F, 0.9F);
         rect(g, BASE, x, y, minus ? 0 : 10, 0, 10, 10);
         GuiDraw.resetColor();
      }
   }

   public static class Scroll extends Btn {
      private final boolean minus;
      private final boolean vertical;

      public Scroll(int id, int x, int y, int w, int h, boolean minus, boolean vertical) {
         super(id, x, y, w, h);
         this.minus = minus;
         this.vertical = vertical;
      }

      @Override
      public void render(GuiGraphics g, Font font, int mx, int my) {
         if (!visible) return;
         if (isOver(mx, my)) color(1, 1, 1, 1);
         else color(0.9F, 0.9F, 0.9F, 0.9F);
         rect(g, BASE, x, y, vertical ? 67 : minus ? 20 : 30, vertical ? minus ? 0 : 10 : 0, 10, 10);
         GuiDraw.resetColor();
      }
   }

   /** GuiHoverButton: a centred icon (texture, aspect or item) that only shows a tooltip. */
   public static class Hover extends Btn {
      private final Component text;
      private final Component description;
      private final Object tex;
      private final int color;

      public Hover(int id, int x, int y, int w, int h, Component text, Component description, Object tex) {
         this(id, x, y, w, h, text, description, tex, 0xFFFFFF);
      }

      public Hover(int id, int x, int y, int w, int h, Component text, Component description, Object tex, int color) {
         super(id, x, y, w, h);
         this.text = text;
         this.description = description;
         this.tex = tex;
         this.color = color;
         this.centred = true;
      }

      @Override
      public boolean clicks(double mx, double my) {
         return false;
      }

      @Override
      public void render(GuiGraphics g, Font font, int mx, int my) {
         if (!visible) return;
         boolean k = isOver(mx, my);
         float r = (color >> 16 & 255) / 255.0F, gr = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
         if (k) color(r, gr, b, 1.0F);
         else color(0.9F * r, 0.9F * gr, 0.9F * b, 0.9F);
         int x0 = x - w / 2, y0 = y - h / 2;
         if (tex instanceof Aspect a) {
            int c = a.getColor();
            float ar = (c >> 16 & 255), ag = (c >> 8 & 255), ab = (c & 255);
            if (!k) color(ar / 290.0F, ag / 290.0F, ab / 290.0F, 0.9F);
            else color(ar / 255.0F, ag / 255.0F, ab / 255.0F, 1.0F);
            RenderSystem.enableBlend();
            GuiDraw.quad(g, a.getImage(), x0, y0, 16, 16, 0, 0, 1, 1, 1, 1);
         } else if (tex instanceof ResourceLocation rl) {
            RenderSystem.enableBlend();
            GuiDraw.quad(g, rl, x0, y0, 16, 16, 0, 0, 1, 1, 1, 1);
         }
         GuiDraw.resetColor();
         if (tex instanceof ItemStack stack) {
            g.renderItem(stack, x0, y0 - (k ? 1 : 0));
            g.renderItemDecorations(font, stack, x0, y0 - (k ? 1 : 0));
         }
      }

      @Override
      public void foreground(GuiGraphics g, Font font, int mx, int my) {
         List<Component> lines = new ArrayList<>();
         Minecraft mc = Minecraft.getInstance();
         if (tex instanceof ItemStack stack) {
            lines.addAll(stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player,
               mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL));
         } else if (text != null) {
            lines.add(text);
         }
         int m = 8;
         if (description != null) {
            m = 0;
            lines.add(description.copy().withStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.BLUE)));
         }
         GuiDraw.tooltip(g, font, lines, mx + 4, my + m);
      }
   }

   /** GuiImageButton: a centred image with a small caption. */
   public static class Image extends Btn {
      private final Component text;
      private final Component description;
      private final ResourceLocation loc;
      private final int lx;
      private final int ly;
      private final int ww;
      private final int hh;

      public Image(int id, int x, int y, int w, int h, Component text, Component description, ResourceLocation loc, int lx, int ly, int ww, int hh) {
         super(id, x, y, w, h);
         this.text = text;
         this.description = description;
         this.loc = loc;
         this.lx = lx;
         this.ly = ly;
         this.ww = ww;
         this.hh = hh;
         this.centred = true;
      }

      @Override
      public void render(GuiGraphics g, Font font, int mx, int my) {
         if (!visible) return;
         boolean hovered = isOver(mx, my);
         float cc = hovered ? 1.0F : 0.9F;
         float ac = 1.0F;
         if (!enabled) {
            cc = 0.5F;
            ac = 0.9F;
         }
         color(cc, cc, cc, ac);
         rect(g, loc, x - ww / 2.0F, y - hh / 2.0F, lx, ly, ww, hh);
         GuiDraw.resetColor();
         if (text != null) {
            int j = !enabled ? 0xA0A0A0 : hovered ? 0xFFFFA0 : 0xFFFFFF;
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            g.drawCenteredString(font, text, 0, -4, j);
            g.pose().popPose();
         }
      }

      @Override
      public void foreground(GuiGraphics g, Font font, int mx, int my) {
         List<Component> lines = new ArrayList<>();
         if (text != null) lines.add(text);
         int m = 8;
         if (description != null) {
            m = 0;
            lines.add(description.copy().withStyle(Style.EMPTY.withItalic(true).withColor(ChatFormatting.BLUE)));
         }
         GuiDraw.tooltip(g, font, lines, mx + 4, my + m);
      }
   }

   /** GuiSliderTC: a vertical or horizontal track with a knob. */
   public static class Slider extends Btn {
      private float sliderPosition;
      private boolean mouseDown;
      private final float min;
      private float max;
      private final boolean vertical;

      public Slider(int id, int x, int y, int w, int h, float min, float max, float def, boolean vertical) {
         super(id, x, y, w, h);
         this.min = min;
         this.max = max;
         this.sliderPosition = max == min ? 0.0F : (def - min) / (max - min);
         this.vertical = vertical;
      }

      public float getMax() {
         return max;
      }

      public void setMax(float max) {
         this.max = max;
         this.sliderPosition = 0.0F;
      }

      public float getSliderValue() {
         return min + (max - min) * sliderPosition;
      }

      public void setSliderValue(float value) {
         sliderPosition = max == min ? 0.0F : (value - min) / (max - min);
      }

      @Override
      public boolean clicks(double mx, double my) {
         if (!enabled || !isOver(mx, my)) return false;
         drag(mx, my);
         mouseDown = true;
         return true;
      }

      private void drag(double mx, double my) {
         sliderPosition = vertical ? (float) (my - (y + 4)) / (h - 8) : (float) (mx - (x + 4)) / (w - 8);
         sliderPosition = Math.max(0.0F, Math.min(1.0F, sliderPosition));
      }

      @Override
      public void mouseDragged(double mx, double my) {
         if (visible && mouseDown) drag(mx, my);
      }

      @Override
      public void mouseReleased() {
         mouseDown = false;
      }

      @Override
      public void render(GuiGraphics g, Font font, int mx, int my) {
         if (!visible) return;
         g.pose().pushPose();
         if (vertical) {
            g.pose().translate(x + 2, y, 0);
            g.pose().scale(1.0F, h / 32.0F, 1.0F);
            rect(g, BASE, 0, 0, 240, 176, 4, 32);
         } else {
            g.pose().translate(x, y + 2, 0);
            g.pose().scale(w / 32.0F, 1.0F, 1.0F);
            rect(g, BASE, 0, 0, 208, 176, 32, 4);
         }
         g.pose().popPose();
         if (vertical) rect(g, BASE, x, y + (int) (sliderPosition * (h - 8)), 20, 20, 8, 8);
         else rect(g, BASE, x + (int) (sliderPosition * (w - 8)), y, 20, 20, 8, 8);
      }
   }

   /** GuiGolemCategoryButton: a centred category icon from row 120 of gui_base. */
   public static class Category extends Btn {
      private final Component text;
      private final int icon;
      private final boolean active;

      public Category(int id, int x, int y, int w, int h, Component text, int icon, boolean active) {
         super(id, x, y, w, h);
         this.text = text;
         this.icon = icon;
         this.active = active;
         this.centred = true;
      }

      @Override
      public void render(GuiGraphics g, Font font, int mx, int my) {
         if (!visible) return;
         boolean k = isOver(mx, my);
         if (active) color(1, 1, 1, 1);
         else if (!k) color(0.7F, 0.7F, 0.7F, 0.7F);
         else color(0.9F, 0.9F, 0.9F, 0.9F);
         rect(g, BASE, x - 8, y - 8, icon * 16, 120, 16, 16);
         GuiDraw.resetColor();
         if (k) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 90);
            g.drawString(font, text, x - 10 - font.width(text), y - 4, 0xFFFFFF);
            g.pose().popPose();
         }
      }
   }

   /** GuiGolemRedstoneButton / GuiGolemLockButton / GuiGolemBWListButton: a two-state icon with a caption. */
   public static class Toggle extends Btn {
      private final BooleanSupplier state;
      private final int uOn;
      private final int uOff;
      private final Supplier<Component> caption;
      private final boolean captionLeft;

      public Toggle(int id, int x, int y, BooleanSupplier state, int uOn, int uOff, Supplier<Component> caption, boolean captionLeft) {
         super(id, x, y, 16, 16);
         this.state = state;
         this.uOn = uOn;
         this.uOff = uOff;
         this.caption = caption;
         this.captionLeft = captionLeft;
      }

      @Override
      public void render(GuiGraphics g, Font font, int mx, int my) {
         if (!visible) return;
         boolean k = isOver(mx, my);
         if (k) color(1, 1, 1, 1);
         else color(0.9F, 0.9F, 0.9F, 0.9F);
         rect(g, BASE, x, y, state.getAsBoolean() ? uOn : uOff, 136, 16, 16);
         GuiDraw.resetColor();
         if (k) {
            Component s = caption.get();
            g.pose().pushPose();
            g.pose().translate(0, 0, 90);
            if (captionLeft) g.drawString(font, s, x - 2 - font.width(s), y + 4, 0xFFFFFF);
            else g.drawCenteredString(font, s, x + 8, y + 17, 0xFFFFFF);
            g.pose().popPose();
         }
      }
   }

   /** GuiGolemPropButton: a check box with the toggle's name. */
   public static class Prop extends Btn {
      private final Component text;
      private final BooleanSupplier value;

      public Prop(int id, int x, int y, int w, int h, Component text, BooleanSupplier value) {
         super(id, x, y, w, h);
         this.text = text;
         this.value = value;
      }

      @Override
      public void render(GuiGraphics g, Font font, int mx, int my) {
         if (!visible) return;
         rect(g, BASE, x - 2, y - 2, 2, 18, 12, 12);
         if (value.getAsBoolean()) rect(g, BASE, x - 2, y - 2, 18, 18, 12, 12);
         g.drawString(font, text, x + 12, y, 0xFFFFFF);
      }
   }

   /** GuiGolemCraftButton. */
   public static class Craft extends Btn {
      public Craft(int id, int x, int y) {
         super(id, x, y, 24, 16);
      }

      @Override
      public void render(GuiGraphics g, Font font, int mx, int my) {
         if (!visible) return;
         if (enabled && isOver(mx, my)) color(1, 1, 1, 1);
         else color(0.9F, 0.9F, 0.9F, 0.9F);
         rect(g, BUILDER, x, y, 216, 64, 24, 16);
         if (!enabled) rect(g, BUILDER, x, y, 216, 40, 24, 16);
         GuiDraw.resetColor();
      }
   }
}
