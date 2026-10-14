package thaumcraft.client.golems;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.client.compat.OverlayRenderTypes;
import thaumcraft.client.fx.DeviceFX;
import thaumcraft.client.renderers.RenderUtil;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealHandler;

/**
 * RenderEventHandler.drawSeals: while a seal displayer (bell, seal, golem) is held, the placed seals within 16 blocks
 * are drawn on their faces with a spinning ring, and the area seals outline the blocks they work on.
 */
public final class SealRenderer {
   private static final ResourceLocation CFRAME = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/frame_corner.png");
   private static final ResourceLocation MIDDLE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/seal_area.png");
   private static final Direction[][] ROTFACES = {
      {Direction.DOWN, Direction.NORTH, Direction.WEST},
      {Direction.UP, Direction.NORTH, Direction.WEST},
      {Direction.DOWN, Direction.NORTH, Direction.EAST},
      {Direction.UP, Direction.NORTH, Direction.EAST},
      {Direction.DOWN, Direction.SOUTH, Direction.EAST},
      {Direction.UP, Direction.SOUTH, Direction.EAST},
      {Direction.DOWN, Direction.SOUTH, Direction.WEST},
      {Direction.UP, Direction.SOUTH, Direction.WEST}};
   private static final int[][] ROTMAT = {{0, 270, 0}, {270, 180, 270}, {90, 0, 90}, {180, 90, 180}, {180, 180, 0}, {90, 270, 270}, {270, 90, 90},
      {0, 0, 180}};
   /** The frame's buffer source while render() runs (batched so shader packs can capture the seals). */
   private static MultiBufferSource.BufferSource buffers;
   /** Sneaking draws the seals through walls (the original disabled the depth test). */
   private static boolean depthTest;

   private SealRenderer() {
   }

   static void render(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (!(mc.getCameraEntity() instanceof Player player) || mc.level == null) return;
      if (!(player.getMainHandItem().getItem() instanceof ISealDisplayer) && !(player.getOffhandItem().getItem() instanceof ISealDisplayer)) return;
      var seals = SealHandler.getSeals(mc.level);
      if (seals.isEmpty()) return;
      float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
      PoseStack pose = event.getPoseStack();
      Vec3 cam = event.getCamera().getPosition();
      pose.pushPose();
      pose.translate(-cam.x, -cam.y, -cam.z);
      buffers = mc.renderBuffers().bufferSource();
      depthTest = !player.isShiftKeyDown();
      for (SealEntity seal : seals.values()) {
         if (seal.getSeal() == null || seal.getSealPos() == null) continue;
         BlockPos pos = seal.getSealPos().pos;
         double dis = player.distanceToSqr(pos.getCenter());
         if (dis > 256.0) continue;
         float alpha = 1.0F - (float) (dis / 256.0);
         boolean ia = false;
         if (seal.isStoppedByRedstone(mc.level)) {
            ia = true;
            if (mc.level.random.nextFloat() < pt / 12.0F) {
               Direction f = seal.getSealPos().face;
               DeviceFX.spark(pos.getX() + 0.5F + f.getStepX() * 0.66F, pos.getY() + 0.5F + f.getStepY() * 0.66F,
                  pos.getZ() + 0.5F + f.getStepZ() * 0.66F, 2.0F, 0.8F - mc.level.random.nextFloat() * 0.2F, 0.0F, 0.0F, 1.0F);
               ia = false;
            }
         }
         renderSeal(pose, pos, alpha, seal.getSealPos().face, seal.getSeal().getSealIcon(), ia, player.isShiftKeyDown());
         drawSealArea(pose, player, seal, alpha, pt);
      }
      buffers.endBatch();
      buffers = null;
      pose.popPose();
   }

   /** renderQuadCentered: a size x size textured quad in the XY plane. */
   private static void quad(PoseStack pose, ResourceLocation tex, float size, float r, float g, float b, float a, boolean depthWrite) {
      Matrix4f m = pose.last().pose();
      float h = size / 2.0F;
      VertexConsumer bb = buffers.getBuffer(OverlayRenderTypes.overlay(tex, false, depthTest, depthWrite));
      bb.addVertex(m, -h, h, 0).setUv(1, 1).setColor(r, g, b, a);
      bb.addVertex(m, h, h, 0).setUv(1, 0).setColor(r, g, b, a);
      bb.addVertex(m, h, -h, 0).setUv(0, 0).setColor(r, g, b, a);
      bb.addVertex(m, -h, -h, 0).setUv(0, 1).setColor(r, g, b, a);
   }

   private static void drawSealArea(PoseStack pose, Player player, ISealEntity seal, float alpha, float pt) {
      float r, g, b;
      BlockPos pos = seal.getSealPos().pos;
      Direction face = seal.getSealPos().face;
      if (seal.getColor() > 0 && seal.getColor() <= 16) {
         int c = DyeColor.byId(seal.getColor() - 1).getTextureDiffuseColor();
         r = (c >> 16 & 255) / 255.0F;
         g = (c >> 8 & 255) / 255.0F;
         b = (c & 255) / 255.0F;
      } else {
         r = 0.7F + Mth.sin((player.tickCount + pt + pos.getX()) / 4.0F) * 0.1F;
         g = 0.7F + Mth.sin((player.tickCount + pt + pos.getY()) / 5.0F) * 0.1F;
         b = 0.7F + Mth.sin((player.tickCount + pt + pos.getZ()) / 6.0F) * 0.1F;
      }
      pose.pushPose();
      pose.pushPose();
      pose.translate(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
      RenderUtil.rot(pose, 90.0F, -face.getStepY(), face.getStepX(), -face.getStepZ());
      pose.translate(0.0, 0.0, face.getStepZ() < 0 ? -0.51F : 0.51F);
      RenderUtil.rot(pose, player.tickCount % 360 + pt, 0.0F, 0.0F, 1.0F);
      quad(pose, MIDDLE, 0.9F, r, g, b, alpha * 0.8F, true);
      pose.popPose();
      if (seal.getSeal() instanceof ISealConfigArea) {
         BlockPos ar = seal.getArea();
         AABB area = new AABB(pos).move(face.getStepX(), face.getStepY(), face.getStepZ())
            .expandTowards(face.getStepX() != 0 ? (ar.getX() - 1) * face.getStepX() : 0.0, face.getStepY() != 0 ? (ar.getY() - 1) * face.getStepY() : 0.0,
               face.getStepZ() != 0 ? (ar.getZ() - 1) * face.getStepZ() : 0.0)
            .inflate(face.getStepX() == 0 ? ar.getX() - 1 : 0.0, face.getStepY() == 0 ? ar.getY() - 1 : 0.0, face.getStepZ() == 0 ? ar.getZ() - 1 : 0.0);
         double[][] locs = {
            {area.minX, area.minY, area.minZ},
            {area.minX, area.maxY - 1.0, area.minZ},
            {area.maxX - 1.0, area.minY, area.minZ},
            {area.maxX - 1.0, area.maxY - 1.0, area.minZ},
            {area.maxX - 1.0, area.minY, area.maxZ - 1.0},
            {area.maxX - 1.0, area.maxY - 1.0, area.maxZ - 1.0},
            {area.minX, area.minY, area.maxZ - 1.0},
            {area.minX, area.maxY - 1.0, area.maxZ - 1.0}};
         for (int q = 0; q < locs.length; q++) {
            pose.pushPose();
            pose.translate(locs[q][0] + 0.5, locs[q][1] + 0.5, locs[q][2] + 0.5);
            for (int w = 0; w < ROTFACES[q].length; w++) {
               Direction f = ROTFACES[q][w];
               pose.pushPose();
               RenderUtil.rot(pose, 90.0F, -f.getStepY(), f.getStepX(), -f.getStepZ());
               pose.translate(0.0, 0.0, f.getStepZ() < 0 ? -0.49F : 0.49F);
               RenderUtil.rot(pose, 90.0F, 0.0F, 0.0F, -1.0F);
               RenderUtil.rot(pose, ROTMAT[q][w], 0.0F, 0.0F, 1.0F);
               quad(pose, CFRAME, 1.0F, r, g, b, alpha * 0.7F, false);
               pose.popPose();
            }
            pose.popPose();
         }
      }
      pose.popPose();
   }

   /** renderSeal: the seal's (animated) item icon, half a block wide, just off the face. */
   private static void renderSeal(PoseStack pose, BlockPos pos, float alpha, Direction face, ResourceLocation icon, boolean ia, boolean sneaking) {
      pose.pushPose();
      float c = ia ? 0.5F : 1.0F;
      translateSeal(pose, pos.getX(), pos.getY(), pos.getZ(), face.get3DDataValue(), -0.05F);
      pose.scale(0.5F, 0.5F, 0.5F);
      TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(icon);
      float thick = sneaking ? 0.0F : 0.1F;
      Matrix4f m = pose.last().pose();
      VertexConsumer bb = buffers.getBuffer(OverlayRenderTypes.overlay(InventoryMenu.BLOCK_ATLAS, false, depthTest, true));
      float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
      for (float z : thick > 0 ? new float[]{0.0F, -thick} : new float[]{0.0F}) {
         bb.addVertex(m, 0, 0, z).setUv(u0, v1).setColor(c, c, c, alpha);
         bb.addVertex(m, 1, 0, z).setUv(u1, v1).setColor(c, c, c, alpha);
         bb.addVertex(m, 1, 1, z).setUv(u1, v0).setColor(c, c, c, alpha);
         bb.addVertex(m, 0, 1, z).setUv(u0, v0).setColor(c, c, c, alpha);
      }
      pose.popPose();
   }

   private static void translateSeal(PoseStack pose, float x, float y, float z, int orientation, float off) {
      switch (orientation) {
         case 1 -> {
            pose.translate(x + 0.25F, y + 1.0F, z + 0.75F);
            RenderUtil.rot(pose, -90.0F, 1.0F, 0.0F, 0.0F);
         }
         case 0 -> {
            pose.translate(x + 0.25F, y, z + 0.25F);
            RenderUtil.rot(pose, 90.0F, 1.0F, 0.0F, 0.0F);
         }
         case 3 -> pose.translate(x + 0.25F, y + 0.25F, z + 1.0F);
         case 2 -> {
            pose.translate(x + 0.75F, y + 0.25F, z);
            RenderUtil.rot(pose, 180.0F, 0.0F, 1.0F, 0.0F);
         }
         case 5 -> {
            pose.translate(x + 1.0F, y + 0.25F, z + 0.75F);
            RenderUtil.rot(pose, 90.0F, 0.0F, 1.0F, 0.0F);
         }
         case 4 -> {
            pose.translate(x, y + 0.25F, z + 0.25F);
            RenderUtil.rot(pose, -90.0F, 0.0F, 1.0F, 0.0F);
         }
         default -> {
         }
      }
      pose.translate(0.0F, 0.0F, -off);
   }
}
