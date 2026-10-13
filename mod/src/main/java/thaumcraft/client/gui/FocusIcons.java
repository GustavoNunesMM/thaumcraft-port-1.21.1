package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusNode;

/** Icons of focus elements ("thaumcraft.BOLT"...) used by research entries: GuiFocalManipulator.drawPart at size 24. */
public final class FocusIcons {
   private FocusIcons() {
   }

   public static void draw(GuiGraphics g, String key, int cx, int cy, boolean bw) {
      if (FocusEngine.getElement(key) instanceof FocusNode node) {
         if (bw) GuiDraw.color(0.2F, 0.2F, 0.2F, 1);
         FocalManipulatorScreen.drawPart(g, node, cx, cy, 24.0F, bw ? 50 : 220, false);
         GuiDraw.resetColor();
         return;
      }
      String name = key.contains(".") ? key.substring(key.indexOf('.') + 1).toLowerCase() : key.toLowerCase();
      ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/foci/" + name + ".png");
      if (bw) GuiDraw.color(0.2F, 0.2F, 0.2F, 1);
      GuiDraw.full(g, tex, cx - 8, cy - 8);
      GuiDraw.resetColor();
   }
}
