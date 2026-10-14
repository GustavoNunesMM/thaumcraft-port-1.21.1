package thaumcraft.client.golems;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.EssentiaFX;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.fx.FXGeneric;
import thaumcraft.common.golems.GolemClientHooks;
import thaumcraft.common.golems.GolemDartEntity;
import thaumcraft.common.golems.GolemProperties;
import thaumcraft.common.registers.ModGolems;

/** Client registrations of the golems milestone: renderers, screens, the golem item colour, seal drawing and golem effects. */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GolemsClient {
   private GolemsClient() {
   }

   @SubscribeEvent
   public static void setup(FMLClientSetupEvent event) {
      event.enqueueWork(() -> {
         GolemClientHooks.flyParticles = GolemsClient::drawGolemFlyParticles;
         GolemClientHooks.emote = GolemsClient::emote;
         GolemClientHooks.pressVent = (pos, n) -> {
            RandomSource r = Minecraft.getInstance().level.random;
            for (int a = 0; a < n; a++) {
               EssentiaFX.drawVentParticles(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, r.nextGaussian() * 0.1, 0.0, r.nextGaussian() * 0.1,
                  0xAAAAAA);
            }
         };
      });
   }

   @SubscribeEvent
   public static void screens(RegisterMenuScreensEvent event) {
      event.register(ModGolems.SEAL_MENU.get(), SealScreen::new);
      event.register(ModGolems.LOGISTICS_MENU.get(), LogisticsScreen::new);
      event.register(ModGolems.GOLEM_BUILDER_MENU.get(), GolemBuilderScreen::new);
   }

   @SubscribeEvent
   public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ModGolems.GOLEM.get(), GolemRenderer::new);
      event.registerEntityRenderer(ModGolems.GOLEM_DART.get(), DartRenderer::new);
      event.registerBlockEntityRenderer(ModGolems.GOLEM_BUILDER_BE.get(), GolemBuilderRenderer::new);
   }

   /** The golem placer takes the colour of its material (ColorHandler). */
   @SubscribeEvent
   public static void itemColors(RegisterColorHandlersEvent.Item event) {
      event.register((stack, tint) -> {
         Long props = stack.get(ModGolems.GOLEM_PROPS.get());
         return props == null ? 0xFFFFFFFF : 0xFF000000 | GolemProperties.fromLong(props).getMaterial().itemColor;
      }, ModGolems.GOLEM_PLACER.get());
   }

   /** FXDispatcher.drawGolemFlyParticles: the downdraft under a levitating golem. */
   static void drawGolemFlyParticles(LivingEntity e) {
      var level = Minecraft.getInstance().level;
      if (level == null) return;
      FXGeneric fb = new FXGeneric(level, e.getX(), e.getY() + 0.1, e.getZ(), level.random.nextGaussian() / 100.0, -0.1,
         level.random.nextGaussian() / 100.0);
      fb.setMaxAge(20 + level.random.nextInt(5));
      fb.setAlphaF(0.3F, 0.0F);
      fb.setGridSize(16);
      fb.setParticles(56, 1, 1);
      fb.setScale(1.5F, 3.0F, 8.0F);
      fb.setLayer(0);
      fb.setSlowDown(1.0);
      fb.setWind(0.001);
      fb.setRotationSpeed(level.random.nextFloat(), level.random.nextBoolean() ? -1.0F : 1.0F);
      Minecraft.getInstance().particleEngine.add(fb);
   }

   /** EntityThaumcraftGolem.handleStatusUpdate 5-9: following, thinking, alarmed, staying and rank up. */
   static void emote(LivingEntity e, Byte id) {
      RandomSource rand = e.getRandom();
      double y = e.getY() + e.getBbHeight() + 0.1;
      switch (id) {
         case 5 -> FXDispatcher.drawGenericParticles(e.getX(), y, e.getZ(), 0.0, 0.0, 0.0, 1.0F, 1.0F, 1.0F, 0.5F, false,
            704 + (rand.nextBoolean() ? 0 : 3), 3, 1, 6, 2.0F, 0.0F, 1);
         case 6 -> FXDispatcher.drawGenericParticles(e.getX(), y, e.getZ(), 0.0, 0.025, 0.0, 0.1F, 1.0F, 1.0F, 0.5F, false, 15, 1, 1, 10, 2.0F, 0.0F, 1);
         case 7 -> FXDispatcher.drawGenericParticles(e.getX(), y, e.getZ(), 0.0, 0.05, 0.0, 1.0F, 1.0F, 1.0F, 0.5F, false, 640, 10, 1, 10, 2.0F, 0.0F, 1);
         case 8 -> FXDispatcher.drawGenericParticles(e.getX(), y, e.getZ(), 0.0, 0.01, 0.0, 1.0F, 1.0F, 0.1F, 0.5F, false, 14, 1, 1, 20, 2.0F, 0.0F, 1);
         case 9 -> {
            for (int a = 0; a < 5; a++) {
               FXDispatcher.drawGenericParticles(e.getX(), e.getY() + e.getBbHeight(), e.getZ(), rand.nextGaussian() * 0.01F, rand.nextFloat() * 0.02,
                  rand.nextGaussian() * 0.01F, 1.0F, 1.0F, 1.0F, 0.5F, false, 13, 1, 1, 20 + rand.nextInt(20), 0.3F + rand.nextFloat() * 0.4F, 0.0F, 1);
            }
         }
         default -> {
         }
      }
   }

   /** RenderDart: a vanilla arrow at the dart's smaller scale (0.033 instead of 0.05625). */
   public static class DartRenderer extends ArrowRenderer<GolemDartEntity> {
      private static final ResourceLocation TEX = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

      public DartRenderer(EntityRendererProvider.Context ctx) {
         super(ctx);
      }

      @Override
      public void render(GolemDartEntity entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         pose.pushPose();
         pose.scale(0.033F / 0.05625F, 0.033F / 0.05625F, 0.033F / 0.05625F);
         super.render(entity, yaw, pt, pose, buffers, light);
         pose.popPose();
      }

      @Override
      public ResourceLocation getTextureLocation(GolemDartEntity entity) {
         return TEX;
      }
   }

   /** Seals are drawn after the particles, like the architect overlay. */
   @EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
   public static final class GameEvents {
      private GameEvents() {
      }

      @SubscribeEvent
      public static void renderLevel(RenderLevelStageEvent event) {
         if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) SealRenderer.render(event);
      }
   }
}
