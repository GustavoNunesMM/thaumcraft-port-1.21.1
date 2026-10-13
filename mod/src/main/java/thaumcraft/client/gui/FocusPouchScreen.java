package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.FocusPouchMenu;

/** GuiFocusPouch: the pouch's 18 focus slots; the pouch's own hotbar slot is marked as locked. */
public class FocusPouchScreen extends AbstractContainerScreen<FocusPouchMenu> {
   private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_focuspouch.png");

   public FocusPouchScreen(FocusPouchMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      imageWidth = 175;
      imageHeight = 232;
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      renderTooltip(g, mouseX, mouseY);
   }

   @Override
   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
      if (menu.pouchSlot >= 0) {
         RenderSystem.enableBlend();
         g.pose().pushPose();
         g.pose().translate(0, 0, 300);
         g.blit(TEX, 8 + menu.pouchSlot * 18, 209, 240, 0, 16, 16);
         g.pose().popPose();
      }
   }

   @Override
   protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      RenderSystem.enableBlend();
      g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
   }

   /** Hotbar number keys would move the pouch itself. */
   @Override
   protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
      return false;
   }
}
