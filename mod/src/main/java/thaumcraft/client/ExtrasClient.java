package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.FXGeneric;
import thaumcraft.client.renderers.CasterRenderers;
import thaumcraft.client.renderers.MobRenderers;
import thaumcraft.client.renderers.RenderUtil;
import thaumcraft.common.entities.projectile.GrappleEntity;
import thaumcraft.common.items.tools.GrappleGunItem;
import thaumcraft.common.registers.ModExtras;

/** Client side of the audit additions: the grapple renderer (RenderGrapple) and the grappler's "type" model property. */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ExtrasClient {
   private ExtrasClient() {
   }

   @SubscribeEvent
   public static void setup(FMLClientSetupEvent event) {
      event.enqueueWork(() -> ItemProperties.register(ModExtras.GRAPPLE_GUN.get(), ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "type"),
         (stack, level, entity, seed) -> GrappleGunItem.isLoaded(stack) ? 1.0F : 0.0F));
   }

   @SubscribeEvent
   public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
      event.registerEntityRenderer(ModExtras.GRAPPLE.get(), GrappleRenderer::new);
   }

   /** RenderGrapple: the ModelGrappler head, a glowing billboard and the swaying rope back to the thrower's hand. */
   public static class GrappleRenderer extends EntityRenderer<GrappleEntity> {
      private static final ResourceLocation BEAM = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/grappler.png");
      private static final ResourceLocation ROPE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/rope.png");
      private final ModelPart model;

      public GrappleRenderer(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.0F;
         model = ctx.bakeLayer(CasterRenderers.MINE_LAYER); // ModelGrappler and ModelFocusMine are the same mesh
      }

      @Override
      public ResourceLocation getTextureLocation(GrappleEntity e) {
         return BEAM;
      }

      @Override
      public boolean shouldRender(GrappleEntity e, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
         return true;
      }

      @Override
      public void render(GrappleEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         pose.pushPose();
         pose.pushPose();
         RenderUtil.rot(pose, Mth.lerp(pt, e.yRotO, e.getYRot()) - 90.0F, 0.0F, 1.0F, 0.0F);
         RenderUtil.rot(pose, Mth.lerp(pt, e.xRotO, e.getXRot()), 0.0F, 0.0F, 1.0F);
         model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(BEAM)), light, OverlayTexture.NO_OVERLAY);
         pose.popPose();
         // the glow billboard (a frame of the particle sheet, additive)
         pose.mulPose(entityRenderDispatcher.cameraOrientation());
         float bob = Mth.sin(e.tickCount / 5.0F) * 0.2F + 0.2F;
         pose.scale(1.0F + bob, 1.0F + bob, 1.0F + bob);
         float f2 = (1 + e.tickCount % 6) / 32.0F, f3 = f2 + 0.03125F, f4 = 0.21875F, f5 = f4 + 0.03125F, h = 0.5F;
         VertexConsumer glow = buffers.getBuffer(MobRenderers.additive(FXGeneric.TEXTURE));
         PoseStack.Pose p = pose.last();
         int a = (int) (0.21F * 255);
         glow.addVertex(p, -h, -h, 0).setColor(255, 255, 255, a).setUv(f2, f5).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, 0, 1, 0);
         glow.addVertex(p, h, -h, 0).setColor(255, 255, 255, a).setUv(f3, f5).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, 0, 1, 0);
         glow.addVertex(p, h, h, 0).setColor(255, 255, 255, a).setUv(f3, f4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, 0, 1, 0);
         glow.addVertex(p, -h, h, 0).setColor(255, 255, 255, a).setUv(f2, f4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, 0, 1, 0);
         pose.popPose();
         List<Vec3> points = calcPoints(e.getThrower(), e, pt);
         if (points.size() > 2) rope(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(ROPE)), points, light);
         super.render(e, yaw, pt, pose, buffers, light);
      }

      /** The rope: a 4-sided tube (glePolyCone with POLYCYL_TESS 4, radius 0.025) through the points. */
      private static void rope(PoseStack pose, VertexConsumer vc, List<Vec3> pts, int light) {
         PoseStack.Pose p = pose.last();
         Matrix4f m = p.pose();
         double r = 0.025;
         float v = 0.0F;
         for (int i = 0; i < pts.size() - 1; i++) {
            Vec3 a = pts.get(i), b = pts.get(i + 1);
            Vec3 dir = b.subtract(a);
            double len = dir.length();
            if (len < 1.0E-6) continue;
            dir = dir.scale(1.0 / len);
            Vec3 up = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 s1 = dir.cross(up).normalize().scale(r);
            Vec3 s2 = dir.cross(s1).normalize().scale(r);
            Vec3[] ring = {s1, s2, s1.scale(-1), s2.scale(-1)};
            float v2 = v + (float) len * 2.0F;
            for (int k = 0; k < 4; k++) {
               Vec3 c0 = ring[k], c1 = ring[(k + 1) % 4];
               Vec3 n = c0.add(c1).normalize();
               float u0 = k / 4.0F, u1 = (k + 1) / 4.0F;
               vert(vc, m, p, a.add(c0), u0, v, n, light);
               vert(vc, m, p, b.add(c0), u0, v2, n, light);
               vert(vc, m, p, b.add(c1), u1, v2, n, light);
               vert(vc, m, p, a.add(c1), u1, v, n, light);
            }
            v = v2;
         }
      }

      private static void vert(VertexConsumer vc, Matrix4f m, PoseStack.Pose p, Vec3 at, float u, float v, Vec3 n, int light) {
         vc.addVertex(m, (float) at.x, (float) at.y, (float) at.z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
            .setNormal(p, (float) n.x, (float) n.y, (float) n.z);
      }

      /** calcPoints: from the grapple (origin) to the thrower's hand, swaying by the grapple's ampl. */
      private static List<Vec3> calcPoints(LivingEntity thrower, GrappleEntity grapple, float pt) {
         List<Vec3> points = new ArrayList<>();
         if (thrower == null) return points;
         double tx = Mth.lerp(pt, thrower.xo, thrower.getX());
         double ty = Mth.lerp(pt, thrower.yo, thrower.getY());
         double tz = Mth.lerp(pt, thrower.zo, thrower.getZ());
         Minecraft mc = Minecraft.getInstance();
         if (mc.options.getCameraType().isFirstPerson() && thrower == mc.getCameraEntity()) {
            double yy = Mth.lerp(pt, thrower.yRotO, thrower.getYRot());
            double side = grapple.hand == InteractionHand.MAIN_HAND ? 1 : -1;
            double px = -Mth.cos((float) ((yy - 0.5) / 180.0 * Math.PI)) * 0.1F * side;
            double pz = -Mth.sin((float) ((yy - 0.5) / 180.0 * Math.PI)) * 0.1F * side;
            Vec3 vl = thrower.getLookAngle();
            tx += px + vl.x / 5.0;
            ty += thrower.getEyeHeight() / 2.6 + vl.y / 5.0;
            tz += pz + vl.z / 5.0;
         }
         double gx = Mth.lerp(pt, grapple.xo, grapple.getX());
         double gy = Mth.lerp(pt, grapple.yo, grapple.getY());
         double gz = Mth.lerp(pt, grapple.zo, grapple.getZ());
         Vec3 ve = new Vec3(tx - gx, ty - gy + thrower.getBbHeight() / 2.0F, tz - gz);
         float length = (float) (ve.length() * 5.0);
         int steps = (int) length;
         points.add(Vec3.ZERO);
         for (int a = 1; a < steps - 1; a++) {
            float dist = a * (length / steps);
            float ss = 1.0F - a / (steps * 1.25F);
            double dx = (tx - gx) / steps * a + Mth.sin(dist / 10.0F) * grapple.ampl * ss;
            double dy = (ty - gy + thrower.getBbHeight() / 2.0F) / steps * a + Mth.sin(dist / 8.0F) * grapple.ampl * ss;
            double dz = (tz - gz) / steps * a + Mth.sin(dist / 6.0F) * grapple.ampl * ss;
            points.add(new Vec3(dx, dy, dz));
         }
         points.add(ve);
         points.add(ve);
         return points;
      }
   }
}
