package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.fx.FXGeneric;
import thaumcraft.common.entities.monster.FireBatEntity;
import thaumcraft.common.entities.monster.GiantBrainyZombieEntity;
import thaumcraft.common.entities.monster.MindSpiderEntity;
import thaumcraft.common.entities.monster.PechEntity;
import thaumcraft.common.entities.monster.WispEntity;
import thaumcraft.common.entities.monster.cult.CultistClericEntity;
import thaumcraft.common.entities.monster.cult.CultistPortalGreaterEntity;
import thaumcraft.common.entities.monster.cult.CultistPortalLesserEntity;
import thaumcraft.common.entities.projectile.GolemOrbEntity;

/**
 * Milestone 13 renderers: RenderWisp, RenderFireBat (ModelFireBat), RenderPech (ModelPech, LayerHeldItemPech),
 * RenderMindSpider, RenderBrainyZombie, RenderThaumicSlime, RenderCultist, RenderCultistLeader, RenderCultistPortalLesser
 * / Greater and RenderElectricOrb.
 */
public final class MobRenderers {
   private MobRenderers() {
   }

   static ResourceLocation tc(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   public static final ModelLayerLocation PECH_LAYER = new ModelLayerLocation(tc("pech"), "main");
   public static final ModelLayerLocation CULTIST_LAYER = new ModelLayerLocation(tc("cultist"), "main");

   private static final ResourceLocation NODES = tc("textures/misc/auranodes.png");
   private static final ResourceLocation WISPY = tc("textures/misc/wispy.png");
   private static final ResourceLocation PORTAL = tc("textures/misc/cultist_portal.png");

   /** GL_SRC_ALPHA, GL_ONE blending (the glBlendFunc(770, 1) of the original), always lit, no culling. */
   static final class Types extends RenderType {
      private Types(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup,
         Runnable clear) {
         super(name, format, mode, size, crumbling, sort, setup, clear);
      }

      static final Function<ResourceLocation, RenderType> ADDITIVE = Util.memoize(tex -> create("thaumcraft_entity_additive",
         DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true, RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(new TextureStateShard(tex, false, false))
            .setTransparencyState(LIGHTNING_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_WRITE)
            .setOverlayState(OVERLAY)
            .createCompositeState(false)));
   }

   public static RenderType additive(ResourceLocation tex) {
      return Types.ADDITIVE.apply(tex);
   }

   private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int argb) {
      vc.addVertex(p.pose(), x, y, z).setColor(argb).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
         .setNormal(p, 0.0F, 0.0F, 1.0F);
   }

   /** UtilsFX.renderFacingQuad: a camera-facing quad showing one cell of a grid texture. Call with the camera rotation applied. */
   static void facingQuad(PoseStack pose, VertexConsumer vc, int gridX, int gridY, int frame, float scale, int color, float alpha) {
      int xm = frame % gridX;
      int ym = frame / gridY;
      float u0 = (float) xm / gridX, u1 = u0 + 1.0F / gridX, v0 = (float) ym / gridY, v1 = v0 + 1.0F / gridY;
      int c = FastColor.ARGB32.color(Mth.clamp((int) (alpha * 255.0F), 0, 255), color);
      PoseStack.Pose p = pose.last();
      vertex(vc, p, scale, -scale, 0.0F, u1, v1, c);
      vertex(vc, p, scale, scale, 0.0F, u1, v0, c);
      vertex(vc, p, -scale, scale, 0.0F, u0, v0, c);
      vertex(vc, p, -scale, -scale, 0.0F, u0, v1, c);
   }

   // ================================================================== wisp

   /** RenderWisp: three glowing billboards, the last one in the colour of the wisp's aspect. */
   public static class Wisp extends EntityRenderer<WispEntity> {
      public Wisp(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.0F;
      }

      @Override
      public void render(WispEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         if (e.getHealth() <= 0.0F) return;
         Aspect a = Aspect.getAspect(e.getWispType());
         int color = a != null ? a.getColor() : 0;
         pose.pushPose();
         pose.mulPose(entityRenderDispatcher.cameraOrientation());
         VertexConsumer vc = buffers.getBuffer(additive(FXGeneric.TEXTURE));
         facingQuad(pose, vc, 64, 64, 512 + e.tickCount % 16, 0.4F, 0xFFFFFF, 1.0F);
         facingQuad(pose, vc, 64, 64, 320 + e.tickCount % 16, 0.75F, 0xFFFFFF, 0.25F);
         facingQuad(pose, buffers.getBuffer(additive(NODES)), 32, 32, 800 + e.tickCount % 16, 0.75F, color, 0.5F);
         pose.popPose();
      }

      @Override
      public ResourceLocation getTextureLocation(WispEntity e) {
         return FXGeneric.TEXTURE;
      }
   }

   // ================================================================== fire bat

   /** ModelFireBat (the mesh is CasterRenderers.BAT_LAYER), with the hanging pose. */
   public static class FireBatModel extends EntityModel<FireBatEntity> {
      private final ModelPart root, head, body, rightWing, leftWing, rightTip, leftTip;

      public FireBatModel(ModelPart root) {
         this.root = root;
         head = root.getChild("head");
         body = root.getChild("body");
         rightWing = body.getChild("right_wing");
         leftWing = body.getChild("left_wing");
         rightTip = rightWing.getChild("right_wing_tip");
         leftTip = leftWing.getChild("left_wing_tip");
      }

      @Override
      public void setupAnim(FireBatEntity e, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
         if (e.getIsBatHanging()) {
            head.xRot = headPitch * Mth.DEG_TO_RAD;
            head.yRot = (float) Math.PI - netHeadYaw * Mth.DEG_TO_RAD;
            head.zRot = (float) Math.PI;
            head.setPos(0.0F, -2.0F, 0.0F);
            rightWing.setPos(-3.0F, 0.0F, 3.0F);
            leftWing.setPos(3.0F, 0.0F, 3.0F);
            body.xRot = (float) Math.PI;
            rightWing.xRot = (float) (-Math.PI / 20);
            rightWing.yRot = (float) (-Math.PI * 2.0 / 5.0);
            rightTip.yRot = -1.7278761F;
            leftWing.xRot = rightWing.xRot;
            leftWing.yRot = -rightWing.yRot;
            leftTip.yRot = -rightTip.yRot;
         } else {
            head.xRot = headPitch * Mth.DEG_TO_RAD;
            head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
            head.zRot = 0.0F;
            head.setPos(0.0F, 0.0F, 0.0F);
            rightWing.setPos(0.0F, 0.0F, 0.0F);
            leftWing.setPos(0.0F, 0.0F, 0.0F);
            body.xRot = (float) (Math.PI / 4) + Mth.cos(age * 0.1F) * 0.15F;
            body.yRot = 0.0F;
            rightWing.xRot = 0.0F;
            leftWing.xRot = 0.0F;
            rightWing.yRot = Mth.cos(age * 1.3F) * (float) Math.PI * 0.25F;
            leftWing.yRot = -rightWing.yRot;
            rightTip.yRot = rightWing.yRot * 0.5F;
            leftTip.yRot = -rightWing.yRot * 0.5F;
         }
      }

      @Override
      public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
         root.render(pose, vc, light, overlay, color);
      }
   }

   /** RenderFireBat: the bat at 35% size, bobbing while it flies. */
   public static class FireBat extends MobRenderer<FireBatEntity, FireBatModel> {
      private static final ResourceLocation TEX = tc("textures/entity/firebat.png");

      public FireBat(EntityRendererProvider.Context ctx) {
         super(ctx, new FireBatModel(ctx.bakeLayer(CasterRenderers.BAT_LAYER)), 0.25F);
      }

      @Override
      protected void scale(FireBatEntity e, PoseStack pose, float pt) {
         pose.scale(0.35F, 0.35F, 0.35F);
      }

      @Override
      protected void setupRotations(FireBatEntity e, PoseStack pose, float bob, float yBodyRot, float pt, float scale) {
         if (!e.getIsBatHanging()) pose.translate(0.0F, Mth.cos(bob * 0.3F) * 0.1F, 0.0F);
         else pose.translate(0.0F, -0.1F, 0.0F);
         super.setupRotations(e, pose, bob, yBodyRot, pt, scale);
      }

      @Override
      public ResourceLocation getTextureLocation(FireBatEntity e) {
         return TEX;
      }
   }

   // ================================================================== pech

   /** ModelPech. */
   public static LayerDefinition pechLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(34, 12).addBox(-3.0F, 0.0F, 0.0F, 6, 10, 6),
         PartPose.offsetAndRotation(0.0F, 9.0F, -3.0F, 0.3129957F, 0.0F, 0.0F));
      root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(35, 1).mirror().addBox(-2.9F, 0.0F, 0.0F, 3, 6, 3),
         PartPose.offset(0.0F, 18.0F, 0.0F));
      root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(35, 1).addBox(-0.1F, 0.0F, 0.0F, 3, 6, 3), PartPose.offset(0.0F, 18.0F, 0.0F));
      root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(2, 11).addBox(-3.5F, -5.0F, -5.0F, 7, 5, 5), PartPose.offset(0.0F, 8.0F, 0.0F));
      root.addOrReplaceChild("jowls", CubeListBuilder.create().texOffs(1, 21).addBox(-4.0F, -1.0F, -6.0F, 8, 3, 5), PartPose.offset(0.0F, 8.0F, 0.0F));
      root.addOrReplaceChild("lower_pack", CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, 0.0F, 0.0F, 10, 5, 5),
         PartPose.offsetAndRotation(0.0F, 10.0F, 3.5F, 0.3013602F, 0.0F, 0.0F));
      root.addOrReplaceChild("upper_pack", CubeListBuilder.create().texOffs(64, 1).addBox(-7.5F, -14.0F, 0.0F, 15, 14, 11),
         PartPose.offsetAndRotation(0.0F, 10.0F, 3.0F, 0.4537856F, 0.0F, 0.0F));
      root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(52, 2).mirror().addBox(-2.0F, 0.0F, -1.0F, 2, 6, 2),
         PartPose.offset(-3.0F, 10.0F, -1.0F));
      root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(52, 2).addBox(0.0F, 0.0F, -1.0F, 2, 6, 2), PartPose.offset(3.0F, 10.0F, -1.0F));
      return LayerDefinition.create(mesh, 128, 64);
   }

   public static class PechModel extends EntityModel<PechEntity> {
      private final ModelPart root, body, rightLeg, leftLeg, head, jowls, lowerPack, rightArm, leftArm;

      public PechModel(ModelPart root) {
         this.root = root;
         body = root.getChild("body");
         rightLeg = root.getChild("right_leg");
         leftLeg = root.getChild("left_leg");
         head = root.getChild("head");
         jowls = root.getChild("jowls");
         lowerPack = root.getChild("lower_pack");
         rightArm = root.getChild("right_arm");
         leftArm = root.getChild("left_arm");
      }

      public ModelPart getArm(HumanoidArm side) {
         return side == HumanoidArm.LEFT ? leftArm : rightArm;
      }

      @Override
      public void setupAnim(PechEntity e, float par1, float par2, float par3, float par4, float par5) {
         head.yRot = par4 * Mth.DEG_TO_RAD;
         head.xRot = par5 * Mth.DEG_TO_RAD;
         float mumble = e.mumble;
         jowls.yRot = head.yRot;
         jowls.xRot = head.xRot + ((float) (Math.PI / 12) + Mth.cos(par1 * 0.6662F) * par2 * 0.25F) + 0.34906587F * Math.abs(Mth.sin(mumble / 8.0F));
         rightArm.xRot = Mth.cos(par1 * 0.6662F + (float) Math.PI) * 2.0F * par2 * 0.5F;
         leftArm.xRot = Mth.cos(par1 * 0.6662F) * 2.0F * par2 * 0.5F;
         rightArm.zRot = 0.0F;
         leftArm.zRot = 0.0F;
         rightLeg.xRot = Mth.cos(par1 * 0.6662F) * 1.4F * par2;
         leftLeg.xRot = Mth.cos(par1 * 0.6662F + (float) Math.PI) * 1.4F * par2;
         rightLeg.yRot = 0.0F;
         leftLeg.yRot = 0.0F;
         lowerPack.yRot = Mth.cos(par1 * 0.6662F) * 2.0F * par2 * 0.125F;
         lowerPack.zRot = Mth.cos(par1 * 0.6662F) * 2.0F * par2 * 0.125F;
         if (riding) {
            rightArm.xRot += (float) (-Math.PI / 5);
            leftArm.xRot += (float) (-Math.PI / 5);
            rightLeg.xRot = (float) (-Math.PI * 2.0 / 5.0);
            leftLeg.xRot = (float) (-Math.PI * 2.0 / 5.0);
            rightLeg.yRot = (float) (Math.PI / 10);
            leftLeg.yRot = (float) (-Math.PI / 10);
         }
         rightArm.yRot = 0.0F;
         leftArm.yRot = 0.0F;
         float swing = attackTime;
         rightArm.yRot += body.yRot;
         leftArm.yRot += body.yRot;
         leftArm.xRot += body.yRot;
         float f6 = 1.0F - swing;
         f6 *= f6;
         f6 *= f6;
         f6 = 1.0F - f6;
         float f7 = Mth.sin(f6 * (float) Math.PI);
         float f8 = Mth.sin(swing * (float) Math.PI) * -(head.xRot - 0.7F) * 0.75F;
         rightArm.xRot = (float) (rightArm.xRot - (f7 * 1.2 + f8));
         rightArm.yRot += body.yRot * 2.0F;
         rightArm.zRot = Mth.sin(swing * (float) Math.PI) * -0.4F;
         if (e.isCrouching()) {
            rightArm.xRot += 0.4F;
            leftArm.xRot += 0.4F;
         }
         rightArm.zRot += Mth.cos(par3 * 0.09F) * 0.05F + 0.05F;
         leftArm.zRot -= Mth.cos(par3 * 0.09F) * 0.05F + 0.05F;
         rightArm.xRot += Mth.sin(par3 * 0.067F) * 0.05F;
         leftArm.xRot -= Mth.sin(par3 * 0.067F) * 0.05F;
      }

      @Override
      public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
         root.render(pose, vc, light, overlay, color);
      }
   }

   /** LayerHeldItemPech: the held items, set a little lower and forward in the pech's small hands. */
   public static class PechHeldItemLayer extends RenderLayer<PechEntity, PechModel> {
      private final ItemInHandRenderer itemRenderer;

      public PechHeldItemLayer(RenderLayerParent<PechEntity, PechModel> parent, ItemInHandRenderer itemRenderer) {
         super(parent);
         this.itemRenderer = itemRenderer;
      }

      @Override
      public void render(PoseStack pose, MultiBufferSource buffers, int light, PechEntity e, float limbSwing, float limbSwingAmount, float pt,
         float age, float netHeadYaw, float headPitch) {
         boolean rightHanded = e.getMainArm() == HumanoidArm.RIGHT;
         ItemStack left = rightHanded ? e.getOffhandItem() : e.getMainHandItem();
         ItemStack right = rightHanded ? e.getMainHandItem() : e.getOffhandItem();
         if (left.isEmpty() && right.isEmpty()) return;
         pose.pushPose();
         renderHeld(e, right, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT, pose, buffers, light);
         renderHeld(e, left, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT, pose, buffers, light);
         pose.popPose();
      }

      private void renderHeld(PechEntity e, ItemStack stack, ItemDisplayContext ctx, HumanoidArm side, PoseStack pose, MultiBufferSource buffers,
         int light) {
         if (stack.isEmpty()) return;
         pose.pushPose();
         getParentModel().getArm(side).translateAndRotate(pose);
         pose.translate(0.0F, -0.1F, 0.0625F);
         if (stack.getItem() instanceof BowItem) pose.translate(-0.075F, -0.1F, 0.0F);
         if (e.isCrouching()) pose.translate(0.0F, 0.2F, 0.0F);
         RenderUtil.rot(pose, -90.0F, 1.0F, 0.0F, 0.0F);
         RenderUtil.rot(pose, 180.0F, 0.0F, 1.0F, 0.0F);
         boolean flag = side == HumanoidArm.LEFT;
         pose.translate(flag ? -0.0625F : 0.0625F, 0.125F, -0.625F);
         itemRenderer.renderItem(e, stack, ctx, flag, pose, buffers, light);
         pose.popPose();
      }
   }

   /** RenderPech: the forager, mage and stalker skins. */
   public static class Pech extends MobRenderer<PechEntity, PechModel> {
      private static final ResourceLocation[] SKIN = {tc("textures/entity/pech_forage.png"), tc("textures/entity/pech_thaum.png"),
         tc("textures/entity/pech_stalker.png")};

      public Pech(EntityRendererProvider.Context ctx) {
         super(ctx, new PechModel(ctx.bakeLayer(PECH_LAYER)), 0.25F);
         addLayer(new PechHeldItemLayer(this, ctx.getItemInHandRenderer()));
      }

      @Override
      public ResourceLocation getTextureLocation(PechEntity e) {
         return SKIN[Mth.clamp(e.getPechType(), 0, SKIN.length - 1)];
      }
   }

   // ================================================================== mind spider

   /** RenderMindSpider: a small ghostly spider, only visible to the player it haunts. */
   public static class MindSpider extends SpiderRenderer<MindSpiderEntity> {
      public MindSpider(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.5F;
      }

      @Override
      protected void scale(MindSpiderEntity e, PoseStack pose, float pt) {
         pose.scale(0.6F, 0.6F, 0.6F);
      }

      /** Never fully visible: LivingEntityRenderer then draws it translucent. */
      @Override
      protected boolean isBodyVisible(MindSpiderEntity e) {
         return false;
      }

      @Override
      public void render(MindSpiderEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         Entity viewer = Minecraft.getInstance().getCameraEntity();
         String v = e.getViewer();
         if (v.isEmpty() || viewer != null && v.equals(viewer.getName().getString())) super.render(e, yaw, pt, pose, buffers, light);
      }
   }

   // ================================================================== brainy zombies

   /** A VertexConsumer that multiplies the colours (the glColor3f tint of the original). */
   record Tint(VertexConsumer vc, float r, float g, float b) implements VertexConsumer {
      @Override
      public VertexConsumer addVertex(float x, float y, float z) {
         vc.addVertex(x, y, z);
         return this;
      }

      @Override
      public VertexConsumer setColor(int red, int green, int blue, int alpha) {
         vc.setColor((int) (red * r), (int) (green * g), (int) (blue * b), alpha);
         return this;
      }

      @Override
      public VertexConsumer setUv(float u, float v) {
         vc.setUv(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv1(int u, int v) {
         vc.setUv1(u, v);
         return this;
      }

      @Override
      public VertexConsumer setUv2(int u, int v) {
         vc.setUv2(u, v);
         return this;
      }

      @Override
      public VertexConsumer setNormal(float x, float y, float z) {
         vc.setNormal(x, y, z);
         return this;
      }
   }

   /** RenderBrainyZombie: the brainy zombie skin; the furious zombie grows and reddens with its anger. */
   public static class BrainyZombie extends ZombieRenderer {
      private static final ResourceLocation TEX = tc("textures/entity/bzombie.png");

      public BrainyZombie(EntityRendererProvider.Context ctx) {
         super(ctx);
      }

      @Override
      public ResourceLocation getTextureLocation(Zombie e) {
         return TEX;
      }

      @Override
      protected void scale(Zombie e, PoseStack pose, float pt) {
         if (e instanceof GiantBrainyZombieEntity z) {
            float s = 1.0F + z.getAnger();
            pose.scale(s, s, s);
         }
      }

      @Override
      public void render(Zombie e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         if (e instanceof GiantBrainyZombieEntity z) {
            float q = Math.min(1.0F, z.getAnger()) / 2.0F;
            MultiBufferSource tinted = type -> new Tint(buffers.getBuffer(type), 1.0F, 1.0F - q, 1.0F - q);
            super.render(e, yaw, pt, pose, tinted, light);
         } else {
            super.render(e, yaw, pt, pose, buffers, light);
         }
      }
   }

   // ================================================================== thaumic slime

   public static class ThaumicSlime extends SlimeRenderer {
      private static final ResourceLocation TEX = tc("textures/entity/tslime.png");

      public ThaumicSlime(EntityRendererProvider.Context ctx) {
         super(ctx);
      }

      @Override
      public ResourceLocation getTextureLocation(Slime e) {
         return TEX;
      }
   }

   // ================================================================== cultists

   public static LayerDefinition cultistLayer() {
      return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 32);
   }

   /**
    * RenderCultist / RenderCultistLeader: a biped in the cult's armor (the armor items supply their own models). A
    * ritualist cleric floats, bobbing, tied to its altar by a wispy ribbon; the praetor is 15% larger.
    */
   public static class Cultist<T extends Monster> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
      private static final ResourceLocation SKIN = tc("textures/entity/cultist.png");
      private final float size;

      public Cultist(EntityRendererProvider.Context ctx, float size) {
         super(ctx, new HumanoidModel<>(ctx.bakeLayer(CULTIST_LAYER)), 0.5F);
         this.size = size;
         addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
            new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
      }

      public static <T extends Monster> EntityRendererProvider<T> of(float size) {
         return ctx -> new Cultist<>(ctx, size);
      }

      @Override
      public ResourceLocation getTextureLocation(T e) {
         return SKIN;
      }

      @Override
      protected void scale(T e, PoseStack pose, float pt) {
         if (size != 1.0F) pose.scale(size, size, size);
      }

      @Override
      public void render(T e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         pose.pushPose();
         float bob = 0.0F;
         boolean rit = e instanceof CultistClericEntity cleric && cleric.getIsRitualist();
         if (rit) {
            int val = new Random(e.getId()).nextInt(1000);
            float ct = e.tickCount + pt + val;
            bob = Mth.sin(ct / 9.0F) * 0.1F + 0.21F;
            pose.translate(0.0, bob, 0.0);
         }
         super.render(e, yaw, pt, pose, buffers, light);
         if (rit) {
            BlockPos home = e.getRestrictCenter();
            double ex = Mth.lerp(pt, e.xo, e.getX()), ey = Mth.lerp(pt, e.yo, e.getY()), ez = Mth.lerp(pt, e.zo, e.getZ());
            pose.pushPose();
            drawFloatyLine(pose, buffers, ex, ey, ez, e.getX(), e.getY() + e.getEyeHeight() * 1.2F, e.getZ(), home.getX() + 0.5,
               home.getY() + 1.5 - bob, home.getZ() + 0.5, 0x110011, -0.03F, Math.min(e.tickCount, 10) / 10.0F, 0.25F);
            pose.popPose();
         }
         pose.popPose();
      }

      /** drawFloatyLine: two crossed wavy ribbons of wispy.png from the altar towards the ritualist. */
      private static void drawFloatyLine(PoseStack pose, MultiBufferSource buffers, double ox, double oy, double oz, double x, double y, double z,
         double x2, double y2, double z2, int color, float speed, float distance, float width) {
         pose.translate(x2 - ox, y2 - oy, z2 - oz);
         float time = (float) (System.nanoTime() / 30000000L);
         float r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
         int c = FastColor.ARGB32.colorFromFloat(0.8F, r, g, b);
         double dc1x = (float) (x - x2), dc1y = (float) (y - y2), dc1z = (float) (z - z2);
         float dist = Mth.sqrt((float) (dc1x * dc1x + dc1y * dc1y + dc1z * dc1z));
         float length = Math.round(dist) * 6.0F;
         if (length <= 0.0F) return;
         VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(WISPY));
         PoseStack.Pose p = pose.last();
         for (int pass = 0; pass < 2; pass++) {
            float[] prev = null;
            for (int i = 0; i <= length * distance; i++) {
               float f2 = i / length;
               float f3 = 1.0F - Math.abs(i - length / 2.0F) / (length / 2.0F);
               double dx = dc1x + Mth.sin((float) ((z % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 4.0)) * 0.5F * f3;
               double dy = dc1y + Mth.sin((float) ((x % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 3.0)) * 0.5F * f3;
               double dz = dc1z + Mth.sin((float) ((y % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 2.0)) * 0.5F * f3;
               float u = (1.0F - f2) * dist - time * speed;
               float px = (float) (dx * f2), py = (float) (dy * f2), pz = (float) (dz * f2);
               float[] cur = pass == 0 ? new float[]{px, py - width, pz, px, py + width, pz, u}
                  : new float[]{px - width, py, pz, px + width, py, pz, u};
               if (prev != null) {
                  vertex(vc, p, prev[0], prev[1], prev[2], prev[6], 1.0F, c);
                  vertex(vc, p, prev[3], prev[4], prev[5], prev[6], 0.0F, c);
                  vertex(vc, p, cur[3], cur[4], cur[5], cur[6], 0.0F, c);
                  vertex(vc, p, cur[0], cur[1], cur[2], cur[6], 1.0F, c);
               }
               prev = cur;
            }
         }
      }
   }

   // ================================================================== portals

   /** RenderCultistPortalLesser / Greater: an animated ruby portal billboard that pulses and flickers as it is hurt. */
   static void renderPortal(EntityRenderer<?> r, Monster portal, int counter, int pulse, float scaley, float sizeMul, float pt, PoseStack pose,
      MultiBufferSource buffers) {
      long time = System.nanoTime() / 50000000L;
      int e = (int) Math.min(50.0F, counter + pt);
      if (portal.hurtTime > 0) {
         double d = Math.sin(portal.hurtTime * 72 * Math.PI / 180.0);
         scaley = (float) (scaley - d / 4.0);
         e = (int) (e + 6.0 * d);
      }
      if (pulse > 0) {
         double d = Math.sin(pulse * 36 * Math.PI / 180.0);
         scaley = (float) (scaley + d / 4.0);
         e = (int) (e + 12.0 * d);
      }
      float scale = e / 50.0F * sizeMul;
      float m = (1.0F - portal.getHealth() / portal.getMaxHealth()) / 3.0F;
      float bob = Mth.sin(counter / (5.0F - 12.0F * m)) * m + m;
      float bob2 = Mth.sin(counter / (6.0F - 15.0F * m)) * m + m;
      float alpha = Mth.clamp(1.0F - bob, 0.0F, 1.0F);
      scaley -= bob / 4.0F;
      scale -= bob2 / 3.0F;
      pose.pushPose();
      pose.translate(0.0F, portal.getBbHeight() / 2.0F, 0.0F);
      pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
      int frame = 15 - (int) time % 16;
      float f2 = frame / 16.0F, f3 = f2 + 0.0625F;
      int c = FastColor.ARGB32.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F);
      VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(PORTAL));
      PoseStack.Pose p = pose.last();
      vertex(vc, p, scale, -scaley, 0.0F, f3, 0.0F, c);
      vertex(vc, p, scale, scaley, 0.0F, f3, 1.0F, c);
      vertex(vc, p, -scale, scaley, 0.0F, f2, 1.0F, c);
      vertex(vc, p, -scale, -scaley, 0.0F, f2, 0.0F, c);
      pose.popPose();
   }

   public static class PortalLesser extends EntityRenderer<CultistPortalLesserEntity> {
      public PortalLesser(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.0F;
         shadowStrength = 0.0F;
      }

      @Override
      public void render(CultistPortalLesserEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         if (e.isActive()) renderPortal(this, e, e.activeCounter, e.pulse, 1.4F, 1.25F, pt, pose, buffers);
      }

      @Override
      public ResourceLocation getTextureLocation(CultistPortalLesserEntity e) {
         return PORTAL;
      }
   }

   public static class PortalGreater extends EntityRenderer<CultistPortalGreaterEntity> {
      public PortalGreater(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.1F;
         shadowStrength = 0.5F;
      }

      @Override
      public void render(CultistPortalGreaterEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         renderPortal(this, e, e.tickCount, e.pulse, 1.5F, 1.3F, pt, pose, buffers);
      }

      @Override
      public ResourceLocation getTextureLocation(CultistPortalGreaterEntity e) {
         return PORTAL;
      }
   }

   // ================================================================== energy orb

   /** RenderElectricOrb: a pulsing crackle of energy (red for the cult's orbs). */
   public static class ElectricOrb extends EntityRenderer<GolemOrbEntity> {
      public ElectricOrb(EntityRendererProvider.Context ctx) {
         super(ctx);
         shadowRadius = 0.0F;
      }

      @Override
      public void render(GolemOrbEntity e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
         float f2 = (1 + e.tickCount % 6) / 32.0F;
         float f3 = f2 + 0.03125F;
         float f4 = e.red ? 0.1875F : 0.21875F;
         float f5 = f4 + 0.03125F;
         pose.pushPose();
         pose.mulPose(entityRenderDispatcher.cameraOrientation());
         float bob = Mth.sin(e.tickCount / 5.0F) * 0.2F + 0.2F;
         pose.scale(1.0F + bob, 1.0F + bob, 1.0F + bob);
         VertexConsumer vc = buffers.getBuffer(additive(FXGeneric.TEXTURE));
         PoseStack.Pose p = pose.last();
         vertex(vc, p, -0.5F, -0.5F, 0.0F, f2, f5, -1);
         vertex(vc, p, 0.5F, -0.5F, 0.0F, f3, f5, -1);
         vertex(vc, p, 0.5F, 0.5F, 0.0F, f3, f4, -1);
         vertex(vc, p, -0.5F, 0.5F, 0.0F, f2, f4, -1);
         pose.popPose();
         super.render(e, yaw, pt, pose, buffers, light);
      }

      @Override
      public ResourceLocation getTextureLocation(GolemOrbEntity e) {
         return FXGeneric.TEXTURE;
      }
   }
}
