package thaumcraft.client.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.container.ContainerThaumatorium;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketSelectThaumotoriumRecipeToServer;
import thaumcraft.common.tiles.crafting.TileThaumatorium;

@SideOnly(Side.CLIENT)
public class GuiThaumatorium extends GuiContainer {
   private TileThaumatorium inventory;
   private ContainerThaumatorium container = null;
   private int index = 0;
   private int lastSize = 0;
   private EntityPlayer player = null;
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_thaumatorium.png");
   ArrayList<Integer> hashList = new ArrayList<>();
   long lastHLUpdate = 0L;
   static HashMap<Integer, CrucibleRecipe> recipeCache = new HashMap<>();

   public GuiThaumatorium(InventoryPlayer par1InventoryPlayer, TileThaumatorium par2TileEntityFurnace) {
      super(new ContainerThaumatorium(par1InventoryPlayer, par2TileEntityFurnace));
      this.xSize = 175;
      this.ySize = 216;
      this.inventory = par2TileEntityFurnace;
      this.container = (ContainerThaumatorium)this.inventorySlots;
      this.player = par1InventoryPlayer.player;
      this.inventory.updateRecipes(this.player);
      this.lastSize = this.hashList.size();
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
      long t = System.currentTimeMillis();
      if (t > this.lastHLUpdate) {
         this.hashList.clear();
         this.hashList = this.inventory.generateRecipeHashlist();
         this.lastHLUpdate = t + 500L;
      }

      if (this.hashList.size() > 0) {
         if (this.index > this.hashList.size() / 2) {
            this.index = this.hashList.size() / 2;
         }

         if (this.index < 0 || this.hashList.size() <= 6) {
            this.index = 0;
         }

         if (this.hashList.size() > 6) {
            if (this.index > 0) {
               this.drawTexturedModalRect(k + 82, l + 56, 176, 56, 8, 11);
            }

            if (this.index < this.hashList.size() / 2.0F - 3.0F) {
               this.drawTexturedModalRect(k + 82, l + 93, 176, 93, 8, 11);
            }
         }
      }

      this.drawAspects(k, l, t);
      if (this.inventory.maxRecipes > 1) {
         GL11.glPushMatrix();
         GL11.glTranslatef(k + 64, l + 48, 0.0F);
         GL11.glScalef(0.5F, 0.5F, 0.0F);
         String text = this.inventory.recipeHash.size() + "/" + this.inventory.maxRecipes;
         int ll = this.fontRenderer.getStringWidth(text) / 2;
         this.fontRenderer.drawString(text, -ll, 0, 16777215);
         GL11.glScalef(1.0F, 1.0F, 1.0F);
         GL11.glPopMatrix();
      }

      this.drawOutput(k, l, mx, my, t);
   }

   private static CrucibleRecipe getRecipeCached(int hash) {
      if (recipeCache.containsKey(hash)) {
         return recipeCache.get(hash);
      } else {
         CrucibleRecipe cr = ThaumcraftApi.getCrucibleRecipeFromHash(hash);
         if (cr != null) {
            recipeCache.put(hash, cr);
         }

         return cr;
      }
   }

   private void drawAspects(int k, int l, long time) {
      if (this.inventory.recipeHash.size() > 0) {
         int count = 0;
         int px = 0;
         int py = 0;
         GL11.glEnable(3042);
         int hash = this.inventory.recipeHash.get((int)(time / 1000L % this.inventory.recipeHash.size()));
         if (this.inventory.recipeHash.contains(hash)) {
            CrucibleRecipe cr = getRecipeCached(hash);
            if (cr == null) {
               return;
            }

            for (Aspect aspect : cr.getAspects().getAspectsSortedByName()) {
               GL11.glPushMatrix();
               GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
               this.drawTexturedModalRect(k + 98 + 16 * px, l + 40 + 20 * py, 176, 4, 12, 3);
               int i1 = (int)((float)this.inventory.essentia.getAmount(aspect) / cr.getAspects().getAmount(aspect) * 12.0F);
               Color c = new Color(aspect.getColor());
               GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F);
               this.drawTexturedModalRect(k + 98 + 16 * px, l + 40 + 20 * py, 176, 0, i1, 3);
               GL11.glPopMatrix();
               if (++px > 1) {
                  px = 0;
                  py++;
               }

               if (++count >= 8) {
                  break;
               }
            }

            count = 0;
            px = 0;
            py = 0;

            for (Aspect aspect : cr.getAspects().getAspectsSortedByName()) {
               UtilsFX.drawTag(k + 96 + 16 * px, l + 24 + 20 * py, aspect, cr.getAspects().getAmount(aspect), 0, this.zLevel);
               if (++px > 1) {
                  px = 0;
                  py++;
               }

               if (++count >= 8) {
                  break;
               }
            }
         }
      }
   }

   private void drawOutput(int x, int y, int mx, int my, long time) {
      GL11.glPushMatrix();
      int px = 0;
      int py = 0;
      int q = 0;
      int idx = 0;

      for (int hash : this.hashList) {
         if (q++ >= this.index * 2) {
            CrucibleRecipe cr = getRecipeCached(hash);
            if (cr != null) {
               this.drawOutputIcon(x + 48 + px * 16, y + 56 + py * 16, getRecipeCached(hash), time);
               if (++px > 1) {
                  px = 0;
                  py++;
               }

               if (++idx >= 6) {
                  break;
               }
            }
         }
      }

      px = 0;
      py = 0;
      q = 0;
      idx = 0;

      for (int hashx : this.hashList) {
         if (q++ >= this.index * 2) {
            CrucibleRecipe cr = getRecipeCached(hashx);
            if (cr != null) {
               int xx = mx - (x + 48 + px * 16);
               int yy = my - (y + 56 + py * 16);
               if (xx >= 0 && yy >= 0 && xx < 16 && yy < 16) {
                  this.renderToolTip(cr.getRecipeOutput(), mx, my);
                  break;
               }

               if (++px > 1) {
                  px = 0;
                  py++;
               }

               if (++idx >= 6) {
                  break;
               }
            }
         }
      }

      GL11.glDisable(3042);
      GL11.glDisable(2896);
      GL11.glPopMatrix();
   }

   private void drawOutputIcon(int x, int y, CrucibleRecipe cr, long time) {
      if (this.inventory.recipeHash.contains(cr.hash)) {
         int hash = this.inventory.recipeHash.get((int)(time / 1000L % this.inventory.recipeHash.size()));
         this.mc.renderEngine.bindTexture(this.tex);
         GL11.glPushMatrix();
         GL11.glEnable(3042);
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.7F);
         this.drawTexturedModalRect(x, y, 176, 8, 16, 16);
         if (this.inventory.recipeHash.size() > 1 && hash == cr.hash) {
            this.drawTexturedModalRect(x, y, 176, 8, 16, 16);
         }

         GL11.glDisable(3042);
         GL11.glPopMatrix();
      }

      GlStateManager.pushMatrix();
      RenderHelper.enableGUIStandardItemLighting();
      GlStateManager.disableLighting();
      GlStateManager.enableRescaleNormal();
      GlStateManager.enableColorMaterial();
      GlStateManager.enableLighting();
      this.itemRender.zLevel = 100.0F;
      this.itemRender.renderItemAndEffectIntoGUI(cr.getRecipeOutput(), x, y);
      this.itemRender.renderItemOverlays(this.fontRenderer, cr.getRecipeOutput(), x, y);
      this.itemRender.zLevel = 0.0F;
      GlStateManager.disableLighting();
      GlStateManager.popMatrix();
   }

   protected void mouseClicked(int mx, int my, int par3) throws IOException {
      super.mouseClicked(mx, my, par3);
      int gx = (this.width - this.xSize) / 2;
      int gy = (this.height - this.ySize) / 2;
      int px = 0;
      int py = 0;
      int q = 0;
      int idx = 0;

      for (int hash : this.hashList) {
         if (q++ >= this.index * 2) {
            CrucibleRecipe cr = getRecipeCached(hash);
            if (cr != null) {
               int x = mx - (gx + 48 + px * 16);
               int y = my - (gy + 56 + py * 16);
               if (x >= 0 && y >= 0 && x < 16 && y < 16) {
                  PacketHandler.INSTANCE.sendToServer(new PacketSelectThaumotoriumRecipeToServer(this.player, this.inventory.getPos(), hash));
                  this.playButtonSelect();
                  this.lastHLUpdate = 0L;
                  break;
               }

               if (++px > 1) {
                  px = 0;
                  py++;
               }

               if (++idx >= 6) {
                  break;
               }
            }
         }
      }

      if (this.hashList.size() > 6) {
         if (this.index > 0) {
            int xx = mx - (gx + 82);
            int yx = my - (gy + 56);
            if (xx >= 0 && yx >= 0 && xx < 8 && yx < 11) {
               this.index--;
               this.playButtonClick();
               this.lastHLUpdate = 0L;
            }
         }

         if (this.index < this.hashList.size() / 2.0F - 3.0F) {
            int xx = mx - (gx + 82);
            int yx = my - (gy + 93);
            if (xx >= 0 && yx >= 0 && xx < 8 && yx < 11) {
               this.index++;
               this.playButtonClick();
               this.lastHLUpdate = 0L;
            }
         }
      }
   }

   private void playButtonSelect() {
      this.mc.getRenderViewEntity().playSound(SoundsTC.hhon, 0.3F, 1.0F);
   }

   private void playButtonClick() {
      this.mc.getRenderViewEntity().playSound(SoundsTC.clack, 0.4F, 1.0F);
   }
}
