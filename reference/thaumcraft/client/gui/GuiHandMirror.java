package thaumcraft.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.container.ContainerHandMirror;

@SideOnly(Side.CLIENT)
public class GuiHandMirror extends GuiContainer {
   int ci = 0;
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_handmirror.png");

   public GuiHandMirror(InventoryPlayer par1InventoryPlayer, World world, int x, int y, int z) {
      super(new ContainerHandMirror(par1InventoryPlayer, world, x, y, z));
      this.ci = par1InventoryPlayer.currentItem;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      super.drawScreen(mouseX, mouseY, partialTicks);
      this.renderHoveredToolTip(mouseX, mouseY);
   }

   protected void drawGuiContainerForegroundLayer() {
   }

   protected boolean checkHotbarKeys(int par1) {
      return false;
   }

   protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
      this.mc.renderEngine.bindTexture(this.tex);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      int var5 = (this.width - this.xSize) / 2;
      int var6 = (this.height - this.ySize) / 2;
      this.drawTexturedModalRect(var5, var6, 0, 0, this.xSize, this.ySize);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      float t = this.zLevel;
      this.zLevel = 300.0F;
      this.drawTexturedModalRect(var5 + 8 + this.ci * 18, var6 + 142, 240, 0, 16, 16);
      this.zLevel = t;
   }
}
