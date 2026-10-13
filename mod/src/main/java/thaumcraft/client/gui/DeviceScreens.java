package thaumcraft.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.menu.HandMirrorMenu;
import thaumcraft.common.menu.PotionSprayerMenu;
import thaumcraft.common.menu.SpaMenu;
import thaumcraft.common.menu.ThaumatoriumMenu;
import thaumcraft.common.network.DeviceNetwork;
import thaumcraft.common.registers.ModSounds;

/** The device GUIs: GuiSpa, GuiThaumatorium, GuiPotionSprayer and GuiHandMirror. */
public final class DeviceScreens {
   private DeviceScreens() {
   }

   private static ResourceLocation tex(String name) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/" + name + ".png");
   }

   private static void click(net.minecraft.sounds.SoundEvent sound, float volume) {
      var p = Minecraft.getInstance().player;
      if (p != null) p.playNotifySound(sound, SoundSource.PLAYERS, volume, 1.0F);
   }

   /** UtilsFX.drawTag: the coloured aspect icon with its amount in small print. */
   static void drawTag(GuiGraphics g, Font font, Aspect aspect, int amount, float x, float y) {
      ResearchPageScreen.drawTag(g, aspect, x, y);
      if (amount > 0) {
         String am = String.valueOf(amount);
         g.pose().pushPose();
         g.pose().translate(0, 0, 200);
         g.pose().scale(0.5F, 0.5F, 1.0F);
         g.drawString(font, am, (int) ((x + 17) * 2 - font.width(am)), (int) ((y + 12) * 2), 0xFFFFFF, true);
         g.pose().popPose();
      }
   }

   private static void colorOf(int c, float a) {
      GuiDraw.color((c >> 16 & 255) / 255F, (c >> 8 & 255) / 255F, (c & 255) / 255F, a);
   }

   // ------------------------------------------------------------------ spa

   public static class Spa extends AbstractContainerScreen<SpaMenu> {
      private static final ResourceLocation TEX = tex("gui_spa");

      public Spa(SpaMenu menu, Inventory inv, Component title) {
         super(menu, inv, title);
      }

      @Override
      public void render(GuiGraphics g, int mx, int my, float pt) {
         super.render(g, mx, my, pt);
         int bx = leftPos, by = topPos;
         if (menu.spa != null) {
            if (mx - (bx + 104) >= 0 && my - (by + 10) >= 0 && mx - (bx + 104) < 10 && my - (by + 10) < 55) {
               FluidStack fluid = menu.spa.tank.getFluid();
               if (!fluid.isEmpty()) {
                  g.renderComponentTooltip(font, List.of(fluid.getHoverName(), Component.literal(fluid.getAmount() + " mb")), mx, my);
               }
            }
            if (mx - (bx + 88) >= 0 && my - (by + 34) >= 0 && mx - (bx + 88) < 10 && my - (by + 34) < 10) {
               g.renderTooltip(font, Component.translatable(menu.spa.getMix() ? "text.spa.mix.true" : "text.spa.mix.false"), mx, my);
            }
         }
         renderTooltip(g, mx, my);
      }

      @Override
      protected void renderLabels(GuiGraphics g, int mx, int my) {
      }

      @Override
      protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
         int k = leftPos, l = topPos;
         GuiDraw.rect(g, TEX, k, l, 0, 0, imageWidth, imageHeight);
         if (menu.spa == null) return;
         GuiDraw.rect(g, TEX, k + 89, l + 35, 208, menu.spa.getMix() ? 16 : 32, 8, 8);
         FluidStack fluid = menu.spa.tank.getFluid();
         if (menu.spa.tank.getFluidAmount() > 0 && !fluid.isEmpty()) {
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite icon = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ext.getStillTexture(fluid));
            float bar = (float) menu.spa.tank.getFluidAmount() / menu.spa.tank.getCapacity();
            int c = ext.getTintColor(fluid);
            colorOf(c, 1.0F);
            for (int a = 0; a < 6; a++) g.blit(k + 107, l + 15 + a * 8, 0, 8, 8, icon);
            GuiDraw.resetColor();
            GuiDraw.rect(g, TEX, k + 107, l + 15, 107, 15, 10, (int) (48.0F - 48.0F * bar));
         }
         GuiDraw.rect(g, TEX, k + 106, l + 11, 232, 0, 10, 55);
      }

      @Override
      public boolean mouseClicked(double mx, double my, int button) {
         int gx = (width - imageWidth) / 2, gy = (height - imageHeight) / 2;
         if (mx - (gx + 89) >= 0 && my - (gy + 35) >= 0 && mx - (gx + 89) < 8 && my - (gy + 35) < 8) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            click(ModSounds.CLACK.get(), 0.4F);
            return true;
         }
         return super.mouseClicked(mx, my, button);
      }
   }

   // ------------------------------------------------------------------ thaumatorium

   public static class Thaumatorium extends AbstractContainerScreen<ThaumatoriumMenu> {
      private static final ResourceLocation TEX = tex("gui_thaumatorium");
      private int index = 0;
      private List<ResourceLocation> list = new ArrayList<>();
      private long lastUpdate = 0L;

      public Thaumatorium(ThaumatoriumMenu menu, Inventory inv, Component title) {
         super(menu, inv, title);
         imageWidth = 175;
         imageHeight = 216;
      }

      @Override
      public void render(GuiGraphics g, int mx, int my, float pt) {
         super.render(g, mx, my, pt);
         renderTooltip(g, mx, my);
         ItemStack hover = hovered(mx, my);
         if (!hover.isEmpty()) g.renderTooltip(font, hover, mx, my);
      }

      @Override
      protected void renderLabels(GuiGraphics g, int mx, int my) {
      }

      private ItemStack output(ResourceLocation id) {
         RecipeHolder<CrucibleRecipe> r = menu.thaumatorium.recipe(id);
         return r == null ? ItemStack.EMPTY : r.value().getResultItem(menu.thaumatorium.getLevel().registryAccess());
      }

      /** Visits the visible recipe buttons: (id, x, y). */
      private interface Visitor {
         boolean visit(ResourceLocation id, int x, int y);
      }

      private void forButtons(Visitor v) {
         int px = 0, py = 0, q = 0, idx = 0;
         for (ResourceLocation id : list) {
            if (q++ < index * 2) continue;
            if (menu.thaumatorium.recipe(id) == null) continue;
            if (v.visit(id, leftPos + 48 + px * 16, topPos + 56 + py * 16)) return;
            if (++px > 1) {
               px = 0;
               py++;
            }
            if (++idx >= 6) return;
         }
      }

      private ItemStack hovered(int mx, int my) {
         ItemStack[] out = {ItemStack.EMPTY};
         if (menu.thaumatorium == null) return out[0];
         forButtons((id, x, y) -> {
            if (mx >= x && my >= y && mx < x + 16 && my < y + 16) {
               out[0] = output(id);
               return true;
            }
            return false;
         });
         return out[0];
      }

      @Override
      protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
         int k = leftPos, l = topPos;
         GuiDraw.rect(g, TEX, k, l, 0, 0, imageWidth, imageHeight);
         var t = menu.thaumatorium;
         if (t == null) return;
         long time = System.currentTimeMillis();
         if (time > lastUpdate) {
            t.updateRecipes(minecraft.player);
            list = t.generateRecipeList();
            lastUpdate = time + 500L;
         }
         if (!list.isEmpty()) {
            if (index > list.size() / 2) index = list.size() / 2;
            if (index < 0 || list.size() <= 6) index = 0;
            if (list.size() > 6) {
               if (index > 0) GuiDraw.rect(g, TEX, k + 82, l + 56, 176, 56, 8, 11);
               if (index < list.size() / 2.0F - 3.0F) GuiDraw.rect(g, TEX, k + 82, l + 93, 176, 93, 8, 11);
            }
         }
         drawAspects(g, k, l, time);
         if (t.maxRecipes > 1) {
            g.pose().pushPose();
            g.pose().translate(k + 64, l + 48, 0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            String text = t.recipeIds.size() + "/" + t.maxRecipes;
            g.drawString(font, text, -font.width(text) / 2, 0, 0xFFFFFF, false);
            g.pose().popPose();
         }
         ResourceLocation cycling = t.recipeIds.isEmpty() ? null : t.recipeIds.get((int) (time / 1000L % t.recipeIds.size()));
         forButtons((id, x, y) -> {
            if (t.recipeIds.contains(id)) {
               GuiDraw.color(1, 1, 1, 0.7F);
               GuiDraw.rect(g, TEX, x, y, 176, 8, 16, 16);
               if (t.recipeIds.size() > 1 && id.equals(cycling)) GuiDraw.rect(g, TEX, x, y, 176, 8, 16, 16);
               GuiDraw.resetColor();
            }
            ItemStack out = output(id);
            g.renderItem(out, x, y);
            g.renderItemDecorations(font, out, x, y);
            return false;
         });
      }

      private void drawAspects(GuiGraphics g, int k, int l, long time) {
         var t = menu.thaumatorium;
         if (t.recipeIds.isEmpty()) return;
         int i = (int) (time / 1000L % t.recipeIds.size());
         AspectList need = i < t.recipeEssentia.size() ? t.recipeEssentia.get(i) : null;
         if (need == null) return;
         int count = 0, px = 0, py = 0;
         for (Aspect aspect : need.getAspectsSortedByName()) {
            GuiDraw.rect(g, TEX, k + 98 + 16 * px, l + 40 + 20 * py, 176, 4, 12, 3);
            int i1 = (int) ((float) t.essentia.getAmount(aspect) / need.getAmount(aspect) * 12.0F);
            colorOf(aspect.getColor(), 1.0F);
            GuiDraw.rect(g, TEX, k + 98 + 16 * px, l + 40 + 20 * py, 176, 0, i1, 3);
            GuiDraw.resetColor();
            if (++px > 1) {
               px = 0;
               py++;
            }
            if (++count >= 8) break;
         }
         count = 0;
         px = 0;
         py = 0;
         for (Aspect aspect : need.getAspectsSortedByName()) {
            drawTag(g, font, aspect, need.getAmount(aspect), k + 96 + 16 * px, l + 24 + 20 * py);
            if (++px > 1) {
               px = 0;
               py++;
            }
            if (++count >= 8) break;
         }
      }

      @Override
      public boolean mouseClicked(double mx, double my, int button) {
         var t = menu.thaumatorium;
         if (t != null) {
            boolean[] hit = {false};
            forButtons((id, x, y) -> {
               if (mx >= x && my >= y && mx < x + 16 && my < y + 16) {
                  PacketDistributor.sendToServer(new DeviceNetwork.ThaumatoriumSelectPayload(t.getBlockPos(), id));
                  click(ModSounds.HHON.get(), 0.3F);
                  lastUpdate = 0L;
                  hit[0] = true;
                  return true;
               }
               return false;
            });
            if (hit[0]) return true;
            if (list.size() > 6) {
               int gx = leftPos, gy = topPos;
               if (index > 0 && mx >= gx + 82 && my >= gy + 56 && mx < gx + 90 && my < gy + 67) {
                  index--;
                  click(ModSounds.CLACK.get(), 0.4F);
                  lastUpdate = 0L;
                  return true;
               }
               if (index < list.size() / 2.0F - 3.0F && mx >= gx + 82 && my >= gy + 93 && mx < gx + 90 && my < gy + 104) {
                  index++;
                  click(ModSounds.CLACK.get(), 0.4F);
                  lastUpdate = 0L;
                  return true;
               }
            }
         }
         return super.mouseClicked(mx, my, button);
      }
   }

   // ------------------------------------------------------------------ potion sprayer

   public static class PotionSprayer extends AbstractContainerScreen<PotionSprayerMenu> {
      private static final ResourceLocation TEX = tex("gui_potion_sprayer");

      public PotionSprayer(PotionSprayerMenu menu, Inventory inv, Component title) {
         super(menu, inv, title);
         imageWidth = 192;
         imageHeight = 233;
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
         GuiDraw.rect(g, TEX, k, l, 0, 0, imageWidth, imageHeight);
         var s = menu.sprayer;
         if (s == null) return;
         if (s.charges > 0) {
            colorOf(s.color, 1.0F);
            int scroll = minecraft.player.tickCount % 256;
            GuiDraw.rect(g, TEX, k + 128, l + 36 + (8 - s.charges) * 9, 232, scroll, 8, s.charges * 9);
            GuiDraw.resetColor();
         }
         int pos = 0;
         for (Aspect aspect : s.recipe.getAspectsSortedByName()) {
            GuiDraw.color(0.2F, 0.2F, 0.2F, 1.0F);
            GuiDraw.rect(g, TEX, k + 96 + 22 * (pos % 2), l + 46 + 16 * (pos / 2) - 14, 192, 56, 2, 14);
            int i1 = (int) ((float) s.recipeProgress.getAmount(aspect) / s.recipe.getAmount(aspect) * 14.0F);
            colorOf(aspect.getColor(), 1.0F);
            GuiDraw.rect(g, TEX, k + 96 + 22 * (pos % 2), l + 46 + 16 * (pos / 2) - i1, 192, 56, 2, i1);
            GuiDraw.resetColor();
            pos++;
         }
         pos = 0;
         for (Aspect aspect : s.recipe.getAspectsSortedByName()) {
            drawTag(g, font, aspect, s.recipe.getAmount(aspect), k + 79 + 22 * (pos % 2), l + 31 + 16 * (pos / 2));
            pos++;
         }
         GuiDraw.rect(g, TEX, k + 125, l + 28, 205, 28, 14, 88);
      }
   }

   // ------------------------------------------------------------------ hand mirror

   public static class HandMirror extends AbstractContainerScreen<HandMirrorMenu> {
      private static final ResourceLocation TEX = tex("gui_handmirror");

      public HandMirror(HandMirrorMenu menu, Inventory inv, Component title) {
         super(menu, inv, title);
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
         GuiDraw.rect(g, TEX, k, l, 0, 0, imageWidth, imageHeight);
         g.pose().pushPose();
         g.pose().translate(0, 0, 300);
         GuiDraw.rect(g, TEX, k + 8 + menu.selected * 18, l + 142, 240, 0, 16, 16);
         g.pose().popPose();
      }
   }
}
