package thaumcraft.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Row of aspect icons with amounts, drawn under an item's tooltip (UtilsFX.drawTag in the original). */
public record AspectTooltip(AspectList aspects) implements TooltipComponent {
   private static final int ICON = 16;
   private static final int STRIDE = 18;

   public record Client(AspectList aspects) implements ClientTooltipComponent {
      public Client(AspectTooltip data) {
         this(data.aspects());
      }

      @Override
      public int getHeight() {
         return STRIDE;
      }

      @Override
      public int getWidth(Font font) {
         return aspects.size() * STRIDE;
      }

      @Override
      public void renderImage(Font font, int x, int y, GuiGraphics g) {
         int index = 0;
         for (Aspect aspect : aspects.getAspectsSortedByAmount()) {
            int ax = x + index * STRIDE;
            int c = aspect.getColor();
            g.setColor(((c >> 16) & 0xFF) / 255F, ((c >> 8) & 0xFF) / 255F, (c & 0xFF) / 255F, 1.0F);
            g.blit(aspect.getImage(), ax, y, 0, 0, ICON, ICON, ICON, ICON);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            String amount = String.valueOf(aspects.getAmount(aspect));
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            g.drawString(font, amount, (ax + ICON + 1) * 2 - font.width(amount), (y + ICON - 3) * 2, 0xFFFFFF, true);
            g.pose().popPose();
            index++;
         }
      }
   }
}
