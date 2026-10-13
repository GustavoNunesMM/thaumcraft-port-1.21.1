package thaumcraft.client.gui;

import java.io.IOException;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.container.ContainerPech;
import thaumcraft.common.entities.monster.EntityPech;
import thaumcraft.common.lib.SoundsTC;

@SideOnly(Side.CLIENT)
public class GuiPech extends GuiContainer {
   EntityPech pech;
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_pech.png");

   public GuiPech(InventoryPlayer par1InventoryPlayer, World world, EntityPech pech) {
      super(new ContainerPech(par1InventoryPlayer, world, pech));
      this.xSize = 175;
      this.ySize = 232;
      this.pech = pech;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      super.drawScreen(mouseX, mouseY, partialTicks);
      this.renderHoveredToolTip(mouseX, mouseY);
   }

   protected void drawGuiContainerForegroundLayer(int par1, int par2) {
   }

   protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
      this.mc.renderEngine.bindTexture(this.tex);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      int var5 = (this.width - this.xSize) / 2;
      int var6 = (this.height - this.ySize) / 2;
      GL11.glEnable(3042);
      this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);
      if (this.pech.isValued(this.inventorySlots.getSlot(0).getStack())
         && !this.inventorySlots.getSlot(0).getStack().isEmpty()
         && this.inventorySlots.getSlot(1).getStack().isEmpty()
         && this.inventorySlots.getSlot(2).getStack().isEmpty()
         && this.inventorySlots.getSlot(3).getStack().isEmpty()
         && this.inventorySlots.getSlot(4).getStack().isEmpty()) {
         this.drawTexturedModalRect(var5 + 67, var6 + 24, 176, 0, 25, 25);
      }

      GL11.glDisable(3042);
   }

   protected void mouseClicked(int mx, int my, int par3) throws IOException {
      super.mouseClicked(mx, my, par3);
      int gx = (this.width - this.xSize) / 2;
      int gy = (this.height - this.ySize) / 2;
      int var7 = mx - (gx + 67);
      int var8 = my - (gy + 24);
      if (var7 >= 0
         && var8 >= 0
         && var7 < 25
         && var8 < 25
         && this.pech.isValued(this.inventorySlots.getSlot(0).getStack())
         && !this.inventorySlots.getSlot(0).getStack().isEmpty()
         && this.inventorySlots.getSlot(1).getStack().isEmpty()
         && this.inventorySlots.getSlot(2).getStack().isEmpty()
         && this.inventorySlots.getSlot(3).getStack().isEmpty()
         && this.inventorySlots.getSlot(4).getStack().isEmpty()) {
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 0);
         this.playButton();
      }
   }

   private void playButton() {
      this.mc
         .getRenderViewEntity()
         .playSound(SoundsTC.pech_dice, 0.5F, 0.95F + this.mc.getRenderViewEntity().world.rand.nextFloat() * 0.1F);
   }
}
