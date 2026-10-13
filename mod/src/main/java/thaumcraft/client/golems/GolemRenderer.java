package thaumcraft.client.golems;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.PartModel;
import thaumcraft.client.renderers.RenderUtil;
import thaumcraft.common.golems.GolemEntity;
import thaumcraft.common.golems.parts.PartModelHauler;

/**
 * RenderThaumcraftGolem: the base OBJ (chest, waist, flag, head, arms) in the material's texture plus the OBJ models of
 * the parts at their attach points, animated as in 1.12 (arm swing and wiggle, leaning into the movement).
 */
public class GolemRenderer extends EntityRenderer<GolemEntity> {
   private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath("thaumcraft", "models/obj/golem_base.obj");
   private final Map<Integer, Map<PartModel.EnumAttachPoint, List<PartModel>>> partsCache = new HashMap<>();

   public GolemRenderer(EntityRendererProvider.Context ctx) {
      super(ctx);
      this.shadowRadius = 0.3F;
   }

   @Override
   public ResourceLocation getTextureLocation(GolemEntity entity) {
      return entity.getProperties().getMaterial().texture;
   }

   private static PartModel.Transform transform(PoseStack pose) {
      return new PartModel.Transform() {
         @Override
         public void translate(double x, double y, double z) {
            pose.translate(x, y, z);
         }

         @Override
         public void rotate(float degrees, float x, float y, float z) {
            RenderUtil.rot(pose, degrees, x, y, z);
         }
      };
   }

   @Override
   public void render(GolemEntity entity, float entityYaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
      if (!entity.isInvisible()) {
         pose.pushPose();
         float bodyYaw = Mth.rotLerp(pt, entity.yBodyRotO, entity.yBodyRot);
         float headYaw = Mth.rotLerp(pt, entity.yHeadRotO, entity.yHeadRot);
         float f4 = headYaw - bodyYaw;
         float pitch = Mth.lerp(pt, entity.xRotO, entity.getXRot());
         pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
         if (entity.deathTime > 0) {
            float f = Math.min(1.0F, Mth.sqrt((entity.deathTime + pt - 1.0F) / 20.0F * 1.6F));
            pose.mulPose(Axis.ZP.rotationDegrees(f * 90.0F));
         }
         float limbAmount = Math.min(1.0F, entity.walkAnimation.speed(pt));
         float limbSwing = entity.walkAnimation.position(pt);
         float rotFloat = entity.tickCount + pt;
         int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
         renderParts(entity, limbSwing, limbAmount, rotFloat, f4, pitch, pt, pose, buffers, light, overlay);
         pose.popPose();
      }
      super.render(entity, entityYaw, pt, pose, buffers, light);
   }

   private Map<PartModel.EnumAttachPoint, List<PartModel>> parts(GolemEntity golem) {
      if (golem.redrawParts || !partsCache.containsKey(golem.getId())) {
         golem.redrawParts = false;
         Map<PartModel.EnumAttachPoint, List<PartModel>> pl = new EnumMap<>(PartModel.EnumAttachPoint.class);
         for (PartModel.EnumAttachPoint p : PartModel.EnumAttachPoint.values()) pl.put(p, new ArrayList<>());
         IGolemProperties props = golem.getProperties();
         for (PartModel m : new PartModel[]{props.getHead().model, props.getArms().model, props.getLegs().model, props.getAddon().model}) {
            if (m != null) pl.get(m.getAttachPoint()).add(m);
         }
         partsCache.put(golem.getId(), pl);
      }
      return partsCache.get(golem.getId());
   }

   private void renderParts(GolemEntity entity, float limbSwing, float prevLimbSwing, float rotFloat, float headPitch, float headYaw, float pt,
                            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
      ResourceLocation matTexture = entity.getProperties().getMaterial().texture;
      ObjModel base = ObjModel.get(BASE);
      boolean holding = !entity.getMainHandItem().isEmpty();
      boolean aflag = entity.getProperties().hasTrait(EnumGolemTrait.WHEELED) || entity.getProperties().hasTrait(EnumGolemTrait.FLYER);
      double dx = entity.getX() - entity.xo;
      double dz = entity.getZ() - entity.zo;
      double speed = dx * dx + dz * dz;
      Map<PartModel.EnumAttachPoint, List<PartModel>> cache = parts(entity);
      float swingProgress = entity.getAttackAnim(pt);
      float f1;
      float bry = 0.0F;
      float rx = (float) Math.toDegrees(Mth.sin(rotFloat * 0.067F) * 0.03F);
      float rz = (float) Math.toDegrees(Mth.cos(rotFloat * 0.09F) * 0.05F + 0.05F);
      float rrx = 0.0F, rry = 0.0F, rrz = 0.0F, rlx = 0.0F, rly = 0.0F, rlz = 0.0F;
      if (holding) {
         rrx = 90.0F - rz / 2.0F;
         rrz = -2.0F;
         rlx = 90.0F - rz / 2.0F;
         rlz = 2.0F;
      } else {
         if (aflag) {
            rrx = rx * 2.0F;
            rlx = -rx * 2.0F;
         } else {
            f1 = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * prevLimbSwing * 0.5F;
            rrx = (float) (Math.toDegrees(f1) + rx);
            f1 = Mth.cos(limbSwing * 0.6662F) * 2.0F * prevLimbSwing * 0.5F;
            rlx = (float) (Math.toDegrees(f1) - rx);
         }
         rrz += rz + 2.0F;
         rlz -= rz + 2.0F;
      }
      if (swingProgress > 0.0F) {
         float wiggle = -Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI * 2.0F) * 0.2F;
         bry = (float) Math.toDegrees(wiggle);
         rrz = -((float) Math.toDegrees(Mth.sin(wiggle) * 3.0F));
         rrx = (float) Math.toDegrees(-Mth.cos(wiggle) * 5.0F);
         rry += bry;
      }
      RenderUtil.rot(pose, bry, 0.0F, 1.0F, 0.0F);
      float lean = aflag ? 75.0F : 25.0F;
      RenderUtil.rot(pose, (float) (speed * lean), -1.0F, 0.0F, 0.0F);
      RenderUtil.rot(pose, (float) (speed * lean * 0.06 * (entity.getYRot() - entity.yRotO)), 0.0F, 0.0F, -1.0F);

      pose.pushPose();
      pose.translate(0.0, 0.5, 0.0);
      var matBuf = buffers.getBuffer(RenderType.entityCutoutNoCull(matTexture));
      base.renderPart("chest", pose, matBuf, light, overlay, -1, true);
      base.renderPart("waist", pose, matBuf, light, overlay, -1, true);
      DyeColor dye = entity.getDyeColor();
      if (dye != null) base.renderPart("flag", pose, matBuf, light, overlay, 0xFF000000 | dye.getTextureDiffuseColor(), true);
      for (PartModel part : cache.get(PartModel.EnumAttachPoint.BODY)) renderPart(entity, part, matTexture, pt, PartModel.EnumLimbSide.MIDDLE, pose, buffers, light, overlay);
      pose.popPose();

      pose.pushPose();
      pose.translate(0.0, 0.75, -0.03125);
      RenderUtil.rot(pose, headPitch, 0.0F, -1.0F, 0.0F);
      RenderUtil.rot(pose, headYaw, -1.0F, 0.0F, 0.0F);
      for (PartModel part : cache.get(PartModel.EnumAttachPoint.HEAD)) renderPart(entity, part, matTexture, pt, PartModel.EnumLimbSide.MIDDLE, pose, buffers, light, overlay);
      base.renderPart("head", pose, buffers.getBuffer(RenderType.entityCutoutNoCull(matTexture)), light, overlay, -1, true);
      pose.popPose();

      List<PartModel> arms = cache.get(PartModel.EnumAttachPoint.ARMS);
      pose.pushPose();
      pose.translate(0.20625, 0.6875, 0.0);
      if (!arms.isEmpty()) {
         PartModel part = arms.get(0);
         rrx = part.preRenderArmRotationX(entity, pt, PartModel.EnumLimbSide.RIGHT, rrx);
         rry = part.preRenderArmRotationY(entity, pt, PartModel.EnumLimbSide.RIGHT, rry);
         rrz = part.preRenderArmRotationZ(entity, pt, PartModel.EnumLimbSide.RIGHT, rrz);
      }
      RenderUtil.rot(pose, rrx, 1.0F, 0.0F, 0.0F);
      RenderUtil.rot(pose, rry, 0.0F, 1.0F, 0.0F);
      RenderUtil.rot(pose, rrz, 0.0F, 0.0F, 1.0F);
      base.renderPart("arm", pose, buffers.getBuffer(RenderType.entityCutoutNoCull(matTexture)), light, overlay, -1, true);
      for (PartModel part : arms) renderPart(entity, part, matTexture, pt, PartModel.EnumLimbSide.RIGHT, pose, buffers, light, overlay);
      pose.popPose();

      pose.pushPose();
      pose.translate(-0.20625, 0.6875, 0.0);
      if (!arms.isEmpty()) {
         PartModel part = arms.get(0);
         rlx = part.preRenderArmRotationX(entity, pt, PartModel.EnumLimbSide.LEFT, rlx);
         rly = part.preRenderArmRotationY(entity, pt, PartModel.EnumLimbSide.LEFT, rly);
         rlz = part.preRenderArmRotationZ(entity, pt, PartModel.EnumLimbSide.LEFT, rlz);
      }
      RenderUtil.rot(pose, rlx, 1.0F, 0.0F, 0.0F);
      RenderUtil.rot(pose, rly + 180.0F, 0.0F, 1.0F, 0.0F);
      RenderUtil.rot(pose, rlz, 0.0F, 0.0F, -1.0F);
      base.renderPart("arm", pose, buffers.getBuffer(RenderType.entityCutoutNoCull(matTexture)), light, overlay, -1, true);
      for (PartModel part : arms) renderPart(entity, part, matTexture, pt, PartModel.EnumLimbSide.LEFT, pose, buffers, light, overlay);
      pose.popPose();

      pose.pushPose();
      pose.translate(0.09375, 0.375, 0.0);
      f1 = Mth.cos(limbSwing * 0.6662F) * prevLimbSwing;
      RenderUtil.rot(pose, (float) Math.toDegrees(f1), 1.0F, 0.0F, 0.0F);
      for (PartModel part : cache.get(PartModel.EnumAttachPoint.LEGS)) renderPart(entity, part, matTexture, pt, PartModel.EnumLimbSide.RIGHT, pose, buffers, light, overlay);
      pose.popPose();

      pose.pushPose();
      pose.translate(-0.09375, 0.375, 0.0);
      f1 = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * prevLimbSwing;
      RenderUtil.rot(pose, (float) Math.toDegrees(f1), 1.0F, 0.0F, 0.0F);
      for (PartModel part : cache.get(PartModel.EnumAttachPoint.LEGS)) renderPart(entity, part, matTexture, pt, PartModel.EnumLimbSide.LEFT, pose, buffers, light, overlay);
      pose.popPose();

      pose.pushPose();
      pose.translate(0.0, 0.625, 0.0);
      RenderUtil.rot(pose, 90.0F - rz * 0.5F, 1.0F, 0.0F, 0.0F);
      drawHeldItem(entity, entity.getMainHandItem(), pose, buffers, light, 0.25F, -1.5F, -0.6F, true);
      pose.popPose();
   }

   private void drawHeldItem(GolemEntity entity, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, float ty, float tz, float itemOff,
                             boolean rotate) {
      if (stack.isEmpty()) return;
      pose.pushPose();
      if (rotate) RenderUtil.rot(pose, 90.0F, -1.0F, 0.0F, 0.0F);
      pose.scale(0.375F, 0.375F, 0.375F);
      pose.translate(0.0F, ty, tz);
      if (!(stack.getItem() instanceof BlockItem)) {
         if (rotate) pose.translate(0.0F, itemOff, 0.0F);
         else pose.translate(0.0F, 0.0F, itemOff);
      }
      Minecraft.getInstance().getItemRenderer().renderStatic(entity, stack, ItemDisplayContext.HEAD, false, pose, buffers, entity.level(), light,
         net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, entity.getId());
      pose.popPose();
   }

   private void renderPart(GolemEntity golem, PartModel part, ResourceLocation matTexture, float pt, PartModel.EnumLimbSide side, PoseStack pose,
                           MultiBufferSource buffers, int light, int overlay) {
      ObjModel model = ObjModel.get(part.getObjModel());
      PartModel.Transform t = transform(pose);
      for (String op : model.getPartNames()) {
         pose.pushPose();
         ResourceLocation tex = part.useMaterialTextureForObjectPart(op) || part.getTexture() == null ? matTexture : part.getTexture();
         part.preRenderObjectPart(op, golem, pt, side, t);
         model.renderPart(op, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(tex)), light, overlay, -1, true);
         part.postRenderObjectPart(op, golem, pt, side, t);
         if (part instanceof PartModelHauler && golem.getCarrying().size() > 1) {
            // PartModelHauler.postRenderObjectPart: the second carried stack sits on the frame.
            drawHeldItem(golem, golem.getCarrying().get(1), pose, buffers, light, 0.33F, 0.825F, -0.25F, false);
         }
         pose.popPose();
      }
   }
}
