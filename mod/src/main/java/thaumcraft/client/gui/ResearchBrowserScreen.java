package thaumcraft.client.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.EnumResearchFlag;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.common.network.ClearResearchFlagsPayload;
import thaumcraft.common.network.CompleteStagePayload;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchManager;

/** Port of GuiResearchBrowser: the Thaumonomicon's pannable, zoomable research map with category tabs and search. */
public class ResearchBrowserScreen extends Screen {
   static final ResourceLocation TX1 = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_research_browser.png");
   private static final String[] TC_CATEGORIES = {"BASICS", "ALCHEMY", "AUROMANCY", "ARTIFICE", "INFUSION", "GOLEMANCY", "ELDRITCH"};

   private static int guiBoundsLeft, guiBoundsTop, guiBoundsRight, guiBoundsBottom;
   public static double lastX = -9999.0;
   public static double lastY = -9999.0;
   static String selectedCategory;
   private static boolean searching;
   private static int catScrollPos;
   private static int catScrollMax;

   private float screenZoom = 1.0F;
   private double curMouseX, curMouseY, guiMapX, guiMapY, tempMapX, tempMapY;
   private int screenX, screenY;
   private final int startX = 16, startY = 16;
   private final List<ResearchEntry> research = new ArrayList<>();
   @Nullable
   private ResearchEntry currentHighlight;
   private final Player player;
   private long popupTime;
   private Component popupMessage = Component.empty();
   private EditBox searchField;
   private final List<String> categoriesTC = new ArrayList<>();
   private final List<String> categoriesOther = new ArrayList<>();
   private int addonShift;
   private final Set<String> invisible = new HashSet<>();
   private final List<SearchResult> searchResults = new ArrayList<>();
   private boolean dragging;

   private record SearchResult(Component name, String key, @Nullable ResourceLocation recipe, boolean cat) {}

   /** A category tab on the left (Thaumcraft's) or right (addons'). */
   private record CategoryButton(ResearchCategory rc, String key, boolean flip, int x, int y, int completion) {}

   private final List<CategoryButton> categoryButtons = new ArrayList<>();
   private boolean scrollButtons;

   public ResearchBrowserScreen() {
      this(lastX, lastY);
   }

   public ResearchBrowserScreen(double x, double y) {
      super(Component.translatable("item.thaumcraft.thaumonomicon"));
      curMouseX = guiMapX = tempMapX = x;
      curMouseY = guiMapY = tempMapY = y;
      player = Minecraft.getInstance().player;
   }

   private PlayerKnowledge knowledge() {
      return ResearchManager.knowledge(player);
   }

   @Override
   protected void init() {
      searchField = new EditBox(font, 20, 20, 89, font.lineHeight, Component.translatable("tc.search"));
      searchField.setMaxLength(15);
      searchField.setBordered(true);
      searchField.setTextColor(0xFFFFFF);
      searchField.setResponder(s -> updateSearch());
      searchField.setVisible(searching);
      if (searching) {
         searchField.setFocused(true);
         setFocused(searchField);
      }
      addWidget(searchField);
      updateResearch();
      if (lastX == -9999.0 || guiMapX > guiBoundsRight || guiMapX < guiBoundsLeft) guiMapX = tempMapX = (guiBoundsLeft + guiBoundsRight) / 2.0;
      if (lastY == -9999.0 || guiMapY > guiBoundsBottom || guiMapY < guiBoundsTop) guiMapY = tempMapY = (guiBoundsBottom + guiBoundsTop) / 2.0;
      if (searching) updateSearch();
   }

   public void updateResearch() {
      screenX = width - 32;
      screenY = height - 32;
      research.clear();
      invisible.clear();
      if (selectedCategory == null) selectedCategory = ResearchCategories.CATEGORIES.keySet().iterator().next();
      int limit = (int) Math.floor((screenY - 28) / 24.0F);
      addonShift = 0;
      int count = 0;
      categoriesTC.clear();
      categoriesOther.clear();
      categoryButtons.clear();
      for (ResearchCategory rc : ResearchCategories.CATEGORIES.values()) {
         int rt = 0, rco = 0;
         for (ResearchEntry res : rc.research.values()) {
            if (!res.hasMeta(ResearchEntry.Meta.AUTOUNLOCK)) {
               rt++;
               if (ResearchManager.knowsResearch(player, res.getKey())) rco++;
            }
         }
         int v = rt == 0 ? 0 : (int) ((float) rco / rt * 100.0F);
         if (rc.researchKey != null && !ResearchManager.knowsResearchStrict(player, rc.researchKey)) continue;
         if (List.of(TC_CATEGORIES).contains(rc.key)) {
            categoriesTC.add(rc.key);
            categoryButtons.add(new CategoryButton(rc, rc.key, false, 1, 10 + categoriesTC.size() * 24, v));
         } else {
            count++;
            if (count <= limit + catScrollPos && count - 1 >= catScrollPos) {
               categoriesOther.add(rc.key);
               categoryButtons.add(new CategoryButton(rc, rc.key, true, width - 17, 10 + categoriesOther.size() * 24, v));
            }
         }
      }
      scrollButtons = count > limit || count < catScrollPos;
      if (scrollButtons) addonShift = (screenY - 28) % 24 / 2;
      catScrollMax = count - limit;

      if (selectedCategory != null && !selectedCategory.isEmpty() && ResearchCategories.getResearchCategory(selectedCategory) != null) {
         research.addAll(ResearchCategories.getResearchCategory(selectedCategory).research.values());
         guiBoundsLeft = 99999;
         guiBoundsTop = 99999;
         guiBoundsRight = -99999;
         guiBoundsBottom = -99999;
         for (ResearchEntry res : research) {
            if (!isVisible(res)) continue;
            guiBoundsLeft = Math.min(guiBoundsLeft, res.getDisplayColumn() * 24 - screenX + 48);
            guiBoundsRight = Math.max(guiBoundsRight, res.getDisplayColumn() * 24 - 24);
            guiBoundsTop = Math.min(guiBoundsTop, res.getDisplayRow() * 24 - screenY + 48);
            guiBoundsBottom = Math.max(guiBoundsBottom, res.getDisplayRow() * 24 - 24);
         }
         if (guiBoundsLeft > guiBoundsRight) guiBoundsLeft = guiBoundsRight = guiBoundsTop = guiBoundsBottom = 0;
      }
   }

   private boolean isVisible(ResearchEntry res) {
      if (ResearchManager.knowsResearch(player, res.getKey())) return true;
      if (invisible.contains(res.getKey()) || (res.hasMeta(ResearchEntry.Meta.HIDDEN) && !canUnlockResearch(res))) return false;
      if (res.getParents().isEmpty() && res.hasMeta(ResearchEntry.Meta.HIDDEN)) return false;
      for (String r : res.getParentsClean()) {
         ResearchEntry ri = ResearchCategories.getResearch(r);
         if (ri != null && !isVisible(ri)) {
            invisible.add(r);
            return false;
         }
      }
      return true;
   }

   private boolean canUnlockResearch(ResearchEntry res) {
      return ResearchManager.doesPlayerHaveRequisites(player, res.getKey());
   }

   @Override
   public void removed() {
      lastX = guiMapX;
      lastY = guiMapY;
      super.removed();
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   // ------------------------------------------------------------------ search

   private void updateSearch() {
      searchResults.clear();
      invisible.clear();
      String s1 = searchField.getValue().toLowerCase();
      for (String cat : categoriesTC) {
         if (cat.toLowerCase().contains(s1)) searchResults.add(new SearchResult(Component.translatable("tc.research_category." + cat), cat, null, true));
      }
      for (String cat : categoriesOther) {
         if (cat.toLowerCase().contains(s1)) searchResults.add(new SearchResult(Component.translatable("tc.research_category." + cat), cat, null, true));
      }
      Set<ResourceLocation> dupCheck = new HashSet<>();
      for (String pre : knowledge().getResearchList()) {
         ResearchEntry ri = ResearchCategories.getResearch(pre);
         if (ri == null) continue;
         if (ri.getLocalizedName().getString().toLowerCase().contains(s1)) searchResults.add(new SearchResult(ri.getLocalizedName(), pre, null, false));
         int stage = knowledge().getResearchStage(pre);
         if (!ri.getStages().isEmpty()) {
            int s = Math.min(ri.getStages().size() - 1, stage + 1);
            ResearchStage page = ri.getStages().get(Math.max(0, s));
            for (ResourceLocation rec : page.recipes()) {
               if (!dupCheck.add(rec)) continue;
               ItemStack out = RecipeCatalog.output(rec);
               if (!out.isEmpty() && out.getHoverName().getString().toLowerCase().contains(s1)) {
                  searchResults.add(new SearchResult(out.getHoverName(), pre, rec, false));
               }
            }
         }
      }
      searchResults.sort((a, b) -> a.name().getString().compareTo(b.name().getString()));
   }

   // ------------------------------------------------------------------ input

   @Override
   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      if (!searching && button == 0 && (dragging || mx >= startX && mx < startX + screenX && my >= startY && my < startY + screenY)) {
         dragging = true;
         guiMapX -= dx * screenZoom;
         guiMapY -= dy * screenZoom;
         tempMapX = curMouseX = guiMapX;
         tempMapY = curMouseY = guiMapY;
         clampTemp();
         return true;
      }
      return super.mouseDragged(mx, my, button, dx, dy);
   }

   @Override
   public boolean mouseReleased(double mx, double my, int button) {
      dragging = false;
      return super.mouseReleased(mx, my, button);
   }

   private void clampTemp() {
      tempMapX = Mth.clamp(tempMapX, guiBoundsLeft * screenZoom, guiBoundsRight * screenZoom - 1.0F);
      tempMapY = Mth.clamp(tempMapY, guiBoundsTop * screenZoom, guiBoundsBottom * screenZoom - 1.0F);
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double sx, double sy) {
      if (!searching) {
         if (sy < 0) screenZoom += 0.25F;
         else if (sy > 0) screenZoom -= 0.25F;
         screenZoom = Mth.clamp(screenZoom, 1.0F, 2.0F);
         return true;
      }
      return super.mouseScrolled(mx, my, sx, sy);
   }

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (!searching && minecraft.options.keyInventory.matches(key, scan)) {
         onClose();
         return true;
      }
      return super.keyPressed(key, scan, mods);
   }

   private void click() {
      minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      popupTime = System.currentTimeMillis() - 1;
      // search button
      if (mx >= 1 && mx < 17 && my >= height - 17 && my < height - 1) {
         click();
         selectedCategory = "";
         searching = true;
         searchField.setVisible(true);
         searchField.setValue("");
         searchField.setFocused(true);
         setFocused(searchField);
         updateSearch();
         return true;
      }
      if (scrollButtons) {
         if (mx >= width - 14 && mx < width - 4 && my >= 20 && my < 31 && catScrollPos > 0) {
            catScrollPos--;
            updateResearch();
            return true;
         }
         if (mx >= width - 14 && mx < width - 4 && my >= screenY + 1 && my < screenY + 12 && catScrollPos < catScrollMax) {
            catScrollPos++;
            updateResearch();
            return true;
         }
      }
      for (CategoryButton b : categoryButtons) {
         if (mx >= b.x() && mx < b.x() + 16 && my >= b.y() + addonShift && my < b.y() + 16 + addonShift) {
            if (!b.key().equals(selectedCategory)) {
               click();
               searching = false;
               searchField.setVisible(false);
               searchField.setFocused(false);
               selectedCategory = b.key();
               updateResearch();
               guiMapX = tempMapX = (guiBoundsLeft + guiBoundsRight) / 2.0;
               guiMapY = tempMapY = (guiBoundsBottom + guiBoundsTop) / 2.0;
            }
            return true;
         }
      }
      if (!searching && currentHighlight != null && !ResearchManager.knowsResearch(player, currentHighlight.getKey()) && canUnlockResearch(currentHighlight)) {
         updateResearch();
         PacketDistributor.sendToServer(new CompleteStagePayload(currentHighlight.getKey(), true, false, true));
         minecraft.setScreen(new ResearchPageScreen(currentHighlight, null, guiMapX, guiMapY));
         popupTime = System.currentTimeMillis() + 3000L;
         popupMessage = Component.translatable("tc.research.popup", currentHighlight.getLocalizedName());
         return true;
      } else if (!searching && currentHighlight != null && ResearchManager.knowsResearch(player, currentHighlight.getKey())) {
         knowledge().clearResearchFlag(currentHighlight.getKey(), EnumResearchFlag.RESEARCH);
         knowledge().clearResearchFlag(currentHighlight.getKey(), EnumResearchFlag.PAGE);
         PacketDistributor.sendToServer(new ClearResearchFlagsPayload(currentHighlight.getKey(), false));
         int stage = knowledge().getResearchStage(currentHighlight.getKey());
         if (stage > 1 && stage >= currentHighlight.getStages().size()) {
            PacketDistributor.sendToServer(new CompleteStagePayload(currentHighlight.getKey(), false, true, false));
         }
         minecraft.setScreen(new ResearchPageScreen(currentHighlight, null, guiMapX, guiMapY));
         return true;
      } else if (searching) {
         int q = 0;
         for (SearchResult sr : searchResults) {
            if (mx > 22 && mx < 18 + screenX && my >= 32 + q * 10 && my < 40 + q * 10) {
               if (!sr.cat() && ResearchManager.knowsResearch(player, sr.key())) {
                  minecraft.setScreen(new ResearchPageScreen(ResearchCategories.getResearch(sr.key()), sr.recipe(), guiMapX, guiMapY));
                  return true;
               }
               if (categoriesTC.contains(sr.key()) || categoriesOther.contains(sr.key())) {
                  searching = false;
                  searchField.setVisible(false);
                  searchField.setFocused(false);
                  selectedCategory = sr.key();
                  updateResearch();
                  guiMapX = tempMapX = (guiBoundsLeft + guiBoundsRight) / 2.0;
                  guiMapY = tempMapY = (guiBoundsBottom + guiBoundsTop) / 2.0;
                  return true;
               }
            }
            if (32 + (++q + 1) * 10 > screenY) break;
         }
      }
      return super.mouseClicked(mx, my, button);
   }

   @Override
   public void tick() {
      curMouseX = guiMapX;
      curMouseY = guiMapY;
      double dx = tempMapX - guiMapX;
      double dy = tempMapY - guiMapY;
      if (dx * dx + dy * dy < 4.0) {
         guiMapX += dx;
         guiMapY += dy;
      } else {
         guiMapX += dx * 0.85;
         guiMapY += dy * 0.85;
      }
   }

   // ------------------------------------------------------------------ rendering

   @Override
   public void render(GuiGraphics g, int mx, int my, float pt) {
      renderTransparentBackground(g);
      int locX = Mth.floor(curMouseX + (guiMapX - curMouseX) * pt);
      int locY = Mth.floor(curMouseY + (guiMapY - curMouseY) * pt);
      locX = (int) Mth.clamp(locX, guiBoundsLeft * screenZoom, guiBoundsRight * screenZoom - 1.0F);
      locY = (int) Mth.clamp(locY, guiBoundsTop * screenZoom, guiBoundsBottom * screenZoom - 1.0F);

      if (!searching && ResearchCategories.getResearchCategory(selectedCategory) != null) {
         g.pose().pushPose();
         g.pose().scale(1.0F / screenZoom, 1.0F / screenZoom, 1.0F);
         renderMap(g, mx, my, locX, locY);
         g.pose().popPose();
      } else {
         currentHighlight = null;
         searchField.render(g, mx, my, pt);
         int q = 0;
         for (SearchResult sr : searchResults) {
            int color = sr.cat() ? 0xDDAAAA : (sr.recipe() == null ? 0xDDDDDD : 0xAAAADD);
            if (sr.recipe() != null) {
               g.pose().pushPose();
               g.pose().scale(0.5F, 0.5F, 1.0F);
               GuiDraw.rect(g, TX1, 44, (32 + q * 10) * 2, 224, 48, 16, 16);
               g.pose().popPose();
            }
            if (mx > 22 && mx < 18 + screenX && my >= 32 + q * 10 && my < 40 + q * 10) {
               color = sr.recipe() == null ? 0xFFFFFF : (sr.cat() ? 0xFFCCCC : 0xCCCCFF);
            }
            g.drawString(font, sr.name(), 32, 32 + q * 10, color);
            if (32 + (++q + 1) * 10 > screenY) {
               g.drawString(font, Component.translatable("tc.search.more"), 22, 34 + q * 10, 0xAAAAAA);
               break;
            }
         }
      }

      renderFrame(g, mx, my);
      if (popupTime > System.currentTimeMillis()) GuiDraw.tooltip(g, font, List.of(popupMessage), 10, 34);
   }

   private void renderMap(GuiGraphics g, int mx, int my, int locX, int locY) {
      ResearchCategory cat = ResearchCategories.getResearchCategory(selectedCategory);
      GuiDraw.resetColor();
      float bx = (startX - 2) * screenZoom, by = (startY - 2) * screenZoom;
      float bgw = (screenX + 4) * screenZoom, bgh = (screenY + 4) * screenZoom;
      GuiDraw.quad(g, cat.background, bx, by, bgw, bgh, (float) (locX / 2.0), (float) (locY / 2.0), bgw, bgh, 256, 256);
      if (cat.background2 != null) {
         GuiDraw.quad(g, cat.background2, bx, by, bgw, bgh, (float) (locX / 1.5), (float) (locY / 1.5), bgw, bgh, 256, 256);
      }

      // lines to parents and siblings
      for (ResearchEntry source : research) {
         List<String> parents = source.getParents();
         List<String> clean = source.getParentsClean();
         for (int a = 0; a < parents.size(); a++) {
            ResearchEntry parent = ResearchCategories.getResearch(clean.get(a));
            if (parent == null || !parent.getCategory().equals(selectedCategory) || parent.getSiblings().contains(source.getKey())) continue;
            boolean knowsParent = ResearchManager.knowsResearchStrict(player, source.getParentsStripped().get(a));
            if (!isVisible(source) || parents.get(a).startsWith("~")) continue;
            if (knowsParent) {
               drawLine(g, source.getDisplayColumn(), source.getDisplayRow(), parent.getDisplayColumn(), parent.getDisplayRow(), 0.6F, 0.6F, 0.6F,
                  locX, locY, true, source.hasMeta(ResearchEntry.Meta.REVERSE));
            } else if (isVisible(parent)) {
               drawLine(g, source.getDisplayColumn(), source.getDisplayRow(), parent.getDisplayColumn(), parent.getDisplayRow(), 0.2F, 0.2F, 0.2F,
                  locX, locY, true, source.hasMeta(ResearchEntry.Meta.REVERSE));
            }
         }
         for (String sib : source.getSiblings()) {
            ResearchEntry sibling = ResearchCategories.getResearch(sib.startsWith("~") ? sib.substring(1) : sib);
            if (sibling == null || !sibling.getCategory().equals(selectedCategory) || !isVisible(source) || sib.startsWith("~")) continue;
            if (ResearchManager.knowsResearchStrict(player, sibling.getKey())) {
               drawLine(g, sibling.getDisplayColumn(), sibling.getDisplayRow(), source.getDisplayColumn(), source.getDisplayRow(), 0.3F, 0.3F, 0.4F,
                  locX, locY, false, source.hasMeta(ResearchEntry.Meta.REVERSE));
            } else if (isVisible(sibling)) {
               drawLine(g, sibling.getDisplayColumn(), sibling.getDisplayRow(), source.getDisplayColumn(), source.getDisplayRow(), 0.1875F, 0.1875F, 0.25F,
                  locX, locY, false, source.hasMeta(ResearchEntry.Meta.REVERSE));
            }
         }
      }

      // icons
      currentHighlight = null;
      for (ResearchEntry icon : research) {
         int dx = icon.getDisplayColumn() * 24 - locX;
         int dy = icon.getDisplayRow() * 24 - locY;
         if (dx < -24 || dy < -24 || dx > screenX * screenZoom || dy > screenY * screenZoom || !isVisible(icon)) continue;
         int iconX = startX + dx;
         int iconY = startY + dy;
         boolean hasWarp = icon.getStages().stream().anyMatch(s -> s.warp() > 0);
         if (hasWarp) drawForbidden(g, iconX + 8, iconY + 8);
         if (knowledge().isResearchComplete(icon.getKey())) {
            GuiDraw.color(1, 1, 1, 1);
         } else if (canUnlockResearch(icon)) {
            float c = (float) Math.sin(System.currentTimeMillis() % 600L / 600.0 * Math.PI * 2.0) * 0.25F + 0.75F;
            GuiDraw.color(c, c, c, 1);
         } else {
            GuiDraw.color(0.3F, 0.3F, 0.3F, 1);
         }
         boolean hidden = icon.hasMeta(ResearchEntry.Meta.HIDDEN);
         if (icon.hasMeta(ResearchEntry.Meta.ROUND)) {
            GuiDraw.rect(g, TX1, iconX - 8, iconY - 8, 144, 48 + (hidden ? 32 : 0), 32, 32);
         } else {
            GuiDraw.rect(g, TX1, iconX - 8, iconY - 8, 80 + (icon.hasMeta(ResearchEntry.Meta.HEX) ? 32 : 0), 48 + (hidden ? 32 : 0), 32, 32);
         }
         if (icon.hasMeta(ResearchEntry.Meta.SPIKY)) GuiDraw.rect(g, TX1, iconX - 8, iconY - 8, 176, 48 + (hidden ? 32 : 0), 32, 32);
         boolean bw = !canUnlockResearch(icon);
         GuiDraw.resetColor();
         if (knowledge().hasResearchFlag(icon.getKey(), EnumResearchFlag.RESEARCH)) {
            g.pose().pushPose();
            g.pose().translate(iconX - 9, iconY - 9, 0);
            g.pose().scale(0.5F, 0.5F, 1);
            GuiDraw.rect(g, TX1, 0, 0, 176, 16, 32, 32);
            g.pose().popPose();
         }
         if (knowledge().hasResearchFlag(icon.getKey(), EnumResearchFlag.PAGE)) {
            g.pose().pushPose();
            g.pose().translate(iconX - 9, iconY + 9, 0);
            g.pose().scale(0.5F, 0.5F, 1);
            GuiDraw.rect(g, TX1, 0, 0, 208, 16, 32, 32);
            g.pose().popPose();
         }
         drawResearchIcon(g, icon, iconX, iconY, bw);
         if (mx >= startX && my >= startY && mx < startX + screenX && my < startY + screenY
            && mx >= (iconX - 2) / screenZoom && mx <= (iconX + 18) / screenZoom && my >= (iconY - 2) / screenZoom && my <= (iconY + 18) / screenZoom) {
            currentHighlight = icon;
         }
         GuiDraw.resetColor();
      }
   }

   /** drawResearchIcon: cycles through the entry's icons once a second. */
   public static void drawResearchIcon(GuiGraphics g, ResearchEntry entry, int x, int y, boolean bw) {
      List<ResearchEntry.Icon> icons = entry.getIcons();
      if (icons.isEmpty()) return;
      ResearchEntry.Icon icon = icons.get((int) (System.currentTimeMillis() / 1000L % icons.size()));
      if (icon.texture() != null) {
         if (bw) GuiDraw.color(0.2F, 0.2F, 0.2F, 1);
         GuiDraw.icon(g, icon.texture(), x, y);
         GuiDraw.resetColor();
      } else if (icon.stack() != null) {
         g.renderItem(icon.stack(), x, y);
         if (bw) g.fill(x, y, x + 16, y + 16, 0xA0000000);
      } else if (icon.focus() != null) {
         FocusIcons.draw(g, icon.focus().replace("focus:", "").trim(), x + 8, y + 8, bw);
      }
   }

   private void drawForbidden(GuiGraphics g, float x, float y) {
      int count = player.tickCount;
      ResourceLocation nodes = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/auranodes.png");
      int frame = 160 + count % 32;
      float size = 90.0F / 4;
      GuiDraw.color(0.33F, 0.0F, 0.44F, 220 / 255.0F);
      com.mojang.blaze3d.systems.RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
         com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
      GuiDraw.quad(g, nodes, x - size, y - size, size * 2, size * 2, frame % 32, frame / 32, 1, 1, 32, 32);
      com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
      GuiDraw.resetColor();
   }

   private void drawLine(GuiGraphics g, int x, int y, int x2, int y2, float r, float gr, float b, int locX, int locY, boolean arrow, boolean flipped) {
      boolean bigCorner;
      int xd, yd, xm, ym, xx, yy;
      if (flipped) {
         xd = Math.abs(x2 - x);
         yd = Math.abs(y2 - y);
         xm = xd == 0 ? 0 : (x2 - x > 0 ? -1 : 1);
         ym = yd == 0 ? 0 : (y2 - y > 0 ? -1 : 1);
         xx = x2 * 24 - 4 - locX + startX;
         yy = y2 * 24 - 4 - locY + startY;
      } else {
         xd = Math.abs(x - x2);
         yd = Math.abs(y - y2);
         xm = xd == 0 ? 0 : (x - x2 > 0 ? -1 : 1);
         ym = yd == 0 ? 0 : (y - y2 > 0 ? -1 : 1);
         xx = x * 24 - 4 - locX + startX;
         yy = y * 24 - 4 - locY + startY;
      }
      bigCorner = xd > 1 && yd > 1;
      GuiDraw.color(r, gr, b, 1);
      if (arrow) {
         if (flipped) {
            int xx3 = x * 24 - 8 - locX + startX;
            int yy3 = y * 24 - 8 - locY + startY;
            if (xm < 0) GuiDraw.rect(g, TX1, xx3, yy3, 160, 112, 32, 32);
            else if (xm > 0) GuiDraw.rect(g, TX1, xx3, yy3, 128, 112, 32, 32);
            else if (ym > 0) GuiDraw.rect(g, TX1, xx3, yy3, 64, 112, 32, 32);
            else if (ym < 0) GuiDraw.rect(g, TX1, xx3, yy3, 96, 112, 32, 32);
         } else if (ym < 0) {
            GuiDraw.rect(g, TX1, xx - 4, yy - 4, 64, 112, 32, 32);
         } else if (ym > 0) {
            GuiDraw.rect(g, TX1, xx - 4, yy - 4, 96, 112, 32, 32);
         } else if (xm > 0) {
            GuiDraw.rect(g, TX1, xx - 4, yy - 4, 160, 112, 32, 32);
         } else if (xm < 0) {
            GuiDraw.rect(g, TX1, xx - 4, yy - 4, 128, 112, 32, 32);
         }
      }
      int v = 1;
      int h = 0;
      for (; v < yd - (bigCorner ? 1 : 0); v++) GuiDraw.rect(g, TX1, xx + xm * 24 * h, yy + ym * 24 * v, 0, 228, 24, 24);
      if (bigCorner) {
         if (xm < 0 && ym > 0) GuiDraw.rect(g, TX1, xx + xm * 24 * h - 24, yy + ym * 24 * v, 0, 180, 48, 48);
         if (xm > 0 && ym > 0) GuiDraw.rect(g, TX1, xx + xm * 24 * h, yy + ym * 24 * v, 48, 180, 48, 48);
         if (xm < 0 && ym < 0) GuiDraw.rect(g, TX1, xx + xm * 24 * h - 24, yy + ym * 24 * v - 24, 96, 180, 48, 48);
         if (xm > 0 && ym < 0) GuiDraw.rect(g, TX1, xx + xm * 24 * h, yy + ym * 24 * v - 24, 144, 180, 48, 48);
      } else {
         if (xm < 0 && ym > 0) GuiDraw.rect(g, TX1, xx + xm * 24 * h, yy + ym * 24 * v, 48, 228, 24, 24);
         if (xm > 0 && ym > 0) GuiDraw.rect(g, TX1, xx + xm * 24 * h, yy + ym * 24 * v, 72, 228, 24, 24);
         if (xm < 0 && ym < 0) GuiDraw.rect(g, TX1, xx + xm * 24 * h, yy + ym * 24 * v, 96, 228, 24, 24);
         if (xm > 0 && ym < 0) GuiDraw.rect(g, TX1, xx + xm * 24 * h, yy + ym * 24 * v, 120, 228, 24, 24);
      }
      v += bigCorner ? 1 : 0;
      for (int i = h + (bigCorner ? 2 : 1); i < xd; i++) GuiDraw.rect(g, TX1, xx + xm * 24 * i, yy + ym * 24 * v, 24, 228, 24, 24);
      GuiDraw.resetColor();
   }

   private void renderFrame(GuiGraphics g, int mx, int my) {
      GuiDraw.resetColor();
      for (int c = 16; c < width - 16; c += 64) {
         int p = Math.min(64, width - 16 - c);
         if (p > 0) {
            GuiDraw.rect(g, TX1, c, -2, 48, 13, p, 22);
            GuiDraw.rect(g, TX1, c, height - 20, 48, 13, p, 22);
         }
      }
      for (int c = 16; c < height - 16; c += 64) {
         int p = Math.min(64, height - 16 - c);
         if (p > 0) {
            GuiDraw.rect(g, TX1, -2, c, 13, 48, 22, p);
            GuiDraw.rect(g, TX1, width - 20, c, 13, 48, 22, p);
         }
      }
      GuiDraw.rect(g, TX1, -2, -2, 13, 13, 22, 22);
      GuiDraw.rect(g, TX1, -2, height - 20, 13, 13, 22, 22);
      GuiDraw.rect(g, TX1, width - 20, -2, 13, 13, 22, 22);
      GuiDraw.rect(g, TX1, width - 20, height - 20, 13, 13, 22, 22);

      // search button
      boolean hs = mx >= 1 && mx < 17 && my >= height - 17 && my < height - 1;
      GuiDraw.color(hs ? 1 : 0.8F, hs ? 1 : 0.8F, hs ? 1 : 0.8F, 1);
      GuiDraw.rect(g, TX1, 1, height - 17, 160, 16, 16, 16);
      GuiDraw.resetColor();
      if (hs) g.drawString(font, Component.translatable("tc.search"), 20, height - 13, 0xFFFFFF);

      if (scrollButtons) {
         boolean up = mx >= width - 14 && mx < width - 4 && my >= 20 && my < 31;
         GuiDraw.color(up ? 1 : 0.7F, up ? 1 : 0.7F, up ? 1 : 0.7F, 1);
         GuiDraw.rect(g, TX1, width - 14, 20, 51, 55, 10, 11);
         boolean down = mx >= width - 14 && mx < width - 4 && my >= screenY + 1 && my < screenY + 12;
         GuiDraw.color(down ? 1 : 0.7F, down ? 1 : 0.7F, down ? 1 : 0.7F, 1);
         GuiDraw.rect(g, TX1, width - 14, screenY + 1, 51, 71, 10, 11);
         GuiDraw.resetColor();
      }

      for (CategoryButton b : categoryButtons) renderCategoryButton(g, b, mx, my);

      if (currentHighlight != null) {
         List<Component> text = new ArrayList<>();
         text.add(currentHighlight.getLocalizedName().copy().withStyle(ChatFormatting.GOLD));
         if (canUnlockResearch(currentHighlight)) {
            if (!knowledge().isResearchComplete(currentHighlight.getKey()) && !currentHighlight.getStages().isEmpty()) {
               int stage = knowledge().getResearchStage(currentHighlight.getKey());
               if (stage > 0) {
                  text.add(Component.literal("@@").append(Component.translatable("tc.research.stage")).append(" " + stage + "/" + currentHighlight.getStages().size())
                     .withStyle(ChatFormatting.AQUA));
               } else {
                  text.add(Component.literal("@@").append(Component.translatable("tc.research.begin")).withStyle(ChatFormatting.GREEN));
               }
            }
         } else {
            text.add(Component.literal("@@").append(Component.translatable("tc.researchmissing")).withStyle(ChatFormatting.RED));
            List<String> stripped = currentHighlight.getParentsStripped();
            List<String> clean = currentHighlight.getParentsClean();
            for (int a = 0; a < stripped.size(); a++) {
               if (!ResearchManager.knowsResearchStrict(player, stripped.get(a))) {
                  ResearchEntry pe = ResearchCategories.getResearch(clean.get(a));
                  Component name = pe != null ? pe.getLocalizedName() : Component.literal("?");
                  text.add(Component.literal("@@ - ").append(name).withStyle(ChatFormatting.YELLOW));
               }
            }
         }
         if (knowledge().hasResearchFlag(currentHighlight.getKey(), EnumResearchFlag.RESEARCH)) {
            text.add(Component.literal("@@").append(Component.translatable("tc.research.newresearch")));
         }
         if (knowledge().hasResearchFlag(currentHighlight.getKey(), EnumResearchFlag.PAGE)) {
            text.add(Component.literal("@@").append(Component.translatable("tc.research.newpage")));
         }
         GuiDraw.tooltip(g, font, text, mx + 3, my - 3);
      }
   }

   private void renderCategoryButton(GuiGraphics g, CategoryButton b, int mx, int my) {
      int y = b.y() + addonShift;
      boolean hovered = mx >= b.x() && my >= y && mx < b.x() + 16 && my < y + 16;
      boolean selected = b.key().equals(selectedCategory);
      GuiDraw.color(selected ? 0.6F : 1, 1, 1, 1);
      GuiDraw.rect(g, TX1, b.x() - 3, y - 3, 13, 13, 22, 22);
      if (!selected && !hovered) GuiDraw.color(0.66F, 0.66F, 0.66F, 0.8F);
      else GuiDraw.resetColor();
      GuiDraw.full(g, b.rc().icon, b.x(), y);
      GuiDraw.resetColor();
      boolean nr = false, np = false;
      for (String rk : b.rc().research.keySet()) {
         if (ResearchManager.knowsResearch(player, rk)) {
            nr |= knowledge().hasResearchFlag(rk, EnumResearchFlag.RESEARCH);
            np |= knowledge().hasResearchFlag(rk, EnumResearchFlag.PAGE);
         }
      }
      if (nr) {
         g.pose().pushPose();
         GuiDraw.color(1, 1, 1, 0.7F);
         g.pose().translate(b.x() - 2, y - 2, 0);
         g.pose().scale(0.25F, 0.25F, 1);
         GuiDraw.rect(g, TX1, 0, 0, 176, 16, 32, 32);
         g.pose().popPose();
      }
      if (np) {
         g.pose().pushPose();
         GuiDraw.color(1, 1, 1, 0.7F);
         g.pose().translate(b.x() - 2, y + 9, 0);
         g.pose().scale(0.25F, 0.25F, 1);
         GuiDraw.rect(g, TX1, 0, 0, 208, 16, 32, 32);
         g.pose().popPose();
      }
      GuiDraw.resetColor();
      if (hovered) {
         Component dp = Component.translatable("tc.research_category." + b.key()).append(" (" + b.completion() + "%)");
         int tx = !b.flip() ? b.x() + 22 : screenX + 9 - font.width(dp);
         g.drawString(font, dp, tx, y + 4, 0xFFFFFF);
         int t = 9;
         if (nr) {
            Component c = Component.translatable("tc.research.newresearch");
            g.drawString(font, c, !b.flip() ? b.x() + 22 : screenX + 9 - font.width(c), y + 4 + t, 0xFFFFFF);
            t += 9;
         }
         if (np) {
            Component c = Component.translatable("tc.research.newpage");
            g.drawString(font, c, !b.flip() ? b.x() + 22 : screenX + 9 - font.width(c), y + 4 + t, 0xFFFFFF);
         }
      }
   }
}
