package thaumcraft.client.gui;

import java.io.IOException;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.gui.plugins.GuiToggleButton;
import thaumcraft.common.container.ContainerTurretAdvanced;
import thaumcraft.common.entities.construct.EntityTurretCrossbowAdvanced;

@SideOnly(Side.CLIENT)
public class GuiTurretAdvanced extends GuiContainer {
   EntityTurretCrossbowAdvanced turret;
   public static ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_turret_advanced.png");

   public GuiTurretAdvanced(InventoryPlayer par1InventoryPlayer, World world, EntityTurretCrossbowAdvanced t) {
      super(new ContainerTurretAdvanced(par1InventoryPlayer, world, t));
      this.xSize = 175;
      this.ySize = 232;
      this.turret = t;
   }

   public void initGui() {
      super.initGui();
      this.buttonList.add(new GuiToggleButton(1, this.guiLeft + 90, this.guiTop + 13, 8, 8, "button.turretfocus.1", new Runnable() {
         @Override
         public void run() {
            GuiToggleButton.toggled = GuiTurretAdvanced.this.turret.getTargetAnimal();
         }
      }));
      this.buttonList.add(new GuiToggleButton(2, this.guiLeft + 90, this.guiTop + 27, 8, 8, "button.turretfocus.2", new Runnable() {
         @Override
         public void run() {
            GuiToggleButton.toggled = GuiTurretAdvanced.this.turret.getTargetMob();
         }
      }));
      this.buttonList.add(new GuiToggleButton(3, this.guiLeft + 90, this.guiTop + 41, 8, 8, "button.turretfocus.3", new Runnable() {
         @Override
         public void run() {
            GuiToggleButton.toggled = GuiTurretAdvanced.this.turret.getTargetPlayer();
         }
      }));
      this.buttonList.add(new GuiToggleButton(4, this.guiLeft + 90, this.guiTop + 55, 8, 8, "button.turretfocus.4", new Runnable() {
         @Override
         public void run() {
            GuiToggleButton.toggled = GuiTurretAdvanced.this.turret.getTargetFriendly();
         }
      }));
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      super.drawScreen(mouseX, mouseY, partialTicks);
      this.renderHoveredToolTip(mouseX, mouseY);
   }

   protected void drawGuiContainerForegroundLayer(int par1, int par2) {
   }

   protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
      this.mc.renderEngine.bindTexture(tex);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      int k = (this.width - this.xSize) / 2;
      int l = (this.height - this.ySize) / 2;
      GL11.glEnable(3042);
      this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);
      int h = (int)(39.0F * (this.turret.getHealth() / this.turret.getMaxHealth()));
      this.drawTexturedModalRect(k + 30, l + 59, 192, 48, h, 6);
   }

   protected void actionPerformed(GuiButton button) throws IOException {
      if (button.id == 1) {
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 1);
      } else if (button.id == 2) {
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 2);
      } else if (button.id == 3) {
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 3);
      } else if (button.id == 4) {
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 4);
      } else {
         super.actionPerformed(button);
      }
   }
}
