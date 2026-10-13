package thaumcraft.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.client.gui.GuiDraw;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.ClientResearchEvents;

/** HudHandler.renderKnowledgeGains: a knowledge icon flies into the Thaumonomicon in the corner for each point gained. */
public final class KnowledgeGainHud {
   public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "knowledge_gains");
   private static final ResourceLocation BOOK = tex("textures/items/thaumonomicon.png");
   private static final ResourceLocation PARTICLES = tex("textures/misc/particles.png");
   private static final ResourceLocation[] KNOW_TYPE = {tex("textures/research/knowledge_theory.png"), tex("textures/research/knowledge_observation.png")};

   private static final class Tracker {
      final EnumKnowledgeType type;
      @Nullable
      final ResearchCategory category;
      int progress;
      final int max;
      final long seed;

      Tracker(EnumKnowledgeType type, @Nullable ResearchCategory category, int progress, long seed) {
         this.type = type;
         this.category = category;
         if (type == EnumKnowledgeType.THEORY) progress += 10;
         this.progress = progress;
         this.max = progress;
         this.seed = seed;
      }
   }

   private static final List<Tracker> TRACKERS = new ArrayList<>();
   private static float kgFade;

   private KnowledgeGainHud() {
   }

   private static ResourceLocation tex(String p) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, p);
   }

   /** Client tick: pick up new gains, advance the animations (RenderEventHandler). */
   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      ClientResearchEvents.Gain gain;
      while ((gain = ClientResearchEvents.KNOWLEDGE_GAINS.poll()) != null) {
         if (mc.player == null) continue;
         ResearchCategory cat = gain.category().isEmpty() ? null : ResearchCategories.getResearchCategory(gain.category());
         TRACKERS.add(new Tracker(gain.type(), cat, 40 + mc.player.getRandom().nextInt(20), mc.player.getRandom().nextLong()));
         mc.player.level().playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), ModSounds.LEARN.get(),
            net.minecraft.sounds.SoundSource.AMBIENT, 1.0F, 1.0F, false);
      }
      if (TRACKERS.isEmpty()) {
         if (kgFade > 0) kgFade--;
      } else {
         kgFade = Math.min(40.0F, kgFade + 10.0F);
         TRACKERS.removeIf(t -> --t.progress <= 0);
      }
   }

   private static void centered(GuiGraphics g, ResourceLocation tex, float size) {
      GuiDraw.quad(g, tex, -size / 2, -size / 2, size, size, 0, 0, 1, 1, 1, 1);
   }

   private static void flash(GuiGraphics g, Random rand, float size) {
      int frame = 320 + rand.nextInt(16);
      float gr = Mth.nextInt(net.minecraft.util.RandomSource.create(rand.nextLong()), 189, 255) / 255.0F;
      float b = Mth.nextInt(net.minecraft.util.RandomSource.create(rand.nextLong()), 64, 255) / 255.0F;
      GuiDraw.color(1, gr, b, 200 / 255F);
      RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
      GuiDraw.quad(g, PARTICLES, -size / 2, -size / 2, size, size, frame % 64, frame / 64, 1, 1, 64, 64);
      RenderSystem.defaultBlendFunc();
      GuiDraw.resetColor();
   }

   public static void render(GuiGraphics g, DeltaTracker delta) {
      if (kgFade <= 0) return;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null) return;
      float pt = delta.getGameTimeDeltaPartialTick(false);
      int ww = g.guiWidth(), hh = g.guiHeight();
      GuiDraw.color(1, 1, 1, kgFade / 40.0F);
      GuiDraw.full(g, BOOK, ww - 17, hh - 17);
      GuiDraw.resetColor();
      int a = 0;
      for (Tracker current : TRACKERS) {
         Random rand = new Random(current.seed);
         g.pose().pushPose();
         float s = 16.0F;
         float x = ww / 4.0F + rand.nextInt(32);
         float y = hh / 3.0F + rand.nextInt(32);
         float wot = 0.0F;
         if (current.progress < current.max * 0.66F) {
            float q = (current.progress - pt) / (current.max * 0.66F);
            s *= q;
            float m = (float) Math.sin(q * Math.PI - Math.PI / 2) * 0.5F + 0.5F;
            y *= m;
            x *= (float) Math.sin(m * Math.PI * 0.5);
         } else {
            wot = current.max - current.progress + pt;
            float wot2 = wot / (current.max * 0.33F);
            float m = (float) Math.sin(wot2 * Math.PI * 2.0 - Math.PI / 2) * 0.5F + 1.5F;
            if (wot2 < 0.5) s *= wot2 * 2.0F;
            s *= m;
         }
         float xx = ww - 12 + rand.nextInt(8) - x;
         float yy = hh - 12 + rand.nextInt(8) - y;
         g.pose().translate(xx, yy, 80 + a);
         g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-(84 + rand.nextInt(12))));
         centered(g, KNOW_TYPE[current.type.ordinal()], s);
         if (current.category != null) centered(g, current.category.icon, s * 0.75F);
         if (current.progress > current.max * 0.9F) {
            float wot3 = wot / (current.max * 0.1F);
            float m2 = (float) Math.sin(wot3 * Math.PI * 2.0 - Math.PI / 2) * 0.25F + 0.25F;
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-rand.nextInt(360)));
            flash(g, rand, 64.0F * m2);
         }
         if (current.progress < current.max * 0.1F) {
            float wot3 = 1.0F - (current.progress - pt) / (current.max * 0.1F);
            float m2 = (float) Math.sin(wot3 * Math.PI * 2.0 - Math.PI / 2) * 0.25F + 0.25F;
            g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-rand.nextInt(360)));
            flash(g, rand, 32.0F * m2);
         }
         g.pose().popPose();
         a++;
      }
   }
}
