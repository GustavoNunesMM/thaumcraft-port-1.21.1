package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.fx.FXGeneric;
import thaumcraft.client.fx.FocusEntityFX;
import thaumcraft.common.blocks.crafting.FocalManipulatorBlockEntity;
import thaumcraft.common.entities.monster.SpellBatEntity;
import thaumcraft.common.entities.projectile.FocusMineEntity;
import thaumcraft.common.entities.projectile.FocusProjectileEntity;
import thaumcraft.common.items.VisCrystalItem;

/** RenderNoProjectile (focus projectile), RenderFocusMine, RenderSpellBat and TileFocalManipulatorRenderer. */
public final class CasterRenderers {
   private CasterRenderers() {
   }

   private static ResourceLocation tc(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   public static final ModelLayerLocation MINE_LAYER = new ModelLayerLocation(tc("focus_mine"), "main");
   public static final ModelLayerLocation BAT_LAYER = new ModelLayerLocation(tc("spell_bat"), "main");

   /** ModelGrappler: a small cube with three crossed prongs. */
   public static LayerDefinition mineLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3), PartPose.ZERO);
      root.addOrReplaceChild("prong1", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5), PartPose.ZERO);
      root.addOrReplaceChild("prong2", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5),
         PartPose.rotation(0.0F, (float) (Math.PI / 2), 0.0F));
      root.addOrReplaceChild("prong3", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5),
         PartPose.rotation((float) (Math.PI / 2), (float) (Math.PI / 2), 0.0F));
      return LayerDefinition.create(mesh, 64, 32);
   }

   /** ModelFireBat. */
   public static LayerDefinition batLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -3, 6, 6, 6), PartPose.ZERO);
      head.addOrReplaceChild("ear_right", CubeListBuilder.create().texOffs(24, 0).addBox(-4, -6, -2, 3, 4, 1), PartPose.ZERO);
      head.addOrReplaceChild("ear_left", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(1, -6, -2, 3, 4, 1), PartPose.ZERO);
      PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-3, 4, -3, 6, 12, 6)
         .texOffs(0, 34).addBox(-5, 16, 0, 10, 6, 1), PartPose.ZERO);
      PartDefinition rw = body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(42, 0).addBox(-12, 1, 1.5F, 10, 16, 1), PartPose.ZERO);
      rw.addOrReplaceChild("right_wing_tip", CubeListBuilder.create().texOffs(24, 16).addBox(-8, 1, 0, 8, 12, 1), PartPose.offset(-12, 1, 1.5F));
      PartDefinition lw = body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(42, 0).mirror().addBox(2, 1, 1.5F, 10, 16, 1),
         PartPose.ZERO);
      lw.addOrReplaceChild("left_wing_tip", CubeListBuilder.create().texOffs(24, 16).mirror().addBox(0, 1, 0, 8, 12, 1), PartPose.offset(12, 1, 1.5F));
      return LayerDefinition.create(mesh, 64, 64);
   }

   // ------------------------------------------------------------------ projectile

   /** RenderNoProjectile: nothing but the particle trail, drawn as it is rendered. */
   public static class Projectile extends EntityRenderer<FocusProjectileEntity> {
      public Projectile(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.1F;
      }

      @Override
      public void render(FocusProjectileEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         float qq = pt - e.lastRenderTick;
         if (qq < 0.0F) qq++;
         if (qq > 0.2F) FocusEntityFX.projectile(e, pt);
      }

      @Override
      public ResourceLocation getTextureLocation(FocusProjectileEntity e) {
         return FXGeneric.TEXTURE;
      }
   }

   // ------------------------------------------------------------------ mine

   public static class Mine extends EntityRenderer<FocusMineEntity> {
      private static final ResourceLocation TEX = tc("textures/entity/mine.png");
      private final ModelPart model;

      public Mine(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.0F;
         model = ctx.bakeLayer(MINE_LAYER);
      }

      @Override
      public void render(FocusMineEntity mine, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         pose.pushPose();
         float f = (mine.counter + pt) % 8.0F / 8.0F;
         RenderUtil.rot(pose, Mth.lerp(pt, mine.yRotO, mine.getYRot()) - 90.0F, 0.0F, 1.0F, 0.0F);
         RenderUtil.rot(pose, Mth.lerp(pt, mine.xRotO, mine.getXRot()), 0.0F, 0.0F, 1.0F);
         VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX));
         int c = FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 1.0F - f, 1.0F - f);
         model.render(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, c);
         pose.popPose();
      }

      @Override
      public ResourceLocation getTextureLocation(FocusMineEntity e) {
         return TEX;
      }
   }

   // ------------------------------------------------------------------ spell bat

   static class BatModel extends EntityModel<SpellBatEntity> {
      private final ModelPart root, head, body, rightWing, leftWing, rightTip, leftTip;

      BatModel(ModelPart root) {
         super(RenderType::entityTranslucent);
         this.root = root;
         head = root.getChild("head");
         body = root.getChild("body");
         rightWing = body.getChild("right_wing");
         leftWing = body.getChild("left_wing");
         rightTip = rightWing.getChild("right_wing_tip");
         leftTip = leftWing.getChild("left_wing_tip");
      }

      @Override
      public void setupAnim(SpellBatEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
         head.xRot = headPitch * Mth.DEG_TO_RAD;
         head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
         head.zRot = 0.0F;
         body.xRot = (float) (Math.PI / 4) + Mth.cos(age * 0.1F) * 0.15F;
         body.yRot = 0.0F;
         rightWing.yRot = Mth.cos(age * 1.3F) * (float) Math.PI * 0.25F;
         leftWing.yRot = -rightWing.yRot;
         rightTip.yRot = rightWing.yRot * 0.5F;
         leftTip.yRot = -rightWing.yRot * 0.5F;
      }

      @Override
      public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
         root.render(pose, vc, light, overlay, color);
      }
   }

   /** RenderSpellBat: a small translucent bat tinted with the package's colour, always lit. */
   public static class SpellBat extends EntityRenderer<SpellBatEntity> {
      private static final ResourceLocation TEX = tc("textures/entity/spellbat.png");
      private final BatModel model;

      public SpellBat(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.25F;
         model = new BatModel(ctx.bakeLayer(BAT_LAYER));
      }

      @Override
      public void render(SpellBatEntity bat, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         pose.pushPose();
         float bodyYaw = Mth.rotLerp(pt, bat.yBodyRotO, bat.yBodyRot);
         float headYaw = Mth.rotLerp(pt, bat.yHeadRotO, bat.yHeadRot);
         float age = bat.tickCount + pt;
         pose.translate(0.0F, -0.1F, 0.0F);
         RenderUtil.rot(pose, 180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
         pose.scale(-1.0F, -1.0F, 1.0F);
         pose.scale(0.35F, 0.35F, 0.35F);
         pose.translate(0.0F, -1.501F, 0.0F);
         model.setupAnim(bat, 0, 0, age, Mth.wrapDegrees(headYaw - bodyYaw), Mth.lerp(pt, bat.xRotO, bat.getXRot()));
         int c = 0x80000000 | (bat.color & 0xFFFFFF);
         model.renderToBuffer(pose, buffers.getBuffer(RenderType.entityTranslucent(TEX)), LightTexture.FULL_BRIGHT,
            OverlayTexture.pack(0.0F, bat.hurtTime > 0 || bat.deathTime > 0), c);
         pose.popPose();
         super.render(bat, yaw, pt, pose, buffers, light);
      }

      @Override
      public ResourceLocation getTextureLocation(SpellBatEntity e) {
         return TEX;
      }
   }

   // ------------------------------------------------------------------ focal manipulator

   /** The focus floats and turns over the table; while crafting, the crystals used circle it, casting rays. */
   public static class Manipulator implements BlockEntityRenderer<FocalManipulatorBlockEntity> {
      public Manipulator(BlockEntityRendererProvider.Context ctx) {
      }

      @Override
      public void render(FocalManipulatorBlockEntity table, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         if (table.getLevel() == null) return;
         Minecraft mc = Minecraft.getInstance();
         float ticks = (mc.player != null ? mc.player.tickCount : 0) + pt;
         ItemStack focus = table.getFocus();
         if (!focus.isEmpty()) {
            pose.pushPose();
            float hoverStart = Mth.sin(ticks / 14.0F) * 0.2F + 0.2F;
            pose.translate(0.5F, 0.8F + Mth.sin(hoverStart) * 0.1F + 0.1F + 0.125F, 0.5F);
            RenderUtil.rot(pose, ticks % 360.0F, 0.0F, 1.0F, 0.0F);
            pose.scale(0.5F, 0.5F, 0.5F);
            mc.getItemRenderer().renderStatic(focus.copyWithCount(1), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers,
               table.getLevel(), 0);
            pose.popPose();
         }
         Aspect[] crystals = table.crystalsSync.getAspects();
         int q = crystals.length;
         if (q == 0) return;
         float ang = 360.0F / q;
         for (int a = 0; a < q; a++) {
            float angle = ticks % 720.0F / 2.0F + ang * a;
            float bob = Mth.sin((ticks + a * 10) / 12.0F) * 0.02F + 0.02F;
            int col = crystals[a].getColor();
            float r = (col >> 16 & 255) / 255.0F, g = (col >> 8 & 255) / 255.0F, b = (col & 255) / 255.0F;
            pose.pushPose();
            pose.translate(0.5F, 1.3F, 0.5F);
            RenderUtil.rot(pose, angle, 0.0F, 1.0F, 0.0F);
            pose.translate(0.0F, bob, 0.4F);
            RenderUtil.rot(pose, -angle, 0.0F, 1.0F, 0.0F);
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            billboard(pose, buffers, 0.175F, 320 + (mc.player != null ? mc.player.tickCount : 0) % 16, r, g, b, 0.66F);
            pose.popPose();
            pose.pushPose();
            pose.translate(0.5F, 1.05F, 0.5F);
            RenderUtil.rot(pose, angle, 0.0F, 1.0F, 0.0F);
            pose.translate(0.0F, bob, 0.4F);
            pose.scale(0.5F, 0.5F, 0.5F);
            renderRay(pose, buffers, angle, a, bob, r, g, b, ticks);
            renderRay(pose, buffers, angle, (a + 1) * 5, bob, r, g, b, ticks);
            pose.translate(0.0F, 0.25F, 0.0F);
            mc.getItemRenderer().renderStatic(VisCrystalItem.of(crystals[a], 1), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose,
               buffers, table.getLevel(), 0);
            pose.popPose();
         }
      }

      /** UtilsFX.renderBillboardQuad: one frame of the 64x64 particle sheet, additive. */
      private static void billboard(PoseStack pose, MultiBufferSource buffers, float size, int frame, float r, float g, float b, float a) {
         VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(FXGeneric.TEXTURE));
         Matrix4f m = pose.last().pose();
         float u0 = (frame % 64) / 64.0F, v0 = (frame / 64) / 64.0F, u1 = u0 + 1 / 64.0F, v1 = v0 + 1 / 64.0F;
         int c = FastColor.ARGB32.colorFromFloat(a, r, g, b);
         vc.addVertex(m, -size, -size, 0).setColor(c).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
            .setNormal(pose.last(), 0, 0, 1);
         vc.addVertex(m, -size, size, 0).setColor(c).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
            .setNormal(pose.last(), 0, 0, 1);
         vc.addVertex(m, size, size, 0).setColor(c).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
            .setNormal(pose.last(), 0, 0, 1);
         vc.addVertex(m, size, -size, 0).setColor(c).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
            .setNormal(pose.last(), 0, 0, 1);
      }

      /** A fading cone of light from the crystal (a triangle fan in the original, quads with RenderType.lightning here). */
      private static void renderRay(PoseStack pose, MultiBufferSource buffers, float angle, int num, float lift, float r, float g, float b, float ticks) {
         Random random = new Random(187L + (long) num * num);
         pose.pushPose();
         float pan = Mth.sin((ticks + num * 10) / 15.0F) * 15.0F;
         float aparture = Mth.sin((ticks + num * 10) / 14.0F) * 2.0F;
         pose.translate(0.0F, 0.475F + lift, 0.0F);
         RenderUtil.rot(pose, 90.0F, -1.0F, 0.0F, 0.0F);
         RenderUtil.rot(pose, angle, 0.0F, 1.0F, 0.0F);
         RenderUtil.rot(pose, random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
         RenderUtil.rot(pose, pan, 1.0F, 0.0F, 0.0F);
         float fa = random.nextFloat() * 20.0F + 10.0F;
         float f4 = random.nextFloat() * 4.0F + 6.0F + aparture;
         fa /= 30.0F / (Math.min(ticks, 10.0F) / 10.0F);
         f4 /= 30.0F / (Math.min(ticks, 10.0F) / 10.0F);
         VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
         Matrix4f m = pose.last().pose();
         float[][] rim = {{-0.8F * f4, fa, -0.5F * f4}, {0.8F * f4, fa, -0.5F * f4}, {0.0F, fa, 1.0F * f4}, {-0.8F * f4, fa, -0.5F * f4}};
         for (int i = 0; i < 3; i++) {
            vc.addVertex(m, 0, 0, 0).setColor(r, g, b, 0.66F);
            vc.addVertex(m, 0, 0, 0).setColor(r, g, b, 0.66F);
            vc.addVertex(m, rim[i][0], rim[i][1], rim[i][2]).setColor(r, g, b, 0.0F);
            vc.addVertex(m, rim[i + 1][0], rim[i + 1][1], rim[i + 1][2]).setColor(r, g, b, 0.0F);
         }
         pose.popPose();
      }
   }
}
