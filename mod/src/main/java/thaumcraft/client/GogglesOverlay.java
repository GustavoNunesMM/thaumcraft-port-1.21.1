package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.items.IGogglesDisplayExtended;
import thaumcraft.common.blocks.infusion.InfusionMatrixBlockEntity;
import thaumcraft.common.events.GearEvents;

/**
 * RenderEventHandler's goggles popups: looking at a block while wearing goggles of revealing shows the aspects in an
 * IAspectContainer (drawTagsOnContainer) and the text of IGogglesDisplayExtended devices (drawTextInAir), through walls.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class GogglesOverlay {
   private static float tagscale = 0.0F;

   private GogglesOverlay() {
   }

   @SubscribeEvent
   public static void render(RenderLevelStageEvent event) {
      if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
         || !GearEvents.hasGoggles(mc.player)) {
         tagscale = 0.0F;
         return;
      }
      BlockPos pos = hit.getBlockPos();
      BlockEntity te = mc.level.getBlockEntity(pos);
      Camera camera = event.getCamera();
      PoseStack ps = new PoseStack();
      MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
      List<Component> lines = null;
      Vec3 off = Vec3.ZERO;
      if (te instanceof IGogglesDisplayExtended g) {
         lines = g.getIGogglesText();
         off = g.getIGogglesTextOffset();
      } else if (mc.level.getBlockState(pos).getBlock() instanceof IGogglesDisplayExtended g) {
         lines = g.getIGogglesText();
         off = g.getIGogglesTextOffset();
      } else if (te instanceof InfusionMatrixBlockEntity m) {
         lines = m.getGogglesText();
      }
      if (lines != null) {
         for (int i = 0; i < lines.size(); i++) {
            drawTextInAir(ps, buffers, camera, pos.getX() + off.x, pos.getY() + off.y - (i - lines.size() / 2.0F) / 5.5F, pos.getZ() + off.z, lines.get(i));
         }
      }
      if (te instanceof IAspectContainer c && c.getAspects() != null && c.getAspects().size() > 0) {
         if (tagscale < 0.3F) tagscale += 0.031F - tagscale / 10.0F;
         boolean spaceAbove = mc.level.isEmptyBlock(pos.above());
         drawTagsOnContainer(ps, buffers, camera, pos.getX(), pos.getY() + (spaceAbove ? 0.4F : 0.0F), pos.getZ(), c.getAspects(),
            spaceAbove ? Direction.UP : hit.getDirection());
      } else {
         tagscale = 0.0F;
      }
      buffers.endBatch();
   }

   /** Rows of five aspect icons with their amounts, turned towards the viewer. */
   static void drawTagsOnContainer(PoseStack ps, MultiBufferSource buffers, Camera camera, double x, double y, double z, AspectList tags, Direction dir) {
      Vec3 cam = camera.getPosition();
      Font font = Minecraft.getInstance().font;
      int rowsize = 5;
      int current = 0;
      float shifty = 0.0F;
      int left = tags.size();
      for (Aspect tag : tags.getAspects()) {
         int div = Math.min(left, rowsize);
         if (current >= rowsize) {
            current = 0;
            shifty -= tagscale * 1.05F;
            left -= rowsize;
            if (left < rowsize) div = left % rowsize;
         }
         float shift = (current - div / 2.0F + 0.5F) * tagscale * 4.0F * tagscale;
         ps.pushPose();
         ps.translate(-cam.x + x + 0.5 + tagscale * 2.0F * dir.getStepX(), -cam.y + y - shifty + 0.5 + tagscale * 2.0F * dir.getStepY(),
            -cam.z + z + 0.5 + tagscale * 2.0F * dir.getStepZ());
         ps.mulPose(camera.rotation());
         ps.translate(shift, 0.0F, 0.0F);
         ps.scale(tagscale, -tagscale, tagscale);
         int color = tag.getColor();
         Matrix4f m = ps.last().pose();
         VertexConsumer vc = buffers.getBuffer(RenderType.textSeeThrough(tag.getImage()));
         int argb = 0xBF000000 | color;
         vc.addVertex(m, -0.5F, -0.5F, 0).setColor(argb).setUv(0, 0).setLight(LightTexture.FULL_BRIGHT);
         vc.addVertex(m, -0.5F, 0.5F, 0).setColor(argb).setUv(0, 1).setLight(LightTexture.FULL_BRIGHT);
         vc.addVertex(m, 0.5F, 0.5F, 0).setColor(argb).setUv(1, 1).setLight(LightTexture.FULL_BRIGHT);
         vc.addVertex(m, 0.5F, -0.5F, 0).setColor(argb).setUv(1, 0).setLight(LightTexture.FULL_BRIGHT);
         String am = "" + tags.getAmount(tag);
         ps.scale(0.04F, 0.04F, 0.04F);
         int sw = font.width(am);
         font.drawInBatch(am, 13 - sw, 3, 0xFFFFFFFF, true, ps.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
         ps.popPose();
         current++;
      }
   }

   static void drawTextInAir(PoseStack ps, MultiBufferSource buffers, Camera camera, double x, double y, double z, Component text) {
      Vec3 cam = camera.getPosition();
      Font font = Minecraft.getInstance().font;
      ps.pushPose();
      ps.translate(-cam.x + x + 0.5, -cam.y + y + 0.5, -cam.z + z + 0.5);
      ps.mulPose(camera.rotation());
      ps.scale(0.0125F, -0.0125F, 0.0125F);
      int sw = font.width(text);
      font.drawInBatch(text, 1 - sw / 2.0F, 1.0F, 0xFFFFFFFF, true, ps.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
      ps.popPose();
   }
}
