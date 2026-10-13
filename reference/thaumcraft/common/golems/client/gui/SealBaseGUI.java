package thaumcraft.common.golems.client.gui;

import java.awt.Color;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.ISealGui;
import thaumcraft.client.gui.plugins.GuiHoverButton;
import thaumcraft.client.gui.plugins.GuiPlusMinusButton;
import thaumcraft.client.lib.CustomRenderItem;

@SideOnly(Side.CLIENT)
public class SealBaseGUI extends GuiContainer {
   ISealEntity seal;
   int middleX;
   int middleY;
   int category = -1;
   int[] categories;
   ResourceLocation tex = new ResourceLocation("thaumcraft", "textures/gui/gui_base.png");

   public SealBaseGUI(InventoryPlayer player, World world, ISealEntity seal) {
      super(new SealBaseContainer(player, world, seal));
      this.seal = seal;
      this.xSize = 176;
      this.ySize = 232;
      this.middleX = this.xSize / 2;
      this.middleY = (this.ySize - 72) / 2 - 8;
      if (seal.getSeal() instanceof ISealGui) {
         this.categories = ((ISealGui)seal.getSeal()).getGuiCategories();
      } else {
         this.categories = new int[]{0, 4};
      }
   }

   private ModelManager getModelmanager() {
      return Minecraft.getMinecraft().modelManager;
   }

   public void initGui() {
      super.initGui();
      this.itemRender = new CustomRenderItem();
      this.setupCategories();
   }

   private void setupCategories() {
      this.buttonList.clear();
      int c = 0;
      float slice = 60.0F / this.categories.length;
      float start = -180.0F + (this.categories.length - 1) * slice / 2.0F;
      if (slice > 24.0F) {
         slice = 24.0F;
      }

      if (slice < 12.0F) {
         slice = 12.0F;
      }

      for (int cat : this.categories) {
         if (this.category < 0) {
            this.category = cat;
         }

         if (this.categories.length > 1) {
            int xx = (int)(MathHelper.cos((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
            int yy = (int)(MathHelper.sin((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
            this.buttonList
               .add(
                  new GuiGolemCategoryButton(
                     c,
                     this.guiLeft + this.middleX + xx,
                     this.guiTop + this.middleY + yy,
                     16,
                     16,
                     "button.category." + cat,
                     cat,
                     this.category == cat
                  )
               );
         }

         c++;
      }

      int xx = (int)(MathHelper.cos((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
      int yy = (int)(MathHelper.sin((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
      this.buttonList
         .add(new GuiGolemRedstoneButton(27, this.guiLeft + this.middleX + xx - 8, this.guiTop + this.middleY + yy - 8, 16, 16, this.seal));
      switch (this.category) {
         case 0:
            this.buttonList
               .add(new GuiPlusMinusButton(80, this.guiLeft + this.middleX - 5 - 14, this.guiTop + this.middleY - 17, 10, 10, true));
            this.buttonList
               .add(new GuiPlusMinusButton(81, this.guiLeft + this.middleX - 5 + 14, this.guiTop + this.middleY - 17, 10, 10, false));
            this.buttonList
               .add(new GuiPlusMinusButton(82, this.guiLeft + this.middleX + 18 - 12, this.guiTop + this.middleY + 4, 10, 10, true));
            this.buttonList
               .add(new GuiPlusMinusButton(83, this.guiLeft + this.middleX + 18 + 11, this.guiTop + this.middleY + 4, 10, 10, false));
            this.buttonList.add(new GuiGolemLockButton(25, this.guiLeft + this.middleX - 32, this.guiTop + this.middleY, 16, 16, this.seal));
            break;
         case 1:
            if (this.seal.getSeal() instanceof ISealConfigFilter) {
               int s = ((ISealConfigFilter)this.seal.getSeal()).getFilterSize();
               int sy = 16 + (s - 1) / 3 * 12;
               this.buttonList
                  .add(
                     new GuiGolemBWListButton(
                        20,
                        this.guiLeft + this.middleX - 8,
                        this.guiTop + this.middleY + (s - 1) / 3 * 24 - sy + 27,
                        16,
                        16,
                        (ISealConfigFilter)this.seal.getSeal()
                     )
                  );
            }
            break;
         case 2:
            this.buttonList
               .add(new GuiPlusMinusButton(90, this.guiLeft + this.middleX - 5 - 14, this.guiTop + this.middleY - 25, 10, 10, true));
            this.buttonList
               .add(new GuiPlusMinusButton(91, this.guiLeft + this.middleX - 5 + 14, this.guiTop + this.middleY - 25, 10, 10, false));
            this.buttonList.add(new GuiPlusMinusButton(92, this.guiLeft + this.middleX - 5 - 14, this.guiTop + this.middleY, 10, 10, true));
            this.buttonList.add(new GuiPlusMinusButton(93, this.guiLeft + this.middleX - 5 + 14, this.guiTop + this.middleY, 10, 10, false));
            this.buttonList
               .add(new GuiPlusMinusButton(94, this.guiLeft + this.middleX - 5 - 14, this.guiTop + this.middleY + 25, 10, 10, true));
            this.buttonList
               .add(new GuiPlusMinusButton(95, this.guiLeft + this.middleX - 5 + 14, this.guiTop + this.middleY + 25, 10, 10, false));
            break;
         case 3:
            if (this.seal.getSeal() instanceof ISealConfigToggles) {
               ISealConfigToggles cp = (ISealConfigToggles)this.seal.getSeal();
               int s = cp.getToggles().length < 4 ? 8 : (cp.getToggles().length < 6 ? 7 : (cp.getToggles().length < 9 ? 6 : 5));
               int h = (cp.getToggles().length - 1) * s;
               int w = 12;

               for (ISealConfigToggles.SealToggle prop : cp.getToggles()) {
                  int ww = 12 + Math.min(100, this.fontRenderer.getStringWidth(I18n.translateToLocal(prop.getName())));
                  ww /= 2;
                  if (ww > w) {
                     w = ww;
                  }
               }

               int p = 0;

               for (ISealConfigToggles.SealToggle propx : cp.getToggles()) {
                  this.buttonList
                     .add(
                        new GuiGolemPropButton(
                           30 + p, this.guiLeft + this.middleX - w, this.guiTop + this.middleY - 5 - h + p * s * 2, 8, 8, propx.getName(), propx
                        )
                     );
                  p++;
               }
            }
            break;
         case 4:
            EnumGolemTrait[] tags = this.seal.getSeal().getRequiredTags();
            if (tags != null && tags.length > 0) {
               int p = 0;

               for (EnumGolemTrait tag : tags) {
                  this.buttonList
                     .add(
                        new GuiHoverButton(
                           this,
                           500 + p,
                           this.guiLeft + this.middleX + p * 18 - (tags.length - 1) * 9,
                           this.guiTop + this.middleY - 8,
                           16,
                           16,
                           tag.getLocalizedName(),
                           tag.getLocalizedDescription(),
                           tag.icon
                        )
                     );
                  p++;
               }
            }

            tags = this.seal.getSeal().getForbiddenTags();
            if (tags != null && tags.length > 0) {
               int p = 0;

               for (EnumGolemTrait tag : tags) {
                  this.buttonList
                     .add(
                        new GuiHoverButton(
                           this,
                           600 + p,
                           this.guiLeft + this.middleX + p * 18 - (tags.length - 1) * 9,
                           this.guiTop + this.middleY + 24,
                           16,
                           16,
                           tag.getLocalizedName(),
                           tag.getLocalizedDescription(),
                           tag.icon
                        )
                     );
                  p++;
               }
            }
      }
   }

   protected boolean checkHotbarKeys(int par1) {
      return false;
   }

   protected void drawGuiContainerBackgroundLayer(float par1, int mouseX, int mouseY) {
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      this.mc.renderEngine.bindTexture(this.tex);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.drawTexturedModalRect(this.guiLeft + this.middleX - 80, this.guiTop + this.middleY - 80, 96, 0, 160, 160);
      this.drawTexturedModalRect(this.guiLeft, this.guiTop + 143, 0, 167, 176, 89);
      this.drawCenteredString(
         this.fontRenderer,
         I18n.translateToLocal("button.category." + this.category),
         this.guiLeft + this.middleX,
         this.guiTop + this.middleY - 64,
         16777215
      );
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      this.mc.renderEngine.bindTexture(this.tex);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      switch (this.category) {
         case 0:
            this.drawTexturedModalRect(this.guiLeft + this.middleX + 17, this.guiTop + this.middleY + 3, 2, 18, 12, 12);
            if (this.seal.getColor() >= 1 && this.seal.getColor() <= 16) {
               Color c = new Color(EnumDyeColor.byMetadata(this.seal.getColor() - 1).getColorValue());
               GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F);
               this.drawTexturedModalRect(this.guiLeft + this.middleX + 20, this.guiTop + this.middleY + 6, 74, 31, 6, 6);
               GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            }

            int mx = mouseX - this.guiLeft;
            int my = mouseY - this.guiTop;
            if (mx >= this.middleX + 5 && mx <= this.middleX + 41 && my >= this.middleY + 3 && my <= this.middleY + 15) {
               if (this.seal.getColor() >= 1 && this.seal.getColor() <= 16) {
                  String s = "color." + EnumDyeColor.byMetadata(this.seal.getColor() - 1).getName();
                  String s2 = I18n.translateToLocal("golem.prop.color");
                  s2 = s2.replace("%s", I18n.translateToLocal(s));
                  this.drawCenteredString(this.fontRenderer, s2, this.guiLeft + this.middleX + 23, this.guiTop + this.middleY + 17, 16777215);
               } else {
                  this.drawCenteredString(
                     this.fontRenderer,
                     I18n.translateToLocal("golem.prop.colorall"),
                     this.guiLeft + this.middleX + 23,
                     this.guiTop + this.middleY + 17,
                     16777215
                  );
               }
            }

            this.drawCenteredString(
               this.fontRenderer,
               I18n.translateToLocal("golem.prop.priority"),
               this.guiLeft + this.middleX,
               this.guiTop + this.middleY - 28,
               12299007
            );
            this.drawCenteredString(
               this.fontRenderer, "" + this.seal.getPriority(), this.guiLeft + this.middleX, this.guiTop + this.middleY - 16, 16777215
            );
            if (this.seal.getOwner().equals(this.mc.player.getUniqueID().toString())) {
               this.drawCenteredString(
                  this.fontRenderer,
                  I18n.translateToLocal("golem.prop.owner"),
                  this.guiLeft + this.middleX,
                  this.guiTop + this.middleY + 32,
                  12299007
               );
            }
            break;
         case 1:
            if (this.seal.getSeal() instanceof ISealConfigFilter) {
               int s = ((ISealConfigFilter)this.seal.getSeal()).getFilterSize();
               int sx = 16 + (s - 1) % 3 * 12;
               int sy = 16 + (s - 1) / 3 * 12;

               for (int a = 0; a < s; a++) {
                  int x = a % 3;
                  int y = a / 3;
                  this.drawTexturedModalRect(this.guiLeft + this.middleX + x * 24 - sx, this.guiTop + this.middleY + y * 24 - sy, 0, 56, 32, 32);
               }
            }
            break;
         case 2:
            this.drawCenteredString(
               this.fontRenderer,
               I18n.translateToLocal("button.caption.y"),
               this.guiLeft + this.middleX,
               this.guiTop + this.middleY - 24 - 9,
               14540253
            );
            this.drawCenteredString(
               this.fontRenderer, I18n.translateToLocal("button.caption.x"), this.guiLeft + this.middleX, this.guiTop + this.middleY - 9, 14540253
            );
            this.drawCenteredString(
               this.fontRenderer,
               I18n.translateToLocal("button.caption.z"),
               this.guiLeft + this.middleX,
               this.guiTop + this.middleY + 24 - 9,
               14540253
            );
            this.drawCenteredString(
               this.fontRenderer,
               "" + this.seal.getArea().getY(),
               this.guiLeft + this.middleX,
               this.guiTop + this.middleY - 24,
               16777215
            );
            this.drawCenteredString(
               this.fontRenderer, "" + this.seal.getArea().getX(), this.guiLeft + this.middleX, this.guiTop + this.middleY, 16777215
            );
            this.drawCenteredString(
               this.fontRenderer,
               "" + this.seal.getArea().getZ(),
               this.guiLeft + this.middleX,
               this.guiTop + this.middleY + 24,
               16777215
            );
         case 3:
         default:
            break;
         case 4:
            this.drawCenteredString(
               this.fontRenderer,
               I18n.translateToLocal("button.caption.required"),
               this.guiLeft + this.middleX,
               this.guiTop + this.middleY - 26,
               14540253
            );
            this.drawCenteredString(
               this.fontRenderer,
               I18n.translateToLocal("button.caption.forbidden"),
               this.guiLeft + this.middleX,
               this.guiTop + this.middleY + 6,
               14540253
            );
      }
   }

   protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
      RenderHelper.disableStandardItemLighting();

      for (GuiButton guibutton : this.buttonList) {
         if (guibutton.isMouseOver()) {
            guibutton.drawButtonForegroundLayer(mouseX - this.guiLeft, mouseY - this.guiTop);
            break;
         }
      }

      RenderHelper.enableGUIStandardItemLighting();
   }

   protected void actionPerformed(GuiButton button) throws IOException {
      if (button.id < this.categories.length && this.categories[button.id] != this.category) {
         this.category = this.categories[button.id];
         ((SealBaseContainer)this.inventorySlots).category = this.category;
         ((SealBaseContainer)this.inventorySlots).setupCategories();
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, button.id);
         this.setupCategories();
      } else if (this.category == 0
         && button.id == 25
         && this.seal.getOwner().equals(this.mc.player.getUniqueID().toString())) {
         this.seal.setLocked(!this.seal.isLocked());
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, this.seal.isLocked() ? 25 : 26);
      } else if (this.category == 1 && this.seal.getSeal() instanceof ISealConfigFilter && button.id == 20) {
         ISealConfigFilter cp = (ISealConfigFilter)this.seal.getSeal();
         cp.setBlacklist(!cp.isBlacklist());
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, cp.isBlacklist() ? 20 : 21);
      } else if (this.category == 3
         && this.seal.getSeal() instanceof ISealConfigToggles
         && button.id >= 30
         && button.id < 30 + ((ISealConfigToggles)this.seal.getSeal()).getToggles().length) {
         ISealConfigToggles cp = (ISealConfigToggles)this.seal.getSeal();
         cp.setToggle(button.id - 30, !cp.getToggles()[button.id - 30].getValue());
         this.mc
            .playerController
            .sendEnchantPacket(
               this.inventorySlots.windowId, cp.getToggles()[button.id - 30].getValue() ? button.id : button.id + 30
            );
      } else if (button.id == 27 && this.seal.getOwner().equals(this.mc.player.getUniqueID().toString())) {
         this.seal.setRedstoneSensitive(!this.seal.isRedstoneSensitive());
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, this.seal.isRedstoneSensitive() ? 27 : 28);
      } else {
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, button.id);
      }
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      super.drawScreen(mouseX, mouseY, partialTicks);
      if (this.category == 1 && this.seal.getSeal() instanceof ISealConfigFilter && !((ISealConfigFilter)this.seal.getSeal()).isBlacklist()) {
         int x = this.guiLeft;
         int y = this.guiTop;
         ISealConfigFilter cp = (ISealConfigFilter)this.seal.getSeal();
         int k = 240;
         int l = 240;
         OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         this.zLevel = 100.0F;

         for (int i1 = 0; i1 < cp.getFilterSize(); i1++) {
            Slot slot = (Slot)this.inventorySlots.inventorySlots.get(i1);
            if (slot.isEnabled() && !slot.getStack().isEmpty()) {
               int i = slot.xPos;
               int j = slot.yPos;
               int sz = cp.getFilterSlotSize(i1);
               String s = String.valueOf(cp.getFilterSlotSize(i1));
               if (sz == 0) {
                  s = TextFormatting.GOLD.toString() + "*";
               }

               GlStateManager.disableLighting();
               GlStateManager.disableDepth();
               GlStateManager.disableBlend();
               this.fontRenderer.drawStringWithShadow(s, x + i + 19 - 2 - this.fontRenderer.getStringWidth(s), y + j + 6 + 3, 16777215);
               GlStateManager.enableLighting();
               GlStateManager.enableDepth();
               GlStateManager.enableBlend();
            }
         }

         this.zLevel = 0.0F;
         RenderHelper.disableStandardItemLighting();
         RenderHelper.enableGUIStandardItemLighting();
         RenderHelper.enableStandardItemLighting();
      }
   }
}
