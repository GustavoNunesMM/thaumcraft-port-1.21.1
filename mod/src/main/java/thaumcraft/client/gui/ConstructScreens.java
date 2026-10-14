package thaumcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.construct.ArcaneBoreEntity;
import thaumcraft.common.entities.construct.TurretCrossbowAdvancedEntity;
import thaumcraft.common.menu.ArcaneBoreMenu;
import thaumcraft.common.menu.ConstructMenu;
import thaumcraft.common.menu.TurretAdvancedMenu;
import thaumcraft.common.menu.TurretBasicMenu;

/** GuiTurretBasic, GuiTurretAdvanced and GuiArcaneBore. */
public final class ConstructScreens {
   private ConstructScreens() {
   }

   private static ResourceLocation tex(String name) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/" + name + ".png");
   }

   /** The shared frame: background and the construct's health bar at (hx, 59). */
   abstract static class Base<M extends ConstructMenu<?>> extends AbstractContainerScreen<M> {
      protected final ResourceLocation tex;
      private final int healthX;

      Base(M menu, Inventory inv, Component title, ResourceLocation tex, int healthX) {
         super(menu, inv, title);
         this.tex = tex;
         this.healthX = healthX;
         imageWidth = 175;
         imageHeight = 232;
      }

      @Override
      public void render(GuiGraphics g, int mx, int my, float pt) {
         super.render(g, mx, my, pt);
         renderTooltip(g, mx, my);
      }

      @Override
      protected void renderLabels(GuiGraphics g, int mx, int my) {
      }

      @Override
      protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
         int k = leftPos, l = topPos;
         GuiDraw.rect(g, tex, k, l, 0, 0, imageWidth, imageHeight);
         if (menu.entity != null) {
            int h = (int) (39.0F * (menu.entity.getHealth() / menu.entity.getMaxHealth()));
            GuiDraw.rect(g, tex, k + healthX, l + 59, 192, 48, h, 6);
         }
      }
   }

   public static class TurretBasic extends Base<TurretBasicMenu> {
      public TurretBasic(TurretBasicMenu menu, Inventory inv, Component title) {
         super(menu, inv, title, tex("gui_turret_basic"), 68);
      }
   }

   public static class TurretAdvanced extends Base<TurretAdvancedMenu> {
      static final ResourceLocation TEX = tex("gui_turret_advanced");

      public TurretAdvanced(TurretAdvancedMenu menu, Inventory inv, Component title) {
         super(menu, inv, title, TEX, 30);
      }

      private boolean flag(int i) {
         TurretCrossbowAdvancedEntity t = menu.entity;
         if (t == null) return false;
         return switch (i) {
            case 1 -> t.getTargetAnimal();
            case 2 -> t.getTargetMob();
            case 3 -> t.getTargetPlayer();
            default -> t.getTargetFriendly();
         };
      }

      /** The four GuiToggleButtons at (90, 13 + 14 * (i - 1)). */
      @Override
      protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
         super.renderBg(g, pt, mx, my);
         for (int i = 1; i <= 4; i++) {
            int x = leftPos + 90, y = topPos + 13 + (i - 1) * 14;
            GuiDraw.rect(g, TEX, x, y, 192, 16, 8, 8);
            if (flag(i)) GuiDraw.rect(g, TEX, x, y, 192, 24, 8, 8);
            g.drawString(font, Component.translatable("button.turretfocus." + i), x + 12, y, 0xFFFFFF, true);
         }
      }

      @Override
      public boolean mouseClicked(double mx, double my, int button) {
         for (int i = 1; i <= 4; i++) {
            int x = leftPos + 90, y = topPos + 13 + (i - 1) * 14;
            if (mx >= x && my >= y && mx < x + 8 && my < y + 8) {
               minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
               if (minecraft.player != null) minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.4F, 1.0F);
               return true;
            }
         }
         return super.mouseClicked(mx, my, button);
      }
   }

   public static class ArcaneBore extends Base<ArcaneBoreMenu> {
      public ArcaneBore(ArcaneBoreMenu menu, Inventory inv, Component title) {
         super(menu, inv, title, tex("gui_arcanebore"), 68);
      }

      @Override
      protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
         super.renderBg(g, pt, mx, my);
         ArcaneBoreEntity bore = menu.entity;
         if (bore == null) return;
         int k = leftPos, l = topPos;
         ItemStack held = bore.getMainHandItem();
         if (!held.isEmpty() && held.getDamageValue() + 1 >= held.getMaxDamage()) GuiDraw.rect(g, tex, k + 80, l + 29, 240, 0, 16, 16);
         g.pose().pushPose();
         g.pose().translate(k + 124, l + 18, 505.0F);
         g.pose().scale(0.5F, 0.5F, 1.0F);
         g.drawString(font, "Width: " + (1 + bore.getDigRadius() * 2), 0, 0, 0xFFFFFF, true);
         g.drawString(font, "Depth: " + bore.getDigDepth(), 64, 0, 0xFFFFFF, true);
         g.drawString(font, "Speed: +" + bore.stoneDigSpeed(), 0, 10, 0xFFFFFF, true);
         int base = 0;
         int refining = bore.getRefining();
         int fortune = bore.getFortune();
         if (bore.hasSilkTouch() || refining > 0 || fortune > 0) g.drawString(font, "Other properties:", 0, 24, 0xFFFFFF, true);
         if (refining > 0) {
            g.drawString(font, "Refining " + refining, 4, 34 + base, 12632256, true);
            base += 9;
         }
         if (fortune > 0) {
            g.drawString(font, "Fortune " + fortune, 4, 34 + base, 15648330, true);
            base += 9;
         }
         if (bore.hasSilkTouch()) g.drawString(font, "Silk Touch", 4, 34 + base, 8421631, true);
         g.pose().popPose();
      }
   }
}
