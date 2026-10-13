package thaumcraft.client.gui;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.container.ContainerArcaneBore;
import thaumcraft.common.entities.construct.EntityArcaneBore;

@SideOnly(Side.CLIENT)
public class GuiArcaneBore extends GuiContainer {
   EntityArcaneBore turret;
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_arcanebore.png");

   public GuiArcaneBore(InventoryPlayer par1InventoryPlayer, World world, EntityArcaneBore t) {
      super(new ContainerArcaneBore(par1InventoryPlayer, world, t));
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
      if (this.turret.getHeldItemMainhand() != null
         && !this.turret.getHeldItemMainhand().isEmpty()
         && this.turret.getHeldItemMainhand().getItemDamage() + 1 >= this.turret.getHeldItemMainhand().getMaxDamage()) {
         this.drawTexturedModalRect(k + 80, l + 29, 240, 0, 16, 16);
      }

      GL11.glPushMatrix();
      GL11.glTranslatef(k + 124, l + 18, 505.0F);
      GL11.glScalef(0.5F, 0.5F, 0.0F);
      String text = "Width: " + (1 + this.turret.getDigRadius() * 2);
      this.fontRenderer.drawStringWithShadow(text, 0.0F, 0.0F, 16777215);
      text = "Depth: " + this.turret.getDigDepth();
      this.fontRenderer.drawStringWithShadow(text, 64.0F, 0.0F, 16777215);
      text = "Speed: +" + this.turret.getDigSpeed(Blocks.STONE.getDefaultState());
      this.fontRenderer.drawStringWithShadow(text, 0.0F, 10.0F, 16777215);
      int base = 0;
      int refining = this.turret.getRefining();
      int fortune = this.turret.getFortune();
      if (this.turret.hasSilkTouch() || refining > 0 || fortune > 0) {
         text = "Other properties:";
         this.fontRenderer.drawStringWithShadow(text, 0.0F, 24.0F, 16777215);
      }

      if (refining > 0) {
         text = "Refining " + refining;
         this.fontRenderer.drawStringWithShadow(text, 4.0F, 34 + base, 12632256);
         base += 9;
      }

      if (fortune > 0) {
         text = "Fortune " + fortune;
         this.fontRenderer.drawStringWithShadow(text, 4.0F, 34 + base, 15648330);
         base += 9;
      }

      if (this.turret.hasSilkTouch()) {
         text = "Silk Touch";
         this.fontRenderer.drawStringWithShadow(text, 4.0F, 34 + base, 8421631);
         base += 9;
      }

      GL11.glScalef(1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }
}
