package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.crafting.VoidSiphonBlockEntity;
import thaumcraft.common.entities.FallingTaintEntity;
import thaumcraft.common.entities.FluxRiftEntity;
import thaumcraft.common.entities.monster.tainted.TaintCrawlerEntity;
import thaumcraft.common.entities.monster.tainted.TaintSeedEntity;
import thaumcraft.common.entities.monster.tainted.TaintSwarmEntity;
import thaumcraft.common.entities.monster.tainted.TaintacleEntity;

/**
 * Renderers of milestone 12. The tentacle models (ModelTaintacle / ModelTaintSeed with their custom ModelRenderers,
 * whose children are scaled down at every joint) are drawn by walking the segment chain by hand with the same GL
 * transforms; the rift (a GLE poly-cone under the end-portal shader) becomes a tube mesh drawn with the end portal
 * render type, plus additive glow shells.
 */
public final class TaintRenderers {
   public static final ModelLayerLocation SEGMENTS = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "taint_segments"), "main");

   private TaintRenderers() {
   }

   /** The four cube kinds of the tentacle models (each at its texture offset). */
   public static LayerDefinition segmentsLayer() {
      MeshDefinition mesh = new MeshDefinition();
      var root = mesh.getRoot();
      root.addOrReplaceChild("root", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -4, 8, 8, 8), PartPose.ZERO);
      root.addOrReplaceChild("tent", CubeListBuilder.create().texOffs(0, 16).addBox(-4, -4, -4, 8, 8, 8), PartPose.ZERO);
      root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 32).addBox(-6, -6, -6, 12, 12, 12), PartPose.ZERO);
      root.addOrReplaceChild("orb", CubeListBuilder.create().texOffs(0, 56).addBox(-2, -2, -2, 4, 4, 4), PartPose.ZERO);
      root.addOrReplaceChild("seed_mid", CubeListBuilder.create().texOffs(0, 48).addBox(-2, -2, -2, 4, 4, 4), PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 64);
   }

   // ================================================================== taintacle

   /** ModelTaintacle (not seed): root, length-1 segments, then the orb and the big head on the second-to-last segment. */
   public static class TaintacleModel extends EntityModel<TaintacleEntity> {
      private final ModelPart root, tent, head, orb;
      private final int length;
      private final float[] rotX, rotZ;
      private float entityHeight = 3.0F;
      private float raise;

      public TaintacleModel(ModelPart parts, int length) {
         super(RenderType::entityTranslucent);
         root = parts.getChild("root");
         tent = parts.getChild("tent");
         head = parts.getChild("head");
         orb = parts.getChild("orb");
         this.length = length;
         rotX = new float[length];
         rotZ = new float[length];
      }

      @Override
      public void setupAnim(TaintacleEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
         float flail = e.flailIntensity;
         float ht = e.hurtTime;
         float mod = 0.0625F * 0.2F;
         float fs = flail > 1.0F ? 3.0F : 1.0F - mod;
         float fi = flail + (ht > 0.0F ? mod : -mod);
         for (int k = 0; k < length - 1; k++) {
            rotX[k] = 0.15F * fi * Mth.sin(ageInTicks * 0.1F * fs - k / 2.0F);
            rotZ[k] = 0.1F / fi * Mth.sin(ageInTicks * 0.15F - k / 2.0F);
         }
         entityHeight = e.getBbHeight();
         float hc = entityHeight * 10.0F;
         raise = e.tickCount < hc ? (hc - e.tickCount) / hc * entityHeight : 0.0F;
      }

      @Override
      public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
         pose.pushPose();
         pose.translate(0.0F, (entityHeight == 3.0F ? 0.6F : 1.2F) + raise, 0.0F);
         pose.scale(entityHeight / 3.0F, entityHeight / 3.0F, entityHeight / 3.0F);
         pose.translate(0.0F, 12.0F / 16.0F, 0.0F);
         root.render(pose, vc, light, overlay, color);
         segment(pose, vc, light, overlay, color, 0, 0.88F);
         pose.popPose();
      }

      private void segment(PoseStack pose, VertexConsumer vc, int light, int overlay, int color, int k, float s) {
         pose.pushPose();
         pose.scale(s, s, s);
         pose.translate(0.0F, -8.0F / 16.0F, 0.0F);
         if (k < length - 1) {
            pose.mulPose(com.mojang.math.Axis.ZP.rotation(rotZ[k]));
            pose.mulPose(com.mojang.math.Axis.XP.rotation(rotX[k]));
         }
         if (k == length - 1) {
            head.render(pose, vc, light, overlay, color);
         } else {
            tent.render(pose, vc, light, overlay, color);
            if (k < length - 2) {
               segment(pose, vc, light, overlay, color, k + 1, s);
            } else {
               pose.pushPose();
               pose.scale(s, s, s);
               pose.translate(0.0F, -8.0F / 16.0F, 0.0F);
               orb.render(pose, vc, light, overlay, color);
               pose.popPose();
               segment(pose, vc, light, overlay, color, k + 1, s);
            }
         }
         pose.popPose();
      }
   }

   public static class TaintacleRenderer extends MobRenderer<TaintacleEntity, TaintacleModel> {
      private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/taintacle.png");

      public TaintacleRenderer(EntityRendererProvider.Context ctx, float shadow, int length) {
         super(ctx, new TaintacleModel(ctx.bakeLayer(SEGMENTS), length), shadow);
      }

      @Override
      public ResourceLocation getTextureLocation(TaintacleEntity entity) {
         return TEX;
      }
   }

   // ================================================================== taint seed

   /** ModelTaintSeed: a pulsing knot; each joint is scaled by its depth and a slow pulse (ModelRendererTaintSeed). */
   public static class TaintSeedModel extends EntityModel<TaintSeedEntity> {
      private static final int LENGTH = 8;
      private final ModelPart root, tent, small, mid;
      private final float[] rotX = new float[LENGTH], rotZ = new float[LENGTH];
      private float entityHeight = 1.25F;
      private float raise;
      private float tt;

      public TaintSeedModel(ModelPart parts) {
         super(RenderType::entityTranslucent);
         root = parts.getChild("root");
         tent = parts.getChild("tent");
         mid = parts.getChild("seed_mid");
         small = parts.getChild("orb");
      }

      @Override
      public void setupAnim(TaintSeedEntity seed, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
         float ht = seed.hurtTime / 200.0F;
         float mod = 0.0625F * 0.2F;
         float fi = (0.1F + (ht > 0.0F ? mod : -mod)) * 3.0F;
         for (int k = 0; k < LENGTH - 1; k++) {
            rotX[k] = 0.1F / fi * Mth.sin(ageInTicks * 0.06F - k / 2.0F) / 5.0F + ht + seed.attackAnim;
            rotZ[k] = 0.1F / fi * Mth.sin(ageInTicks * 0.05F - k / 2.0F) / 5.0F;
         }
         entityHeight = seed.getBbHeight();
         float hc = entityHeight * 10.0F;
         raise = seed.tickCount < hc ? (hc - seed.tickCount) / hc * entityHeight : 0.0F;
         tt = ageInTicks;
      }

      @Override
      public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
         pose.pushPose();
         pose.translate(0.0F, (entityHeight == 3.0F ? 0.6F : 1.2F) + raise, 0.0F);
         pose.scale(entityHeight / 2.0F, entityHeight / 2.0F, entityHeight / 2.0F);
         pose.translate(0.0F, 12.0F / 16.0F, 0.0F);
         root.render(pose, vc, light, overlay, color);
         node(pose, vc, light, overlay, color, 0);
         pose.popPose();
      }

      private void node(PoseStack pose, VertexConsumer vc, int light, int overlay, int color, int k) {
         if (k >= LENGTH - 1) return;
         float qq = (float) (Math.PI * (k / 7.0F));
         float scale = 1.6F - (float) Math.sin(qq);
         float pulse = (float) Math.sin(tt / 12.0F - qq) * 0.33F;
         pulse *= pulse;
         pose.pushPose();
         pose.scale(scale + pulse, scale * 0.9F, scale + pulse);
         pose.translate(0.0F, (k < LENGTH - 4 || k == LENGTH - 4 ? -8.0F : -4.0F) / 16.0F, 0.0F);
         pose.mulPose(com.mojang.math.Axis.ZP.rotation(rotZ[k]));
         pose.mulPose(com.mojang.math.Axis.XP.rotation(rotX[k]));
         (k < LENGTH - 4 ? tent : k == LENGTH - 4 ? mid : small).render(pose, vc, light, overlay, color);
         node(pose, vc, light, overlay, color, k + 1);
         pose.popPose();
      }
   }

   public static class TaintSeedRenderer extends MobRenderer<TaintSeedEntity, TaintSeedModel> {
      private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/taintseed.png");

      public TaintSeedRenderer(EntityRendererProvider.Context ctx, float shadow) {
         super(ctx, new TaintSeedModel(ctx.bakeLayer(SEGMENTS)), shadow);
      }

      @Override
      public ResourceLocation getTextureLocation(TaintSeedEntity entity) {
         return TEX;
      }

      @Override
      protected float getFlipDegrees(TaintSeedEntity entity) {
         return 0.0F;
      }
   }

   // ================================================================== crawler, swarm, falling taint

   public static class CrawlerRenderer extends MobRenderer<TaintCrawlerEntity, SilverfishModel<TaintCrawlerEntity>> {
      private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/crawler.png");

      public CrawlerRenderer(EntityRendererProvider.Context ctx) {
         super(ctx, new SilverfishModel<>(ctx.bakeLayer(ModelLayers.SILVERFISH)), 0.2F);
      }

      @Override
      protected void scale(TaintCrawlerEntity entity, PoseStack pose, float partialTick) {
         pose.scale(0.7F, 0.7F, 0.7F);
      }

      @Override
      protected float getFlipDegrees(TaintCrawlerEntity entity) {
         return 180.0F;
      }

      @Override
      public ResourceLocation getTextureLocation(TaintCrawlerEntity entity) {
         return TEX;
      }
   }

   /** RenderTaintSwarm: nothing; the swarm is its particles. */
   public static class SwarmRenderer extends EntityRenderer<TaintSwarmEntity> {
      public SwarmRenderer(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.0F;
      }

      @Override
      public ResourceLocation getTextureLocation(TaintSwarmEntity entity) {
         return TextureAtlas.LOCATION_BLOCKS;
      }
   }

   public static class FallingTaintRenderer extends EntityRenderer<FallingTaintEntity> {
      public FallingTaintRenderer(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.5F;
      }

      @Override
      public void render(FallingTaintEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         BlockState state = e.getBlock();
         if (state.isAir() || state == e.level().getBlockState(e.blockPosition())) return;
         pose.pushPose();
         pose.translate(-0.5, 0.0, -0.5);
         Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, pose, buffers, light, OverlayTexture.NO_OVERLAY);
         pose.popPose();
         super.render(e, yaw, pt, pose, buffers, light);
      }

      @Override
      public ResourceLocation getTextureLocation(FallingTaintEntity entity) {
         return TextureAtlas.LOCATION_BLOCKS;
      }
   }

   // ================================================================== flux rift

   public static class FluxRiftRenderer extends EntityRenderer<FluxRiftEntity> {
      public FluxRiftRenderer(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.0F;
      }

      @Override
      public boolean shouldRender(FluxRiftEntity e, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
         return e.points.size() > 2 && frustum.isVisible(e.getBoundingBoxForCulling().inflate(1.0));
      }

      @Override
      public void render(FluxRiftEntity rift, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         List<Vec3> pts = rift.points;
         if (pts.size() <= 2) return;
         float stab = Mth.clamp(1.0F - rift.getRiftStability() / 50.0F, 0.0F, 1.5F);
         int n = pts.size();
         Vec3[] pp = new Vec3[n];
         float[] radii = new float[n];
         float[] wobble = new float[n];
         for (int a = 0; a < n; a++) {
            float var = rift.tickCount + pt;
            if (a > n / 2) var -= a * 10;
            else if (a < n / 2) var += a * 10;
            Vec3 p = pts.get(a);
            pp[a] = new Vec3(p.x + Math.sin(var / 50.0F) * 0.1F * stab, p.y + Math.sin(var / 60.0F) * 0.1F * stab, p.z + Math.sin(var / 70.0F) * 0.1F * stab);
            wobble[a] = (float) (1.0 - Math.sin(var / 8.0F) * 0.1F * stab);
            radii[a] = rift.pointsWidth.get(a);
         }
         // The glow shells (q 0..2: additive, 1.25 / 1.75 / 2.25 times as wide), then the rift itself.
         Matrix4f m = pose.last().pose();
         VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
         for (int q = 0; q < 3; q++) tube(m, glow, pp, radii, wobble, 1.25F + 0.5F * q, true);
         tube(m, buffers.getBuffer(RenderType.endPortal()), pp, radii, wobble, 1.0F, false);
      }

      private static void tube(Matrix4f m, VertexConsumer vc, Vec3[] pp, float[] radii, float[] wobble, float mult, boolean colored) {
         int sides = 6;
         Vector3f[][] rings = new Vector3f[pp.length][sides];
         for (int i = 0; i < pp.length; i++) {
            Vec3 t = pp[Math.min(i + 1, pp.length - 1)].subtract(pp[Math.max(i - 1, 0)]);
            if (t.lengthSqr() < 1.0E-8) t = new Vec3(0, 1, 0);
            t = t.normalize();
            Vec3 u = Math.abs(t.y) < 0.9 ? t.cross(new Vec3(0, 1, 0)).normalize() : t.cross(new Vec3(1, 0, 0)).normalize();
            Vec3 v = t.cross(u);
            float r = radii[i] * wobble[i] * mult;
            for (int s = 0; s < sides; s++) {
               double ang = Math.PI * 2.0 * s / sides;
               Vec3 o = pp[i].add(u.scale(Math.cos(ang) * r)).add(v.scale(Math.sin(ang) * r));
               rings[i][s] = new Vector3f((float) o.x, (float) o.y, (float) o.z);
            }
         }
         for (int i = 0; i < pp.length - 1; i++) {
            for (int s = 0; s < sides; s++) {
               int s2 = (s + 1) % sides;
               vertex(m, vc, rings[i][s], colored);
               vertex(m, vc, rings[i + 1][s], colored);
               vertex(m, vc, rings[i + 1][s2], colored);
               vertex(m, vc, rings[i][s2], colored);
            }
         }
      }

      private static void vertex(Matrix4f m, VertexConsumer vc, Vector3f p, boolean colored) {
         var v = vc.addVertex(m, p.x, p.y, p.z);
         if (colored) v.setColor(0.35F, 0.1F, 0.45F, 0.12F);
      }

      @Override
      public ResourceLocation getTextureLocation(FluxRiftEntity entity) {
         return TextureAtlas.LOCATION_BLOCKS;
      }
   }

   // ================================================================== void siphon

   /** TileVoidSiphonRenderer: the end-portal "orb" and core while the siphon is enabled. */
   public static class VoidSiphonRenderer implements BlockEntityRenderer<VoidSiphonBlockEntity> {
      public VoidSiphonRenderer(BlockEntityRendererProvider.Context ctx) {
      }

      @Override
      public void render(VoidSiphonBlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         if (!DeviceBlock.isEnabled(be.getBlockState())) return;
         VertexConsumer vc = buffers.getBuffer(RenderType.endPortal());
         Matrix4f m = pose.last().pose();
         box(m, vc, 0.5F, 0.875F, 0.5F, 0.1F, 0.1F, 0.1F);
         box(m, vc, 0.5F, 0.3125F, 0.5F, 0.125F, 0.25F, 0.125F);
      }

      private static void box(Matrix4f m, VertexConsumer vc, float cx, float cy, float cz, float hx, float hy, float hz) {
         float x0 = cx - hx, x1 = cx + hx, y0 = cy - hy, y1 = cy + hy, z0 = cz - hz, z1 = cz + hz;
         quad(m, vc, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
         quad(m, vc, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
         quad(m, vc, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);
         quad(m, vc, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
         quad(m, vc, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
         quad(m, vc, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
      }

      private static void quad(Matrix4f m, VertexConsumer vc, float... c) {
         for (int i = 0; i < 12; i += 3) vc.addVertex(m, c[i], c[i + 1], c[i + 2]);
      }
   }
}
