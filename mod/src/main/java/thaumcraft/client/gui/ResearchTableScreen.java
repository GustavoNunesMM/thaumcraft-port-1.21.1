package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.common.blocks.crafting.ResearchTableBlockEntity;
import thaumcraft.common.menu.ResearchTableMenu;
import thaumcraft.common.network.StartTheoryPayload;
import thaumcraft.common.registers.ModSounds;

/** Port of GuiResearchTable: choose research aids, then draw and play cards until the inspiration runs out. */
public class ResearchTableScreen extends AbstractContainerScreen<ResearchTableMenu> {
   private static final ResourceLocation TX_BACKGROUND = tex("textures/gui/gui_research_table.png");
   private static final ResourceLocation TX_BASE = tex("textures/gui/gui_base.png");
   private static final ResourceLocation TX_PAPER = tex("textures/gui/paper.png");
   private static final ResourceLocation TX_PAPER_GILDED = tex("textures/gui/papergilded.png");
   private static final ResourceLocation TX_QUESTION = tex("textures/aspects/_unknown.png");

   private ResearchTableBlockEntity table;
   @Nullable
   private ResearchTableData.CardChoice lastDraw;
   private float[] cardHover = new float[3], cardZoomOut = new float[3], cardZoomIn = new float[3];
   private boolean[] cardActive = {true, true, true};
   private boolean cardSelected;
   private final Map<String, Integer> tempCatTotals = new HashMap<>();
   private long nexCatCheck, nextCheck;
   private int dummyInspirationStart;
   private Set<String> currentAids = new HashSet<>();
   private final Set<String> selectedAids = new HashSet<>();
   private final List<ResearchTableData.CardChoice> cardChoices = new ArrayList<>();

   /** GuiImageButton, drawn centred on (x, y). */
   private record ImageButton(int id, int x, int y, String text, int color) {}

   private ImageButton buttonCreate, buttonComplete, buttonScrap;
   private boolean createActive, createVisible, completeVisible, scrapVisible;

   private static ResourceLocation tex(String p) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, p);
   }

   public ResearchTableScreen(ResearchTableMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.table = menu.table();
      imageWidth = 255;
      imageHeight = 255;
      if (table.data != null) {
         tempCatTotals.putAll(table.data.categoryTotals);
         syncFromTableChoices();
         lastDraw = table.data.lastDraw;
      }
   }

   @Override
   protected void init() {
      super.init();
      buttonCreate = new ImageButton(1, leftPos + 128, topPos + 22, "button.create.theory", 0x88FFAA);
      buttonComplete = new ImageButton(7, leftPos + 191, topPos + 96, "button.complete.theory", 0x88FFAA);
      buttonScrap = new ImageButton(9, leftPos + 128, topPos + 168, "button.scrap.theory", 0xFF2222);
   }

   private void syncFromTableChoices() {
      cardChoices.clear();
      cardChoices.addAll(table.data.cardChoices);
   }

   private void sound(net.minecraft.sounds.SoundEvent s, float vol) {
      if (minecraft.player != null) minecraft.player.playSound(s, vol, 1.0F);
   }

   private void button(int id) {
      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
   }

   @Override
   protected void renderLabels(GuiGraphics g, int mx, int my) {
   }

   // ------------------------------------------------------------------ background

   @Override
   protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
      table = menu.table();
      checkButtons();
      int xx = leftPos, yy = topPos;
      GuiDraw.resetColor();
      GuiDraw.rect(g, TX_BACKGROUND, xx, yy, 0, 0, 255, 255);
      if (table.data == null) {
         if (nextCheck < minecraft.player.tickCount) {
            currentAids = table.checkSurroundingAids();
            dummyInspirationStart = ResearchTableData.getAvailableInspiration(minecraft.player);
            nextCheck = minecraft.player.tickCount + 100;
         }
         g.pose().pushPose();
         g.pose().translate(xx + 128 - dummyInspirationStart * 5, yy + 55, 0);
         g.pose().scale(0.5F, 0.5F, 1);
         for (int a = 0; a < dummyInspirationStart; a++) {
            GuiDraw.rect(g, TX_BASE, 20 * a, 0, dummyInspirationStart - selectedAids.size() <= a ? 48 : 32, 96, 16, 16);
         }
         g.pose().popPose();
         int side = Math.min(currentAids.size(), 6);
         int c = 0, r = 0;
         for (String key : currentAids) {
            ITheorycraftAid aid = TheorycraftManager.AIDS.get(key);
            if (aid == null) continue;
            int x = xx + 128 + 20 * c - side * 10;
            int y = yy + 85 + 35 * r;
            if (selectedAids.contains(key)) GuiDraw.rect(g, TX_BASE, x, y, 0, 96, 16, 16);
            Object o = aid.getAidObject();
            ItemStack s = o instanceof ItemStack is ? is : o instanceof Block b ? new ItemStack(b) : ItemStack.EMPTY;
            if (!s.isEmpty()) g.renderItem(s, x, y);
            if (++c >= side) {
               r++;
               c = 0;
            }
         }
         renderButton(g, buttonCreate, mx, my, createActive, createVisible);
         return;
      }

      checkCards();
      g.pose().pushPose();
      g.pose().translate(xx + 15, yy + 150, 0);
      for (int a = 0; a < table.data.bonusDraws; a++) GuiDraw.rect(g, TX_BASE, a * 2, a, 64, 96, 16, 16);
      g.pose().popPose();
      g.pose().pushPose();
      g.pose().translate(xx + 128 - table.data.inspirationStart * 5, yy + 16, 0);
      g.pose().scale(0.5F, 0.5F, 1);
      for (int a = 0; a < table.data.inspirationStart; a++) GuiDraw.rect(g, TX_BASE, 20 * a, 0, table.data.inspiration <= a ? 48 : 32, 96, 16, 16);
      g.pose().popPose();

      ItemStack paper = table.getItems().getStackInSlot(1);
      int sheets = paper.isEmpty() ? 0 : 1 + paper.getCount() / 4;
      Random rand = new Random(55L);
      if (sheets > 0 && !table.data.isComplete()) {
         for (int a = 0; a < sheets; a++) drawSheet(g, xx + 65, yy + 100, 6.0, rand, 1.0F, 1.0F, null);
         boolean highlight = cardChoices.isEmpty() && mx - (25 + xx) >= 0 && my - (55 + yy) >= 0 && mx - (25 + xx) < 75 && my - (55 + yy) < 90;
         g.pose().pushPose();
         GuiDraw.color(1, 1, 1, highlight ? 1.0F : 0.5F);
         g.pose().translate(xx + 65, yy + 100, 50);
         float sc = highlight ? 1.75F : 1.5F;
         g.pose().scale(sc, sc, 1);
         GuiDraw.full(g, TX_QUESTION, -8, -8);
         GuiDraw.resetColor();
         g.pose().popPose();
      }
      for (Long seed : table.data.savedCards) drawSheet(g, xx + 191, yy + 100, 6.0, new Random(seed), 1.0F, 1.0F, null);
      if (lastDraw != null) drawSheet(g, xx + 191, yy + 100, 6.0, new Random(lastDraw.card.getSeed()), 1.0F, 1.0F, lastDraw);

      // category totals, counting up towards the real values
      if (nexCatCheck < minecraft.player.tickCount) {
         for (String cat : ResearchCategories.CATEGORIES.keySet()) {
            int t0 = table.data.categoryTotals.getOrDefault(cat, 0);
            int t1 = tempCatTotals.getOrDefault(cat, 0);
            if (t0 == 0 && t1 == 0) {
               tempCatTotals.remove(cat);
            } else {
               if (t1 > t0) t1--;
               if (t1 < t0) t1++;
               tempCatTotals.put(cat, t1);
            }
         }
         nexCatCheck = minecraft.player.tickCount + 1;
      }
      Map<String, Integer> sorted = new LinkedHashMap<>();
      tempCatTotals.entrySet().stream().filter(e -> e.getValue() != 0).sorted((a, b) -> b.getValue().compareTo(a.getValue()))
         .forEach(e -> sorted.put(e.getKey(), e.getValue()));
      int i = 0;
      for (Map.Entry<String, Integer> e : sorted.entrySet()) {
         ResearchCategory rc = ResearchCategories.getResearchCategory(e.getKey());
         if (rc == null) continue;
         int ty = yy + 16 + i * 18 + (i > 0 ? 4 : 0);
         g.pose().pushPose();
         g.pose().translate(xx + 253, ty, 0);
         g.pose().scale(0.0625F, 0.0625F, 1);
         GuiDraw.quad(g, rc.icon, 0, 0, 256, 256, 0, 0, 1, 1, 1, 1);
         g.pose().popPose();
         String s = e.getValue() + "%";
         if (i > table.data.penaltyStart) s += " (-" + e.getValue() / 3 + ")";
         int color = table.data.categoriesBlocked.contains(e.getKey()) ? 0x606060 : (i <= table.data.penaltyStart ? 0xE0C0 : 0xFFFFFF);
         g.drawString(font, s, xx + 276, yy + 20 + i * 18 + (i > table.data.penaltyStart ? 4 : 0), color, true);
         if (mx - (xx + 256) >= 0 && my - ty >= 0 && mx - (xx + 256) < 16 && my - ty < 16) {
            GuiDraw.tooltip(g, font, List.of(Component.translatable("tc.research_category." + e.getKey())), mx + 8, my + 8);
         }
         i++;
      }

      // drawn cards: zoom out from the deck, hover, then the played one flies to the right
      int sx = 128, cw = 110, sz = cardChoices.size();
      for (int a = 0; a < sz; a++) {
         ResearchTableData.CardChoice cardChoice = cardChoices.get(a);
         Random r = new Random(cardChoice.card.getSeed());
         int var7 = mx - (5 + sx - 55 * sz + xx + cw * a);
         int var8 = my - (100 + yy - 60);
         if (cardZoomOut[a] >= 0.95 && !cardSelected) {
            if (var7 >= 0 && var8 >= 0 && var7 < 100 && var8 < 120) cardHover[a] += Math.max((0.25F - cardHover[a]) / 3.0F * pt, 0.0025F);
            else cardHover[a] -= 0.1F * pt;
         }
         if (a == sz - 1 || cardZoomOut[a + 1] > 0.6) {
            float f = cardZoomOut[a];
            cardZoomOut[a] += Math.max((1.0F - cardZoomOut[a]) / 5.0F * pt, 0.0025F);
            if (cardZoomOut[a] > 0.0F && f == 0.0F) sound(ModSounds.PAGE.get(), 1.0F);
         }
         float prevZoomIn = cardZoomIn[a];
         if (cardSelected) {
            cardZoomIn[a] += cardActive[a] ? Math.max((1.0F - cardZoomIn[a]) / 3.0F * pt, 0.0025F) : 0.3F * pt;
            cardHover[a] = 1.0F - cardZoomIn[a];
         }
         cardZoomIn[a] = Mth.clamp(cardZoomIn[a], 0.0F, 1.0F);
         cardHover[a] = Mth.clamp(cardHover[a], 0.0F, 0.25F);
         cardZoomOut[a] = Mth.clamp(cardZoomOut[a], 0.0F, 1.0F);
         float dx = 55 + sx - 55 * sz + xx + cw * a - (xx + 65);
         float fx = xx + 65 + dx * cardZoomOut[a];
         float qx = xx + 191 - fx;
         if (cardActive[a]) fx += qx * cardZoomIn[a];
         drawSheet(g, fx, yy + 100, 6.0F + cardZoomOut[a] * 2.0F - cardZoomIn[a] * 2.0F + cardHover[a], r,
            cardActive[a] ? 1.0F : 1.0F - cardZoomIn[a], Math.max(1.0F - cardZoomOut[a], cardZoomIn[a]), cardChoice);
         if (cardSelected && cardActive[a] && cardZoomIn[a] >= 1.0F && prevZoomIn < 1.0F) {
            sound(ModSounds.WRITE.get(), 0.3F);
            cardChoices.clear();
            cardSelected = false;
            lastDraw = table.data.lastDraw;
            break;
         }
      }
      renderButton(g, buttonComplete, mx, my, true, completeVisible);
      renderButton(g, buttonScrap, mx, my, true, scrapVisible);
   }

   private void renderButton(GuiGraphics g, ImageButton b, int mx, int my, boolean active, boolean visible) {
      if (!visible) return;
      boolean hovered = mx >= b.x() - 49 / 2 && my >= b.y() - 11 / 2 && mx < b.x() - 49 / 2 + 49 && my < b.y() - 11 / 2 + 11;
      float cc = hovered ? 1.0F : 0.9F, ac = 1.0F;
      if (!active) {
         cc = 0.5F;
         ac = 0.9F;
      }
      int c = b.color();
      GuiDraw.color(cc * (c >> 16 & 255) / 255F, cc * (c >> 8 & 255) / 255F, cc * (c & 255) / 255F, ac);
      GuiDraw.rect(g, TX_BASE, b.x() - 51 / 2, b.y() - 13 / 2, 37, 66, 51, 13);
      GuiDraw.resetColor();
      g.pose().pushPose();
      g.pose().translate(b.x(), b.y(), 0);
      g.pose().scale(0.5F, 0.5F, 1);
      g.drawCenteredString(font, Component.translatable(b.text()), 0, -4, !active ? 0xA0A0A0 : hovered ? 0xFFFFA0 : 0xFFFFFF);
      g.pose().popPose();
   }

   private boolean buttonHit(ImageButton b, double mx, double my) {
      return mx >= b.x() - 49 / 2.0 && my >= b.y() - 11 / 2.0 && mx < b.x() - 49 / 2.0 + 49 && my < b.y() - 11 / 2.0 + 11;
   }

   private void drawSheet(GuiGraphics g, double x, double y, double scale, Random r, float alpha, float tilt, @Nullable ResearchTableData.CardChoice cardChoice) {
      g.pose().pushPose();
      GuiDraw.color(1, 1, 1, alpha);
      g.pose().translate(x + r.nextGaussian(), y + r.nextGaussian(), 10);
      g.pose().scale((float) scale, (float) scale, 1);
      g.pose().mulPose(Axis.ZP.rotationDegrees((float) (r.nextGaussian() * tilt)));
      g.pose().pushPose();
      if (r.nextBoolean()) g.pose().mulPose(Axis.ZP.rotationDegrees(180));
      if (r.nextBoolean()) g.pose().mulPose(Axis.YP.rotationDegrees(180));
      RenderSystem.disableCull();
      GuiDraw.full(g, cardChoice != null && cardChoice.fromAid ? TX_PAPER_GILDED : TX_PAPER, -8, -8);
      RenderSystem.enableCull();
      g.pose().popPose();
      if (cardChoice != null && alpha == 1.0F) {
         if (cardChoice.card.getResearchCategory() != null) {
            ResearchCategory rc = ResearchCategories.getResearchCategory(cardChoice.card.getResearchCategory());
            if (rc != null) {
               GuiDraw.color(1, 1, 1, alpha / 6.0F);
               g.pose().pushPose();
               g.pose().scale(0.5F, 0.5F, 1);
               GuiDraw.full(g, rc.icon, -8, -8);
               g.pose().popPose();
            }
         }
         GuiDraw.resetColor();
         g.pose().pushPose();
         g.pose().scale(0.0625F, 0.0625F, 1);
         g.pose().translate(0, 0, 1);
         Component name = cardChoice.card.getLocalizedName().copy().withStyle(ChatFormatting.BOLD);
         g.drawString(font, name, -font.width(name) / 2, -65, 0, false);
         int ly = -48;
         for (FormattedCharSequence line : font.split(cardChoice.card.getLocalizedText(), 140)) {
            g.drawString(font, line, -70, ly, 0, false);
            ly += font.lineHeight;
         }
         g.pose().popPose();
         g.pose().pushPose();
         g.pose().scale(0.0625F, 0.0625F, 1);
         int cc = cardChoice.card.getInspirationCost();
         boolean add = cc < 0;
         if (add) cc = Math.abs(cc) + 1;
         for (int a = 0; a < cc; a++) {
            if (a == 0 && add) GuiDraw.rect(g, TX_BASE, -10 * cc + 20 * a, -95, 48, 0, 16, 16);
            else GuiDraw.rect(g, TX_BASE, -10 * cc + 20 * a, -95, 32, 96, 16, 16);
         }
         g.pose().popPose();
         ItemStack[] items = cardChoice.card.getRequiredItems();
         if (items != null) {
            boolean[] consumed = cardChoice.card.getRequiredItemsConsumed();
            for (int ax = 0; ax < items.length; ax++) {
               g.pose().pushPose();
               g.pose().scale(0.125F, 0.125F, 1);
               if (!items[ax].isEmpty()) {
                  g.renderItem(items[ax], -9 * items.length + 18 * ax, 35);
                  if (consumed != null && ax < consumed.length && consumed[ax]) {
                     float s = (float) Math.sin((minecraft.player.tickCount + ax * 2 + minecraft.getTimer().getGameTimeDeltaPartialTick(false)) / 2.0F) * 0.03F;
                     g.pose().translate(-2 - 9 * items.length + 18 * ax, 45.0F + s * 10.0F, 200);
                     g.pose().scale(0.5F, 0.5F + s, 1);
                     GuiDraw.rect(g, TX_BASE, 0, 0, 64, 120, 16, 16);
                  }
               } else {
                  GuiDraw.color(0.75F, 0.75F, 0.75F, alpha);
                  GuiDraw.full(g, TX_QUESTION, -9 * items.length + 18 * ax, 35);
                  GuiDraw.resetColor();
               }
               g.pose().popPose();
            }
         }
      }
      GuiDraw.resetColor();
      g.pose().popPose();
   }

   // ------------------------------------------------------------------ foreground

   @Override
   public void render(GuiGraphics g, int mx, int my, float pt) {
      super.render(g, mx, my, pt);
      int xx = leftPos, yy = topPos;
      if (table.data == null) {
         int side = Math.min(currentAids.size(), 6);
         int c = 0, r = 0;
         for (String key : currentAids) {
            int x = xx + 128 + 20 * c - side * 10;
            int y = yy + 85 + 35 * r;
            if (mx >= x && my >= y && mx < x + 16 && my < y + 16 && !selectedAids.contains(key)) {
               GuiDraw.color(1, 1, 1, 0.2F);
               g.pose().pushPose();
               g.pose().translate(0, 0, 300);
               GuiDraw.rect(g, TX_BASE, x, y, 0, 96, 16, 16);
               g.pose().popPose();
               GuiDraw.resetColor();
            }
            if (++c >= side) {
               r++;
               c = 0;
            }
         }
      } else {
         int sx = 128, cw = 110, sz = cardChoices.size();
         if (!cardSelected) {
            for (int a = 0; a < sz; a++) {
               if (cardZoomOut[a] < 1.0F) continue;
               float dx = 55 + sx - 55 * sz + cw * a - 65;
               float fx = 65.0F + dx * cardZoomOut[a];
               if (cardActive[a]) fx += (191.0F - fx) * cardZoomIn[a];
               drawSheetOverlay(g, fx, 100, cardChoices.get(a), mx, my);
            }
         }
         int qq = 0;
         if (!table.hasInk()) {
            List<Component> t = List.of(Component.translatable("tile.researchtable.noink.0"), Component.translatable("tile.researchtable.noink.1"));
            int w = Math.max(font.width(t.get(0)), font.width(t.get(1))) / 2;
            GuiDraw.tooltip(g, font, t, xx - w + 116 - 12, yy + 60 + qq + 12);
            qq += 40;
         }
         if (table.getItems().getStackInSlot(1).isEmpty()) {
            Component t = Component.translatable("tile.researchtable.nopaper.0");
            GuiDraw.tooltip(g, font, List.of(t), xx - font.width(t) / 2 + 116 - 12, yy + 60 + qq + 12);
         }
      }
      renderTooltip(g, mx, my);
   }

   private void drawSheetOverlay(GuiGraphics g, double x, double y, ResearchTableData.CardChoice choice, int mx, int my) {
      ItemStack[] items = choice.card.getRequiredItems();
      if (items == null) return;
      for (int a = 0; a < items.length; a++) {
         int ix = leftPos + (int) (x - 9 * items.length + 18 * a);
         int iy = topPos + (int) (y + 36);
         if (mx >= ix && my >= iy && mx < ix + 15 && my < iy + 15) {
            if (!items[a].isEmpty()) g.renderTooltip(font, items[a], mx, my);
            else g.renderTooltip(font, Component.translatable("tc.card.unknown"), mx, my);
         }
      }
   }

   // ------------------------------------------------------------------ logic

   private void checkButtons() {
      completeVisible = false;
      scrapVisible = false;
      if (table.data != null) {
         createVisible = false;
         if (table.data.isComplete()) completeVisible = true;
         else scrapVisible = true;
      } else {
         createVisible = true;
         createActive = table.hasInk() && table.hasPaper();
      }
   }

   private void checkCards() {
      if (!table.data.cardChoices.isEmpty() && cardChoices.isEmpty()) syncFromTableChoices();
      if (cardSelected) return;
      for (int a = 0; a < cardChoices.size(); a++) {
         if (table.data.cardChoices.size() > a && table.data.cardChoices.get(a).selected) {
            for (int q = 0; q < cardChoices.size() && q < table.data.cardChoices.size(); q++) cardActive[q] = table.data.cardChoices.get(q).selected;
            cardSelected = true;
            sound(ModSounds.PAGETURN.get(), 1.0F);
            button(1);
            break;
         }
      }
   }

   private void drawCards() {
      cardSelected = false;
      cardHover = new float[3];
      cardZoomOut = new float[3];
      cardZoomIn = new float[3];
      cardActive = new boolean[]{true, true, true};
      int draw = 2;
      if (table.data.bonusDraws > 0) draw++;
      button(draw);
      cardChoices.clear();
   }

   @Override
   public boolean mouseClicked(double mx, double my, int buttonId) {
      int xx = leftPos, yy = topPos;
      if (table.data == null && createVisible && createActive && buttonHit(buttonCreate, mx, my)) {
         sound(ModSounds.CLACK.get(), 0.4F);
         PacketDistributor.sendToServer(new StartTheoryPayload(table.getBlockPos(), List.copyOf(selectedAids)));
         return true;
      }
      if (table.data != null && completeVisible && buttonHit(buttonComplete, mx, my)) {
         sound(ModSounds.CLACK.get(), 0.4F);
         button(7);
         tempCatTotals.clear();
         lastDraw = null;
         return true;
      }
      if (table.data != null && scrapVisible && buttonHit(buttonScrap, mx, my)) {
         sound(ModSounds.CLACK.get(), 0.4F);
         button(9);
         tempCatTotals.clear();
         lastDraw = null;
         cardChoices.clear();
         return true;
      }
      if (table.data == null) {
         int side = Math.min(currentAids.size(), 6);
         int c = 0, r = 0;
         for (String key : currentAids) {
            int x = xx + 128 + 20 * c - side * 10;
            int y = yy + 85 + 35 * r;
            if (mx >= x && my >= y && mx < x + 16 && my < y + 16) {
               if (selectedAids.contains(key)) selectedAids.remove(key);
               else if (selectedAids.size() + 1 < dummyInspirationStart) selectedAids.add(key);
               return true;
            }
            if (++c >= side) {
               r++;
               c = 0;
            }
         }
      } else {
         int sx = 128, cw = 110;
         if (!cardChoices.isEmpty()) {
            for (int a = 0; a < cardChoices.size(); a++) {
               double var7 = mx - (5 + sx - 55 * cardChoices.size() + xx + cw * a);
               double var8 = my - (100 + yy - 60);
               if (cardZoomOut[a] >= 0.95 && !cardSelected && var7 >= 0 && var8 >= 0 && var7 < 100 && var8 < 120) {
                  if (table.hasInk()) button(4 + a);
                  return true;
               }
            }
         } else if (mx - (25 + xx) >= 0 && my - (55 + yy) >= 0 && mx - (25 + xx) < 75 && my - (55 + yy) < 90 && table.hasPaper()) {
            drawCards();
            return true;
         }
      }
      return super.mouseClicked(mx, my, buttonId);
   }
}
