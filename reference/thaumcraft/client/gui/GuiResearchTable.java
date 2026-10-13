package thaumcraft.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.gui.plugins.GuiImageButton;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketStartTheoryToServer;
import thaumcraft.common.tiles.crafting.TileResearchTable;

@SideOnly(Side.CLIENT)
public class GuiResearchTable extends GuiContainer {
   private float xSize_lo;
   private float ySize_lo;
   private TileResearchTable table;
   private FontRenderer galFontRenderer;
   private String username;
   EntityPlayer player;
   ResourceLocation txBackground = new ResourceLocation("thaumcraft", "textures/gui/gui_research_table.png");
   ResourceLocation txBase = new ResourceLocation("thaumcraft", "textures/gui/gui_base.png");
   ResourceLocation txPaper = new ResourceLocation("thaumcraft", "textures/gui/paper.png");
   ResourceLocation txPaperGilded = new ResourceLocation("thaumcraft", "textures/gui/papergilded.png");
   ResourceLocation txQuestion = new ResourceLocation("thaumcraft", "textures/aspects/_unknown.png");
   ResearchTableData.CardChoice lastDraw;
   float[] cardHover = new float[]{0.0F, 0.0F, 0.0F};
   float[] cardZoomOut = new float[]{0.0F, 0.0F, 0.0F};
   float[] cardZoomIn = new float[]{0.0F, 0.0F, 0.0F};
   boolean[] cardActive = new boolean[]{true, true, true};
   boolean cardSelected = false;
   public HashMap<String, Integer> tempCatTotals = new HashMap<>();
   long nexCatCheck = 0L;
   long nextCheck = 0L;
   int dummyInspirationStart = 0;
   Set<String> currentAids = new HashSet<>();
   Set<String> selectedAids = new HashSet<>();
   GuiImageButton buttonCreate = new GuiImageButton(
      this, 1, this.guiLeft + 128, this.guiTop + 22, 49, 11, "button.create.theory", null, this.txBase, 37, 66, 51, 13, 8978346
   );
   GuiImageButton buttonComplete = new GuiImageButton(
      this, 7, this.guiLeft + 191, this.guiTop + 96, 49, 11, "button.complete.theory", null, this.txBase, 37, 66, 51, 13, 8978346
   );
   GuiImageButton buttonScrap = new GuiImageButton(
      this, 9, this.guiLeft + 128, this.guiTop + 168, 49, 11, "button.scrap.theory", null, this.txBase, 37, 66, 51, 13, 16720418
   );
   public ArrayList<ResearchTableData.CardChoice> cardChoices = new ArrayList<>();

   public GuiResearchTable(EntityPlayer player, TileResearchTable e) {
      super(new ContainerResearchTable(player.inventory, e));
      this.table = e;
      this.xSize = 255;
      this.ySize = 255;
      this.galFontRenderer = FMLClientHandler.instance().getClient().standardGalacticFontRenderer;
      this.username = player.getName();
      this.player = player;
      if (this.table.data != null) {
         for (String cat : this.table.data.categoryTotals.keySet()) {
            this.tempCatTotals.put(cat, this.table.data.categoryTotals.get(cat));
         }

         this.syncFromTableChoices();
         this.lastDraw = this.table.data.lastDraw;
      }
   }

   private void syncFromTableChoices() {
      this.cardChoices.clear();

      for (ResearchTableData.CardChoice cc : this.table.data.cardChoices) {
         this.cardChoices.add(cc);
      }
   }

   protected void drawGuiContainerForegroundLayer(int mx, int my) {
   }

   public void drawScreen(int mx, int my, float par3) {
      this.drawDefaultBackground();
      super.drawScreen(mx, my, par3);
      this.xSize_lo = mx;
      this.ySize_lo = my;
      int xx = this.guiLeft;
      int yy = this.guiTop;
      RenderHelper.disableStandardItemLighting();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.table.data == null) {
         if (!this.currentAids.isEmpty()) {
            int side = Math.min(this.currentAids.size(), 6);
            int c = 0;
            int r = 0;
            GL11.glPushMatrix();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.2F);
            this.mc.renderEngine.bindTexture(this.txBase);

            for (String key : this.currentAids) {
               ITheorycraftAid mutator = TheorycraftManager.aids.get(key);
               if (mutator != null) {
                  int x = xx + 128 + 20 * c - side * 10;
                  int y = yy + 85 + 35 * r;
                  if (this.isPointInRegion(x - xx, y - yy, 16, 16, mx, my) && !this.selectedAids.contains(key)) {
                     this.drawTexturedModalRect(x, y, 0, 96, 16, 16);
                  }

                  if (++c >= side) {
                     r++;
                     c = 0;
                  }
               }
            }

            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
         }
      } else {
         int sx = 128;
         int cw = 110;
         int sz = this.cardChoices.size();
         int a = 0;
         if (!this.cardSelected) {
            for (ResearchTableData.CardChoice cardChoice : this.cardChoices) {
               if (this.cardZoomOut[a] >= 1.0F) {
                  float dx = 55 + sx - 55 * sz + cw * a - 65;
                  float fx = 65.0F + dx * this.cardZoomOut[a];
                  float qx = 191.0F - fx;
                  if (this.cardActive[a]) {
                     fx += qx * this.cardZoomIn[a];
                  }

                  this.drawSheetOverlay(fx, 100.0, cardChoice, mx, my);
                  a++;
               }
            }
         }

         int qq = 0;
         if (this.table.getStackInSlot(0) == null
            || this.table.getStackInSlot(0).isEmpty()
            || this.table.getStackInSlot(0).getItemDamage() == this.table.getStackInSlot(0).getMaxDamage()) {
            sx = Math.max(
                  this.fontRenderer.getStringWidth(I18n.translateToLocal("tile.researchtable.noink.0")),
                  this.fontRenderer.getStringWidth(I18n.translateToLocal("tile.researchtable.noink.1"))
               )
               / 2;
            UtilsFX.drawCustomTooltip(
               this,
               this.fontRenderer,
               Arrays.asList(I18n.translateToLocal("tile.researchtable.noink.0"), I18n.translateToLocal("tile.researchtable.noink.1")),
               xx - sx + 116,
               yy + 60 + qq,
               11,
               true
            );
            qq += 40;
         }

         if (this.table.getStackInSlot(1) == null || this.table.getStackInSlot(1).isEmpty()) {
            sx = this.fontRenderer.getStringWidth(I18n.translateToLocal("tile.researchtable.nopaper.0")) / 2;
            UtilsFX.drawCustomTooltip(
               this, this.fontRenderer, Arrays.asList(I18n.translateToLocal("tile.researchtable.nopaper.0")), xx - sx + 116, yy + 60 + qq, 11, true
            );
         }
      }

      this.renderHoveredToolTip(mx, my);
   }

   protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my) {
      this.checkButtons();
      int xx = this.guiLeft;
      int yy = this.guiTop;
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      this.mc.renderEngine.bindTexture(this.txBackground);
      this.drawTexturedModalRect(xx, yy, 0, 0, 255, 255);
      this.fontRenderer.drawString(" ", 0, 0, 0);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.table.data == null) {
         if (this.nextCheck < this.player.ticksExisted) {
            this.currentAids = this.table.checkSurroundingAids();
            this.dummyInspirationStart = ResearchTableData.getAvailableInspiration(this.player);
            this.nextCheck = this.player.ticksExisted + 100;
         }

         this.mc.renderEngine.bindTexture(this.txBase);
         GL11.glPushMatrix();
         GL11.glTranslated(xx + 128 - this.dummyInspirationStart * 5, yy + 55, 0.0);
         GL11.glScaled(0.5, 0.5, 0.0);

         for (int a = 0; a < this.dummyInspirationStart; a++) {
            this.drawTexturedModalRect(20 * a, 0, this.dummyInspirationStart - this.selectedAids.size() <= a ? 48 : 32, 96, 16, 16);
         }

         GL11.glPopMatrix();
         if (!this.currentAids.isEmpty()) {
            int side = Math.min(this.currentAids.size(), 6);
            int c = 0;
            int r = 0;

            for (String key : this.currentAids) {
               ITheorycraftAid mutator = TheorycraftManager.aids.get(key);
               if (mutator != null) {
                  int x = xx + 128 + 20 * c - side * 10;
                  int y = yy + 85 + 35 * r;
                  if (this.selectedAids.contains(key)) {
                     this.mc.renderEngine.bindTexture(this.txBase);
                     this.drawTexturedModalRect(x, y, 0, 96, 16, 16);
                  }

                  if (mutator.getAidObject() instanceof ItemStack || mutator.getAidObject() instanceof Block) {
                     GL11.glPushMatrix();
                     RenderHelper.enableGUIStandardItemLighting();
                     GlStateManager.disableLighting();
                     GlStateManager.enableRescaleNormal();
                     GlStateManager.enableColorMaterial();
                     GlStateManager.enableLighting();
                     ItemStack s = mutator.getAidObject() instanceof ItemStack
                        ? (ItemStack)mutator.getAidObject()
                        : new ItemStack((Block)mutator.getAidObject());
                     this.itemRender.renderItemAndEffectIntoGUI(s, x, y);
                     GlStateManager.disableLighting();
                     GlStateManager.depthMask(true);
                     GlStateManager.enableDepth();
                     GL11.glPopMatrix();
                  }

                  if (++c >= side) {
                     r++;
                     c = 0;
                  }
               }
            }
         }
      } else {
         this.checkCards();
         this.mc.renderEngine.bindTexture(this.txBase);
         GL11.glPushMatrix();
         GL11.glTranslated(xx + 15, yy + 150, 0.0);
         if (this.table.data != null) {
            for (int a = 0; a < this.table.data.bonusDraws; a++) {
               this.drawTexturedModalRect(a * 2, a, 64, 96, 16, 16);
            }
         }

         GL11.glPopMatrix();
         GL11.glPushMatrix();
         GL11.glTranslated(xx + 128 - this.table.data.inspirationStart * 5, yy + 16, 0.0);
         GL11.glScaled(0.5, 0.5, 0.0);

         for (int a = 0; a < this.table.data.inspirationStart; a++) {
            this.drawTexturedModalRect(20 * a, 0, this.table.data.inspiration <= a ? 48 : 32, 96, 16, 16);
         }

         GL11.glPopMatrix();
         int sheets = 0;
         if (this.table.getStackInSlot(1) != null) {
            sheets = 1 + this.table.getStackInSlot(1).getCount() / 4;
         }

         Random r = new Random(55L);
         if (sheets > 0 && !this.table.data.isComplete()) {
            for (int a = 0; a < sheets; a++) {
               this.drawSheet(xx + 65, yy + 100, 6.0, r, 1.0F, 1.0F, null);
            }

            boolean highlight = false;
            int var7 = mx - (25 + xx);
            int var8 = my - (55 + yy);
            if (this.cardChoices.isEmpty() && var7 >= 0 && var8 >= 0 && var7 < 75 && var8 < 90) {
               highlight = true;
            }

            GL11.glPushMatrix();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, highlight ? 1.0F : 0.5F);
            GlStateManager.enableBlend();
            this.mc.renderEngine.bindTexture(this.txQuestion);
            GL11.glTranslated(xx + 65, yy + 100, 0.0);
            GL11.glScaled(highlight ? 1.75 : 1.5, highlight ? 1.75 : 1.5, 0.0);
            UtilsFX.drawTexturedQuadFull(-8.0F, -8.0F, 0.0);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
         }

         for (Long seed : this.table.data.savedCards) {
            r = new Random(seed);
            this.drawSheet(xx + 191, yy + 100, 6.0, r, 1.0F, 1.0F, null);
         }

         if (this.lastDraw != null) {
            r = new Random(this.lastDraw.card.getSeed());
            this.drawSheet(xx + 191, yy + 100, 6.0, r, 1.0F, 1.0F, this.lastDraw);
         }

         ArrayList<String> sparkle = new ArrayList<>();
         if (this.nexCatCheck < this.player.ticksExisted) {
            for (String cat : ResearchCategories.researchCategories.keySet()) {
               int t0 = 0;
               if (this.table.data.categoryTotals.containsKey(cat)) {
                  t0 = this.table.data.categoryTotals.get(cat);
               }

               int t1 = 0;
               if (this.tempCatTotals.containsKey(cat)) {
                  t1 = this.tempCatTotals.get(cat);
               }

               if (t0 == 0 && t1 == 0) {
                  this.tempCatTotals.remove(cat);
               } else {
                  if (t1 > t0) {
                     t1--;
                  }

                  if (t1 < t0) {
                     t1++;
                     sparkle.add(cat);
                  }

                  this.tempCatTotals.put(cat, t1);
               }
            }

            this.nexCatCheck = this.player.ticksExisted + 1;
         }

         HashMap<String, Integer> unsortedMap = new HashMap<>();
         String hf = null;
         int lf = 0;

         for (String cat : this.tempCatTotals.keySet()) {
            int cf = this.tempCatTotals.get(cat);
            if (cf != 0) {
               if (cf > lf) {
                  lf = cf;
                  hf = cat;
               }

               unsortedMap.put(cat, cf);
            }
         }

         if (hf != null) {
            unsortedMap.put(hf, unsortedMap.get(hf));
         }

         Comparator<Entry<String, Integer>> valueComparator = (e1, e2) -> e2.getValue().compareTo(e1.getValue());
         Map<String, Integer> sortedMap = unsortedMap.entrySet()
            .stream()
            .sorted(valueComparator)
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         int i = 0;

         for (String field : sortedMap.keySet()) {
            GL11.glPushMatrix();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glTranslatef(xx + 253, yy + 16 + i * 18 + (i > 0 ? 4 : 0), 0.0F);
            GL11.glScaled(0.0625, 0.0625, 0.0625);
            this.mc.renderEngine.bindTexture(ResearchCategories.getResearchCategory(field).icon);
            this.drawTexturedModalRect(0, 0, 0, 0, 255, 255);
            GL11.glPopMatrix();
            GL11.glTranslatef(0.0F, 0.0F, 5.0F);
            String s = sortedMap.get(field) + "%";
            if (i > this.table.data.penaltyStart) {
               int q = sortedMap.get(field) / 3;
               s = s + " (-" + q + ")";
            }

            this.mc
               .fontRenderer
               .drawStringWithShadow(
                  s,
                  xx + 276,
                  yy + 20 + i * 18 + (i > this.table.data.penaltyStart ? 4 : 0),
                  this.table.data.categoriesBlocked.contains(field) ? 6316128 : (i <= this.table.data.penaltyStart ? '\ue0c0' : 16777215)
               );
            if (sparkle.contains(field)) {
               for (int q = 0; q < 2; q++) {
                  float rr = MathHelper.getInt(this.player.getRNG(), 255, 255) / 255.0F;
                  float gg = MathHelper.getInt(this.player.getRNG(), 189, 255) / 255.0F;
                  float bb = MathHelper.getInt(this.player.getRNG(), 64, 255) / 255.0F;
                  FXDispatcher.INSTANCE
                     .drawSimpleSparkleGui(
                        this.player.getRNG(),
                        xx + 276 + this.player.getRNG().nextFloat() * this.fontRenderer.getStringWidth(s),
                        yy + 20 + this.player.getRNG().nextFloat() * 8.0F + i * 18 + (i > this.table.data.penaltyStart ? 4 : 0),
                        this.player.world.rand.nextGaussian() * 0.5,
                        this.player.world.rand.nextGaussian() * 0.5,
                        24.0F,
                        rr,
                        gg,
                        bb,
                        0,
                        0.9F,
                        -1.0F
                     );
               }
            }

            int var7 = mx - (xx + 256);
            int var8 = my - (yy + 16 + i * 18 + (i > this.table.data.penaltyStart ? 4 : 0));
            if (var7 >= 0 && var8 >= 0 && var7 < 16 && var8 < 16) {
               GL11.glPushMatrix();
               UtilsFX.drawCustomTooltip(this, this.fontRenderer, Arrays.asList(ResearchCategories.getCategoryName(field)), mx + 8, my + 8, 11);
               GL11.glPopMatrix();
               RenderHelper.disableStandardItemLighting();
            }

            i++;
         }

         int sx = 128;
         int cw = 110;
         int sz = this.cardChoices.size();
         int a = 0;

         for (ResearchTableData.CardChoice cardChoice : this.cardChoices) {
            r = new Random(cardChoice.card.getSeed());
            int var7 = mx - (5 + sx - 55 * sz + xx + cw * a);
            int var8 = my - (100 + yy - 60);
            if (this.cardZoomOut[a] >= 0.95 && !this.cardSelected) {
               if (var7 >= 0 && var8 >= 0 && var7 < 100 && var8 < 120) {
                  this.cardHover[a] = this.cardHover[a] + Math.max((0.25F - this.cardHover[a]) / 3.0F * partialTicks, 0.0025F);
               } else {
                  this.cardHover[a] = this.cardHover[a] - 0.1F * partialTicks;
               }
            }

            if (a == sz - 1 || this.cardZoomOut[a + 1] > 0.6) {
               float f = this.cardZoomOut[a];
               this.cardZoomOut[a] = this.cardZoomOut[a] + Math.max((1.0F - this.cardZoomOut[a]) / 5.0F * partialTicks, 0.0025F);
               if (this.cardZoomOut[a] > 0.0F && f == 0.0F) {
                  this.playButtonPageFlip();
               }
            }

            float prevZoomIn = this.cardZoomIn[a];
            if (this.cardSelected) {
               this.cardZoomIn[a] = (float)(
                  this.cardZoomIn[a]
                     + (this.cardActive[a] ? Math.max((double)((1.0F - this.cardZoomIn[a]) / 3.0F * partialTicks), 0.0025) : 0.3F * partialTicks)
               );
               this.cardHover[a] = 1.0F - this.cardZoomIn[a];
            }

            this.cardZoomIn[a] = MathHelper.clamp(this.cardZoomIn[a], 0.0F, 1.0F);
            this.cardHover[a] = MathHelper.clamp(this.cardHover[a], 0.0F, 0.25F);
            this.cardZoomOut[a] = MathHelper.clamp(this.cardZoomOut[a], 0.0F, 1.0F);
            float dx = 55 + sx - 55 * sz + xx + cw * a - (xx + 65);
            float fx = xx + 65 + dx * this.cardZoomOut[a];
            float qx = xx + 191 - fx;
            if (this.cardActive[a]) {
               fx += qx * this.cardZoomIn[a];
            }

            this.drawSheet(
               fx,
               yy + 100,
               6.0F + this.cardZoomOut[a] * 2.0F - this.cardZoomIn[a] * 2.0F + this.cardHover[a],
               r,
               this.cardActive[a] ? 1.0F : 1.0F - this.cardZoomIn[a],
               Math.max(1.0F - this.cardZoomOut[a], this.cardZoomIn[a]),
               cardChoice
            );
            if (this.cardSelected && this.cardActive[a] && this.cardZoomIn[a] >= 1.0F && prevZoomIn < 1.0F) {
               this.playButtonWrite();
               this.cardChoices.clear();
               this.cardSelected = false;
               this.lastDraw = this.table.data.lastDraw;
               break;
            }

            a++;
         }
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      RenderHelper.disableStandardItemLighting();
   }

   private void drawSheet(double x, double y, double scale, Random r, float alpha, float tilt, ResearchTableData.CardChoice cardChoice) {
      GL11.glPushMatrix();
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
      GL11.glTranslated(x + r.nextGaussian(), y + r.nextGaussian(), 0.0);
      GL11.glScaled(scale, scale, 0.0);
      GL11.glRotated(r.nextGaussian() * tilt, 0.0, 0.0, 1.0);
      GL11.glPushMatrix();
      if (cardChoice != null && cardChoice.fromAid) {
         this.mc.renderEngine.bindTexture(this.txPaperGilded);
      } else {
         this.mc.renderEngine.bindTexture(this.txPaper);
      }

      if (r.nextBoolean()) {
         GL11.glRotated(180.0, 0.0, 0.0, 1.0);
      }

      if (r.nextBoolean()) {
         GL11.glRotated(180.0, 0.0, 1.0, 0.0);
      }

      GL11.glDisable(2884);
      UtilsFX.drawTexturedQuadFull(-8.0F, -8.0F, 0.0);
      GL11.glEnable(2884);
      GL11.glPopMatrix();
      if (cardChoice != null && alpha == 1.0F) {
         if (cardChoice.card.getResearchCategory() != null) {
            ResearchCategory rc = ResearchCategories.getResearchCategory(cardChoice.card.getResearchCategory());
            if (rc != null) {
               GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha / 6.0F);
               GL11.glPushMatrix();
               GL11.glScaled(0.5, 0.5, 0.0);
               this.mc.renderEngine.bindTexture(rc.icon);
               UtilsFX.drawTexturedQuadFull(-8.0F, -8.0F, 0.0);
               GL11.glPopMatrix();
            }
         }

         GL11.glPushMatrix();
         GL11.glScaled(0.0625, 0.0625, 0.0);
         GL11.glColor4f(0.0F, 0.0F, 0.0F, alpha);
         String name = TextFormatting.BOLD + cardChoice.card.getLocalizedName() + TextFormatting.RESET;
         int sz = this.fontRenderer.getStringWidth(name);
         this.fontRenderer.drawString(name, -sz / 2, -65, 0);
         this.fontRenderer.drawSplitString(cardChoice.card.getLocalizedText(), -70, -48, 140, 0);
         GL11.glPopMatrix();
         GL11.glPushMatrix();
         this.mc.renderEngine.bindTexture(this.txBase);
         GL11.glScaled(0.0625, 0.0625, 0.0);
         int cc = cardChoice.card.getInspirationCost();
         boolean add = false;
         if (cc < 0) {
            add = true;
            cc = Math.abs(cc) + 1;
         }

         GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);

         for (int a = 0; a < cc; a++) {
            if (a == 0 && add) {
               this.drawTexturedModalRect(-10 * cc + 20 * a, -95, 48, 0, 16, 16);
            } else {
               this.drawTexturedModalRect(-10 * cc + 20 * a, -95, 32, 96, 16, 16);
            }
         }

         GL11.glPopMatrix();
         if (cardChoice.card.getRequiredItems() != null) {
            ItemStack[] items = cardChoice.card.getRequiredItems();
            GL11.glPushMatrix();

            for (int ax = 0; ax < items.length; ax++) {
               if (items[ax] != null && !items[ax].isEmpty()) {
                  GL11.glPushMatrix();
                  GL11.glScaled(0.125, 0.125, 0.0);
                  RenderHelper.enableGUIStandardItemLighting();
                  GlStateManager.disableLighting();
                  GlStateManager.enableRescaleNormal();
                  GlStateManager.enableColorMaterial();
                  GlStateManager.enableLighting();
                  this.itemRender.renderItemAndEffectIntoGUI(items[ax], -9 * items.length + 18 * ax, 35);
                  GlStateManager.disableLighting();
                  GlStateManager.depthMask(true);
                  GlStateManager.enableDepth();
                  GL11.glPopMatrix();

                  try {
                     if (cardChoice.card.getRequiredItemsConsumed()[ax]) {
                        GL11.glPushMatrix();
                        this.mc.renderEngine.bindTexture(this.txBase);
                        GL11.glScaled(0.125, 0.125, 0.0);
                        float s = (float)Math.sin((this.player.ticksExisted + ax * 2 + this.mc.getRenderPartialTicks()) / 2.0F) * 0.03F;
                        GL11.glTranslated(-2 - 9 * items.length + 18 * ax, 45.0F + s * 10.0F, 0.0);
                        GL11.glScaled(0.5, 0.5 + s, 0.0);
                        this.drawTexturedModalRect(0, 0, 64, 120, 16, 16);
                        GL11.glPopMatrix();
                     }
                  } catch (Exception var18) {
                  }
               } else {
                  GL11.glPushMatrix();
                  this.mc.renderEngine.bindTexture(this.txQuestion);
                  GL11.glScaled(0.125, 0.125, 0.0);
                  GL11.glColor4f(0.75F, 0.75F, 0.75F, alpha);
                  GL11.glTranslated(-9 * items.length + 18 * ax, 35.0, 0.0);
                  UtilsFX.drawTexturedQuadFull(0.0F, 0.0F, 0.0);
                  GL11.glPopMatrix();
               }
            }

            GL11.glPopMatrix();
         }
      }

      GlStateManager.disableBlend();
      GL11.glPopMatrix();
   }

   private void drawSheetOverlay(double x, double y, ResearchTableData.CardChoice cardChoice, int mx, int my) {
      GL11.glPushMatrix();
      if (cardChoice != null && cardChoice.card.getRequiredItems() != null) {
         ItemStack[] items = cardChoice.card.getRequiredItems();

         for (int a = 0; a < items.length; a++) {
            if (this.isPointInRegion((int)(x - 9 * items.length + 18 * a), (int)(y + 36.0), 15, 15, mx, my)) {
               if (items[a] != null && !items[a].isEmpty()) {
                  this.renderToolTip(items[a], mx, my);
               } else {
                  this.drawHoveringText(Arrays.asList(I18n.translateToLocal("tc.card.unknown")), mx, my);
               }
            }
         }
      }

      GL11.glPopMatrix();
   }

   private void drawCards() {
      this.cardSelected = false;
      this.cardHover = new float[]{0.0F, 0.0F, 0.0F};
      this.cardZoomOut = new float[]{0.0F, 0.0F, 0.0F};
      this.cardZoomIn = new float[]{0.0F, 0.0F, 0.0F};
      this.cardActive = new boolean[]{true, true, true};
      int draw = 2;
      if (this.table.data.bonusDraws > 0) {
         draw++;
         this.table.data.bonusDraws--;
      }

      this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, draw);
      this.cardChoices.clear();
   }

   public void initGui() {
      super.initGui();
      this.buttonList.add(this.buttonCreate);
      this.buttonCreate.x = this.guiLeft + 128;
      this.buttonCreate.y = this.guiTop + 22;
      this.buttonList.add(this.buttonComplete);
      this.buttonComplete.x = this.guiLeft + 191;
      this.buttonComplete.y = this.guiTop + 96;
      this.buttonList.add(this.buttonScrap);
      this.buttonScrap.x = this.guiLeft + 128;
      this.buttonScrap.y = this.guiTop + 168;
   }

   protected void actionPerformed(GuiButton button) throws IOException {
      if (button.id == 1) {
         this.playButtonClick();
         PacketHandler.INSTANCE.sendToServer(new PacketStartTheoryToServer(this.table.getPos(), this.selectedAids));
      } else if (button.id == 7) {
         this.playButtonClick();
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 7);
         this.tempCatTotals.clear();
         this.lastDraw = null;
      } else if (button.id == 9) {
         this.playButtonClick();
         this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 9);
         this.tempCatTotals.clear();
         this.lastDraw = null;
         this.table.data = null;
         this.cardChoices.clear();
      } else {
         super.actionPerformed(button);
      }
   }

   private void checkButtons() {
      this.buttonComplete.active = false;
      this.buttonComplete.visible = false;
      this.buttonScrap.active = false;
      this.buttonScrap.visible = false;
      if (this.table.data != null) {
         this.buttonCreate.active = false;
         this.buttonCreate.visible = false;
         if (this.table.data.isComplete()) {
            this.buttonComplete.active = true;
            this.buttonComplete.visible = true;
         } else {
            this.buttonScrap.active = true;
            this.buttonScrap.visible = true;
         }
      } else {
         this.buttonCreate.visible = true;
         if (this.table.getStackInSlot(1) != null
            && this.table.getStackInSlot(0) != null
            && this.table.getStackInSlot(0).getItemDamage() != this.table.getStackInSlot(0).getMaxDamage()) {
            this.buttonCreate.active = true;
         } else {
            this.buttonCreate.active = false;
         }
      }
   }

   protected void mouseClicked(int mx, int my, int par3) throws IOException {
      super.mouseClicked(mx, my, par3);
      int xx = (this.width - this.xSize) / 2;
      int yy = (this.height - this.ySize) / 2;
      if (this.table.data == null) {
         if (!this.currentAids.isEmpty()) {
            int side = Math.min(this.currentAids.size(), 6);
            int c = 0;
            int r = 0;

            for (String key : this.currentAids) {
               ITheorycraftAid mutator = TheorycraftManager.aids.get(key);
               if (mutator != null) {
                  int x = 128 + 20 * c - side * 10;
                  int y = 85 + 35 * r;
                  if (this.isPointInRegion(x, y, 16, 16, mx, my)) {
                     if (this.selectedAids.contains(key)) {
                        this.selectedAids.remove(key);
                     } else if (this.selectedAids.size() + 1 < this.dummyInspirationStart) {
                        this.selectedAids.add(key);
                     }
                  }

                  if (++c >= side) {
                     r++;
                     c = 0;
                  }
               }
            }
         }
      } else {
         int sx = 128;
         int cw = 110;
         if (this.cardChoices.size() > 0) {
            int pressed = -1;

            for (int a = 0; a < this.cardChoices.size(); a++) {
               int var7 = mx - (5 + sx - 55 * this.cardChoices.size() + xx + cw * a);
               int var8 = my - (100 + yy - 60);
               if (this.cardZoomOut[a] >= 0.95 && !this.cardSelected && var7 >= 0 && var8 >= 0 && var7 < 100 && var8 < 120) {
                  pressed = a;
                  break;
               }
            }

            if (pressed >= 0 && this.table.getStackInSlot(0) != null && this.table.getStackInSlot(0).getItemDamage() != this.table.getStackInSlot(0).getMaxDamage()) {
               this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 4 + pressed);
            }
         } else {
            int var7 = mx - (25 + xx);
            int var8 = my - (55 + yy);
            if (var7 >= 0 && var8 >= 0 && var7 < 75 && var8 < 90 && this.table.getStackInSlot(1) != null) {
               this.drawCards();
            }
         }
      }
   }

   void checkCards() {
      if (this.table.data.cardChoices.size() > 0 && this.cardChoices.isEmpty()) {
         this.syncFromTableChoices();
      }

      if (!this.cardSelected) {
         for (int a = 0; a < this.cardChoices.size(); a++) {
            try {
               if (this.table.data != null && this.table.data.cardChoices.size() > a && this.table.data.cardChoices.get(a).selected) {
                  for (int q = 0; q < this.cardChoices.size(); q++) {
                     this.cardActive[q] = this.table.data.cardChoices.get(q).selected;
                  }

                  this.cardSelected = true;
                  this.playButtonPageSelect();
                  this.mc.playerController.sendEnchantPacket(this.inventorySlots.windowId, 1);
                  break;
               }
            } catch (Exception var3) {
            }
         }
      }
   }

   private void playButtonPageFlip() {
      this.mc.getRenderViewEntity().playSound(SoundsTC.page, 1.0F, 1.0F);
   }

   private void playButtonPageSelect() {
      this.mc.getRenderViewEntity().playSound(SoundsTC.pageturn, 1.0F, 1.0F);
   }

   private void playButtonClick() {
      this.mc.getRenderViewEntity().playSound(SoundsTC.clack, 0.4F, 1.0F);
   }

   private void playButtonWrite() {
      this.mc.getRenderViewEntity().playSound(SoundsTC.write, 0.3F, 1.0F);
   }
}
