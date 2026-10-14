package thaumcraft.client.golems;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.ISealGui;
import thaumcraft.client.gui.GuiDraw;
import thaumcraft.common.golems.menu.SealMenu;

/**
 * SealBaseGUI: the seal's settings around a ring of category buttons. Changes are made here at once and sent to the
 * server as menu button ids (sendEnchantPacket in 1.12), which applies them and syncs the seal back.
 */
public class SealScreen extends AbstractContainerScreen<SealMenu> {
   private final ISealEntity seal;
   private final int middleX;
   private final int middleY;
   private final List<GolemWidgets.Btn> buttons = new ArrayList<>();

   public SealScreen(SealMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.seal = menu.seal;
      this.imageWidth = 176;
      this.imageHeight = 232;
      this.middleX = imageWidth / 2;
      this.middleY = (imageHeight - 72) / 2 - 8;
   }

   @Override
   protected void init() {
      super.init();
      setupCategories();
   }

   private boolean isOwner() {
      return minecraft != null && minecraft.player != null && seal.getOwner().equals(minecraft.player.getUUID().toString());
   }

   private void setupCategories() {
      buttons.clear();
      int[] categories = menu.categories;
      int c = 0;
      float slice = 60.0F / categories.length;
      float start = -180.0F + (categories.length - 1) * slice / 2.0F;
      if (slice > 24.0F) slice = 24.0F;
      if (slice < 12.0F) slice = 12.0F;
      int cx = leftPos + middleX;
      int cy = topPos + middleY;
      for (int cat : categories) {
         if (categories.length > 1) {
            int xx = (int) (Mth.cos((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
            int yy = (int) (Mth.sin((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
            buttons.add(new GolemWidgets.Category(c, cx + xx, cy + yy, 16, 16, Component.translatable("button.category." + cat), cat,
               menu.category == cat));
         }
         c++;
      }
      int xx = (int) (Mth.cos((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
      int yy = (int) (Mth.sin((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
      buttons.add(new GolemWidgets.Toggle(27, cx + xx - 8, cy + yy - 8, seal::isRedstoneSensitive, 64, 80,
         () -> Component.translatable(seal.isRedstoneSensitive() ? "golem.prop.redon" : "golem.prop.redoff"), true));
      switch (menu.category) {
         case ISealGui.CAT_PRIORITY -> {
            buttons.add(new GolemWidgets.PlusMinus(80, cx - 5 - 14, cy - 17, 10, 10, true));
            buttons.add(new GolemWidgets.PlusMinus(81, cx - 5 + 14, cy - 17, 10, 10, false));
            buttons.add(new GolemWidgets.PlusMinus(82, cx + 18 - 12, cy + 4, 10, 10, true));
            buttons.add(new GolemWidgets.PlusMinus(83, cx + 18 + 11, cy + 4, 10, 10, false));
            buttons.add(new GolemWidgets.Toggle(25, cx - 32, cy, seal::isLocked, 32, 48,
               () -> Component.translatable(seal.isLocked() ? "golem.prop.lock" : "golem.prop.unlock"), false));
         }
         case ISealGui.CAT_FILTER -> {
            if (seal.getSeal() instanceof ISealConfigFilter f) {
               int s = f.getFilterSize();
               int sy = 16 + (s - 1) / 3 * 12;
               buttons.add(new GolemWidgets.Toggle(20, cx - 8, cy + (s - 1) / 3 * 24 - sy + 27, f::isBlacklist, 0, 16,
                  () -> Component.translatable(f.isBlacklist() ? "button.bl" : "button.wl"), false));
            }
         }
         case ISealGui.CAT_AREA -> {
            buttons.add(new GolemWidgets.PlusMinus(90, cx - 5 - 14, cy - 25, 10, 10, true));
            buttons.add(new GolemWidgets.PlusMinus(91, cx - 5 + 14, cy - 25, 10, 10, false));
            buttons.add(new GolemWidgets.PlusMinus(92, cx - 5 - 14, cy, 10, 10, true));
            buttons.add(new GolemWidgets.PlusMinus(93, cx - 5 + 14, cy, 10, 10, false));
            buttons.add(new GolemWidgets.PlusMinus(94, cx - 5 - 14, cy + 25, 10, 10, true));
            buttons.add(new GolemWidgets.PlusMinus(95, cx - 5 + 14, cy + 25, 10, 10, false));
         }
         case ISealGui.CAT_TOGGLES -> {
            if (seal.getSeal() instanceof ISealConfigToggles cp) {
               int n = cp.getToggles().length;
               int s = n < 4 ? 8 : n < 6 ? 7 : n < 9 ? 6 : 5;
               int h = (n - 1) * s;
               int w = 12;
               for (ISealConfigToggles.SealToggle prop : cp.getToggles()) {
                  int ww = (12 + Math.min(100, font.width(Component.translatable(prop.getName())))) / 2;
                  if (ww > w) w = ww;
               }
               int p = 0;
               for (ISealConfigToggles.SealToggle prop : cp.getToggles()) {
                  buttons.add(new GolemWidgets.Prop(30 + p, cx - w, cy - 5 - h + p * s * 2, 8, 8, Component.translatable(prop.getName()),
                     prop::getValue));
                  p++;
               }
            }
         }
         case ISealGui.CAT_TAGS -> {
            EnumGolemTrait[] tags = seal.getSeal() == null ? null : seal.getSeal().getRequiredTags();
            if (tags != null) {
               int p = 0;
               for (EnumGolemTrait tag : tags) {
                  buttons.add(new GolemWidgets.Hover(500 + p, cx + p * 18 - (tags.length - 1) * 9, cy - 8, 16, 16, tag.getLocalizedName(),
                     tag.getLocalizedDescription(), tag.icon));
                  p++;
               }
            }
            tags = seal.getSeal() == null ? null : seal.getSeal().getForbiddenTags();
            if (tags != null) {
               int p = 0;
               for (EnumGolemTrait tag : tags) {
                  buttons.add(new GolemWidgets.Hover(600 + p, cx + p * 18 - (tags.length - 1) * 9, cy + 24, 16, 16, tag.getLocalizedName(),
                     tag.getLocalizedDescription(), tag.icon));
                  p++;
               }
            }
         }
         default -> {
         }
      }
   }

   @Override
   protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
      return false;
   }

   @Override
   protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
      int cx = leftPos + middleX;
      int cy = topPos + middleY;
      GuiDraw.resetColor();
      GolemWidgets.rect(g, GolemWidgets.BASE, cx - 80, cy - 80, 96, 0, 160, 160);
      GolemWidgets.rect(g, GolemWidgets.BASE, leftPos, topPos + 143, 0, 167, 176, 89);
      g.drawCenteredString(font, Component.translatable("button.category." + menu.category), cx, cy - 64, 0xFFFFFF);
      switch (menu.category) {
         case ISealGui.CAT_PRIORITY -> {
            GolemWidgets.rect(g, GolemWidgets.BASE, cx + 17, cy + 3, 2, 18, 12, 12);
            if (seal.getColor() >= 1 && seal.getColor() <= 16) {
               int c = DyeColor.byId(seal.getColor() - 1).getTextureDiffuseColor();
               GuiDraw.color((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F, 1.0F);
               GolemWidgets.rect(g, GolemWidgets.BASE, cx + 20, cy + 6, 74, 31, 6, 6);
               GuiDraw.resetColor();
            }
            int mx = mouseX - leftPos;
            int my = mouseY - topPos;
            if (mx >= middleX + 5 && mx <= middleX + 41 && my >= middleY + 3 && my <= middleY + 15) {
               Component s;
               if (seal.getColor() >= 1 && seal.getColor() <= 16) {
                  s = Component.translatable("golem.prop.color",
                     Component.translatable("color.minecraft." + DyeColor.byId(seal.getColor() - 1).getName()));
               } else {
                  s = Component.translatable("golem.prop.colorall");
               }
               g.drawCenteredString(font, s, cx + 23, cy + 17, 0xFFFFFF);
            }
            g.drawCenteredString(font, Component.translatable("golem.prop.priority"), cx, cy - 28, 0xBBABFF);
            g.drawCenteredString(font, String.valueOf(seal.getPriority()), cx, cy - 16, 0xFFFFFF);
            if (isOwner()) g.drawCenteredString(font, Component.translatable("golem.prop.owner"), cx, cy + 32, 0xBBABFF);
         }
         case ISealGui.CAT_FILTER -> {
            if (seal.getSeal() instanceof ISealConfigFilter f) {
               int s = f.getFilterSize();
               int sx = 16 + (s - 1) % 3 * 12;
               int sy = 16 + (s - 1) / 3 * 12;
               for (int a = 0; a < s; a++) {
                  GolemWidgets.rect(g, GolemWidgets.BASE, cx + a % 3 * 24 - sx, cy + a / 3 * 24 - sy, 0, 56, 32, 32);
               }
            }
         }
         case ISealGui.CAT_AREA -> {
            g.drawCenteredString(font, Component.translatable("button.caption.y"), cx, cy - 24 - 9, 0xDDDDDD);
            g.drawCenteredString(font, Component.translatable("button.caption.x"), cx, cy - 9, 0xDDDDDD);
            g.drawCenteredString(font, Component.translatable("button.caption.z"), cx, cy + 24 - 9, 0xDDDDDD);
            g.drawCenteredString(font, String.valueOf(seal.getArea().getY()), cx, cy - 24, 0xFFFFFF);
            g.drawCenteredString(font, String.valueOf(seal.getArea().getX()), cx, cy, 0xFFFFFF);
            g.drawCenteredString(font, String.valueOf(seal.getArea().getZ()), cx, cy + 24, 0xFFFFFF);
         }
         case ISealGui.CAT_TAGS -> {
            g.drawCenteredString(font, Component.translatable("button.caption.required"), cx, cy - 26, 0xDDDDDD);
            g.drawCenteredString(font, Component.translatable("button.caption.forbidden"), cx, cy + 6, 0xDDDDDD);
         }
         default -> {
         }
      }
      for (GolemWidgets.Btn b : buttons) b.render(g, font, mouseX, mouseY);
      GuiDraw.resetColor();
   }

   @Override
   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
      super.render(g, mouseX, mouseY, pt);
      if (menu.category == ISealGui.CAT_FILTER && seal.getSeal() instanceof ISealConfigFilter cp && !cp.isBlacklist()) {
         g.pose().pushPose();
         g.pose().translate(0, 0, 300);
         for (int i = 0; i < cp.getFilterSize() && i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.isActive() || slot.getItem().isEmpty()) continue;
            int sz = cp.getFilterSlotSize(i);
            String s = sz == 0 ? ChatFormatting.GOLD + "*" : String.valueOf(sz);
            g.drawString(font, s, leftPos + slot.x + 19 - 2 - font.width(s), topPos + slot.y + 6 + 3, 0xFFFFFF, true);
         }
         g.pose().popPose();
      }
      for (GolemWidgets.Btn b : buttons) {
         if (b.isOver(mouseX, mouseY)) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            b.foreground(g, font, mouseX, mouseY);
            g.pose().popPose();
            break;
         }
      }
      renderTooltip(g, mouseX, mouseY);
   }

   @Override
   public boolean mouseClicked(double mx, double my, int button) {
      if (button == 0) {
         for (GolemWidgets.Btn b : new ArrayList<>(buttons)) {
            if (b.clicks(mx, my)) {
               minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
               actionPerformed(b.id);
               return true;
            }
         }
      }
      return super.mouseClicked(mx, my, button);
   }

   private void send(int id) {
      minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
   }

   private void actionPerformed(int id) {
      int[] categories = menu.categories;
      if (id < categories.length && categories[id] != menu.category) {
         menu.clickMenuButton(minecraft.player, id);
         send(id);
         setupCategories();
      } else if (menu.category == ISealGui.CAT_PRIORITY && id == 25 && isOwner()) {
         seal.setLocked(!seal.isLocked());
         send(seal.isLocked() ? 25 : 26);
      } else if (menu.category == ISealGui.CAT_FILTER && seal.getSeal() instanceof ISealConfigFilter cp && id == 20) {
         cp.setBlacklist(!cp.isBlacklist());
         send(cp.isBlacklist() ? 20 : 21);
      } else if (menu.category == ISealGui.CAT_TOGGLES && seal.getSeal() instanceof ISealConfigToggles cp && id >= 30
         && id < 30 + cp.getToggles().length) {
         cp.setToggle(id - 30, !cp.getToggles()[id - 30].getValue());
         send(cp.getToggles()[id - 30].getValue() ? id : id + 30);
      } else if (id == 27 && isOwner()) {
         seal.setRedstoneSensitive(!seal.isRedstoneSensitive());
         send(seal.isRedstoneSensitive() ? 27 : 28);
      } else if (id >= categories.length) {
         send(id);
      }
   }
}
