package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.SmelterMenu;

/** GuiSmelter: fuel flame, melting progress and the essentia gauge. */
public class SmelterScreen extends AbstractContainerScreen<SmelterMenu> {
   private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_smelter.png");

   public SmelterScreen(SmelterMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      renderTooltip(g, mouseX, mouseY);
   }

   @Override
   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
   }

   @Override
   protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      int k = leftPos, l = topPos;
      g.blit(TEX, k, l, 0, 0, imageWidth, imageHeight);
      int burn = menu.getBurnTimeRemainingScaled(20);
      if (burn > 0) g.blit(TEX, k + 80, l + 26 + 20 - burn, 176, 20 - burn, 16, burn);
      int cook = menu.getCookProgressScaled(46);
      g.blit(TEX, k + 106, l + 13 + 46 - cook, 216, 46 - cook, 9, cook);
      int vis = menu.getVisScaled(48);
      g.blit(TEX, k + 61, l + 12 + 48 - vis, 200, 48 - vis, 8, vis);
      g.blit(TEX, k + 60, l + 8, 232, 0, 10, 55);
   }
}
