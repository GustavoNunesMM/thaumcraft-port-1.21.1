package thaumcraft.client.gui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.Knowledge;
import thaumcraft.api.research.ResearchAddendum;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.common.lib.InventoryHelper;
import thaumcraft.common.network.CompleteStagePayload;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.ResearchManager;

/**
 * Port of GuiResearchPage: an open Thaumonomicon entry. Shows the text of the current stage over as many pages
 * as needed, what the stage requires (and the button to complete it), recipe bookmarks and the recipes themselves,
 * plus the known-aspects and knowledge inserts.
 */
public class ResearchPageScreen extends Screen {
   private static final ResourceLocation TEX1 = tex("textures/gui/gui_researchbook.png");
   private static final ResourceLocation TEX2 = tex("textures/gui/gui_researchbook_overlay.png");
   private static final ResourceLocation TEX3 = tex("textures/aspects/_back.png");
   private static final ResourceLocation TEX4 = tex("textures/gui/paper.png");
   private static final ResourceLocation DUMMY_RESEARCH = tex("textures/aspects/_unknown.png");
   private static final ResourceLocation DUMMY_MAP = tex("textures/research/rd_map.png");
   private static final ResourceLocation DUMMY_FLASK = tex("textures/research/rd_flask.png");
   private static final ResourceLocation DUMMY_CHEST = tex("textures/research/rd_chest.png");
   static final ResourceLocation[] KNOW_TYPE = {tex("textures/research/knowledge_theory.png"), tex("textures/research/knowledge_observation.png")};
   private static final int PAGE_WIDTH = 140;
   private static final PageImage PILINE = PageImage.parse("thaumcraft:textures/gui/gui_researchbook.png:24:184:95:6:1");
   private static final PageImage PIDIV = PageImage.parse("thaumcraft:textures/gui/gui_researchbook.png:28:192:140:6:1");

   public static final Deque<ResourceLocation> HISTORY = new ArrayDeque<>();
   @Nullable
   static ResourceLocation shownRecipe;
   private static int aspectsPage;

   private final int paneWidth = 256, paneHeight = 181;
   private final double guiMapX, guiMapY;
   private final ResearchEntry research;
   private final Player player;
   private int currentStage;
   private int lastStage;
   private boolean hold;
   private int page;
   private int maxPages;
   private int maxAspectPages;
   private final List<Aspect> knownAspects = new ArrayList<>();
   private int rhash;
   private long lastCheck;
   private int hrx, hry;
   private int recipePage, recipePageMax;
   private long lastCycle;
   private int cycle = -1;
   private boolean showingAspects, showingKnowledge;
   private final Map<ResourceLocation, List<RecipeCatalog.RecipeView>> recipeLists = new LinkedHashMap<>();
   private final Map<ResourceLocation, List<RecipeCatalog.RecipeView>> drilldownLists = new LinkedHashMap<>();
   private boolean hasRecipePages;
   private boolean allowWithPagePopup;
   @Nullable
   private List<Component> tipText;
   private final List<Page> pages = new ArrayList<>();
   private boolean isComplete, hasAllRequisites;
   private boolean[] hasItem, hasCraft, hasResearch, hasKnow;
   private final Map<ItemStack, String> keyCache = new HashMap<>();
   private final List<Reference> reference = new ArrayList<>();

   private record Reference(int x, int y, ResourceLocation recipe, int page) {}

   private static ResourceLocation tex(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   public ResearchPageScreen(ResearchEntry research, @Nullable ResourceLocation recipe, double x, double y) {
      super(research.getLocalizedName());
      this.research = research;
      this.guiMapX = x;
      this.guiMapY = y;
      this.player = Minecraft.getInstance().player;
      for (Aspect a : Aspect.all()) {
         if (ResearchManager.knowsResearch(player, "!" + a.getTag())) knownAspects.add(a);
      }
      knownAspects.sort((a, b) -> a.getName().compareTo(b.getName()));
      maxAspectPages = Mth.ceil(knownAspects.size() / 5.0F);
      if (recipe != null) shownRecipe = recipe;
   }

   @Override
   protected void init() {
      parsePages();
   }

   private PlayerKnowledge knowledge() {
      return ResearchManager.knowledge(player);
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private void sound(net.minecraft.sounds.SoundEvent s, float vol, float pitch) {
      player.playSound(s, vol, pitch);
   }

   // ------------------------------------------------------------------ rendering

   @Override
   public void render(GuiGraphics g, int mx, int my, float pt) {
      hasRecipePages = false;
      long nano = System.nanoTime();
      if (nano > lastCheck) {
         parsePages();
         lastCheck = nano + (hold ? 250_000_000L : 2_000_000_000L);
         if (currentStage > lastStage) hold = false;
      }
      renderTransparentBackground(g);
      genResearchBackground(g, mx, my);
      int sw = (width - paneWidth) / 2;
      int sh = (height - paneHeight) / 2;
      if (!HISTORY.isEmpty() && mouseInside(sw + 118, sh + 190, 20, 12, mx, my)) {
         g.drawString(font, Component.translatable("recipe.return"), mx, my, 0xFFFFFF, true);
      }
   }

   private void genResearchBackground(GuiGraphics g, int mx, int my) {
      int sw = (width - paneWidth) / 2;
      int sh = (height - paneHeight) / 2;
      float bx = (width - paneWidth * 1.3F) / 2.0F;
      float by = (height - paneHeight * 1.3F) / 2.0F;
      GuiDraw.resetColor();
      g.pose().pushPose();
      g.pose().translate(bx, by, 0);
      g.pose().scale(1.3F, 1.3F, 1.0F);
      GuiDraw.rect(g, TEX1, 0, 0, 0, 0, paneWidth, paneHeight);
      g.pose().popPose();
      reference.clear();
      int current = 0;
      for (Page p : pages) {
         if ((current == page || current == page + 1) && current < maxPages) drawPage(g, p, current % 2, sw, sh - 10, mx, my);
         if (++current > page + 1) break;
      }
      if (pages.isEmpty()) drawPage(g, new Page(), 0, sw, sh - 10, mx, my);
      GuiDraw.resetColor();
      float bob = Mth.sin(player.tickCount / 3.0F) * 0.2F + 0.1F;
      if (!HISTORY.isEmpty()) rectScaled(g, TEX1, sw + 118, sh + 190, 38, 202, 20, 12, bob);
      if (page > 0 && shownRecipe == null) rectScaled(g, TEX1, sw - 16, sh + 190, 0, 184, 12, 8, bob);
      if (page < maxPages - 2 && shownRecipe == null) rectScaled(g, TEX1, sw + 262, sh + 190, 12, 184, 12, 8, bob);
      if (tipText != null) {
         GuiDraw.tooltip(g, font, tipText, mx, my + 12);
         tipText = null;
      }
   }

   private void drawPage(GuiGraphics g, Page pageParm, int side, int x, int y, int mx, int my) {
      if (lastCycle < System.currentTimeMillis()) {
         cycle++;
         lastCycle = System.currentTimeMillis() + 1000L;
      }
      if (page == 0 && side == 0) {
         GuiDraw.rect(g, TEX1, x + 4, y - 7, 24, 184, 96, 4);
         GuiDraw.rect(g, TEX1, x + 4, y + 10, 24, 184, 96, 4);
         Component name = research.getLocalizedName();
         int offset = font.width(name);
         if (offset <= PAGE_WIDTH) {
            g.drawString(font, name, x - 15 + PAGE_WIDTH / 2 - offset / 2, y, 0x202020, false);
         } else {
            float vv = (float) PAGE_WIDTH / offset;
            g.pose().pushPose();
            g.pose().translate(x - 15 + PAGE_WIDTH / 2 - offset / 2 * vv, y + vv, 0);
            g.pose().scale(vv, vv, vv);
            g.drawString(font, name, 0, 0, 0x202020, false);
            g.pose().popPose();
         }
         y += 28;
      }
      for (Object content : pageParm.contents) {
         if (content instanceof String s) {
            GuiDraw.resetColor();
            g.drawString(font, s.replace("~B", ""), x - 15 + side * 152, y - 6, 0, false);
            y += font.lineHeight;
            if (s.endsWith("~B")) y += (int) (font.lineHeight * 0.66);
         } else if (content instanceof PageImage pi) {
            GuiDraw.resetColor();
            int pad = (PAGE_WIDTH - pi.aw) / 2;
            g.pose().pushPose();
            g.pose().translate(x - 15 + side * 152 + pad, y - 5, 0);
            g.pose().scale(pi.scale, pi.scale, pi.scale);
            GuiDraw.rect(g, pi.loc, 0, 0, pi.x, pi.y, pi.w, pi.h);
            g.pose().popPose();
            y += pi.ah + 2;
         }
      }
      if (knowledge().isResearchComplete("FIRSTSTEPS")) {
         int ty = (height - paneHeight) / 2 + 9;
         int le = mouseInside(x - 48, ty, 25, 16, mx, my) ? 0 : 3;
         GuiDraw.resetColor();
         popupAt(x - 48, ty, 25, 16, mx, my, Component.translatable("tc.aspect.name"));
         GuiDraw.rect(g, TEX1, x - 48 + le, ty, 76, 232, 24 - le, 16);
         GuiDraw.rect(g, TEX1, x - 28, ty, 100, 232, 4, 16);
      }
      if (knowledge().isResearchComplete("KNOWLEDGETYPES") && !research.getKey().equals("KNOWLEDGETYPES")) {
         int ty = (height - paneHeight) / 2 + 32;
         int le = mouseInside(x - 48, ty, 25, 16, mx, my) ? 0 : 3;
         GuiDraw.resetColor();
         popupAt(x - 48, ty, 25, 16, mx, my, Component.translatable("tc.knowledge.name"));
         GuiDraw.rect(g, TEX1, x - 49 + le, ty, 44, 232, 24 - le, 16);
         GuiDraw.rect(g, TEX1, x - 29, ty, 100, 232, 4, 16);
      }
      if (research.getStages().isEmpty()) return;
      ResearchStage stage = research.getStages().get(currentStage);
      if (!stage.recipes().isEmpty()) drawRecipeBookmarks(g, x, mx, my);
      if (page == 0 && side == 0 && !isComplete) drawRequirements(g, x, mx, my, stage);
      if (knowledge().isResearchComplete("KNOWLEDGETYPES") && research.getKey().equals("KNOWLEDGETYPES")) {
         drawKnowledges(g, x, (height - paneHeight) / 2 - 16 + 210, mx, my, true);
      }
      if (showingAspects) {
         drawAspectsInsert(g, mx, my);
      } else if (showingKnowledge) {
         drawKnowledgesInsert(g, mx, my);
      } else if (shownRecipe != null) {
         drawRecipe(g, mx, my);
      } else if (stage.warp() > 0 && !isComplete) {
         int warp = Math.min(5, stage.warp());
         Component s = Component.translatable("tc.forbidden.level." + warp);
         g.drawString(font, s, x - 56 - font.width(s) / 2, y - 43, 0xAA99FF, false);
         String text = Language.getInstance().getOrDefault("tc.warp.warn").replace("%n", s.getString());
         popupAt(x - 67, y - 50, 20, 20, mx, my, Component.literal(text));
      }
   }

   private void drawKnowledgesInsert(GuiGraphics g, int mx, int my) {
      allowWithPagePopup = true;
      GuiDraw.resetColor();
      int x = (width - 256) / 2;
      int y = (height - 256) / 2;
      GuiDraw.rect(g, TEX4, x, y, 0, 0, 255, 255);
      drawKnowledges(g, x + 60, (height - paneHeight) / 2 + 75, mx, my, false);
      allowWithPagePopup = false;
   }

   private void drawKnowledges(GuiGraphics g, int x, int y, int mx, int my, boolean inpage) {
      y -= 18;
      boolean drewSomething = false;
      int tc = 0;
      int ka = ResearchCategories.CATEGORIES.size();
      for (EnumKnowledgeType type : EnumKnowledgeType.values()) {
         int fc = 0;
         int hs = (int) (164.0F / ka);
         boolean b = false;
         for (ResearchCategory category : ResearchCategories.CATEGORIES.values()) {
            int amt = knowledge().getKnowledge(type, category);
            int par = knowledge().getKnowledgeRaw(type, category) % type.getProgression();
            if (amt <= 0 && par <= 0) continue;
            drewSomething = true;
            int px = x - 10 + (inpage ? 18 : hs) * fc;
            int py = y - tc * (inpage ? 20 : 28);
            GuiDraw.resetColor();
            g.pose().pushPose();
            g.pose().translate(px, py, 0);
            g.pose().scale(0.0625F, 0.0625F, 1);
            GuiDraw.quad(g, KNOW_TYPE[type.ordinal()], 0, 0, 256, 256, 0, 0, 1, 1, 1, 1);
            GuiDraw.color(1, 1, 1, 0.75F);
            g.pose().scale(0.66F, 0.66F, 1);
            GuiDraw.quad(g, category.icon, 66, 66, 256, 256, 0, 0, 1, 1, 1, 1);
            g.pose().popPose();
            GuiDraw.resetColor();
            String s = "" + amt;
            g.pose().pushPose();
            g.pose().translate(0, 0, 5);
            g.drawString(font, s, px + 16 - font.width(s), py + 8, 0xFFFFFF, true);
            g.pose().popPose();
            Component name = Component.translatable("tc.type." + type.name().toLowerCase()).append(": ")
               .append(Component.translatable("tc.research_category." + category.key));
            popupAt(px, py, 16, 16, mx, my, name);
            if (par > 0) {
               GuiDraw.color(1, 1, 1, 0.75F);
               int l = (int) ((float) par / type.getProgression() * 16.0F);
               GuiDraw.rect(g, TEX1, px, py + 17, 0, 232, l, 2);
               GuiDraw.rect(g, TEX1, px + l, py + 17, l, 234, 16 - l, 2);
               GuiDraw.resetColor();
            }
            fc++;
            b = true;
         }
         if (b) tc++;
      }
      if (inpage && drewSomething) {
         GuiDraw.resetColor();
         GuiDraw.rect(g, TEX1, x + 4, y - tc * 20 + 12, 24, 184, 96, 8);
      }
   }

   private void drawSmallKnowIcon(GuiGraphics g, Knowledge kn, int x, int y) {
      g.pose().pushPose();
      g.pose().translate(x, y, 0);
      g.pose().scale(0.0625F, 0.0625F, 1);
      GuiDraw.quad(g, KNOW_TYPE[kn.type().ordinal()], 0, 0, 256, 256, 0, 0, 1, 1, 1, 1);
      if (kn.category() != null) {
         g.pose().translate(32, 32, 1);
         g.pose().scale(0.75F, 0.75F, 1);
         GuiDraw.quad(g, kn.category().icon, 0, 0, 256, 256, 0, 0, 1, 1, 1, 1);
      }
      g.pose().popPose();
   }

   private void tick(GuiGraphics g, int x, int y) {
      GuiDraw.resetColor();
      g.pose().pushPose();
      g.pose().translate(0, 0, 250);
      GuiDraw.rect(g, TEX1, x, y, 159, 207, 10, 10);
      g.pose().popPose();
   }

   private void drawRequirements(GuiGraphics g, int x, int mx, int my, ResearchStage stage) {
      if (hasResearch == null || hasResearch.length != stage.research().size() || hasItem.length != stage.obtain().size()
            || hasCraft.length != stage.craft().size() || hasKnow.length != stage.know().size()) {
         checkRequisites();
      }
      int y = (height - paneHeight) / 2 - 16 + 210;
      boolean b = false;
      if (!stage.research().isEmpty()) {
         y -= 18;
         b = true;
         int shift = 24;
         GuiDraw.color(1, 1, 1, 0.25F);
         GuiDraw.rect(g, TEX1, x - 12, y - 1, 200, 232, 56, 16);
         GuiDraw.resetColor();
         popupAt(x - 15, y, 16, 16, mx, my, Component.translatable("tc.need.research"));
         if (hasResearch != null) {
            int ss = stage.research().size() > 6 ? 110 / stage.research().size() : 18;
            for (int a = 0; a < stage.research().size(); a++) {
               String key = stage.research().get(a);
               Object loc = stage.researchIcon(a) != null ? ResourceLocation.parse(stage.researchIcon(a)) : DUMMY_RESEARCH;
               Component text = Component.translatable("research." + key + ".text");
               if (key.startsWith("!")) {
                  Aspect as = Aspect.getAspect(key.substring(1));
                  if (as != null) {
                     loc = as;
                     text = Component.literal(as.getName());
                  }
               }
               ResearchEntry re = ResearchCategories.getResearch(key);
               GuiDraw.resetColor();
               boolean tint = false;
               if (re != null && !re.getIcons().isEmpty()) {
                  loc = re;
                  text = re.getLocalizedName();
               } else if (key.startsWith("m_")) {
                  loc = DUMMY_MAP;
               } else if (key.startsWith("c_")) {
                  loc = DUMMY_CHEST;
               } else if (key.startsWith("f_")) {
                  loc = DUMMY_FLASK;
               } else if (!(loc instanceof Aspect)) {
                  tint = true;
               }
               int ix = x - 15 + shift;
               if (loc instanceof Aspect as) {
                  int c = as.getColor();
                  GuiDraw.color((c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F, 1);
                  GuiDraw.full(g, as.getImage(), ix, y);
               } else if (loc instanceof ResearchEntry e) {
                  ResearchBrowserScreen.drawResearchIcon(g, e, ix, y, false);
               } else if (loc instanceof ResourceLocation rl) {
                  if (tint) GuiDraw.color(0.5F, 0.75F, 1.0F, 1);
                  GuiDraw.full(g, rl, ix, y);
               }
               GuiDraw.resetColor();
               if (hasResearch[a]) tick(g, ix + 8, y);
               popupAt(ix, y, 16, 16, mx, my, text);
               shift += ss;
            }
         }
      }
      if (!stage.obtain().isEmpty()) {
         y -= 18;
         b = true;
         int shift = 24;
         GuiDraw.color(1, 1, 1, 0.25F);
         GuiDraw.rect(g, TEX1, x - 12, y - 1, 200, 216, 56, 16);
         GuiDraw.resetColor();
         popupAt(x - 15, y, 16, 16, mx, my, Component.translatable("tc.need.obtain"));
         if (hasItem != null) {
            int ss = stage.obtain().size() > 6 ? 110 / stage.obtain().size() : 18;
            for (int idx = 0; idx < stage.obtain().size(); idx++) {
               ResearchItem it = stage.obtain().get(idx);
               ItemStack stack = it.display().copyWithCount(it.count());
               drawStackAt(g, stack, x - 15 + shift, y, mx, my, true);
               if (hasItem[idx]) tick(g, x - 15 + shift + 8, y);
               shift += ss;
            }
         }
      }
      if (!stage.craft().isEmpty()) {
         y -= 18;
         b = true;
         int shift = 24;
         GuiDraw.color(1, 1, 1, 0.25F);
         GuiDraw.rect(g, TEX1, x - 12, y - 1, 200, 200, 56, 16);
         GuiDraw.resetColor();
         popupAt(x - 15, y, 16, 16, mx, my, Component.translatable("tc.need.craft"));
         if (hasCraft != null) {
            int ss = stage.craft().size() > 6 ? 110 / stage.craft().size() : 18;
            for (int idx = 0; idx < stage.craft().size(); idx++) {
               drawStackAt(g, stage.craft().get(idx).display(), x - 15 + shift, y, mx, my, true);
               if (hasCraft[idx]) tick(g, x - 15 + shift + 8, y);
               shift += ss;
            }
         }
      }
      if (!stage.know().isEmpty()) {
         y -= 18;
         b = true;
         int shift = 24;
         GuiDraw.color(1, 1, 1, 0.25F);
         GuiDraw.rect(g, TEX1, x - 12, y - 1, 200, 184, 56, 16);
         GuiDraw.resetColor();
         popupAt(x - 15, y, 16, 16, mx, my, Component.translatable("tc.need.know"));
         if (hasKnow != null) {
            int ss = stage.know().size() > 6 ? 110 / stage.know().size() : 18;
            for (int idx = 0; idx < stage.know().size(); idx++) {
               Knowledge kn = stage.know().get(idx);
               drawSmallKnowIcon(g, kn, x - 15 + shift, y);
               String am = (!hasKnow[idx] ? ChatFormatting.RED.toString() : "") + kn.amount();
               g.pose().pushPose();
               g.pose().translate(x - 15 + shift + 16 - font.width(am) / 2.0F, y + 12, 5);
               g.pose().scale(0.5F, 0.5F, 0.5F);
               g.drawString(font, am, 0, 0, 0xFFFFFF, true);
               g.pose().popPose();
               if (hasKnow[idx]) tick(g, x - 15 + shift + 8, y);
               Component s = Component.translatable("tc.type." + kn.type().name().toLowerCase());
               if (kn.category() != null) s = s.copy().append(": ").append(Component.translatable("tc.research_category." + kn.category().key));
               popupAt(x - 15 + shift, y, 16, 16, mx, my, s);
               shift += ss;
            }
         }
      }
      if (b) {
         y -= 12;
         GuiDraw.resetColor();
         GuiDraw.rect(g, TEX1, x + 4, y - 2, 24, 184, 96, 8);
         if (hasAllRequisites) {
            hrx = x + 20;
            hry = y - 6;
            if (hold) {
               Component s = Component.translatable("tc.stage.hold");
               g.drawString(font, s, (int) (x + 52 - font.width(s) / 2.0F), y - 4, 0xFFFFFF, true);
            } else {
               if (mouseInside(hrx, hry, 64, 12, mx, my)) GuiDraw.resetColor();
               else GuiDraw.color(0.8F, 0.8F, 0.9F, 1);
               GuiDraw.rect(g, TEX1, hrx, hry, 84, 216, 64, 12);
               GuiDraw.resetColor();
               Component s = Component.translatable("tc.stage.complete");
               g.drawString(font, s, (int) (x + 52 - font.width(s) / 2.0F), y - 4, 0xFFFFFF, true);
            }
         }
      }
   }

   private void drawRecipeBookmarks(GuiGraphics g, int x, int mx, int my) {
      Random rng = new Random(rhash);
      int y = (height - paneHeight) / 2 - 8;
      allowWithPagePopup = true;
      if (!recipeLists.isEmpty()) {
         int space = Math.min(25, 200 / recipeLists.size());
         for (Map.Entry<ResourceLocation, List<RecipeCatalog.RecipeView>> e : recipeLists.entrySet()) {
            List<RecipeCatalog.RecipeView> list = e.getValue();
            if (list.isEmpty()) continue;
            RecipeCatalog.RecipeView view = list.get(Math.floorMod(cycle, list.size()));
            int sh = rng.nextInt(3);
            int le = rng.nextInt(3) + (mouseInside(x + 280, y - 1, 30, 16, mx, my) ? 0 : 3);
            if (e.getKey().equals(shownRecipe)) GuiDraw.color(1, 0.5F, 0.5F, 1);
            else GuiDraw.resetColor();
            GuiDraw.rect(g, TEX1, x + 280 + sh, y - 1, 120 + le, 232, 28, 16);
            GuiDraw.rect(g, TEX1, x + 280 + sh, y - 1, 116, 232, 4, 16);
            GuiDraw.resetColor();
            drawStackAt(g, view.output(), x + 287 + sh - le, y - 1, mx, my, false);
            y += space;
         }
      }
      allowWithPagePopup = false;
   }

   private void generateRecipesLists(ResearchStage stage, @Nullable List<ResearchAddendum> addenda) {
      recipeLists.clear();
      for (ResourceLocation rk : stage.recipes()) addRecipesToList(rk, recipeLists);
      if (addenda != null) {
         for (ResearchAddendum addendum : addenda) {
            if (ResearchManager.knowsResearchStrict(player, addendum.research().toArray(new String[0]))) {
               for (ResourceLocation rk : addendum.recipes()) addRecipesToList(rk, recipeLists);
            }
         }
      }
   }

   private void addRecipesToList(ResourceLocation rk, Map<ResourceLocation, List<RecipeCatalog.RecipeView>> lists) {
      List<RecipeCatalog.RecipeView> views = RecipeCatalog.lookup(rk).stream().filter(v -> v.known(player)).toList();
      if (!views.isEmpty()) lists.computeIfAbsent(rk, k -> new ArrayList<>()).addAll(views);
   }

   private void drawRecipe(GuiGraphics g, int mx, int my) {
      allowWithPagePopup = true;
      GuiDraw.resetColor();
      int x = (width - 256) / 2;
      int y = (height - 256) / 2;
      g.pose().pushPose();
      g.pose().translate(0, 0, 100);
      GuiDraw.rect(g, TEX4, x, y, 0, 0, 255, 255);
      List<RecipeCatalog.RecipeView> list = recipeLists.get(shownRecipe);
      if (list == null || list.isEmpty()) list = drilldownLists.get(shownRecipe);
      if (list != null && !list.isEmpty()) {
         hasRecipePages = list.size() > 1;
         recipePageMax = list.size() - 1;
         if (recipePage > recipePageMax) recipePage = recipePageMax;
         list.get(recipePage % list.size()).render(this, g, x + 128, y + 128, mx, my);
         if (hasRecipePages) {
            float bob = Mth.sin(player.tickCount / 3.0F) * 0.2F + 0.1F;
            if (recipePage > 0) rectScaled(g, TEX1, x + 40, y + 232, 0, 184, 12, 8, bob);
            if (recipePage < recipePageMax) rectScaled(g, TEX1, x + 204, y + 232, 12, 184, 12, 8, bob);
         }
      }
      g.pose().popPose();
      allowWithPagePopup = false;
   }

   /** The crafting grid backdrop of drawCraftingPage. */
   public void drawCraftingGrid(GuiGraphics g, int x, int y) {
      GuiDraw.resetColor();
      g.pose().pushPose();
      g.pose().translate(x, y, 0);
      g.pose().scale(2.0F, 2.0F, 1.0F);
      GuiDraw.rect(g, TEX2, -26, -26, 60, 15, 51, 52);
      GuiDraw.rect(g, TEX2, -8, -46, 20, 3, 16, 16);
      g.pose().popPose();
   }

   /** The arcane workbench backdrop of drawArcaneCraftingPage, with the vis cost. */
   public void drawArcaneGrid(GuiGraphics g, int x, int y, int vis, int mx, int my) {
      GuiDraw.resetColor();
      g.pose().pushPose();
      g.pose().translate(x, y, 0);
      g.pose().scale(2.0F, 2.0F, 1.0F);
      GuiDraw.rect(g, TEX2, -26, -26, 112, 15, 52, 52);
      GuiDraw.rect(g, TEX2, -8, -46, 20, 3, 16, 16);
      GuiDraw.color(1.0F, 1.0F, 1.0F, 0.4F);
      GuiDraw.rect(g, TEX2, -6, 40, 68, 76, 12, 12);
      GuiDraw.resetColor();
      g.pose().popPose();
      String text = "" + vis;
      int offset = font.width(text);
      g.drawString(font, text, x - offset / 2, y + 90, 0x505050, false);
      popupAt(x - offset / 2 - 15, y + 75, 30, 30, mx, my, Component.translatable("wandtable.text1"));
   }

   public void drawCentered(GuiGraphics g, Component text, int x, int y) {
      g.drawString(font, text, x - font.width(text) / 2, y, 0x505050, false);
   }

   private void drawAspectsInsert(GuiGraphics g, int mx, int my) {
      allowWithPagePopup = true;
      GuiDraw.resetColor();
      int x = (width - 256) / 2;
      int y = (height - 256) / 2;
      g.pose().pushPose();
      g.pose().translate(0, 0, 100);
      GuiDraw.rect(g, TEX4, x, y, 0, 0, 255, 255);
      drawAspectPage(g, x + 60, y + 24, mx, my);
      g.pose().popPose();
      allowWithPagePopup = false;
   }

   /** The crucible backdrop of drawCruciblePage. */
   public void drawCrucibleBackdrop(GuiGraphics g, int x, int y) {
      GuiDraw.resetColor();
      g.pose().pushPose();
      g.pose().translate(x, y, 0);
      g.pose().scale(2.0F, 2.0F, 1.0F);
      GuiDraw.rect(g, TEX2, -28, -29, 0, 3, 56, 17);
      g.pose().translate(0, 32, 0);
      GuiDraw.rect(g, TEX2, -28, -44, 0, 20, 56, 48);
      g.pose().translate(0, -8, 0);
      GuiDraw.rect(g, TEX2, -25, -50, 100, 84, 11, 13);
      g.pose().popPose();
   }

   /** UtilsFX.drawTag with the amount, plus the aspect's name/description on hover. */
   public void drawAspectTag(GuiGraphics g, Aspect aspect, int amount, int x, int y, int mx, int my) {
      drawTag(g, aspect, x, y);
      String am = String.valueOf(amount);
      g.pose().pushPose();
      g.pose().translate(0, 0, 200);
      g.pose().scale(0.5F, 0.5F, 1.0F);
      g.drawString(font, am, (x + 17) * 2 - font.width(am), (y + 12) * 2, 0xFFFFFF, true);
      g.pose().popPose();
      if (mx >= x && my >= y && mx < x + 16 && my < y + 16) tipText = List.of(aspect.getLocalizedName(), aspect.getLocalizedDescription());
   }

   static void drawTag(GuiGraphics g, Aspect aspect, float x, float y) {
      int c = aspect.getColor();
      GuiDraw.color((c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F, 1);
      GuiDraw.full(g, aspect.getImage(), x, y);
      GuiDraw.resetColor();
   }

   private void drawAspectPage(GuiGraphics g, int x, int y, int mx, int my) {
      if (knownAspects.isEmpty()) return;
      int count = -1;
      int start = aspectsPage * 5;
      for (Aspect aspect : knownAspects) {
         if (++count < start) continue;
         if (count > start + 4) break;
         int ty = y + count % 5 * 40;
         if (mx >= x && my >= ty && mx < x + 40 && my < ty + 40) {
            g.pose().pushPose();
            g.pose().translate(x - 2, ty - 2, 0);
            g.pose().scale(2, 2, 1);
            GuiDraw.color(1, 1, 1, 0.5F);
            GuiDraw.full(g, TEX3, 0, 0);
            GuiDraw.resetColor();
            g.pose().popPose();
         }
         g.pose().pushPose();
         g.pose().translate(x + 2, y + 2 + count % 5 * 40, 0);
         g.pose().scale(1.5F, 1.5F, 1.5F);
         drawTag(g, aspect, 0, 0);
         g.pose().popPose();
         drawSmallText(g, aspect.getName(), x + 16, y + 29 + count % 5 * 40);
         Aspect[] comps = aspect.getComponents();
         if (comps != null && comps.length == 2) {
            for (int i = 0; i < 2; i++) {
               g.pose().pushPose();
               g.pose().translate(x + 60 + 42 * i, y + 4 + count % 5 * 40, 0);
               g.pose().scale(1.25F, 1.25F, 1.25F);
               if (knowledge().isResearchKnown("!" + comps[i].getTag())) {
                  drawTag(g, comps[i], 0, 0);
               } else {
                  GuiDraw.color(0.8F, 0.8F, 0.8F, 1);
                  GuiDraw.full(g, DUMMY_RESEARCH, 0, 0);
                  GuiDraw.resetColor();
               }
               g.pose().popPose();
               if (knowledge().isResearchKnown("!" + comps[i].getTag())) drawSmallText(g, comps[i].getName(), x + 22 + 50 + 42 * i, y + 29 + count % 5 * 40);
            }
            g.drawString(font, "=", x + 9 + 32, y + 12 + count % 5 * 40, 0x999999, false);
            g.drawString(font, "+", x + 10 + 79, y + 12 + count % 5 * 40, 0x999999, false);
         } else {
            g.drawString(font, Component.translatable("tc.aspect.primal"), x + 54, y + 12 + count % 5 * 40, 0x777777, false);
         }
      }
      float bob = Mth.sin(player.tickCount / 3.0F) * 0.2F + 0.1F;
      if (aspectsPage > 0) rectScaled(g, TEX1, x - 20, y + 208, 0, 184, 12, 8, bob);
      if (aspectsPage < maxAspectPages - 1) rectScaled(g, TEX1, x + 144, y + 208, 12, 184, 12, 8, bob);
   }

   private void drawSmallText(GuiGraphics g, String text, int cx, int y) {
      g.pose().pushPose();
      g.pose().translate(cx, y, 0);
      g.pose().scale(0.5F, 0.5F, 0.5F);
      g.drawString(font, text, -font.width(text) / 2, 0, 0x505050, false);
      g.pose().popPose();
   }

   private void rectScaled(GuiGraphics g, ResourceLocation tex, float x, float y, float u, float v, float w, float h, float scale) {
      g.pose().pushPose();
      g.pose().translate(x + w / 2.0F, y + h / 2.0F, 0);
      g.pose().scale(1.0F + scale, 1.0F + scale, 1.0F);
      GuiDraw.rect(g, tex, -w / 2.0F, -h / 2.0F, u, v, w, h);
      g.pose().popPose();
   }

   // ------------------------------------------------------------------ popups and stacks

   private void popupAt(int x, int y, int w, int h, int mx, int my, Component text) {
      if ((shownRecipe == null || allowWithPagePopup) && mx >= x && my >= y && mx < x + w && my < y + h) tipText = List.of(text);
   }

   boolean mouseInside(int x, int y, int w, int h, int mx, int my) {
      return mx >= x && my >= y && mx < x + w && my < y + h;
   }

   /** Draws a stack with its tooltip; clickthrough stacks link to the research recipe that makes them. */
   public void drawStackAt(GuiGraphics g, ItemStack stack, int x, int y, int mx, int my, boolean clickthrough) {
      if (stack.isEmpty()) return;
      g.pose().pushPose();
      g.pose().translate(0, 0, 150);
      g.renderItem(stack, x, y);
      g.renderItemDecorations(font, stack, x, y);
      g.pose().popPose();
      if ((shownRecipe == null || allowWithPagePopup) && mx >= x && my >= y && mx < x + 16 && my < y + 16) {
         List<Component> text = new ArrayList<>(stack.getTooltipLines(Item.TooltipContext.of(player.level()), player,
            minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL));
         if (clickthrough) {
            String ref = getCraftingRecipeKey(stack);
            if (ref != null) {
               String[] sr = ref.split(";", 2);
               if (sr.length > 1) {
                  text.add(Component.translatable("recipe.clickthrough").withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
                  reference.add(new Reference(mx, my, ResourceLocation.parse(sr[0]), Integer.parseInt(sr[1])));
               } else {
                  text.add(Component.translatable("recipe.unknown").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
               }
            }
         }
         tipText = text;
      }
   }

   /** findRecipePage: index of a recipe making this stack within the id, -99 if unresearched, -1 if none. */
   private int findRecipePage(ResourceLocation rk, ItemStack stack) {
      List<RecipeCatalog.RecipeView> views = RecipeCatalog.lookup(rk);
      for (int i = 0; i < views.size(); i++) {
         if (ItemStack.isSameItem(views.get(i).output(), stack)) return views.get(i).known(player) ? i : -99;
      }
      return -1;
   }

   @Nullable
   private String getCraftingRecipeKey(ItemStack stack) {
      ItemStack key = stack.copyWithCount(1);
      for (Map.Entry<ItemStack, String> e : keyCache.entrySet()) {
         if (ItemStack.isSameItemSameComponents(e.getKey(), key)) return e.getValue();
      }
      String found = null;
      outer:
      for (ResearchEntry ri : ResearchCategories.allResearch()) {
         for (ResearchStage stage : ri.getStages()) {
            for (ResourceLocation rec : stage.recipes()) {
               int result = findRecipePage(rec, key);
               if (result != -1) {
                  found = result == -99 ? "UNKNOWN" : rec + ";" + result;
                  break outer;
               }
            }
         }
      }
      keyCache.put(key, found);
      return found;
   }

   // ------------------------------------------------------------------ input

   @Override
   public boolean keyPressed(int key, int scan, int mods) {
      if (key == GLFW.GLFW_KEY_ESCAPE || minecraft.options.keyInventory.matches(key, scan)) {
         HISTORY.clear();
         if (shownRecipe == null && !showingAspects && !showingKnowledge) {
            minecraft.setScreen(new ResearchBrowserScreen(guiMapX, guiMapY));
         } else {
            shownRecipe = null;
            showingAspects = false;
            showingKnowledge = false;
            sound(ModSounds.PAGE.get(), 0.4F, 1.1F);
         }
         return true;
      }
      if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_PAGE_UP) {
         prevPage();
         return true;
      }
      if (key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_PAGE_DOWN) {
         nextPage();
         return true;
      }
      if (key == GLFW.GLFW_KEY_BACKSPACE) {
         goBack();
         return true;
      }
      return super.keyPressed(key, scan, mods);
   }

   private void nextPage() {
      if (page < maxPages - 2) {
         page += 2;
         lastCycle = 0L;
         cycle = -1;
         sound(ModSounds.PAGE.get(), 0.66F, 1.0F);
      }
   }

   private void prevPage() {
      if (page >= 2) {
         page -= 2;
         lastCycle = 0L;
         cycle = -1;
         sound(ModSounds.PAGE.get(), 0.66F, 1.0F);
      }
   }

   private void goBack() {
      if (!HISTORY.isEmpty()) {
         sound(ModSounds.PAGE.get(), 0.66F, 1.0F);
         shownRecipe = HISTORY.pop();
      } else {
         shownRecipe = null;
      }
   }

   @Override
   public boolean mouseClicked(double mxd, double myd, int button) {
      int px = (int) mxd, py = (int) myd;
      checkRequisites();
      int sw = (width - paneWidth) / 2;
      int sh = (height - paneHeight) / 2;
      if (shownRecipe == null && !hold && hasAllRequisites && mouseInside(hrx, hry, 64, 12, px, py)) {
         PacketDistributor.sendToServer(new CompleteStagePayload(research.getKey(), false, true, true));
         sound(ModSounds.WRITE.get(), 0.66F, 1.0F);
         lastCheck = 0L;
         lastStage = currentStage;
         hold = true;
         keyCache.clear();
         drilldownLists.clear();
         return true;
      }
      if (knowledge().isResearchComplete("FIRSTSTEPS") && mouseInside(sw - 48, sh + 8, 25, 16, px, py)) {
         shownRecipe = null;
         showingKnowledge = false;
         showingAspects = !showingAspects;
         HISTORY.clear();
         if (aspectsPage > maxAspectPages) aspectsPage = 0;
         sound(ModSounds.PAGE.get(), 0.7F, 0.9F);
         return true;
      }
      if (knowledge().isResearchComplete("KNOWLEDGETYPES") && !research.getKey().equals("KNOWLEDGETYPES") && mouseInside(sw - 48, sh + 31, 25, 16, px, py)) {
         shownRecipe = null;
         showingAspects = false;
         showingKnowledge = !showingKnowledge;
         HISTORY.clear();
         sound(ModSounds.PAGE.get(), 0.7F, 0.9F);
         return true;
      }
      if (showingAspects && aspectsPage < maxAspectPages - 1 && mouseInside(sw + 205, sh + 192, 14, 14, px, py)) {
         aspectsPage++;
         sound(ModSounds.PAGE.get(), 0.7F, 0.9F);
         return true;
      }
      if (showingAspects && aspectsPage > 0 && mouseInside(sw + 38, sh + 192, 14, 14, px, py)) {
         aspectsPage--;
         sound(ModSounds.PAGE.get(), 0.7F, 0.9F);
         return true;
      }
      if (!recipeLists.isEmpty()) {
         int aa = 0;
         int space = Math.min(25, 200 / recipeLists.size());
         for (ResourceLocation rk : recipeLists.keySet()) {
            if (mouseInside(sw + 280, sh - 8 + aa * space, 30, 16, px, py)) {
               shownRecipe = rk.equals(shownRecipe) ? null : rk;
               showingAspects = false;
               showingKnowledge = false;
               HISTORY.clear();
               sound(ModSounds.PAGE.get(), 0.7F, 0.9F);
               return true;
            }
            aa++;
         }
      }
      if (shownRecipe != null) {
         List<RecipeCatalog.RecipeView> list = recipeLists.get(shownRecipe);
         if (list == null || list.isEmpty()) list = drilldownLists.get(shownRecipe);
         if (list != null && !list.isEmpty()
            && list.get(recipePage % list.size()).mouseClicked(this, (width - 256) / 2 + 128, (height - 256) / 2 + 128, px, py)) {
            return true;
         }
      }
      if (hasRecipePages && recipePage < recipePageMax && mouseInside(sw + 205, sh + 192, 14, 14, px, py)) {
         recipePage++;
         sound(ModSounds.PAGE.get(), 0.7F, 0.9F);
         return true;
      }
      if (hasRecipePages && recipePage > 0 && mouseInside(sw + 38, sh + 192, 14, 14, px, py)) {
         recipePage--;
         sound(ModSounds.PAGE.get(), 0.7F, 0.9F);
         return true;
      }
      if (shownRecipe == null && mouseInside(sw + 261, sh + 189, 14, 10, px, py)) {
         nextPage();
         return true;
      }
      if (shownRecipe == null && mouseInside(sw - 17, sh + 189, 14, 10, px, py)) {
         prevPage();
         return true;
      }
      if (mouseInside(sw + 118, sh + 190, 20, 12, px, py)) {
         goBack();
         return true;
      }
      for (Reference ref : reference) {
         if (px >= ref.x() - 16 && py >= ref.y() - 16 && px < ref.x() + 16 && py < ref.y() + 16) {
            sound(ModSounds.PAGE.get(), 0.66F, 1.0F);
            if (shownRecipe != null) HISTORY.push(shownRecipe);
            shownRecipe = ref.recipe();
            recipePage = ref.page();
            if (!drilldownLists.containsKey(shownRecipe)) addRecipesToList(shownRecipe, drilldownLists);
            return true;
         }
      }
      return super.mouseClicked(mxd, myd, button);
   }

   // ------------------------------------------------------------------ text layout

   /** listFormattedStringToWidth: word wraps, carrying the active formatting codes onto the next line. */
   private List<String> wrap(String text, int width) {
      List<String> out = new ArrayList<>();
      for (String paragraph : text.split("\n", -1)) {
         StringBuilder line = new StringBuilder();
         String carry = "";
         for (String word : paragraph.split(" ", -1)) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (font.width(carry + candidate) > width && line.length() > 0) {
               out.add(carry + line);
               carry = activeFormat(carry + line);
               line = new StringBuilder(word);
            } else {
               line = new StringBuilder(candidate);
            }
         }
         out.add(carry + line);
      }
      return out;
   }

   private static String activeFormat(String s) {
      StringBuilder fmt = new StringBuilder();
      for (int i = 0; i < s.length() - 1; i++) {
         if (s.charAt(i) == '§') {
            ChatFormatting f = ChatFormatting.getByCode(s.charAt(i + 1));
            if (f != null) {
               if (f == ChatFormatting.RESET || f.isColor()) fmt.setLength(0);
               if (f != ChatFormatting.RESET) fmt.append('§').append(s.charAt(i + 1));
            }
         }
      }
      return fmt.toString();
   }

   private void parsePages() {
      pages.clear();
      if (research.getStages().isEmpty()) return;
      boolean complete = false;
      currentStage = knowledge().getResearchStage(research.getKey()) - 1;
      while (currentStage >= research.getStages().size()) {
         currentStage--;
         complete = true;
      }
      if (currentStage < 0) currentStage = 0;
      // requisites must be computed for the stage being shown, after currentStage advances
      checkRequisites();
      ResearchStage stage = research.getStages().get(currentStage);
      List<ResearchAddendum> addenda = complete && !research.getAddenda().isEmpty() ? research.getAddenda() : null;
      generateRecipesLists(stage, addenda);
      String rawText = Language.getInstance().getOrDefault(stage.text());
      if (addenda != null) {
         int ac = 0;
         for (ResearchAddendum addendum : addenda) {
            if (ResearchManager.knowsResearchStrict(player, addendum.research().toArray(new String[0]))) {
               rawText += "<PAGE>" + Component.translatable("tc.addendumtext", ++ac).getString() + "<BR>"
                  + Language.getInstance().getOrDefault(addendum.text());
            }
         }
      }
      rawText = rawText.replace("<BR>", "~B\n\n").replace("<BR/>", "~B\n\n").replace("<LINE>", "~L").replace("<LINE/>", "~L")
         .replace("<DIV>", "~D").replace("<DIV/>", "~D").replace("<PAGE>", "~P").replace("<PAGE/>", "~P");
      List<PageImage> images = new ArrayList<>();
      for (String s : rawText.split("<IMG>")) {
         int i = s.indexOf("</IMG>");
         if (i >= 0) {
            String clean = s.substring(0, i);
            PageImage pi = PageImage.parse(clean);
            if (pi == null) {
               rawText = rawText.replace(clean, "\n");
            } else {
               images.add(pi);
               rawText = rawText.replace(clean, "~I");
            }
         }
      }
      rawText = rawText.replace("<IMG>", "").replace("</IMG>", "");
      List<String> firstPass = new ArrayList<>();
      String[] p0 = rawText.split("~P", -1);
      for (int a = 0; a < p0.length; a++) {
         String[] p1 = p0[a].split("~D", -1);
         for (int x = 0; x < p1.length; x++) {
            String[] p2 = p1[x].split("~L", -1);
            for (int b = 0; b < p2.length; b++) {
               String[] p3 = p2[b].split("~I", -1);
               for (int c = 0; c < p3.length; c++) {
                  firstPass.add(p3[c]);
                  if (c != p3.length - 1) firstPass.add("~I");
               }
               if (b != p2.length - 1) firstPass.add("~L");
            }
            if (x != p1.length - 1) firstPass.add("~D");
         }
         if (a != p0.length - 1) firstPass.add("~P");
      }
      List<String> parsed = new ArrayList<>();
      for (String s : firstPass) parsed.addAll(wrap(s, PAGE_WIDTH));

      int lineHeight = font.lineHeight;
      int heightRemaining = 182;
      int dividerSpace = 0;
      if (research.getKey().equals("KNOWLEDGETYPES")) {
         heightRemaining -= 2;
         int tc = 0;
         for (EnumKnowledgeType type : EnumKnowledgeType.values()) {
            for (ResearchCategory category : ResearchCategories.CATEGORIES.values()) {
               if (knowledge().getKnowledgeRaw(type, category) > 0) {
                  tc++;
                  break;
               }
            }
         }
         heightRemaining -= 20 * tc;
         dividerSpace = 12;
      }
      if (!isComplete) {
         if (!stage.craft().isEmpty()) {
            heightRemaining -= 18;
            dividerSpace = 15;
         }
         if (!stage.obtain().isEmpty()) {
            heightRemaining -= 18;
            dividerSpace = 15;
         }
         if (!stage.know().isEmpty()) {
            heightRemaining -= 18;
            dividerSpace = 15;
         }
         if (!stage.research().isEmpty()) {
            heightRemaining -= 18;
            dividerSpace = 15;
         }
      }
      heightRemaining -= dividerSpace;
      Page page1 = new Page();
      List<PageImage> tempImages = new ArrayList<>();
      for (String line : parsed) {
         if (line.contains("~I")) {
            if (!images.isEmpty()) tempImages.add(images.remove(0));
            line = "";
         }
         if (line.contains("~L")) {
            tempImages.add(PILINE);
            line = "";
         }
         if (line.contains("~D")) {
            tempImages.add(PIDIV);
            line = "";
         }
         if (line.contains("~P")) {
            heightRemaining = 210;
            pages.add(page1.copy());
            page1 = new Page();
            line = "";
         }
         if (!line.isEmpty()) {
            line = line.trim();
            page1.contents.add(line);
            heightRemaining -= lineHeight;
            if (line.endsWith("~B")) heightRemaining -= (int) (lineHeight * 0.66);
         }
         while (!tempImages.isEmpty() && heightRemaining >= tempImages.get(0).ah + 2) {
            heightRemaining -= tempImages.get(0).ah + 2;
            page1.contents.add(tempImages.remove(0));
         }
         if (heightRemaining < lineHeight && !page1.contents.isEmpty()) {
            heightRemaining = 210;
            pages.add(page1.copy());
            page1 = new Page();
         }
      }
      if (!page1.contents.isEmpty()) pages.add(page1.copy());
      page1 = new Page();
      heightRemaining = 210;
      while (!tempImages.isEmpty()) {
         if (heightRemaining < tempImages.get(0).ah + 2) {
            heightRemaining = 210;
            pages.add(page1.copy());
            page1 = new Page();
         } else {
            heightRemaining -= tempImages.get(0).ah + 2;
            page1.contents.add(tempImages.remove(0));
         }
      }
      if (!page1.contents.isEmpty()) pages.add(page1.copy());
      rhash = research.getKey().hashCode() + currentStage * 50;
      maxPages = pages.size();
   }

   private void checkRequisites() {
      if (research.getStages().isEmpty()) return;
      isComplete = knowledge().isResearchComplete(research.getKey());
      while (currentStage >= research.getStages().size()) currentStage--;
      if (currentStage < 0) return;
      hasAllRequisites = true;
      ResearchStage stage = research.getStages().get(currentStage);
      hasItem = new boolean[stage.obtain().size()];
      for (int a = 0; a < hasItem.length; a++) {
         hasItem[a] = InventoryHelper.isCarrying(player, stage.obtain().get(a));
         if (!hasItem[a]) hasAllRequisites = false;
      }
      hasCraft = new boolean[stage.craft().size()];
      for (int a = 0; a < hasCraft.length; a++) {
         hasCraft[a] = knowledge().isResearchKnown("[#]" + stage.craft().get(a).craftKey());
         if (!hasCraft[a]) hasAllRequisites = false;
      }
      hasResearch = new boolean[stage.research().size()];
      for (int a = 0; a < hasResearch.length; a++) {
         hasResearch[a] = ResearchManager.knowsResearchStrict(player, stage.research().get(a));
         if (!hasResearch[a]) hasAllRequisites = false;
      }
      hasKnow = new boolean[stage.know().size()];
      for (int a = 0; a < hasKnow.length; a++) {
         Knowledge k = stage.know().get(a);
         hasKnow[a] = knowledge().getKnowledge(k.type(), k.category()) >= k.amount();
         if (!hasKnow[a]) hasAllRequisites = false;
      }
   }

   private static final class Page {
      final List<Object> contents = new ArrayList<>();

      Page copy() {
         Page p = new Page();
         p.contents.addAll(contents);
         return p;
      }
   }

   /** An image inside research text: "namespace:path:x:y:w:h:scale". */
   private static final class PageImage {
      int x, y, w, h, aw, ah;
      float scale;
      ResourceLocation loc;

      @Nullable
      static PageImage parse(String text) {
         String[] s = text.split(":");
         if (s.length != 7) return null;
         try {
            PageImage pi = new PageImage();
            pi.loc = ResourceLocation.fromNamespaceAndPath(s[0], s[1]);
            pi.x = Integer.parseInt(s[2]);
            pi.y = Integer.parseInt(s[3]);
            pi.w = Integer.parseInt(s[4]);
            pi.h = Integer.parseInt(s[5]);
            pi.scale = Float.parseFloat(s[6]);
            pi.aw = (int) (pi.w * pi.scale);
            pi.ah = (int) (pi.h * pi.scale);
            return pi.ah <= 208 && pi.aw <= 140 ? pi : null;
         } catch (Exception e) {
            return null;
         }
      }
   }
}
