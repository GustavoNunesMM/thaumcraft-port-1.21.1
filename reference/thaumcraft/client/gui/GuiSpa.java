package thaumcraft.client.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.container.ContainerSpa;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.devices.TileSpa;

@SideOnly(Side.CLIENT)
public class GuiSpa extends GuiContainer {
   private TileSpa spa;
   private float xSize_lo;
   private float ySize_lo;
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_spa.png");

   public GuiSpa(InventoryPlayer par1InventoryPlayer, TileSpa teSpa) {
      super(new ContainerSpa(par1InventoryPlayer, teSpa));
      this.spa = teSpa;
   }

   public void drawScreen(int par1, int par2, float par3) {
      this.drawDefaultBackground();
      super.drawScreen(par1, par2, par3);
      this.xSize_lo = par1;
      this.ySize_lo = par2;
      int baseX = this.guiLeft;
      int baseY = this.guiTop;
      int mposx = par1 - (baseX + 104);
      int mposy = par2 - (baseY + 10);
      if (mposx >= 0 && mposy >= 0 && mposx < 10 && mposy < 55) {
         List list = new ArrayList();
         FluidStack fluid = this.spa.tank.getFluid();
         if (fluid != null) {
            list.add(fluid.getFluid().getLocalizedName(fluid));
            list.add(fluid.amount + " mb");
            this.drawHoveringText(list, par1, par2, this.fontRenderer);
         }
      }

      mposx = par1 - (baseX + 88);
      mposy = par2 - (baseY + 34);
      if (mposx >= 0 && mposy >= 0 && mposx < 10 && mposy < 10) {
         List list = new ArrayList();
         if (this.spa.getMix()) {
            list.add(I18n.translateToLocal("text.spa.mix.true"));
         } else {
            list.add(I18n.translateToLocal("text.spa.mix.false"));
         }

         this.drawHoveringText(list, par1, par2, this.fontRenderer);
      }

      this.renderHoveredToolTip(par2, par2);
   }

   protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.mc.renderEngine.bindTexture(this.tex);
      int k = (this.width - this.xSize) / 2;
      int l = (this.height - this.ySize) / 2;
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);
      if (this.spa.getMix()) {
         this.drawTexturedModalRect(k + 89, l + 35, 208, 16, 8, 8);
      } else {
         this.drawTexturedModalRect(k + 89, l + 35, 208, 32, 8, 8);
      }

      if (this.spa.tank.getFluidAmount() > 0) {
         FluidStack fluid = this.spa.tank.getFluid();
         if (fluid != null) {
            TextureAtlasSprite icon = this.func_175371_a(fluid.getFluid().getBlock());
            if (icon != null) {
               float bar = (float)this.spa.tank.getFluidAmount() / this.spa.tank.getCapacity();
               this.renderFluid(icon, fluid.getFluid());
               this.mc.renderEngine.bindTexture(this.tex);
               this.drawTexturedModalRect(k + 107, l + 15, 107, 15, 10, (int)(48.0F - 48.0F * bar));
            }
         }
      }

      this.drawTexturedModalRect(k + 106, l + 11, 232, 0, 10, 55);
      GL11.glDisable(3042);
   }

   private TextureAtlasSprite func_175371_a(Block p_175371_1_) {
      return Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getTexture(p_175371_1_.getDefaultState());
   }

   public void renderFluid(TextureAtlasSprite icon, Fluid fluid) {
      GL11.glPushMatrix();
      this.mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
      GL11.glScalef(0.5F, 0.5F, 0.5F);
      Color cc = new Color(fluid.getColor());
      GL11.glColor3f(cc.getRed() / 255.0F, cc.getGreen() / 255.0F, cc.getBlue() / 255.0F);

      for (int a = 0; a < 6; a++) {
         this.drawTexturedModalRect((this.guiLeft + 107) * 2, (this.guiTop + 15) * 2 + a * 16, icon, 16, 16);
      }

      GL11.glColor3f(1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }

   protected void mouseClicked(int mx, int my, int par3) throws IOException {
      super.mouseClicked(mx, my, par3);
      int gx = (this.width - this.xSize) / 2;
      int gy = (this.height - this.ySize) / 2;
      int var7 = mx - (gx + 89);
      int var8 = my - (gy + 35);
      if (var7 >= 0 && var8 >= 0 && var7 < 8 && var8 < 8) {
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 1);
         this.playButtonClick();
      }
   }

   private void playButtonClick() {
      this.mc.getRenderViewEntity().playSound(SoundsTC.clack, 0.4F, 1.0F);
   }
}
