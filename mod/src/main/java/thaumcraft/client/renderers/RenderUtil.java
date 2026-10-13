package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import thaumcraft.api.aspects.Aspect;

/** Bits of UtilsFX for block entity renderers, written so the 1.12 GL call sequences translate one to one. */
public final class RenderUtil {
   private RenderUtil() {
   }

   /** glRotatef(angle, x, y, z). */
   public static void rot(PoseStack pose, float degrees, float x, float y, float z) {
      if (x == 0 && y == 0 && z == 0) return;
      float len = (float) Math.sqrt(x * x + y * y + z * z);
      pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(degrees), x / len, y / len, z / len));
   }

   /** Turns the pose so its +Y axis points along dir. */
   public static void pointUpTo(PoseStack pose, Direction dir) {
      switch (dir) {
         case DOWN -> rot(pose, 180, 1, 0, 0);
         case NORTH -> rot(pose, -90, 1, 0, 0);
         case SOUTH -> rot(pose, 90, 1, 0, 0);
         case EAST -> rot(pose, -90, 0, 0, 1);
         case WEST -> rot(pose, 90, 0, 0, 1);
         default -> {
         }
      }
   }

   /** A textured quad in the XY plane centred on the origin, facing -Z (renderQuadCentered). */
   public static void quadCentered(PoseStack pose, MultiBufferSource buffers, ResourceLocation tex, float size, int argb, int light) {
      VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentCull(tex));
      Matrix4f m = pose.last().pose();
      float h = size / 2.0F;
      vc.addVertex(m, -h, h, 0).setColor(argb).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, -1);
      vc.addVertex(m, h, h, 0).setColor(argb).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, -1);
      vc.addVertex(m, h, -h, 0).setColor(argb).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, -1);
      vc.addVertex(m, -h, -h, 0).setColor(argb).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, -1);
   }

   /** The aspect icon printed on a label: dark, as drawTag(..., bw = true). */
   public static void labelIcon(PoseStack pose, MultiBufferSource buffers, Aspect aspect, float size, int light) {
      quadCentered(pose, buffers, aspect.getImage(), size, 0xCC1A1A1A, light);
   }
}
