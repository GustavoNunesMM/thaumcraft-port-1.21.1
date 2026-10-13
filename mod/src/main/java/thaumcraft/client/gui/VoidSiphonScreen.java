package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.VoidSiphonMenu;

/** GuiVoidSiphon: just the background and the output slot. */
public class VoidSiphonScreen extends AbstractContainerScreen<VoidSiphonMenu> {
   private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_void_siphon.png");

   public VoidSiphonScreen(VoidSiphonMenu menu, Inventory inv, Component title) {
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
      g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
   }
}
