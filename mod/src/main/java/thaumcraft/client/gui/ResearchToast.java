package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.ResearchEntry;

/** ResearchToast: "Research complete" with the entry's icon and name, for 5 seconds. */
public class ResearchToast implements Toast {
   private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/hud.png");
   private final ResearchEntry entry;

   public ResearchToast(ResearchEntry entry) {
      this.entry = entry;
   }

   @Override
   public Visibility render(GuiGraphics g, ToastComponent toasts, long time) {
      GuiDraw.resetColor();
      GuiDraw.rect(g, TEX, 0, 0, 0, 224, 160, 32);
      ResearchBrowserScreen.drawResearchIcon(g, entry, 6, 8, false);
      var font = toasts.getMinecraft().font;
      g.drawString(font, Component.translatable("research.complete"), 30, 7, 0xA238F1, false);
      Component s = entry.getLocalizedName();
      float w = font.width(s);
      if (w > 124.0F) {
         float k = 124.0F / w;
         g.pose().pushPose();
         g.pose().translate(30, 18, 0);
         g.pose().scale(k, k, k);
         g.drawString(font, s, 0, 0, 0xFFD709, false);
         g.pose().popPose();
      } else {
         g.drawString(font, s, 30, 18, 0xFFD709, false);
      }
      return time < 5000L ? Visibility.SHOW : Visibility.HIDE;
   }
}
