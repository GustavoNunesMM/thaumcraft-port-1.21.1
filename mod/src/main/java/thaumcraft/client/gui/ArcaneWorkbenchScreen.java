package thaumcraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.menu.ArcaneWorkbenchMenu;

/** GuiArcaneWorkbench. */
public class ArcaneWorkbenchScreen extends AbstractContainerScreen<ArcaneWorkbenchMenu> {
   private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/arcaneworkbench.png");

   public ArcaneWorkbenchScreen(ArcaneWorkbenchMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      imageWidth = 190;
      imageHeight = 234;
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      super.render(g, mouseX, mouseY, partialTick);
      renderTooltip(g, mouseX, mouseY);
   }

   @Override
   protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
   }

   @Override
   protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
      int x = leftPos, y = topPos;
      RenderSystem.enableBlend();
      g.blit(TEX, x, y, 0, 0, imageWidth, imageHeight);
      IArcaneRecipe recipe = menu.currentArcaneRecipe();
      int cost = 0, discount = 0;
      AspectList crystals = null;
      if (recipe != null) {
         float df = CasterManager.getTotalVisDiscount(minecraft.player);
         cost = menu.visCost(recipe);
         discount = (int) (df * 100.0F);
         crystals = recipe.getCrystals();
      }
      if (crystals != null && !crystals.isEmpty()) {
         RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
         float t = minecraft.player.tickCount + partialTick;
         for (Aspect a : crystals.getAspects()) {
            int id = slotOf(a);
            if (id < 0) continue;
            int c = a.getColor();
            RenderSystem.setShaderColor((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F, 0.33F);
            g.pose().pushPose();
            g.pose().translate(x + ArcaneWorkbenchMenu.XX[id] + 7.5F, y + ArcaneWorkbenchMenu.YY[id] + 8.0F, 0.0F);
            g.pose().mulPose(Axis.ZP.rotationDegrees(id * 60 + t % 360));
            g.pose().scale(0.5F, 0.5F, 1.0F);
            g.blit(TEX, -32, -32, 192, 0, 64, 64);
            g.pose().popPose();
         }
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.defaultBlendFunc();
      }
      RenderSystem.disableBlend();

      int available = menu.getAvailableVis();
      small(g, available + " " + Component.translatable("workbench.available").getString(), x + 168, y + 46,
         available < cost ? 0xEE6E6E : 0x6E6EEE);
      if (cost > 0) {
         if (available < cost) {
            ItemStack out = recipe.getResultItem(minecraft.level.registryAccess());
            g.renderItem(out, x + 160, y + 64);
            g.renderItemDecorations(font, out, x + 160, y + 64, "");
            g.fill(RenderType_GUI_OVERLAY, x + 160, y + 64, x + 176, y + 80, 0xAA555555);
         }
         String text = cost + " " + Component.translatable("workbench.cost").getString();
         if (discount > 0) text += " (" + discount + "% " + Component.translatable("workbench.discount").getString() + ")";
         small(g, text, x + 168, y + 38, 0xC0FFFF);
      }
   }

   private static final net.minecraft.client.renderer.RenderType RenderType_GUI_OVERLAY = net.minecraft.client.renderer.RenderType.guiOverlay();

   private static int slotOf(Aspect a) {
      for (int i = 0; i < ArcaneWorkbenchMenu.CRYSTALS.length; i++) {
         if (ArcaneWorkbenchMenu.CRYSTALS[i] == a) return i;
      }
      return -1;
   }

   private void small(GuiGraphics g, String text, int cx, int cy, int color) {
      g.pose().pushPose();
      g.pose().translate(cx, cy, 0.0F);
      g.pose().scale(0.5F, 0.5F, 1.0F);
      g.drawString(font, text, -font.width(text) / 2, 0, color, false);
      g.pose().popPose();
   }
}
