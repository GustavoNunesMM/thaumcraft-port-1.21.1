package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.models.EldritchModelLayers;
import thaumcraft.common.blocks.eldritch.EldritchPortalBlockEntity;
import thaumcraft.common.blocks.eldritch.HoleBlockEntity;
import thaumcraft.common.entities.monster.boss.EldritchGolemEntity;
import thaumcraft.common.entities.monster.boss.EldritchWardenEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchCrabEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchGuardianEntity;
import thaumcraft.common.entities.projectile.EldritchOrbEntity;
import thaumcraft.common.registers.ModEldritch;
import thaumcraft.common.world.eldritch.OuterLands;

/**
 * Renderers of the eldritch milestone. The three creature models are generated from the 1.12 ModelRenderer code
 * (EldritchModelLayers); the animations below follow their setRotationAngles / setLivingAnimations one to one.
 */
public final class EldritchRenderers {
   public static final ModelLayerLocation GUARDIAN = layer("eldritch_guardian");
   public static final ModelLayerLocation GOLEM = layer("eldritch_golem");
   public static final ModelLayerLocation CRAB = layer("eldritch_crab");

   private EldritchRenderers() {
   }

   private static ModelLayerLocation layer(String name) {
      return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name), "main");
   }

   private static ResourceLocation tex(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/" + path);
   }

   // ================================================================== guardian / warden

   /** ModelEldritchGuardian: hooded, cloaked, the arms raised to cast, the leg panels and cloak swaying. */
   public static class GuardianModel<T extends Monster> extends EntityModel<T> {
      final ModelPart root;
      final Map<String, ModelPart> p;
      float alpha = 1.0F;

      public GuardianModel(ModelPart root) {
         super(RenderType::entityTranslucent);
         this.root = root;
         this.p = EldritchModelLayers.guardianParts(root);
         p.get("HoodEye").visible = false;
      }

      @Override
      public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
         ModelPart hood = p.get("Hood1");
         hood.yRot = netHeadYaw * Mth.DEG_TO_RAD;
         hood.xRot = headPitch * Mth.DEG_TO_RAD;
         p.get("HoodEye").xRot = hood.xRot;
         p.get("HoodEye").yRot = hood.yRot;
         float alr = 0.0F, all = 0.0F;
         if (entity instanceof EldritchGuardianEntity g) {
            alr = g.armLiftR;
            all = g.armLiftL;
         } else if (entity instanceof EldritchWardenEntity w) {
            alr = w.armLiftR;
            all = w.armLiftL;
         }
         p.get("ArmL1").xRot = -1.0F - all + Mth.sin((age + 20) / 10.0F) * 0.08F;
         p.get("ArmR1").xRot = -1.0F - alr + Mth.sin(age / 10.0F) * 0.08F;
         p.get("LegpanelC1").xRot = -0.15F + Mth.sin(age / 8.0F) * 0.12F;
         p.get("LegpanelC2").xRot = Mth.sin((age - 5.0F) / 8.0F) * 0.13F;
         p.get("LegpanelC3").xRot = Mth.sin((age - 10.0F) / 8.0F) * 0.14F;
         p.get("Cloak1").xRot = 0.2F + Mth.sin(age / 7.0F) * 0.08F;
         p.get("Cloak2").xRot = Mth.sin((age - 5.0F) / 7.0F) * 0.1F;
         p.get("Cloak3").xRot = Mth.sin((age - 10.0F) / 7.0F) * 0.12F;
         p.get("SidepanelL2").zRot = -0.2F + Mth.sin((age + 10) / 8.0F) * 0.12F;
         p.get("SidepanelL3").zRot = Mth.sin((age + 5.0F) / 8.0F) * 0.13F;
         p.get("SidepanelL4").zRot = Mth.sin(age / 8.0F) * 0.14F;
         p.get("SidepanelR2").zRot = 0.2F + Mth.sin((age - 5) / 8.0F) * 0.12F;
         p.get("SidepanelR3").zRot = Mth.sin((age - 10.0F) / 8.0F) * 0.13F;
         p.get("SidepanelR4").zRot = Mth.sin((age - 15.0F) / 8.0F) * 0.14F;
      }

      @Override
      public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
         int a = (int) (FastColor.ARGB32.alpha(color) * alpha);
         root.render(pose, vc, light, overlay, FastColor.ARGB32.color(a, color));
      }

      /** The warden's glowing eye, drawn additively and fullbright just outside the hood. */
      void renderEye(PoseStack pose, VertexConsumer vc, int light, int overlay) {
         ModelPart eye = p.get("HoodEye");
         eye.visible = true;
         pose.pushPose();
         pose.scale(1.01F, 1.01F, 1.01F);
         eye.render(pose, vc, light, overlay);
         pose.popPose();
         eye.visible = false;
      }
   }

   /** RenderEldritchGuardian: guardians fade with distance outside the Outer Lands; wardens rise out of the floor. */
   public static class GuardianRenderer<T extends Monster> extends MobRenderer<T, GuardianModel<T>> {
      private static final ResourceLocation GUARDIAN_TEX = tex("entity/eldritch_guardian.png");
      private static final ResourceLocation WARDEN_TEX = tex("entity/eldritch_warden.png");

      public GuardianRenderer(EntityRendererProvider.Context ctx, float shadow) {
         super(ctx, new GuardianModel<>(ctx.bakeLayer(GUARDIAN)), shadow);
         addLayer(new WardenEyeLayer<>(this));
      }

      @Override
      public ResourceLocation getTextureLocation(T entity) {
         return entity instanceof EldritchWardenEntity ? WARDEN_TEX : GUARDIAN_TEX;
      }

      @Override
      public void render(T e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         float base = 1.0F;
         pose.pushPose();
         if (e instanceof EldritchWardenEntity w) {
            pose.translate(0.0, -e.getBbHeight() * (w.getSpawnTimer() / 150.0F), 0.0);
         } else if (!OuterLands.isOuterLands(e.level())) {
            Entity view = Minecraft.getInstance().getCameraEntity();
            float d6 = e.level().getDifficulty() == Difficulty.HARD ? 576.0F : 1024.0F;
            float d7 = 256.0F;
            double d8 = view == null ? 0.0 : e.distanceToSqr(view);
            base = d8 < 256.0 ? 0.6F : (float) (1.0 - Math.min(d6 - d7, d8 - d7) / (d6 - d7)) * 0.6F;
         }
         model.alpha = base;
         super.render(e, yaw, pt, pose, buffers, light);
         model.alpha = 1.0F;
         pose.popPose();
      }

      @Override
      protected RenderType getRenderType(T e, boolean bodyVisible, boolean translucent, boolean glowing) {
         return RenderType.entityTranslucent(getTextureLocation(e));
      }
   }

   /** The warden's eye, as a render layer so it follows the body pose. */
   public static class WardenEyeLayer<T extends Monster> extends net.minecraft.client.renderer.entity.layers.RenderLayer<T, GuardianModel<T>> {
      public WardenEyeLayer(net.minecraft.client.renderer.entity.RenderLayerParent<T, GuardianModel<T>> parent) {
         super(parent);
      }

      @Override
      public void render(PoseStack pose, MultiBufferSource buffers, int light, T e, float limbSwing, float limbSwingAmount, float pt, float age,
         float netHeadYaw, float headPitch) {
         if (!(e instanceof EldritchWardenEntity)) return;
         int j = (int) (195.0F + Mth.sin(e.tickCount / 3.0F) * 15.0F + 15.0F);
         getParentModel().renderEye(pose, buffers.getBuffer(RenderType.eyes(tex("entity/eldritch_warden.png"))), j, OverlayTexture.NO_OVERLAY);
      }
   }

   // ================================================================== construct

   /** ModelEldritchGolem: cloaked giant; the head drops off for good when it loses it, arms smash down when it attacks. */
   public static class GolemModel extends EntityModel<EldritchGolemEntity> {
      final ModelPart root;
      final Map<String, ModelPart> p;

      public GolemModel(ModelPart root) {
         super(RenderType::entityTranslucent);
         this.root = root;
         this.p = EldritchModelLayers.golemParts(root);
      }

      private static float doAbs(float a, float b) {
         return (Math.abs(a % b - b * 0.5F) - b * 0.25F) / (b * 0.25F);
      }

      @Override
      public void prepareMobModel(EldritchGolemEntity golem, float limbSwing, float limbSwingAmount, float pt) {
         int i = golem.getAttackTimer();
         if (i > 0) {
            p.get("ArmR").xRot = -2.0F + 1.5F * doAbs(i - pt, 10.0F);
            p.get("ArmL").xRot = -2.0F + 1.5F * doAbs(i - pt, 10.0F);
         } else {
            p.get("ArmR").xRot = Mth.cos(limbSwing * 0.4F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
            p.get("ArmL").xRot = Mth.cos(limbSwing * 0.4F) * 2.0F * limbSwingAmount * 0.5F;
         }
      }

      @Override
      public void setupAnim(EldritchGolemEntity e, float f, float f1, float age, float netHeadYaw, float headPitch) {
         ModelPart head = p.get("Head");
         ModelPart head2 = p.get("Head2");
         if (e.getSpawnTimer() > 0) {
            head.yRot = 0.0F;
            head.xRot = e.getSpawnTimer() / 2 * Mth.DEG_TO_RAD;
         } else {
            head.yRot = netHeadYaw / 4.0F * Mth.DEG_TO_RAD;
            head.xRot = headPitch / 2.0F * Mth.DEG_TO_RAD;
            head2.yRot = netHeadYaw * Mth.DEG_TO_RAD;
            head2.xRot = headPitch * Mth.DEG_TO_RAD;
         }
         p.get("LegR").xRot = Mth.cos(f * 0.4662F) * 1.4F * f1;
         p.get("LegL").xRot = Mth.cos(f * 0.4662F + (float) Math.PI) * 1.4F * f1;
         float a = Mth.cos(f * 0.44F) * 1.4F * f1;
         float b = Mth.cos(f * 0.44F + (float) Math.PI) * 1.4F * f1;
         float c = Math.min(a, b);
         p.get("Frontcloth1").xRot = c - 0.1047198F;
         p.get("Frontcloth2").xRot = c - 0.3316126F;
         p.get("Cloak1").xRot = -c / 3.0F + 0.1396263F;
         p.get("Cloak2").xRot = -c / 3.0F + 0.3069452F;
         p.get("Cloak3").xRot = -c / 3.0F + 0.4465716F;
         head.visible = !e.isHeadless();
         head2.visible = e.isHeadless();
      }

      @Override
      public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
         root.render(pose, vc, light, overlay, color);
      }
   }

   public static class GolemRenderer extends MobRenderer<EldritchGolemEntity, GolemModel> {
      private static final ResourceLocation TEX = tex("entity/eldritch_golem.png");

      public GolemRenderer(EntityRendererProvider.Context ctx) {
         super(ctx, new GolemModel(ctx.bakeLayer(GOLEM)), 0.5F);
      }

      @Override
      public ResourceLocation getTextureLocation(EldritchGolemEntity entity) {
         return TEX;
      }

      @Override
      protected void scale(EldritchGolemEntity e, PoseStack pose, float pt) {
         pose.scale(1.7F, 1.7F, 1.7F);
      }

      @Override
      protected RenderType getRenderType(EldritchGolemEntity e, boolean bodyVisible, boolean translucent, boolean glowing) {
         return RenderType.entityTranslucent(TEX);
      }
   }

   // ================================================================== crab

   /** ModelEldritchCrab: scuttling legs, snapping claws, and the helm (TailHelm) or bare abdomen. */
   public static class CrabModel extends EntityModel<EldritchCrabEntity> {
      final ModelPart root;
      final Map<String, ModelPart> p;

      public CrabModel(ModelPart root) {
         this.root = root;
         this.p = EldritchModelLayers.crabParts(root);
      }

      private void rot(String n, float x, float y, float z) {
         ModelPart m = p.get(n);
         m.xRot = x;
         m.yRot = y;
         m.zRot = z;
      }

      @Override
      public void setupAnim(EldritchCrabEntity e, float par1, float par2, float age, float netHeadYaw, float headPitch) {
         rot("RRLeg1", 0.0F, 0.2094395F, 0.4363323F);
         rot("RFLeg1", 0.0F, -0.2094395F, 0.4363323F);
         rot("LRLeg1", 0.0F, -0.2094395F, -0.4363323F);
         rot("LFLeg1", 0.0F, 0.2094395F, -0.4363323F);
         rot("RRLeg0", 0.0F, 0.2094395F, 0.4363323F);
         rot("RFLeg0", 0.0F, -0.2094395F, 0.4363323F);
         rot("LFLeg0", 0.0F, 0.2094395F, -0.4363323F);
         rot("LRLeg0", 0.0F, -0.2094395F, -0.4363323F);
         float f9 = -(Mth.cos(par1 * 0.6662F * 2.0F) * 0.4F) * par2;
         float f10 = -(Mth.cos(par1 * 0.6662F * 2.0F + (float) Math.PI) * 0.4F) * par2;
         for (String n : new String[]{"RRLeg1", "RRLeg0"}) {
            p.get(n).yRot += f9;
            p.get(n).zRot += f9;
         }
         for (String n : new String[]{"LRLeg1", "LRLeg0"}) {
            p.get(n).yRot -= f9;
            p.get(n).zRot -= f9;
         }
         for (String n : new String[]{"RFLeg1", "RFLeg0"}) {
            p.get(n).yRot += f10;
            p.get(n).zRot += f10;
         }
         for (String n : new String[]{"LFLeg1", "LFLeg0"}) {
            p.get(n).yRot -= f10;
            p.get(n).zRot -= f10;
         }
         float ty = Mth.cos(par1 * 0.6662F) * 2.0F * par2 * 0.125F;
         float tz = Mth.cos(par1 * 0.6662F) * par2 * 0.125F;
         p.get("TailBare").yRot = p.get("TailHelm").yRot = ty;
         p.get("TailBare").zRot = p.get("TailHelm").zRot = tz;
         p.get("RClaw2").xRot = 0.3141593F - Mth.sin(e.tickCount / 4.0F) * 0.25F;
         p.get("LClaw2").xRot = 0.3141593F + Mth.sin(e.tickCount / 4.1F) * 0.25F;
         p.get("RClaw1").xRot = Mth.sin(e.tickCount / 4.0F) * 0.125F;
         p.get("LClaw1").xRot = -Mth.sin(e.tickCount / 4.1F) * 0.125F;
         p.get("TailHelm").visible = e.hasHelm();
         p.get("TailBare").visible = !e.hasHelm();
      }

      @Override
      public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
         root.render(pose, vc, light, overlay, color);
      }
   }

   public static class CrabRenderer extends MobRenderer<EldritchCrabEntity, CrabModel> {
      private static final ResourceLocation TEX = tex("entity/crab.png");

      public CrabRenderer(EntityRendererProvider.Context ctx) {
         super(ctx, new CrabModel(ctx.bakeLayer(CRAB)), 0.5F);
      }

      @Override
      public ResourceLocation getTextureLocation(EldritchCrabEntity entity) {
         return TEX;
      }

      @Override
      protected void scale(EldritchCrabEntity e, PoseStack pose, float pt) {
         pose.scale(0.8F, 0.8F, 0.8F);
      }
   }

   /** RenderInhabitedZombie: a zombie wearing czombie.png. */
   public static class InhabitedZombieRenderer extends net.minecraft.client.renderer.entity.ZombieRenderer {
      private static final ResourceLocation TEX = tex("entity/czombie.png");

      public InhabitedZombieRenderer(EntityRendererProvider.Context ctx) {
         super(ctx);
      }

      @Override
      public ResourceLocation getTextureLocation(net.minecraft.world.entity.monster.Zombie e) {
         return TEX;
      }
   }

   // ================================================================== eldritch orb

   /** RenderEldritchOrb: twelve dark rays spinning out of the orb, and a flickering wisp sprite in the middle. */
   public static class OrbRenderer extends EntityRenderer<EldritchOrbEntity> {
      public OrbRenderer(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.0F;
      }

      @Override
      public void render(EldritchOrbEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         java.util.Random random = new java.util.Random(187L);
         float f1 = e.tickCount / 80.0F;
         float grow = Math.min(e.tickCount, 10) / 10.0F;
         VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
         pose.pushPose();
         for (int i = 0; i < 12; i++) {
            RenderUtil.rot(pose, random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            RenderUtil.rot(pose, random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            RenderUtil.rot(pose, random.nextFloat() * 360.0F, 0.0F, 0.0F, 1.0F);
            RenderUtil.rot(pose, random.nextFloat() * 360.0F, 1.0F, 0.0F, 0.0F);
            RenderUtil.rot(pose, random.nextFloat() * 360.0F, 0.0F, 1.0F, 0.0F);
            RenderUtil.rot(pose, random.nextFloat() * 360.0F + f1 * 360.0F, 0.0F, 0.0F, 1.0F);
            float fa = (random.nextFloat() * 20.0F + 5.0F) / 30.0F * grow;
            float f4 = (random.nextFloat() * 2.0F + 1.0F) / 30.0F * grow;
            Matrix4f m = pose.last().pose();
            // triangle fan of 1.12 as two triangles drawn as quads (last vertex repeated)
            tri(vc, m, 0, 0, 0, -0.866F * f4, fa, -0.5F * f4, 0.866F * f4, fa, -0.5F * f4);
            tri(vc, m, 0, 0, 0, 0.866F * f4, fa, -0.5F * f4, 0.0F, fa, f4);
            tri(vc, m, 0, 0, 0, 0.0F, fa, f4, -0.866F * f4, fa, -0.5F * f4);
         }
         pose.popPose();
         pose.pushPose();
         pose.mulPose(entityRenderDispatcher.cameraOrientation());
         pose.scale(0.75F, 0.75F, 0.75F);
         int frame = e.tickCount % 13;
         float u0 = frame / 64.0F, u1 = u0 + 1.0F / 64.0F, v0 = 3.0F / 64.0F, v1 = v0 + 1.0F / 64.0F;
         VertexConsumer sprite = buffers.getBuffer(RenderType.entityTranslucent(thaumcraft.client.fx.FXGeneric.TEXTURE));
         Matrix4f m = pose.last().pose();
         sprite.addVertex(m, -0.5F, -0.5F, 0).setColor(-1).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
         sprite.addVertex(m, 0.5F, -0.5F, 0).setColor(-1).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
         sprite.addVertex(m, 0.5F, 0.5F, 0).setColor(-1).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
         sprite.addVertex(m, -0.5F, 0.5F, 0).setColor(-1).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
         pose.popPose();
         super.render(e, yaw, pt, pose, buffers, light);
      }

      private static void tri(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2) {
         vc.addVertex(m, x0, y0, z0).setColor(1.0F, 1.0F, 1.0F, 1.0F);
         vc.addVertex(m, x1, y1, z1).setColor(0.25F, 0.25F, 0.25F, 1.0F);
         vc.addVertex(m, x2, y2, z2).setColor(0.25F, 0.25F, 0.25F, 1.0F);
         vc.addVertex(m, x2, y2, z2).setColor(0.25F, 0.25F, 0.25F, 1.0F);
      }

      @Override
      public ResourceLocation getTextureLocation(EldritchOrbEntity entity) {
         return thaumcraft.client.fx.FXGeneric.TEXTURE;
      }
   }

   // ================================================================== tear and portal

   /** TileHoleRenderer: the other reality (end portal shader) seen on every opaque face around the tear. */
   public static class HoleRenderer implements BlockEntityRenderer<HoleBlockEntity> {
      public HoleRenderer(BlockEntityRendererProvider.Context ctx) {
      }

      @Override
      public void render(HoleBlockEntity te, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         if (te.getLevel() == null) return;
         VertexConsumer vc = buffers.getBuffer(RenderType.endPortal());
         Matrix4f m = pose.last().pose();
         float e = 0.001F;
         for (Direction face : Direction.values()) {
            BlockState bs = te.getLevel().getBlockState(te.getBlockPos().relative(face));
            if (!bs.isSolidRender(te.getLevel(), te.getBlockPos().relative(face)) || bs.is(ModEldritch.HOLE.get())) continue;
            switch (face) {
               case DOWN -> quad(vc, m, 0, e, 0, 1, e, 0, 1, e, 1, 0, e, 1);
               case UP -> quad(vc, m, 0, 1 - e, 1, 1, 1 - e, 1, 1, 1 - e, 0, 0, 1 - e, 0);
               case NORTH -> quad(vc, m, 0, 1, e, 1, 1, e, 1, 0, e, 0, 0, e);
               case SOUTH -> quad(vc, m, 0, 0, 1 - e, 1, 0, 1 - e, 1, 1, 1 - e, 0, 1, 1 - e);
               case WEST -> quad(vc, m, e, 0, 0, e, 0, 1, e, 1, 1, e, 1, 0);
               case EAST -> quad(vc, m, 1 - e, 1, 0, 1 - e, 1, 1, 1 - e, 0, 1, 1 - e, 0, 0);
            }
         }
      }

      private static void quad(VertexConsumer vc, Matrix4f m, float... c) {
         for (int i = 0; i < 12; i += 3) vc.addVertex(m, c[i], c[i + 1], c[i + 2]);
      }
   }

   /** The eldritch portal: the 16 frame swirl of misc/eldritch_portal.png as a camera facing billboard. */
   public static class PortalRenderer implements BlockEntityRenderer<EldritchPortalBlockEntity> {
      private static final ResourceLocation TEX = tex("misc/eldritch_portal.png");

      public PortalRenderer(BlockEntityRendererProvider.Context ctx) {
      }

      @Override
      public void render(EldritchPortalBlockEntity te, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         Minecraft mc = Minecraft.getInstance();
         long t = te.getLevel() == null ? 0 : te.getLevel().getGameTime();
         int frame = (int) (t / 2 % 16);
         float u0 = frame / 16.0F, u1 = u0 + 1.0F / 16.0F;
         pose.pushPose();
         pose.translate(0.5, 0.5, 0.5);
         pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
         float s = 1.5F + Mth.sin((t + pt) / 10.0F) * 0.1F;
         pose.scale(s, s, s);
         VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX));
         Matrix4f m = pose.last().pose();
         vc.addVertex(m, -0.5F, -0.5F, 0).setColor(-1).setUv(u1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
         vc.addVertex(m, 0.5F, -0.5F, 0).setColor(-1).setUv(u0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
         vc.addVertex(m, 0.5F, 0.5F, 0).setColor(-1).setUv(u0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
         vc.addVertex(m, -0.5F, 0.5F, 0).setColor(-1).setUv(u1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 0, 1);
         pose.popPose();
      }
   }
}
