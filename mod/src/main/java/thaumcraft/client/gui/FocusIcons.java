package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Icons of focus effects ("thaumcraft.BOLT"...) used by research entries. GuiFocalManipulator.drawPart arrives
 * with the casting milestone; until then the focus texture of the element is drawn if it exists.
 */
public final class FocusIcons {
   private FocusIcons() {
   }

   public static void draw(GuiGraphics g, String key, int cx, int cy, boolean bw) {
      String name = key.contains(".") ? key.substring(key.indexOf('.') + 1).toLowerCase() : key.toLowerCase();
      ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/foci/" + name + ".png");
      if (bw) GuiDraw.color(0.2F, 0.2F, 0.2F, 1);
      GuiDraw.full(g, tex, cx - 8, cy - 8);
      GuiDraw.resetColor();
   }
}
