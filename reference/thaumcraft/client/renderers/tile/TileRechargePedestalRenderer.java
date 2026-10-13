package thaumcraft.client.renderers.tile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.tiles.devices.TileRechargePedestal;

@SideOnly(Side.CLIENT)
public class TileRechargePedestalRenderer extends TileEntitySpecialRenderer<TileRechargePedestal> {
   public void render(TileRechargePedestal ped, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
      super.render(ped, x, y, z, partialTicks, destroyStage, alpha);
      if (ped != null && !ped.getSyncedStackInSlot(0).isEmpty()) {
         EntityItem entityitem = null;
         float ticks = Minecraft.getMinecraft().getRenderViewEntity().ticksExisted + partialTicks;
         GL11.glPushMatrix();
         GL11.glTranslatef((float)x + 0.5F, (float)y + 0.75F, (float)z + 0.5F);
         GL11.glScaled(1.5, 1.5, 1.5);
         GL11.glRotatef(ticks % 360.0F, 0.0F, 1.0F, 0.0F);
         ItemStack is = ped.getSyncedStackInSlot(0).copy();
         is.setCount(1);
         entityitem = new EntityItem(Minecraft.getMinecraft().world, 0.0, 0.0, 0.0, is);
         entityitem.hoverStart = 0.0F;
         RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
         rendermanager.renderEntity(entityitem, 0.0, 0.0, 0.0, 0.0F, 0.0F, false);
         GL11.glPopMatrix();
      }
   }
}
