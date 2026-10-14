package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.construct.ArcaneBoreEntity;
import thaumcraft.common.entities.construct.TurretCrossbowAdvancedEntity;
import thaumcraft.common.entities.construct.TurretCrossbowEntity;
import thaumcraft.common.entities.projectile.GolemDartEntity;

/** RenderTurretCrossbow (ModelCrossbow), RenderTurretCrossbowAdvanced (crossbow_advanced.obj), RenderArcaneBore (ModelArcaneBore) and RenderDart. */
public final class ConstructRenderers {
   public static final ModelLayerLocation CROSSBOW_LAYER = new ModelLayerLocation(tc("crossbow"), "main");
   public static final ModelLayerLocation BORE_LAYER = new ModelLayerLocation(tc("arcane_bore"), "main");

   private ConstructRenderers() {
   }

   static ResourceLocation tc(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   static float sqrtSin(float f) {
      return Mth.sin(Mth.sqrt(f) * (float) Math.PI * 2.0F);
   }

   // ------------------------------------------------------------------ ModelCrossbow

   private static CubeListBuilder box(int u, int v, float x, float y, float z, int w, int h, int d) {
      return CubeListBuilder.create().texOffs(u, v).mirror().addBox(x, y, z, w, h, d);
   }

   private static void tripod(PartDefinition root) {
      root.addOrReplaceChild("leg2", box(20, 10, -1, 1, -1, 2, 13, 2), PartPose.offsetAndRotation(0, 12, 0, (float) (Math.PI / 6), 1.570796F, 0));
      root.addOrReplaceChild("tripod", box(13, 0, -1.5F, 0, -1.5F, 3, 2, 3), PartPose.offset(0, 12, 0));
      root.addOrReplaceChild("leg3", box(20, 10, -1, 1, -1, 2, 13, 2), PartPose.offsetAndRotation(0, 12, 0, (float) (Math.PI / 6), 3.141593F, 0));
      root.addOrReplaceChild("leg4", box(20, 10, -1, 1, -1, 2, 13, 2),
         PartPose.offsetAndRotation(0, 12, 0, (float) (Math.PI / 6), (float) (Math.PI * 3.0 / 2.0), 0));
      root.addOrReplaceChild("leg1", box(20, 10, -1, 1, -1, 2, 13, 2), PartPose.offsetAndRotation(0, 12, 0, (float) (Math.PI / 6), 0, 0));
   }

   public static LayerDefinition crossbowLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition cb = root.addOrReplaceChild("crossbow", box(28, 14, -2, 0, -7, 4, 2, 14), PartPose.offset(0, 10, 0));
      cb.addOrReplaceChild("ammobox", box(38, 0, -2, -5, -6, 4, 5, 9), PartPose.ZERO);
      cb.addOrReplaceChild("barrel", box(20, 28, -1, -1, -8, 2, 2, 2), PartPose.ZERO);
      cb.addOrReplaceChild("basebarcross", box(0, 13, -2, 0.5F, 10, 4, 1, 1), PartPose.ZERO);
      cb.addOrReplaceChild("basebarr", box(40, 23, -1, 0, 7, 1, 2, 5), PartPose.rotation(0, -0.1396263F, 0));
      cb.addOrReplaceChild("basebarl", box(40, 23, 0, 0, 7, 1, 2, 5), PartPose.rotation(0, 0.1396263F, 0));
      cb.addOrReplaceChild("loadbarcross", box(0, 13, -2, -8.5F, -0.5F, 4, 1, 1), PartPose.rotation(-0.5585054F, 0, 0));
      cb.addOrReplaceChild("loadbarl", box(0, 15, 2, -9, -1, 1, 11, 2), PartPose.rotation(-0.5585054F, 0, 0));
      cb.addOrReplaceChild("loadbarr", box(0, 15, -3, -9, -1, 1, 11, 2), PartPose.rotation(-0.5585054F, 0, 0));
      cb.addOrReplaceChild("crossl1", box(0, 0, 0, 0, -6, 5, 2, 1), PartPose.rotation(0, -0.2443461F, 0));
      cb.addOrReplaceChild("crossl2", box(0, 0, 4, 0, -5, 3, 2, 1), PartPose.rotation(0, -0.2443461F, 0));
      cb.addOrReplaceChild("crossl3", box(0, 0, 6, 0, -4, 2, 2, 1), PartPose.rotation(0, -0.2443461F, 0));
      cb.addOrReplaceChild("crossr1", box(0, 0, -5, 0, -6, 5, 2, 1), PartPose.rotation(0, 0.2443461F, 0));
      cb.addOrReplaceChild("crossr2", box(0, 0, -7, 0, -5, 3, 2, 1), PartPose.rotation(0, 0.2443461F, 0));
      cb.addOrReplaceChild("crossr3", box(0, 0, -8, 0, -4, 2, 2, 1), PartPose.rotation(0, 0.2443461F, 0));
      tripod(root);
      return LayerDefinition.create(mesh, 64, 32);
   }

   public static class CrossbowModel extends EntityModel<TurretCrossbowEntity> {
      private final ModelPart root, crossbow, loadbarcross, loadbarl, loadbarr;
      private final ModelPart[] crossL, crossR, legs;

      public CrossbowModel(ModelPart root) {
         this.root = root;
         crossbow = root.getChild("crossbow");
         loadbarcross = crossbow.getChild("loadbarcross");
         loadbarl = crossbow.getChild("loadbarl");
         loadbarr = crossbow.getChild("loadbarr");
         crossL = new ModelPart[]{crossbow.getChild("crossl1"), crossbow.getChild("crossl2"), crossbow.getChild("crossl3")};
         crossR = new ModelPart[]{crossbow.getChild("crossr1"), crossbow.getChild("crossr2"), crossbow.getChild("crossr3")};
         legs = new ModelPart[]{root.getChild("leg1"), root.getChild("leg2"), root.getChild("leg3"), root.getChild("leg4")};
      }

      @Override
      public void setupAnim(TurretCrossbowEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
         crossbow.yRot = netHeadYaw * Mth.DEG_TO_RAD;
         crossbow.xRot = headPitch * Mth.DEG_TO_RAD;
         float s = sqrtSin(attackTime);
         for (ModelPart p : crossL) p.yRot = -0.2F + s * 0.2F;
         for (ModelPart p : crossR) p.yRot = 0.2F - s * 0.2F;
         float lp = e.loadProgressForRender;
         loadbarcross.xRot = loadbarl.xRot = loadbarr.xRot = -0.5F + sqrtSin(lp) * 0.5F;
         boolean cart = e.isPassenger() && e.getVehicle() instanceof AbstractMinecart;
         for (ModelPart leg : legs) {
            leg.y = cart ? 12.0F - 8.0F : 12.0F;
            leg.xRot = cart ? 0.1F : 0.5F;
         }
      }

      @Override
      public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
         root.render(pose, vc, light, overlay, color);
      }
   }

   public static class TurretCrossbow extends LivingEntityRenderer<TurretCrossbowEntity, CrossbowModel> {
      private static final ResourceLocation TEX = tc("textures/entity/crossbow.png");

      public TurretCrossbow(EntityRendererProvider.Context ctx) {
         super(ctx, new CrossbowModel(ctx.bakeLayer(CROSSBOW_LAYER)), 0.5F);
      }

      @Override
      public void render(TurretCrossbowEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         e.loadProgressForRender = e.getLoadProgress(pt);
         e.yBodyRot = 0.0F;
         e.yBodyRotO = 0.0F;
         super.render(e, yaw, pt, pose, buffers, light);
      }

      @Override
      protected boolean shouldShowName(TurretCrossbowEntity e) {
         return e.hasCustomName() && super.shouldShowName(e);
      }

      @Override
      public ResourceLocation getTextureLocation(TurretCrossbowEntity e) {
         return TEX;
      }
   }

   // ------------------------------------------------------------------ crossbow_advanced.obj

   /** A minimal Wavefront OBJ (TC's WavefrontObject): groups of faces with positions and texture coordinates. */
   public static final class ObjModel {
      private record Vert(Vector3f pos, float u, float v) {
      }

      private final Map<String, List<Vert[]>> groups = new HashMap<>();

      public ObjModel(ResourceLocation loc) {
         List<Vector3f> vs = new ArrayList<>();
         List<float[]> vts = new ArrayList<>();
         List<Vert[]> cur = null;
         try (var in = Minecraft.getInstance().getResourceManager().open(loc);
              var r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
               line = line.trim().replaceAll("\\s+", " ");
               if (line.startsWith("v ")) {
                  String[] t = line.split(" ");
                  vs.add(new Vector3f(Float.parseFloat(t[1]), Float.parseFloat(t[2]), Float.parseFloat(t[3])));
               } else if (line.startsWith("vt ")) {
                  String[] t = line.split(" ");
                  vts.add(new float[]{Float.parseFloat(t[1]), 1.0F - Float.parseFloat(t[2])});
               } else if (line.startsWith("g ") || line.startsWith("o ")) {
                  cur = groups.computeIfAbsent(line.substring(2).trim(), k -> new ArrayList<>());
               } else if (line.startsWith("f ")) {
                  if (cur == null) cur = groups.computeIfAbsent("Default", k -> new ArrayList<>());
                  String[] t = line.split(" ");
                  Vert[] face = new Vert[t.length - 1];
                  for (int i = 1; i < t.length; i++) {
                     String[] idx = t[i].split("/");
                     Vector3f p = vs.get(Integer.parseInt(idx[0]) - 1);
                     float[] uv = idx.length > 1 && !idx[1].isEmpty() ? vts.get(Integer.parseInt(idx[1]) - 1) : new float[]{0, 0};
                     face[i - 1] = new Vert(p, uv[0], uv[1]);
                  }
                  cur.add(face);
               }
            }
         } catch (Exception ex) {
            Thaumcraft.LOGGER.error("Could not load OBJ model {}", loc, ex);
         }
      }

      /** renderPart: each face as a quad (triangles repeat their last vertex), with its flat normal. */
      public void renderPart(String name, PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
         List<Vert[]> faces = groups.get(name);
         if (faces == null) return;
         Matrix4f m = pose.last().pose();
         for (Vert[] f : faces) {
            if (f.length < 3) continue;
            Vector3f a = new Vector3f(f[1].pos()).sub(f[0].pos());
            Vector3f b = new Vector3f(f[2].pos()).sub(f[0].pos());
            Vector3f n = a.cross(b);
            if (n.lengthSquared() > 0) n.normalize();
            for (int i = 0; i < 4; i++) {
               Vert v = f[Math.min(i, f.length - 1)];
               vc.addVertex(m, v.pos().x, v.pos().y, v.pos().z).setColor(color).setUv(v.u(), v.v()).setOverlay(overlay).setLight(light)
                  .setNormal(pose.last(), n.x, n.y, n.z);
            }
         }
      }
   }

   public static class TurretCrossbowAdvanced extends EntityRenderer<TurretCrossbowAdvancedEntity> {
      private static final ResourceLocation TEX = tc("textures/entity/crossbow_advanced.png");
      private static ObjModel model;

      public TurretCrossbowAdvanced(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.5F;
      }

      private static ObjModel model() {
         if (model == null) model = new ObjModel(tc("models/obj/crossbow_advanced.obj"));
         return model;
      }

      @Override
      public void render(TurretCrossbowAdvancedEntity turret, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         turret.loadProgressForRender = turret.getLoadProgress(pt);
         ObjModel obj = model();
         VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX));
         int overlay = OverlayTexture.NO_OVERLAY;
         pose.pushPose();
         pose.translate(0.0, 0.75, 0.0);
         pose.pushPose();
         if (turret.isPassenger() && turret.getVehicle() instanceof AbstractMinecart) pose.scale(0.66F, 0.75F, 0.66F);
         obj.renderPart("legs", pose, vc, light, overlay, 0xFFFFFFFF);
         pose.popPose();
         pose.pushPose();
         int color = 0xFFFFFFFF;
         if (turret.hurtTime > 0) {
            color = 0xFFFF7F7F;
            float jiggle = turret.hurtTime / 500.0F;
            pose.translate(turret.getRandom().nextGaussian() * jiggle, turret.getRandom().nextGaussian() * jiggle,
               turret.getRandom().nextGaussian() * jiggle);
         }
         RenderUtil.rot(pose, Mth.lerp(pt, turret.yHeadRotO, turret.yHeadRot), 0.0F, -1.0F, 0.0F);
         RenderUtil.rot(pose, Mth.lerp(pt, turret.xRotO, turret.getXRot()), 1.0F, 0.0F, 0.0F);
         obj.renderPart("mech", pose, vc, light, overlay, color);
         obj.renderPart("box", pose, vc, light, overlay, color);
         obj.renderPart("shield", pose, vc, light, overlay, color);
         obj.renderPart("brain", pose, vc, light, overlay, color);
         pose.pushPose();
         pose.translate(0.0, 0.0, sqrtSin(turret.loadProgressForRender) / 12.0F);
         obj.renderPart("loader", pose, vc, light, overlay, color);
         pose.popPose();
         float sp = sqrtSin(turret.getAttackAnim(pt)) * 20.0F;
         pose.translate(0.0, 0.0, 0.375);
         pose.pushPose();
         RenderUtil.rot(pose, sp, 0.0F, 1.0F, 0.0F);
         obj.renderPart("bow1", pose, vc, light, overlay, color);
         pose.popPose();
         pose.pushPose();
         RenderUtil.rot(pose, sp, 0.0F, -1.0F, 0.0F);
         obj.renderPart("bow2", pose, vc, light, overlay, color);
         pose.popPose();
         pose.popPose();
         pose.popPose();
         super.render(turret, yaw, pt, pose, buffers, light);
      }

      @Override
      public ResourceLocation getTextureLocation(TurretCrossbowAdvancedEntity e) {
         return TEX;
      }
   }

   // ------------------------------------------------------------------ ModelArcaneBore

   public static LayerDefinition boreLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      tripod(root);
      PartDefinition base = root.addOrReplaceChild("base", box(32, 0, -3, -6, -3, 6, 6, 6), PartPose.offset(0, 13, 0));
      base.addOrReplaceChild("crystal", box(32, 25, -1, -4, 5, 2, 2, 2), PartPose.ZERO);
      base.addOrReplaceChild("dome", box(44, 16, -2, -5, 4, 4, 4, 4), PartPose.ZERO);
      base.addOrReplaceChild("domebase", box(32, 19, -2, -5, 3, 4, 4, 1), PartPose.ZERO);
      base.addOrReplaceChild("magbase", box(0, 18, -1, -4, -6, 2, 2, 3), PartPose.ZERO);
      base.addOrReplaceChild("tip", box(0, 9, -1.5F, 0, -1.5F, 3, 3, 3), PartPose.offsetAndRotation(0, -3, -6, -1.570796F, 0, 0));
      return LayerDefinition.create(mesh, 64, 32);
   }

   public static class BoreModel extends EntityModel<ArcaneBoreEntity> {
      private final ModelPart root, base;

      public BoreModel(ModelPart root) {
         super(RenderType::entityTranslucent);
         this.root = root;
         base = root.getChild("base");
      }

      @Override
      public void setupAnim(ArcaneBoreEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
         base.yRot = netHeadYaw * Mth.DEG_TO_RAD;
         base.xRot = headPitch * Mth.DEG_TO_RAD;
      }

      @Override
      public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
         root.render(pose, vc, light, overlay, color);
      }
   }

   public static class ArcaneBore extends LivingEntityRenderer<ArcaneBoreEntity, BoreModel> {
      private static final ResourceLocation TEX = tc("textures/entity/arcanebore.png");
      private static final ResourceLocation BEAM = tc("textures/misc/beam1.png");
      private static final ResourceLocation NODES = tc("textures/misc/auranodes.png");

      public ArcaneBore(EntityRendererProvider.Context ctx) {
         super(ctx, new BoreModel(ctx.bakeLayer(BORE_LAYER)), 0.5F);
      }

      @Override
      protected boolean shouldShowName(ArcaneBoreEntity e) {
         return e.hasCustomName() && super.shouldShowName(e);
      }

      @Override
      public ResourceLocation getTextureLocation(ArcaneBoreEntity e) {
         return TEX;
      }

      private static void vtx(VertexConsumer vc, PoseStack pose, float x, float y, float z, float u, float v, int argb) {
         vc.addVertex(pose.last().pose(), x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 1, 0);
      }

      @Override
      public void render(ArcaneBoreEntity bore, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         bore.yBodyRot = 0.0F;
         bore.yBodyRotO = 0.0F;
         super.render(bore, yaw, pt, pose, buffers, light);
         if (!(bore.clientDigging && bore.isActive() && bore.validInventory())) return;
         pose.pushPose();
         int part = bore.tickCount % 32;
         Vec3 lv2 = new Vec3(0.5, 0.075, 0.0);
         lv2 = rotZ(lv2, bore.getXRot() / 180.0F * (float) Math.PI);
         lv2 = rotY(lv2, -((bore.getYHeadRot() + 90.0F) / 180.0F * (float) Math.PI));
         pose.translate(lv2.x, bore.getEyeHeight() + lv2.y, lv2.z);
         // the glowing node at the tip (renderBillboardQuad 0.5, 32x32 grid of auranodes.png)
         pose.pushPose();
         pose.mulPose(entityRenderDispatcher.cameraOrientation());
         int frame = 96 + part;
         float f1 = (frame % 32) / 32.0F, f2 = f1 + 1.0F / 32.0F, f3 = (frame / 32) / 32.0F, f4 = f3 + 1.0F / 32.0F;
         int nc = 0xCC00FF66;
         VertexConsumer nv = buffers.getBuffer(thaumcraft.client.compat.OverlayRenderTypes.glow(NODES));
         vtx(nv, pose, -0.5F, -0.5F, 0, f2, f4, nc);
         vtx(nv, pose, -0.5F, 0.5F, 0, f2, f3, nc);
         vtx(nv, pose, 0.5F, 0.5F, 0, f1, f3, nc);
         vtx(nv, pose, 0.5F, -0.5F, 0, f1, f4, nc);
         pose.popPose();
         // the spinning beam (three crossed quads of beam1.png, scrolling)
         float rot = (bore.level().getGameTime() % 72L * 5L) + 5.0F * pt;
         float var11 = -(bore.tickCount + pt);
         float var12 = -var11 * 0.2F - Mth.floor(-var11 * 0.1F);
         float ry = Mth.lerp(pt, bore.yRotO, bore.getYRot());
         float rp = Mth.lerp(pt, bore.xRotO, bore.getXRot());
         RenderUtil.rot(pose, 90.0F, -1.0F, 0.0F, 0.0F);
         RenderUtil.rot(pose, 180.0F + ry, 0.0F, 0.0F, -1.0F);
         RenderUtil.rot(pose, rp, -1.0F, 0.0F, 0.0F);
         RenderUtil.rot(pose, rot, 0.0F, 1.0F, 0.0F);
         VertexConsumer bv = buffers.getBuffer(thaumcraft.client.compat.OverlayRenderTypes.glow(BEAM));
         float len = 5.0F;
         int bc = 0x6600FF66;
         for (int t = 0; t < 3; t++) {
            float v0 = -1.0F + var12 + t / 3.0F;
            float v1 = len + v0;
            RenderUtil.rot(pose, 60.0F, 0.0F, 1.0F, 0.0F);
            vtx(bv, pose, 0, len, 0, 1, v1, bc);
            vtx(bv, pose, -0.15F, 0, 0, 1, v0, bc);
            vtx(bv, pose, 0.15F, 0, 0, 0, v0, bc);
            vtx(bv, pose, 0, len, 0, 0, v1, bc);
            // back face (GL_CULL_FACE was disabled)
            vtx(bv, pose, 0, len, 0, 0, v1, bc);
            vtx(bv, pose, 0.15F, 0, 0, 0, v0, bc);
            vtx(bv, pose, -0.15F, 0, 0, 1, v0, bc);
            vtx(bv, pose, 0, len, 0, 1, v1, bc);
         }
         pose.popPose();
      }

      static Vec3 rotY(Vec3 v, float a) {
         float c = Mth.cos(a), s = Mth.sin(a);
         return new Vec3(v.x * c + v.z * s, v.y, v.z * c - v.x * s);
      }

      static Vec3 rotZ(Vec3 v, float a) {
         float c = Mth.cos(a), s = Mth.sin(a);
         return new Vec3(v.x * c + v.y * s, v.y * c - v.x * s, v.z);
      }
   }

   // ------------------------------------------------------------------ RenderDart

   /** RenderDart: a small arrow (0.033 scale instead of 0.05625) with the vanilla arrow texture. */
   public static class Dart extends ArrowRenderer<GolemDartEntity> {
      private static final ResourceLocation TEX = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

      public Dart(EntityRendererProvider.Context ctx) {
         super(ctx);
      }

      @Override
      public void render(GolemDartEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         pose.pushPose();
         pose.scale(0.033F / 0.05625F, 0.033F / 0.05625F, 0.033F / 0.05625F);
         super.render(e, yaw, pt, pose, buffers, light);
         pose.popPose();
      }

      @Override
      public ResourceLocation getTextureLocation(GolemDartEntity e) {
         return TEX;
      }
   }
}
