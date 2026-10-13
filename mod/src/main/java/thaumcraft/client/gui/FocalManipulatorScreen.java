package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.IFocusElement;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.common.blocks.crafting.FocalManipulatorBlockEntity;
import thaumcraft.common.blocks.crafting.FocusElementNode;
import thaumcraft.common.items.casters.FocusItem;
import thaumcraft.common.menu.FocalManipulatorMenu;
import thaumcraft.common.network.CasterPayloads;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.ResearchManager;

/**
 * GuiFocalManipulator: the node-tree editor. The root (the caster) sits at the top; selecting an empty socket lists
 * the elements that can be linked there on the left; the selected node's settings are on the right; the costs and
 * crystals needed at the top right.
 */
public class FocalManipulatorScreen extends AbstractContainerScreen<FocalManipulatorMenu> {
   private static final ResourceLocation TEX = tc("textures/gui/gui_wandtable.png");
   private static final ResourceLocation TEX2 = tc("textures/gui/gui_wandtable2.png");
   private static final ResourceLocation TEX3 = tc("textures/gui/gui_wandtable3.png");
   private static final ResourceLocation TEXBASE = tc("textures/gui/gui_base.png");
   private static final ResourceLocation I_MEDIUM = tc("textures/foci/_medium.png");
   private static final ResourceLocation I_EFFECT = tc("textures/foci/_effect.png");
   private static final DecimalFormat FORMAT = new DecimalFormat("#######.##");

   private final FocalManipulatorBlockEntity table;
   private boolean dragging;
   private EditBox nameField;
   private int totalComplexity;
   private int maxComplexity;
   private int lastNodeHover = -1;
   private final List<String> shownParts = new ArrayList<>();
   private int partsStart;
   private ItemStack[] components;
   private boolean valid;
   private int nodeID;
   private int sMinX, sMinY, sMaxX, sMaxY;
   private int selectedNode = -1;
   private float costCast;
   private int costXp;
   private int costVis;
   private int scrollX, scrollY;
   private Slider scrollbarParts, scrollbarMainSide, scrollbarMainBottom;
   private final List<Spinner> spinners = new ArrayList<>();
   private final List<HoverButton> hoverButtons = new ArrayList<>();
   private boolean confirmActive;

   public FocalManipulatorScreen(FocalManipulatorMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.table = menu.table;
      imageWidth = 231;
      imageHeight = 231;
   }

   private static ResourceLocation tc(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   @Override
   protected void init() {
      super.init();
      nameField = new EditBox(font, leftPos + 30, topPos + 11, 170, 12, Component.empty());
      nameField.setTextColor(-1);
      nameField.setTextColorUneditable(-1);
      nameField.setBordered(false);
      nameField.setMaxLength(50);
      nameField.setResponder(s -> {
         if (!s.equals(table.focusName)) {
            table.focusName = s;
            CasterPayloads.send(new CasterPayloads.FocusName(table.getBlockPos(), s));
         }
      });
      if (table.data.isEmpty() && !table.getFocus().isEmpty()) table.doGuiReset = true;
      gatherInfo(false);
      if (table.focusName.isEmpty() && !table.getFocus().isEmpty()) table.focusName = table.getFocus().getHoverName().getString();
      nameField.setValue(table.focusName);
   }

   /** isPointInRegion with an absolute rectangle and point (1 pixel of slack, as GuiContainer had). */
   private static boolean region(int rx, int ry, int w, int h, double px, double py) {
      return px >= rx - 1 && px < rx + w + 1 && py >= ry - 1 && py < ry + h + 1;
   }

   /** isPointInRegion with a rectangle relative to the screen's corner. */
   private boolean regionRel(int rx, int ry, int w, int h, double px, double py) {
      return region(leftPos + rx, topPos + ry, w, h, px, py);
   }

   // ------------------------------------------------------------------ drawing

   @Override
   public void render(GuiGraphics g, int mx, int my, float pt) {
      super.render(g, mx, my, pt);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      int gx = leftPos, gy = topPos;
      for (Spinner s : spinners) s.render(g, mx, my);
      for (HoverButton b : hoverButtons) b.render(g, mx, my);
      renderConfirm(g, mx, my);
      for (Slider s : new Slider[]{scrollbarParts, scrollbarMainSide, scrollbarMainBottom}) {
         if (s != null) s.render(g, mx, my);
      }
      if (scrollbarMainSide != null) scrollY = Math.round(scrollbarMainSide.getSliderValue());
      if (scrollbarMainBottom != null) scrollX = Math.round(scrollbarMainBottom.getSliderValue());
      if (scrollbarParts != null) partsStart = Math.round(scrollbarParts.getSliderValue());
      int count = 0, index = 0;
      for (String sk : shownParts) {
         if (++count - 1 >= partsStart) {
            if (FocusEngine.getElement(sk) instanceof FocusNode node) {
               g.pose().pushPose();
               g.pose().translate(0, 0, 5);
               drawPart(g, node, gx + 38, 43 + gy + 25 * index, node.getType() == IFocusElement.EnumUnitType.MOD ? 24.0F : 32.0F, 220,
                  region(gx + 28, 32 + gy + 24 * index, 20, 20, mx, my));
               g.pose().popPose();
            }
            if (++index > 5) break;
         }
      }
      count = 0;
      index = 0;
      for (String skx : shownParts) {
         if (++count - 1 >= partsStart) {
            if (FocusEngine.getElement(skx) instanceof FocusNode node && region(gx + 28, 33 + gy + 25 * index, 20, 20, mx, my)) {
               hoverText(g, genPartText(node, -1), gx + 44, 46 + gy + 24 * index);
            }
            if (++index > 5) break;
         }
      }
      if (lastNodeHover >= 0) {
         FocusElementNode fn = table.data.get(lastNodeHover);
         if (fn != null && fn.node != null) {
            hoverText(g, genPartText(fn.node, lastNodeHover), leftPos + 140 - scrollX + fn.x * 24, topPos + 50 - scrollY + fn.y * 32);
         }
      }
      confirmActive = table.vis <= 0.0F && valid;
      if (!table.data.isEmpty()) nameField.render(g, mx, my, pt);
      for (HoverButton b : hoverButtons) {
         if (b.isHovered(mx, my)) b.renderTooltip(g, mx, my);
      }
      if (confirmHovered(mx, my)) {
         GuiDraw.tooltip(g, font, List.of(Component.translatable("wandtable.text3")), mx + 4, my + 8);
      }
      renderTooltip(g, mx, my);
   }

   private void hoverText(GuiGraphics g, List<Component> lines, int x, int y) {
      int w = Math.max(40, width - (leftPos + imageWidth - 16));
      List<FormattedCharSequence> rows = new ArrayList<>();
      for (Component c : lines) rows.addAll(font.split(c, w));
      g.pose().pushPose();
      g.pose().translate(0, 0, 300);
      g.renderTooltip(font, rows, x, y);
      g.pose().popPose();
   }

   private List<Component> genPartText(FocusNode node, int idx) {
      List<Component> list = new ArrayList<>();
      FocusElementNode placed = idx >= 0 ? table.data.get(idx) : null;
      list.add(Component.translatable(node.getUnlocalizedName()));
      list.add(Component.translatable(node.getUnlocalizedText()).withStyle(ChatFormatting.DARK_PURPLE));
      int c = node.getComplexity();
      if (placed != null) c = (int) (node.getComplexity() * placed.complexityMultiplier);
      MutableComponent com = Component.translatable("focuspart.com").withStyle(ChatFormatting.GOLD);
      com.append(Component.literal(" " + c).withStyle(placed != null && placed.complexityMultiplier > 1.0F ? ChatFormatting.RED : ChatFormatting.GOLD));
      list.add(com);
      float p = placed != null ? placed.getPower(table.data) : node.getPowerMultiplier();
      if (p != 1.0F) {
         list.add(Component.translatable("focuspart.eff").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(" x" + FORMAT.format(p)).withStyle(p < 1.0F ? ChatFormatting.RED : ChatFormatting.GREEN)));
      }
      if (node instanceof FocusEffect fe) {
         float d = fe.getDamageForDisplay(placed == null ? 1.0F : placed.getPower(table.data));
         if (d > 0.0F) {
            list.add(Component.translatable("attribute.modifier.equals.0", FORMAT.format(d), Component.translatable("attribute.name.generic.attack_damage"))
               .withStyle(ChatFormatting.DARK_RED));
         } else if (d < 0.0F) {
            list.add(Component.translatable("attribute.modifier.equals.0", FORMAT.format(-d), Component.translatable("focus.heal.power"))
               .withStyle(ChatFormatting.DARK_GREEN));
         }
      }
      return list;
   }

   @Override
   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
   }

   @Override
   protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
      int k = leftPos, l = topPos;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      g.blit(TEX2, k, l, 0, 0, imageWidth, imageHeight);
      g.blit(TEX3, k - 71, l - 3, 0, 0, 71, 239);
      if (table.getFocus().isEmpty() || table.doGuiReset) {
         if (table.doGuiReset && !table.getFocus().isEmpty()) {
            resetNodes();
         } else {
            table.data.clear();
            gatherInfo(false);
         }
         table.focusName = "";
         if (!table.getFocus().isEmpty()) {
            table.focusName = table.getFocus().getHoverName().getString();
            nameField.setValue(table.focusName);
         }
         table.doGuiReset = false;
      }
      if (table.doGather) {
         gatherInfo(false);
         table.doGather = false;
      }
      drawNodes(g, leftPos + 132 - scrollX, topPos + 48 - scrollY, mx, my);
      g.pose().pushPose();
      g.pose().translate(0, 0, 1);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      g.blit(TEX, k, l, 0, 0, imageWidth, imageHeight);
      if (maxComplexity > 0) {
         g.drawString(font, totalComplexity + "/" + maxComplexity, k + 242, l + 36, totalComplexity > maxComplexity ? 16151160 : 16761407, true);
      }
      g.drawString(font, "" + costXp, k + 242, l + 50, costXp > minecraft.player.experienceLevel ? 16151160 : 10092429, true);
      int v = table.vis > 0.0F ? (int) table.vis : costVis;
      g.drawString(font, Component.literal("" + v).withStyle(ChatFormatting.AQUA), k + 242, l + 64, 10092429, true);
      if (costCast > 0.0F) {
         g.drawString(font, Component.translatable("item.Focus.cost1").append(": " + FORMAT.format(costCast)).withStyle(ChatFormatting.AQUA), k + 230, l + 80,
            10092429, true);
      }
      if (components != null && components.length > 0) {
         g.drawString(font, Component.translatable("wandtable.text4").withStyle(ChatFormatting.GOLD), k + 230, l + 92, 10092429, true);
      }
      if (table.focusName != null && !table.data.isEmpty()) {
         g.blit(TEXBASE, leftPos + 24, topPos + 8, 192, 224, 8, 14);
         int a;
         for (a = 1; a < 22; a++) g.blit(TEXBASE, leftPos + 24 + a * 8, topPos + 8, 200, 224, 8, 14);
         g.blit(TEXBASE, leftPos + 24 + a * 8, topPos + 8, 208, 224, 8, 14);
      }
      g.pose().popPose();
   }

   private void drawClippedRect(GuiGraphics g, int x, int y, int sx, int sy, int w, int h) {
      if (regionRel(48, 26, 166, 174, x + w / 2, y + h / 2)) g.blit(TEX, x, y, sx, sy, w, h);
   }

   private void drawNodes(GuiGraphics g, int x, int y, int mx, int my) {
      if (table.data.isEmpty()) return;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      int hover = -1;
      for (FocusElementNode fn : table.data.values()) {
         int xx = x + fn.x * 24;
         int yy = y + fn.y * 32;
         boolean mouseover = region(leftPos + 63, topPos + 31, 136, 160, mx, my) && region(xx - 10, yy - 10, 20, 20, mx, my);
         if (mouseover && fn.parent >= 0) hover = fn.id;
         if (fn.node != null) {
            if (regionRel(48, 16, 154, 192, xx - 8, yy - 8)) drawPart(g, fn.node, xx, yy, 32.0F, 220, mouseover);
         } else {
            drawClippedRect(g, xx - 12, yy - 12, 120, 232, 24, 24);
         }
         if (selectedNode == fn.id || mouseover && fn.parent >= 0) drawClippedRect(g, xx - 12, yy - 12, 96, 232, 24, 24);
         FocusElementNode parent = table.data.get(fn.parent);
         if (parent != null) {
            drawClippedRect(g, xx - 6, yy - 22, 54, 232, 12, 12);
            if (parent.node instanceof FocusModSplit) {
               int q = Math.abs(fn.x - parent.x);
               for (int a = 0; a < q; a++) {
                  if (fn.x < parent.x) {
                     if (a == 0) drawClippedRect(g, xx - 4, yy - 36, 8, 240, 16, 16);
                     else drawClippedRect(g, xx - 12 + a * 24, yy - 36, 72, 240, 24, 16);
                  } else if (a == 0) {
                     drawClippedRect(g, xx - 12, yy - 36, 24, 240, 16, 16);
                  } else {
                     drawClippedRect(g, xx - 12 - a * 24, yy - 36, 72, 240, 24, 16);
                  }
               }
            }
            if (fn.node == null) {
               int s = parent.target && parent.trajectory ? 4 : 0;
               if (regionRel(48, 16, 168, 192, xx - 4, yy - 4)) {
                  if (parent.target) {
                     g.pose().pushPose();
                     g.pose().translate(xx - s, yy, 0);
                     g.pose().scale(0.5F, 0.5F, 1.0F);
                     g.blit(TEX, -8, -8, 152, 240, 16, 16);
                     g.pose().popPose();
                  }
                  if (parent.trajectory) {
                     g.pose().pushPose();
                     g.pose().translate(xx + s, yy, 0);
                     g.pose().scale(0.5F, 0.5F, 1.0F);
                     g.blit(TEX, -8, -8, 168, 240, 16, 16);
                     g.pose().popPose();
                  }
               }
            }
         }
      }
      if (hover >= 0 && lastNodeHover != hover) {
         playSound(ModSounds.CLACK.get(), 2.0F);
         lastNodeHover = hover;
      }
      if (hover < 0) lastNodeHover = -1;
      if (selectedNode >= 0) drawNodeSettings(g, selectedNode);
   }

   private void drawNodeSettings(GuiGraphics g, int selected) {
      FocusElementNode fn = table.data.get(selected);
      if (fn == null || fn.node == null || fn.node.getSettingList().isEmpty()) return;
      int a = 0;
      for (String sk : fn.node.getSettingList()) {
         NodeSetting setting = fn.node.getSetting(sk);
         if (setting.getResearch() == null || ResearchManager.knowsResearchStrict(minecraft.player, setting.getResearch())) {
            int x = leftPos + imageWidth;
            int y = topPos + imageHeight - 10 - fn.node.getSettingList().size() * 26 + a * 26;
            g.drawString(font, setting.getLocalizedName().copy().withStyle(ChatFormatting.GOLD), x, y, 0xFFFFFF, true);
            a++;
         }
      }
   }

   /** GuiFocalManipulator.drawPart: an element's icon on its coloured medium/effect backing (modifiers and the root bigger). */
   public static void drawPart(GuiGraphics g, FocusNode node, int x, int y, float scale, int bright, boolean mouseover) {
      g.pose().pushPose();
      g.pose().translate(x, y, 0);
      g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
      boolean root = node.getType() == IFocusElement.EnumUnitType.MOD || node.getKey().equals("thaumcraft.ROOT");
      if (root) scale *= 2.0F;
      int color = FocusEngine.getElementColor(node.getKey());
      float r = (color >> 16 & 255) / 255.0F, gr = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
      switch (node.getType()) {
         case EFFECT -> quadCentered(g, I_EFFECT, (float) (scale * 0.9 + (mouseover ? 2 : 0)), r, gr, b, 1.0F);
         case MEDIUM -> {
            if (!root) quadCentered(g, I_MEDIUM, (float) (scale * 0.9 + (mouseover ? 2 : 0)), r, gr, b, 1.0F);
         }
         default -> {
         }
      }
      g.pose().translate(0, 0, 1);
      ResourceLocation icon = FocusEngine.getElementIcon(node.getKey());
      if (icon != null) quadCentered(g, icon, scale / 2.0F + (mouseover ? 2 : 0), 1.0F, 1.0F, 1.0F, 1.0F);
      g.pose().popPose();
   }

   /** UtilsFX.renderQuadCentered in a GUI: a quad of the given size centred on the origin. */
   private static void quadCentered(GuiGraphics g, ResourceLocation tex, float size, float r, float gr, float b, float a) {
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, tex);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      Matrix4f m = g.pose().last().pose();
      float h = size / 2.0F;
      BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      bb.addVertex(m, -h, h, 0).setUv(1, 1).setColor(r, gr, b, a);
      bb.addVertex(m, h, h, 0).setUv(1, 0).setColor(r, gr, b, a);
      bb.addVertex(m, h, -h, 0).setUv(0, 0).setColor(r, gr, b, a);
      bb.addVertex(m, -h, -h, 0).setUv(0, 1).setColor(r, gr, b, a);
      BufferUploader.drawWithShader(bb.buildOrThrow());
   }

   private void renderConfirm(GuiGraphics g, int mx, int my) {
      int cx = leftPos + 242, cy = topPos + 18;
      float cc = confirmHovered(mx, my) ? 1.0F : 0.9F;
      float ac = 1.0F;
      if (!confirmActive) {
         cc = 0.5F;
         ac = 0.9F;
      }
      RenderSystem.enableBlend();
      GuiDraw.color(cc, cc, cc, ac);
      g.blit(TEXBASE, cx - 12, cy - 8, 232, 240, 24, 16);
      GuiDraw.resetColor();
      g.pose().pushPose();
      g.pose().translate(cx, cy, 0);
      g.pose().scale(0.5F, 0.5F, 1.0F);
      g.drawCenteredString(font, Component.translatable("wandtable.text3"), 0, -4, confirmHovered(mx, my) ? 16777120 : 16777215);
      g.pose().popPose();
   }

   private boolean confirmHovered(double mx, double my) {
      int cx = leftPos + 242, cy = topPos + 18;
      return mx >= cx - 12 && my >= cy - 8 && mx < cx + 12 && my < cy + 8;
   }

   private void playSound(SoundEvent sound, float pitch) {
      minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 0.4F));
   }

   // ------------------------------------------------------------------ input

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      for (Slider s : new Slider[]{scrollbarParts, scrollbarMainSide, scrollbarMainBottom}) {
         if (s != null && s.mousePressed(mx, my)) return true;
      }
      if (button == 0 && confirmActive && confirmHovered(mx, my)) {
         minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
         return true;
      }
      for (Spinner s : spinners) {
         if (s.mousePressed(mx, my)) {
            gatherInfo(true);
            return true;
         }
      }
      dragging = button == 0 && region(leftPos + 63, topPos + 31, 136, 160, mx, my);
      boolean handled = super.mouseClicked(mx, my, button);
      int gx = leftPos, gy = topPos;
      if (table.vis <= 0.0F && !table.data.isEmpty()) {
         nameField.setFocused(nameField.isMouseOver(mx, my));
         if (nameField.isMouseOver(mx, my)) nameField.mouseClicked(mx, my, button);
         if (lastNodeHover >= 0) {
            selectedNode = lastNodeHover;
            if (button == 1 && table.data.get(selectedNode).node != null) {
               FocusElementNode fn = table.data.get(selectedNode);
               FocusElementNode parent = table.data.get(fn.parent);
               if (parent != null && parent.node != null) addNodeAt(parent.node.getKey(), fn.parent, true);
            }
            gatherInfo(false);
            playSound(ModSounds.CLACK.get(), 1.0F);
         }
         int count = 0, index = 0;
         if (selectedNode >= 0) {
            for (String sk : new ArrayList<>(shownParts)) {
               if (++count - 1 >= partsStart) {
                  if (region(gx + 28, gy + 32 + 24 * index, 20, 20, mx, my)) {
                     addNodeAt(sk, selectedNode, true);
                     return true;
                  }
                  if (++index > 5) break;
               }
            }
         }
      }
      return handled;
   }

   @Override
   public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
      for (Slider s : new Slider[]{scrollbarParts, scrollbarMainSide, scrollbarMainBottom}) {
         if (s != null && s.isMouseDown) {
            s.drag(mx, my);
            return true;
         }
      }
      if (dragging && button == 0) {
         if (scrollbarMainBottom != null) {
            scrollX = (int) Math.max(scrollbarMainBottom.getMin(), Math.min(scrollbarMainBottom.getMax(), scrollX - dx));
            scrollbarMainBottom.setSliderValue(scrollX);
         }
         if (scrollbarMainSide != null) {
            scrollY = (int) Math.max(scrollbarMainSide.getMin(), Math.min(scrollbarMainSide.getMax(), scrollY - dy));
            scrollbarMainSide.setSliderValue(scrollY);
         }
         return true;
      }
      return super.mouseDragged(mx, my, button, dx, dy);
   }

   @Override
   public boolean mouseReleased(double mx, double my, int button) {
      dragging = false;
      for (Slider s : new Slider[]{scrollbarParts, scrollbarMainSide, scrollbarMainBottom}) {
         if (s != null) s.isMouseDown = false;
      }
      return super.mouseReleased(mx, my, button);
   }

   @Override
   public boolean mouseScrolled(double mx, double my, double sx, double sy) {
      if (scrollbarParts != null && shownParts.size() > 6 && regionRel(24, 24, 32, 157, mx, my)) {
         if (sy > 0 && partsStart > 0) partsStart--;
         else if (sy < 0 && partsStart < shownParts.size() - 6) partsStart++;
         scrollbarParts.setSliderValue(partsStart);
         return true;
      }
      return super.mouseScrolled(mx, my, sx, sy);
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (nameField.isFocused()) {
         if (keyCode == GLFW.GLFW_KEY_ESCAPE) return super.keyPressed(keyCode, scanCode, modifiers);
         nameField.keyPressed(keyCode, scanCode, modifiers);
         return true;
      }
      return super.keyPressed(keyCode, scanCode, modifiers);
   }

   @Override
   public boolean charTyped(char c, int modifiers) {
      if (nameField.isFocused() && nameField.charTyped(c, modifiers)) return true;
      return super.charTyped(c, modifiers);
   }

   // ------------------------------------------------------------------ tree editing

   private int getNextId() {
      while (table.data.containsKey(nodeID)) nodeID++;
      return nodeID;
   }

   private void cullChildren(int idx) {
      FocusElementNode n = table.data.get(idx);
      if (n == null) return;
      for (int i : n.children) {
         cullChildren(i);
         table.data.remove(i);
      }
   }

   /** Puts the element with this key at the node (replacing what was there and its branch) and adds empty sockets below. */
   private List<Integer> addNodeAt(String key, int idx, boolean gather) {
      List<Integer> ret = new ArrayList<>();
      boolean same = false;
      FocusElementNode previous = null;
      if (table.data.containsKey(idx)) {
         cullChildren(idx);
         FocusElementNode at = table.data.get(idx);
         if (at.node != null && at.node.getKey().equals(key)) same = true;
         else previous = table.data.remove(idx);
      }
      Supplier<? extends IFocusElement> sup = FocusEngine.elements.get(key);
      if (sup == null || !(sup.get() instanceof FocusNode)) return ret;
      FocusElementNode fn;
      FocusNode node;
      if (!same) {
         fn = new FocusElementNode();
         node = (FocusNode) sup.get();
         fn.node = node;
         if (previous != null) {
            fn.y = previous.y;
            fn.x = previous.x;
         }
         fn.id = getNextId();
         ret.add(fn.id);
         selectedNode = fn.id;
         if (previous != null && table.data.containsKey(previous.parent)) {
            fn.parent = previous.parent;
            int[] c = table.data.get(previous.parent).children;
            for (int a = 0; a < c.length; a++) {
               if (c[a] == previous.id) {
                  c[a] = fn.id;
                  break;
               }
            }
         }
         fn.target = node.canSupply(FocusNode.EnumSupplyType.TARGET);
         fn.trajectory = node.canSupply(FocusNode.EnumSupplyType.TRAJECTORY);
         table.data.put(nodeID, fn);
      } else {
         fn = table.data.get(idx);
         node = fn.node;
      }
      if (fn.target || fn.trajectory) {
         if (node instanceof FocusModSplit) {
            FocusElementNode blank1 = new FocusElementNode();
            blank1.parent = fn.id;
            blank1.id = getNextId();
            ret.add(nodeID);
            blank1.x = fn.x - 1;
            blank1.y = fn.y + 1;
            table.data.put(nodeID, blank1);
            selectedNode = nodeID;
            FocusElementNode blank2 = new FocusElementNode();
            blank2.parent = fn.id;
            blank2.x = fn.x + 1;
            blank2.y = fn.y + 1;
            blank2.id = getNextId();
            ret.add(nodeID);
            table.data.put(nodeID, blank2);
            fn.children = new int[]{blank1.id, blank2.id};
         } else {
            FocusElementNode blank = new FocusElementNode();
            blank.parent = fn.id;
            blank.x = fn.x;
            blank.y = fn.y + 1;
            blank.id = getNextId();
            ret.add(nodeID);
            table.data.put(nodeID, blank);
            fn.children = new int[]{blank.id};
            selectedNode = nodeID;
         }
      }
      if (gather) {
         calcNodeTreeLayout();
         gatherInfo(true);
      }
      return ret;
   }

   private void processLeftNodes(FocusElementNode start, int[] bounds) {
      if (start.children.length > 0) processLeftNodes(table.data.get(start.children[0]), bounds);
      int ox = 0;
      if (start.children.length == 1) {
         ox = bounds[0] - 1;
         bounds[0] = table.data.get(start.children[0]).x;
      }
      start.x = bounds[0];
      if (start.children.length == 1) bounds[0] = ox;
      bounds[0]++;
      if (start.children.length > 1) processLeftNodes(table.data.get(start.children[1]), bounds);
   }

   private void moveNodes(FocusElementNode start, int amt) {
      for (int ci : start.children) moveNodes(table.data.get(ci), amt);
      start.x -= amt;
   }

   private void calcNodeTreeLayout() {
      int fsi = -1;
      for (FocusElementNode node : table.data.values()) {
         if (fsi < 0 && node.node instanceof FocusModSplit) fsi = node.id;
      }
      if (fsi >= 0) {
         int[] bounds = {0};
         processLeftNodes(table.data.get(fsi), bounds);
         moveNodes(table.data.get(fsi), bounds[0] / 2);
      }
      for (FocusElementNode nodex : table.data.values()) {
         if (nodex.node instanceof FocusModSplit) {
            FocusElementNode parent = table.data.get(nodex.parent);
            if (parent != null && parent.node != null && !(parent.node instanceof FocusModSplit)) {
               nodex.x = parent.x;
            } else {
               int xx = 0;
               for (int a : nodex.children) xx += table.data.get(a).x;
               nodex.x = xx / nodex.children.length;
            }
         }
      }
      if (selectedNode >= 0 && !table.data.containsKey(selectedNode)) selectedNode = -1;
   }

   private void resetNodes() {
      nodeID = 0;
      table.data.clear();
      List<Integer> r1 = addNodeAt("ROOT", 0, false);
      if (r1.size() > 1) selectedNode = r1.get(1);
      calcNodeTreeLayout();
      gatherInfo(true);
   }

   private void calcScrollBounds() {
      sMinX = sMinY = sMaxX = sMaxY = 0;
      for (FocusElementNode fn : table.data.values()) {
         sMinX = Math.min(sMinX, fn.x);
         sMinY = Math.min(sMinY, fn.y);
         sMaxX = Math.max(sMaxX, fn.x);
         sMaxY = Math.max(sMaxY, fn.y);
      }
   }

   private boolean knows(String research) {
      return research == null || ResearchManager.knowsResearchStrict(minecraft.player, research);
   }

   /** The elements that can go in the selected socket: they need what the parent supplies (exclusive ones once only). */
   private void gatherPartsList() {
      shownParts.clear();
      if (minecraft == null || selectedNode < 0 || !table.data.containsKey(selectedNode)) return;
      partsStart = 0;
      List<String> pMed = new ArrayList<>(), pEff = new ArrayList<>(), pMod = new ArrayList<>(), excluded = new ArrayList<>();
      boolean hasExlusive = false;
      boolean hasMedium = false;
      for (FocusElementNode fn : table.data.values()) {
         if (fn.node instanceof FocusMedium fm) {
            hasMedium = !fm.getKey().equals("ROOT");
            if (fm.isExclusive()) {
               hasExlusive = true;
               break;
            }
         }
         if (fn.node != null && fn.node.isExclusive()) excluded.add(fn.node.getKey());
      }
      FocusElementNode node = table.data.get(selectedNode);
      FocusElementNode parent = table.data.get(node.parent);
      if (parent != null && parent.node != null) {
         for (String key : FocusEngine.elements.keySet()) {
            IFocusElement element = FocusEngine.getElement(key);
            if (!(element instanceof FocusNode fn) || !knows(element.getResearch()) || element.getKey().equals("thaumcraft.ROOT")) continue;
            if (excluded.contains(fn.getKey()) || fn.mustBeSupplied() == null) continue;
            for (FocusNode.EnumSupplyType type : fn.mustBeSupplied()) {
               if (parent.node.canSupply(type)) {
                  switch (element.getType()) {
                     case EFFECT -> pEff.add(key);
                     case MEDIUM -> {
                        if (!hasExlusive && (!((FocusMedium) element).isExclusive() || !hasMedium)) pMed.add(key);
                     }
                     case MOD -> pMod.add(key);
                     default -> {
                     }
                  }
                  break;
               }
            }
         }
      }
      Collections.sort(pMed);
      Collections.sort(pEff);
      Collections.sort(pMod);
      shownParts.addAll(pMed);
      shownParts.addAll(pEff);
      shownParts.addAll(pMod);
   }

   /** Rebuilds the buttons, costs, part list and scroll bars; with sync, sends the tree to the server. */
   private void gatherInfo(boolean sync) {
      spinners.clear();
      hoverButtons.clear();
      hoverButtons.add(new HoverButton(leftPos + 241, topPos + 39, 32, 16, Component.translatable("wandtable.text5"), tc("textures/gui/complex.png"), null));
      hoverButtons.add(new HoverButton(leftPos + 241, topPos + 53, 32, 16, Component.translatable("wandtable.text2"), tc("textures/gui/costxp.png"), null));
      hoverButtons.add(new HoverButton(leftPos + 241, topPos + 67, 32, 16, Component.translatable("wandtable.text1"), tc("textures/gui/costvis.png"), null));
      FocusElementNode sel = table.data.get(selectedNode);
      if (sel != null && sel.node != null && !sel.node.getSettingList().isEmpty()) {
         int a = 0;
         for (String sk : sel.node.getSettingList()) {
            NodeSetting setting = sel.node.getSetting(sk);
            if (knows(setting.getResearch())) {
               int x = leftPos + imageWidth;
               int y = topPos + imageHeight + 3 - sel.node.getSettingList().size() * 26 + a * 26;
               spinners.add(new Spinner(x, y, setting.getType() instanceof NodeSetting.NodeSettingIntList ? 72 : 32, setting));
               a++;
            }
         }
      }
      shownParts.clear();
      components = null;
      if (table.getFocus().isEmpty()) return;
      Map<String, Integer> compCount = new HashMap<>();
      totalComplexity = 0;
      maxComplexity = table.getFocus().getItem() instanceof FocusItem fi ? fi.getMaxComplexity() : 0;
      boolean emptyNodes = false;
      AspectList crystals = new AspectList();
      for (FocusElementNode node : table.data.values()) {
         if (node.node != null) {
            int a = compCount.getOrDefault(node.node.getKey(), 0);
            node.complexityMultiplier = 0.5F * (++a + 1);
            compCount.put(node.node.getKey(), a);
            totalComplexity = (int) (totalComplexity + node.node.getComplexity() * node.complexityMultiplier);
            if (node.node.getAspect() != null) crystals.add(node.node.getAspect(), 1);
         } else {
            emptyNodes = true;
         }
      }
      costCast = totalComplexity / 5.0F;
      costVis = totalComplexity * 10 + maxComplexity / 5;
      // The original showed min(1, ...) here; the server charges max(1, sqrt(complexity)) levels, so show that.
      costXp = (int) Math.max(1L, Math.round(Math.sqrt(totalComplexity)));
      boolean validCrystals = false;
      if (crystals.getAspects().length > 0) {
         validCrystals = true;
         Aspect[] as = crystals.getAspects();
         components = new ItemStack[as.length];
         for (int r = 0; r < as.length; r++) {
            components[r] = FocalManipulatorBlockEntity.crystal(as[r], crystals.getAmount(as[r]));
            if (FocalManipulatorBlockEntity.countCrystals(minecraft.player, as[r]) < crystals.getAmount(as[r])) validCrystals = false;
         }
         int i = 0, q = 0;
         for (ItemStack stack : components) {
            hoverButtons.add(new HoverButton(leftPos + 234 + i * 16, topPos + 110 + 16 * q, 16, 16, stack.getHoverName(), null, stack));
            if (++i > 4) {
               i = 0;
               q++;
            }
         }
      }
      gatherPartsList();
      scrollbarParts = null;
      if (shownParts.size() > 6) {
         scrollbarParts = new Slider(leftPos + 51, topPos + 30, 8, 149, 0.0F, shownParts.size() - 6, 0.0F, true);
      }
      valid = totalComplexity <= maxComplexity && !emptyNodes && validCrystals
         && (costXp <= minecraft.player.experienceLevel || minecraft.player.getAbilities().instabuild);
      calcScrollBounds();
      if (scrollY > (sMaxY - 3) * 32) scrollY = (sMaxY - 3) * 32;
      if (scrollX > sMaxX * 24) scrollX = sMaxX * 24;
      if (scrollX < sMinX * 24) scrollX = sMinX * 24;
      scrollbarMainSide = null;
      scrollbarMainBottom = null;
      if (sMaxY * 32 > 130) {
         scrollbarMainSide = new Slider(leftPos + 203, topPos + 32, 8, 156, 0.0F, (sMaxY - 3) * 32, scrollY, true);
      } else {
         scrollY = 0;
      }
      if (sMinX * 24 >= -70 && sMaxX * 24 <= 70) {
         scrollX = 0;
      } else {
         scrollbarMainBottom = new Slider(leftPos + 64, topPos + 195, 132, 8, sMinX * 24, sMaxX * 24, scrollX, false);
      }
      if (table.focusName.isEmpty()) table.focusName = table.getFocus().getHoverName().getString();
      if (!nameField.getValue().equals(table.focusName)) nameField.setValue(table.focusName);
      if (sync) CasterPayloads.send(CasterPayloads.FocusNodes.of(table.getBlockPos(), table.data, table.focusName));
   }

   // ------------------------------------------------------------------ widgets

   /** GuiSliderTC: a thin bar with a square knob. */
   private static final class Slider {
      final int x, y, w, h;
      final float min, max;
      final boolean vertical;
      float sliderPosition;
      boolean isMouseDown;

      Slider(int x, int y, int w, int h, float min, float max, float value, boolean vertical) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.min = min;
         this.max = max;
         this.vertical = vertical;
         this.sliderPosition = max == min ? 0.0F : (value - min) / (max - min);
      }

      float getMin() {
         return min;
      }

      float getMax() {
         return max;
      }

      float getSliderValue() {
         return min + (max - min) * sliderPosition;
      }

      void setSliderValue(float value) {
         sliderPosition = max == min ? 0.0F : (value - min) / (max - min);
      }

      boolean mousePressed(double mx, double my) {
         if (mx >= x && my >= y && mx < x + w && my < y + h) {
            drag(mx, my);
            isMouseDown = true;
            return true;
         }
         return false;
      }

      void drag(double mx, double my) {
         sliderPosition = vertical ? (float) (my - (y + 4)) / (h - 8) : (float) (mx - (x + 4)) / (w - 8);
         sliderPosition = Math.max(0.0F, Math.min(1.0F, sliderPosition));
      }

      void render(GuiGraphics g, int mx, int my) {
         RenderSystem.enableBlend();
         g.pose().pushPose();
         if (vertical) {
            g.pose().translate(x + 2, y, 0);
            g.pose().scale(1.0F, h / 32.0F, 1.0F);
            g.blit(TEXBASE, 0, 0, 240, 176, 4, 32);
         } else {
            g.pose().translate(x, y + 2, 0);
            g.pose().scale(w / 32.0F, 1.0F, 1.0F);
            g.blit(TEXBASE, 0, 0, 208, 176, 32, 4);
         }
         g.pose().popPose();
         if (vertical) g.blit(TEXBASE, x, y + (int) (sliderPosition * (h - 8)), 20, 20, 8, 8);
         else g.blit(TEXBASE, x + (int) (sliderPosition * (w - 8)), y, 20, 20, 8, 8);
      }
   }

   /** GuiFocusSettingSpinnerButton: "< value >" for a node setting. */
   private final class Spinner {
      final int x, y, w, h = 10;
      final NodeSetting setting;

      Spinner(int x, int y, int w, NodeSetting setting) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.setting = setting;
      }

      void render(GuiGraphics g, int mx, int my) {
         boolean hovered = mx >= x && my >= y && mx < x + w + 10 && my < y + h;
         RenderSystem.enableBlend();
         GuiDraw.color(hovered ? 1.0F : 0.9F, hovered ? 1.0F : 0.9F, hovered ? 1.0F : 0.9F, hovered ? 1.0F : 0.9F);
         g.blit(TEXBASE, x, y, 20, 0, 10, 10);
         g.blit(TEXBASE, x + w, y, 30, 0, 10, 10);
         GuiDraw.resetColor();
         Component s = setting.getValueText();
         g.drawString(font, s, x + (w + 10) / 2 - font.width(s) / 2, y + 1, 0xFFFFFF, true);
      }

      boolean mousePressed(double mx, double my) {
         if (mx >= x && my >= y && mx < x + 10 && my < y + h) {
            setting.decrement();
            return true;
         }
         if (mx >= x + w && my >= y && mx < x + w + 10 && my < y + h) {
            setting.increment();
            return true;
         }
         return false;
      }
   }

   /** GuiHoverButton: an icon (texture or item) that only shows a tooltip. */
   private final class HoverButton {
      final int x, y, w, h;
      final Component text;
      final ResourceLocation tex;
      final ItemStack stack;

      HoverButton(int x, int y, int w, int h, Component text, ResourceLocation tex, ItemStack stack) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.text = text;
         this.tex = tex;
         this.stack = stack;
      }

      boolean isHovered(double mx, double my) {
         return mx >= x - w / 2 && my >= y - h / 2 && mx < x - w / 2 + w && my < y - h / 2 + h;
      }

      void render(GuiGraphics g, int mx, int my) {
         boolean k = isHovered(mx, my);
         if (tex != null) {
            RenderSystem.enableBlend();
            GuiDraw.color(k ? 1.0F : 0.9F, k ? 1.0F : 0.9F, k ? 1.0F : 0.9F, k ? 1.0F : 0.9F);
            g.blit(tex, x - w / 2, y - h / 2, 0, 0, 16, 16, 16, 16);
            GuiDraw.resetColor();
         }
         if (stack != null) {
            g.renderItem(stack, x - w / 2, y - h / 2 - (k ? 1 : 0));
            g.renderItemDecorations(font, stack, x - w / 2, y - h / 2 - (k ? 1 : 0));
         }
      }

      void renderTooltip(GuiGraphics g, int mx, int my) {
         if (stack != null) {
            List<Component> lines = stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.Default.NORMAL);
            g.renderComponentTooltip(font, lines, mx + 4, my + 8);
         } else {
            GuiDraw.tooltip(g, font, List.of(text), mx + 4, my + 8);
         }
      }
   }
}
