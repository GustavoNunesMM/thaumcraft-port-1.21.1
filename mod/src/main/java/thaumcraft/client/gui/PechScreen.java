package thaumcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.menu.PechMenu;
import thaumcraft.common.registers.ModSounds;

/** GuiPech: offer a valued item on the left and press the dice to see what the pech gives for it. */
public class PechScreen extends AbstractContainerScreen<PechMenu> {
   private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_pech.png");

   public PechScreen(PechMenu menu, Inventory inv, Component title) {
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
   }

   @Override
   protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      g.blit(TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
      if (menu.canTrade()) g.blit(TEX, leftPos + 67, topPos + 24, 176, 0, 25, 25);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      int x = (int) mx - (leftPos + 67);
      int y = (int) my - (topPos + 24);
      if (x >= 0 && y >= 0 && x < 25 && y < 25 && menu.canTrade()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.gameMode != null) mc.gameMode.handleInventoryButtonClick(menu.containerId, 0);
         if (mc.player != null) mc.player.playSound(ModSounds.PECH_DICE.get(), 0.5F, 0.95F + mc.player.getRandom().nextFloat() * 0.1F);
         return true;
      }
      return super.mouseClicked(mx, my, button);
   }
}
