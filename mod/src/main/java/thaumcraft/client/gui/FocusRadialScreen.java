package thaumcraft.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item;
import thaumcraft.Thaumcraft;
import thaumcraft.api.casters.ICaster;
import thaumcraft.client.CastersClient;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.network.CasterPayloads;

/**
 * WandRenderingHandler.handleFociRadial: the ring of carried foci shown while F is held. Releasing F over a focus (or
 * clicking one) fits it to the caster. A screen here, so the mouse is free while it is open.
 */
public class FocusRadialScreen extends Screen {
   private static final ResourceLocation R1 = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/radial.png");
   private static final ResourceLocation R2 = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/radial2.png");
   private final TreeMap<String, Integer> foci;
   private final Map<String, ItemStack> fociItem;
   private final Map<String, Float> fociScale = new HashMap<>();
   private String hovered;
   private float radialHudScale;
   private long lastTime = System.currentTimeMillis();
   private boolean done;
   /** Dev smoke test: keeps the selector open as if F were held. */
   public static boolean smokeHold;

   private FocusRadialScreen(TreeMap<String, Integer> foci, Map<String, ItemStack> items) {
      super(Component.empty());
      this.foci = foci;
      this.fociItem = items;
      for (String k : foci.keySet()) fociScale.put(k, 1.0F);
   }

   /** Opens the selector if the player carries any foci. */
   public static void open() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.screen != null) return;
      Map<String, ItemStack> items = new HashMap<>();
      TreeMap<String, Integer> foci = CasterManager.collectFoci(mc.player, new HashMap<>(), items);
      if (foci.isEmpty()) return;
      mc.setScreen(new FocusRadialScreen(foci, items));
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
   }

   private boolean keyHeld() {
      if (smokeHold) return true;
      Minecraft mc = Minecraft.getInstance();
      InputConstants.Key key = CastersClient.KEY_F.getKey();
      return key.getType() == InputConstants.Type.KEYSYM && InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
   }

   private ItemStack caster(Player p) {
      ItemStack s = p.getMainHandItem();
      return s.getItem() instanceof ICaster ? s : p.getOffhandItem();
   }

   private void choose(String key) {
      if (done) return;
      done = true;
      CasterPayloads.send(new CasterPayloads.FocusChange(key));
      CastersClient.radialLock = true;
   }

   @Override
   public void tick() {
      Player p = Minecraft.getInstance().player;
      if (p == null || !(caster(p).getItem() instanceof ICaster)) {
         onClose();
         return;
      }
      if (!keyHeld()) {
         if (hovered != null) choose(hovered);
         onClose();
      }
   }

   @Override
   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button == 0 && hovered != null) {
         choose(hovered);
         onClose();
         return true;
      }
      return super.mouseClicked(mouseX, mouseY, button);
   }

   @Override
   public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null) return;
      long time = System.currentTimeMillis();
      float delta = (time - lastTime) / 150.0F;
      lastTime = time;
      radialHudScale = Math.min(1.0F, radialHudScale + delta);
      ItemStack s = caster(player);
      if (!(s.getItem() instanceof ICaster wand)) return;
      double sw = width, sh = height;
      float width = 16.0F + fociItem.size() * 2.5F;
      ItemStack tt = null;
      g.pose().pushPose();
      g.pose().translate((int) (sw / 2.0), (int) (sh / 2.0), 0);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      float spin = partialTicks + player.tickCount % 720 / 2.0F;
      ring(g, R1, spin, width * 2.75F * radialHudScale);
      ring(g, R2, -spin, width * 2.55F * radialHudScale);
      ItemStack focusStack = wand.getFocusStack(s);
      if (focusStack != null) {
         g.renderItem(focusStack, -8, -8);
         int mx = (int) (mouseX - sw / 2.0), my = (int) (mouseY - sh / 2.0);
         if (mx >= -10 && mx <= 10 && my >= -10 && my <= 10) tt = focusStack;
      }
      g.pose().scale(radialHudScale, radialHudScale, 1.0F);
      float currentRot = -90.0F * radialHudScale;
      float pieSlice = 360.0F / fociItem.size();
      hovered = null;
      String key = foci.firstKey();
      for (int a = 0; a < fociItem.size() && key != null; a++) {
         double xx = Mth.cos(currentRot / 180.0F * (float) Math.PI) * width;
         double yy = Mth.sin(currentRot / 180.0F * (float) Math.PI) * width;
         currentRot += pieSlice;
         int mx = (int) (mouseX - sw / 2.0 - xx), my = (int) (mouseY - sh / 2.0 - yy);
         boolean over = mx >= -10 && mx <= 10 && my >= -10 && my <= 10;
         if (over) {
            hovered = key;
            tt = fociItem.get(key);
         }
         float sc = fociScale.get(key);
         sc = over ? Math.min(1.3F, sc + delta) : Math.max(1.0F, sc - delta * 150.0F / 250.0F);
         fociScale.put(key, sc);
         g.pose().pushPose();
         g.pose().translate((int) xx, (int) yy, 100);
         g.pose().scale(sc, sc, sc);
         g.renderItem(fociItem.get(key), -8, -8);
         g.pose().popPose();
         key = foci.higherKey(key);
      }
      g.pose().popPose();
      if (tt != null) {
         List<Component> lines = tt.getTooltipLines(Item.TooltipContext.of(mc.level), player,
            mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
         g.renderComponentTooltip(font, lines, mouseX, mouseY);
      }
   }

   private static void ring(GuiGraphics g, ResourceLocation tex, float rot, float size) {
      g.pose().pushPose();
      g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rot));
      GuiDraw.color(0.5F, 0.5F, 0.5F, 0.5F);
      GuiDraw.quad(g, tex, -size / 2.0F, -size / 2.0F, size, size, 0, 0, 1, 1, 1, 1);
      GuiDraw.resetColor();
      g.pose().popPose();
   }
}
