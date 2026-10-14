package thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registers.ModFlux;

/**
 * ShaderHandler.checkShaders: the warp potions tint the screen with a post-processing shader (death gaze desaturates,
 * blurred vision blurs, unnatural hunger reddens, sun scorned blooms).
 *
 * <p>Port note: 1.12 stacked one ShaderGroup per effect; the 1.21 GameRenderer holds a single post effect, so only the
 * first active one of the list below is shown. The shaders are the original ones moved to GLSL 150
 * (assets/thaumcraft/shaders, written by tools/gen_marco12.py). The death gaze vignette (RenderEventHandler) is not drawn.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class FluxShaders {
   private static ResourceLocation active;
   /** Shaders that failed to load (no shader support, broken pack): not retried every tick. */
   private static final java.util.Set<ResourceLocation> FAILED = new java.util.HashSet<>();

   private FluxShaders() {
   }

   private static ResourceLocation shader(String name) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "shaders/post/" + name + ".json");
   }

   private static ResourceLocation wanted(Player player) {
      Object[][] list = {{ModFlux.DEATH_GAZE, "desaturatetc"}, {ModFlux.BLURRED_VISION, "blurtc"}, {ModFlux.UNNATURAL_HUNGER, "hunger"},
         {ModFlux.SUN_SCORNED, "sunscorned"}};
      for (Object[] e : list) {
         @SuppressWarnings("unchecked")
         Holder<MobEffect> effect = (Holder<MobEffect>) e[0];
         if (player.hasEffect(effect)) return shader((String) e[1]);
      }
      return null;
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      GameRenderer gr = mc.gameRenderer;
      if (mc.player == null || mc.level == null) {
         active = null;
         return;
      }
      ResourceLocation want = wanted(mc.player);
      PostChain current = gr.currentEffect();
      boolean ours = active != null && current != null && current.getName().equals(active.toString());
      if (want == null) {
         if (ours) gr.shutdownEffect();
         active = null;
      } else if (!ours || !want.equals(active)) {
         // Don't fight the vanilla spectator effects (creeper/spider/enderman views).
         if (current != null && !ours || FAILED.contains(want)) return;
         gr.loadEffect(want);
         if (gr.currentEffect() == null) {
            FAILED.add(want);
            active = null;
         } else {
            active = want;
         }
      }
   }
}
