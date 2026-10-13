package thaumcraft.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.container.ContainerTurretBasic;
import thaumcraft.common.entities.construct.EntityTurretCrossbow;

@SideOnly(Side.CLIENT)
public class GuiTurretBasic extends GuiContainer {
   EntityTurretCrossbow turret;
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_turret_basic.png");

   public GuiTurretBasic(InventoryPlayer par1InventoryPlayer, World world, EntityTurretCrossbow t) {
      super(new ContainerTurretBasic(par1InventoryPlayer, world, t));
      this.xSize = 175;
      this.ySize = 232;
      this.turret = t;
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
      int k = (this.width - this.xSize) / 2;
      int l = (this.height - this.ySize) / 2;
      GL11.glEnable(3042);
      this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);
      int h = (int)(39.0F * (this.turret.getHealth() / this.turret.getMaxHealth()));
      this.drawTexturedModalRect(k + 68, l + 59, 192, 48, h, 6);
   }
}
