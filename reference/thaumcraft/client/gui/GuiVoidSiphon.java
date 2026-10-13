package thaumcraft.client.gui;

import com.sasmaster.glelwjgl.java.CoreGLE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.lib.ender.ShaderCallback;
import thaumcraft.common.container.ContainerVoidSiphon;
import thaumcraft.common.tiles.crafting.TileVoidSiphon;

@SideOnly(Side.CLIENT)
public class GuiVoidSiphon extends GuiContainer {
   private TileVoidSiphon inventory;
   private ContainerVoidSiphon container = null;
   private EntityPlayer player = null;
   CoreGLE gle = new CoreGLE();
   private final ShaderCallback shaderCallback;
   private static final ResourceLocation starsTexture = new ResourceLocation("textures/entity/end_portal.png");
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_void_siphon.png");

   public GuiVoidSiphon(InventoryPlayer par1InventoryPlayer, TileVoidSiphon tileVoidSiphon) {
      super(new ContainerVoidSiphon(par1InventoryPlayer, tileVoidSiphon));
      this.xSize = 176;
      this.ySize = 166;
      this.inventory = tileVoidSiphon;
      this.container = (ContainerVoidSiphon)this.inventorySlots;
      this.player = par1InventoryPlayer.player;
      this.shaderCallback = new ShaderCallback() {
         @Override
         public void call(int shader) {
            Minecraft mc = Minecraft.getMinecraft();
            int x = ARBShaderObjects.glGetUniformLocationARB(shader, "yaw");
            ARBShaderObjects.glUniform1fARB(x, (float)(mc.player.rotationYaw * 2.0F * Math.PI / 360.0));
            int z = ARBShaderObjects.glGetUniformLocationARB(shader, "pitch");
            ARBShaderObjects.glUniform1fARB(z, -((float)(mc.player.rotationPitch * 2.0F * Math.PI / 360.0)));
         }
      };
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      super.drawScreen(mouseX, mouseY, partialTicks);
      this.renderHoveredToolTip(mouseX, mouseY);
   }

   protected void drawGuiContainerBackgroundLayer(float par1, int mx, int my) {
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.mc.renderEngine.bindTexture(this.tex);
      int k = (this.width - this.xSize) / 2;
      int l = (this.height - this.ySize) / 2;
      GL11.glEnable(3042);
      this.drawTexturedModalRect(k, l, 0, 0, this.xSize, this.ySize);
   }
}
