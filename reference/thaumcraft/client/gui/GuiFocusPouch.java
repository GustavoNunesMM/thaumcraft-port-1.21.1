package thaumcraft.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.container.ContainerFocusPouch;

@SideOnly(Side.CLIENT)
public class GuiFocusPouch extends GuiContainer {
   private int blockSlot;
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_focuspouch.png");

   public GuiFocusPouch(InventoryPlayer par1InventoryPlayer, World world, int x, int y, int z) {
      super(new ContainerFocusPouch(par1InventoryPlayer, world, x, y, z));
      this.blockSlot = par1InventoryPlayer.currentItem;
      this.xSize = 175;
      this.ySize = 232;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      super.drawScreen(mouseX, mouseY, partialTicks);
      this.renderHoveredToolTip(mouseX, mouseY);
   }

   protected void drawGuiContainerForegroundLayer(int par1, int par2) {
      this.mc.renderEngine.bindTexture(this.tex);
      float t = this.zLevel;
      this.zLevel = 300.0F;
      GL11.glEnable(3042);
      this.drawTexturedModalRect(8 + this.blockSlot * 18, 209, 240, 0, 16, 16);
      GL11.glDisable(3042);
      this.zLevel = t;
   }

   protected boolean checkHotbarKeys(int par1) {
      return false;
   }

   protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
      if (((ItemStack)this.mc.player.inventory.mainInventory.get(this.blockSlot)).isEmpty()) {
         this.mc.player.closeScreen();
      }

      this.mc.renderEngine.bindTexture(this.tex);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      int var5 = (this.width - this.xSize) / 2;
      int var6 = (this.height - this.ySize) / 2;
      GL11.glEnable(3042);
      this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);
      GL11.glDisable(3042);
   }
}
