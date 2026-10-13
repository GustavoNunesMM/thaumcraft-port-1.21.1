package thaumcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.text.DecimalFormat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.ICaster;
import thaumcraft.client.gui.GuiDraw;
import thaumcraft.common.items.casters.FocusItem;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.ClientAura;

/**
 * HudHandler.renderCastingWandHud: the dial in the top-left corner while a caster is held: the aura's vis gauge, the
 * fitted focus (or the block an exchange focus will place) and, sneaking, the numbers.
 */
public final class CasterHud {
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "caster_hud");
   private static final ResourceLocation HUD = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/hud.png");
   private static final DecimalFormat FORMAT = new DecimalFormat("#######.#");

   private CasterHud() {
   }

   public static void render(GuiGraphics g, DeltaTracker delta) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null || mc.options.hideGui || mc.screen != null) return;
      ItemStack wandstack = player.getMainHandItem();
      if (!(wandstack.getItem() instanceof ICaster)) wandstack = player.getOffhandItem();
      if (!(wandstack.getItem() instanceof ICaster wand)) return;
      AuraChunk aura = ClientAura.current;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      g.pose().pushPose();
      GuiDraw.quad(g, HUD, 0, 0, 32, 32, 0, 0, 64, 64, 256, 256);
      g.pose().translate(16.0F, 16.0F, 0.0F);
      int max = Math.max(1, aura.getBase());
      int amt = (int) aura.getVis();
      ItemStack focusStack = wand.getFocusStack(wandstack);
      FocusItem focus = focusStack != null && focusStack.getItem() instanceof FocusItem f ? f : null;
      g.pose().pushPose();
      g.pose().translate(16.0F, -10.0F, 0.0F);
      g.pose().scale(0.5F, 0.5F, 1.0F);
      int loc = Math.min(30, (int) (30.0F * amt / max));
      int ac = Aspect.ENERGY.getColor();
      GuiDraw.color((ac >> 16 & 255) / 255.0F, (ac >> 8 & 255) / 255.0F, (ac & 255) / 255.0F, 0.8F);
      GuiDraw.quad(g, HUD, -4.0F, 35 - loc, 8.0F, loc, 104.0F, 0.0F, 8.0F, loc, 256, 256);
      GuiDraw.resetColor();
      GuiDraw.quad(g, HUD, -8.0F, -3.0F, 16.0F, 42.0F, 72.0F, 0.0F, 16.0F, 42.0F, 256, 256);
      if (player.isShiftKeyDown()) {
         g.pose().pushPose();
         g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
         g.drawString(mc.font, FORMAT.format(amt), -32, -4, 0xFFFFFF);
         g.pose().popPose();
         if (focus != null && focus.getVisCost(focusStack) > 0.0F) {
            float mod = wand.getConsumptionModifier(wandstack, player, false);
            String msg = FORMAT.format(focus.getVisCost(focusStack) * mod);
            g.drawString(mc.font, msg, -32 - mc.font.width(msg) / 2, 32, 0xFFFFFF);
         }
      }
      g.pose().popPose();
      if (focus != null) {
         ItemStack picked = wand.getPickedBlock(wandstack);
         g.pose().pushPose();
         g.pose().translate(-24.0F, -24.0F, 90.0F);
         if (picked != null && !picked.isEmpty()) {
            g.renderItem(picked, 16, 16);
            int count = 0;
            for (ItemStack s : player.getInventory().items) {
               if (ItemStack.isSameItem(s, picked)) count += s.getCount();
            }
            g.pose().translate(0, 0, 200);
            String c = String.valueOf(count);
            g.drawString(mc.font, c, 33 - mc.font.width(c), 25, 0xFFFFFF);
         } else {
            g.renderItem(focusStack, 16, 16);
         }
         g.pose().popPose();
      }
      g.pose().popPose();
      RenderSystem.disableBlend();
   }
}
