package thaumcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.items.IArchitect;
import thaumcraft.client.compat.OverlayRenderTypes;
import thaumcraft.client.renderers.RenderUtil;

/** WandRenderingHandler.handleArchitectOverlay: outlines the blocks a plan focus will affect and the axes G resizes. */
public final class ArchitectOverlay {
   private static final ResourceLocation CFRAME = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/frame_corner.png");
   private static final ResourceLocation SFRAME = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/frame_side.png");
   private static final ResourceLocation ARROWS = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/architect_arrows.png");
   private static final int[][] MOS = {{4, 5, 6, 7}, {0, 1, 2, 3}, {0, 1, 4, 5}, {2, 3, 6, 7}, {0, 2, 4, 6}, {1, 3, 5, 7}};
   private static final int[][] ROTMAT = {{0, 90, 270, 180}, {270, 180, 0, 90}, {180, 90, 270, 0}, {0, 270, 90, 180}, {270, 180, 0, 90},
      {180, 270, 90, 0}};
   private static int lastArcHash;
   private static Set<BlockPos> architectBlocks = new HashSet<>();
   private static final Map<BlockPos, boolean[]> bmCache = new HashMap<>();
   /** The frame's buffer source while render() runs (batched so shader packs can capture the overlay). */
   private static MultiBufferSource.BufferSource buffers;

   private ArchitectOverlay() {
   }

   static void render(RenderLevelStageEvent event) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player == null || mc.level == null) return;
      ItemStack stack = player.getMainHandItem().getItem() instanceof IArchitect ? player.getMainHandItem() : player.getOffhandItem();
      if (!(stack.getItem() instanceof IArchitect af)) return;
      HitResult hr = af.getArchitectMOP(stack, mc.level, player);
      if (!(hr instanceof BlockHitResult target) || hr.getType() != HitResult.Type.BLOCK) return;
      int hc = (target.getBlockPos().toShortString() + target.getDirection() + player.tickCount / 5).hashCode();
      if (hc != lastArcHash) {
         lastArcHash = hc;
         bmCache.clear();
         List<BlockPos> l = af.getArchitectBlocks(stack, mc.level, target.getBlockPos(), target.getDirection(), player);
         architectBlocks = l == null ? new HashSet<>() : new HashSet<>(l);
      }
      if (architectBlocks.isEmpty()) return;
      PoseStack pose = event.getPoseStack();
      Vec3 cam = event.getCamera().getPosition();
      float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
      // Additive, no depth test/write, no culling: see OverlayRenderTypes.overlay(tex, true, false, false).
      buffers = mc.renderBuffers().bufferSource();
      drawArchitectAxis(pose, cam, target.getBlockPos(), player, pt, af.showAxis(stack, mc.level, player, target.getDirection(), IArchitect.EnumAxis.X),
         af.showAxis(stack, mc.level, player, target.getDirection(), IArchitect.EnumAxis.Y),
         af.showAxis(stack, mc.level, player, target.getDirection(), IArchitect.EnumAxis.Z));
      for (BlockPos cc : architectBlocks) drawOverlayBlock(pose, cam, cc);
      buffers.endBatch();
      buffers = null;
   }

   private static boolean conn(BlockPos p) {
      return architectBlocks.contains(p);
   }

   private static boolean[] getConnectedSides(BlockPos pos) {
      return bmCache.computeIfAbsent(pos, p -> new boolean[]{
         !conn(p.offset(-1, 0, 0)) && !conn(p.offset(0, 0, -1)) && !conn(p.offset(0, 1, 0)),
         !conn(p.offset(1, 0, 0)) && !conn(p.offset(0, 0, -1)) && !conn(p.offset(0, 1, 0)),
         !conn(p.offset(-1, 0, 0)) && !conn(p.offset(0, 0, 1)) && !conn(p.offset(0, 1, 0)),
         !conn(p.offset(1, 0, 0)) && !conn(p.offset(0, 0, 1)) && !conn(p.offset(0, 1, 0)),
         !conn(p.offset(-1, 0, 0)) && !conn(p.offset(0, 0, -1)) && !conn(p.offset(0, -1, 0)),
         !conn(p.offset(1, 0, 0)) && !conn(p.offset(0, 0, -1)) && !conn(p.offset(0, -1, 0)),
         !conn(p.offset(-1, 0, 0)) && !conn(p.offset(0, 0, 1)) && !conn(p.offset(0, -1, 0)),
         !conn(p.offset(1, 0, 0)) && !conn(p.offset(0, 0, 1)) && !conn(p.offset(0, -1, 0))});
   }

   /** renderQuadCentered: a size x size textured quad in the XY plane. */
   private static void quad(PoseStack pose, ResourceLocation tex, float size, float r, float g, float b, float a, boolean additive) {
      Matrix4f m = pose.last().pose();
      float h = size / 2.0F;
      VertexConsumer bb = buffers.getBuffer(OverlayRenderTypes.overlay(tex, additive, false, false));
      bb.addVertex(m, -h, h, 0).setUv(1, 1).setColor(r, g, b, a);
      bb.addVertex(m, h, h, 0).setUv(1, 0).setColor(r, g, b, a);
      bb.addVertex(m, h, -h, 0).setUv(0, 0).setColor(r, g, b, a);
      bb.addVertex(m, -h, -h, 0).setUv(0, 1).setColor(r, g, b, a);
   }

   private static void drawOverlayBlock(PoseStack pose, Vec3 cam, BlockPos pos) {
      boolean[] bitMatrix = getConnectedSides(pos);
      pose.pushPose();
      pose.translate(pos.getX() + 0.5 - cam.x, pos.getY() + 0.5 - cam.y, pos.getZ() + 0.5 - cam.z);
      for (Direction face : Direction.values()) {
         if (conn(pos.relative(face))) continue;
         pose.pushPose();
         RenderUtil.rot(pose, 90.0F, -face.getStepY(), face.getStepX(), -face.getStepZ());
         pose.translate(0.0, 0.0, face.getStepZ() < 0 ? -0.5 : 0.5);
         RenderUtil.rot(pose, 90.0F, 0.0F, 0.0F, -1.0F);
         quad(pose, SFRAME, 1.0F, 1, 1, 1, 0.1F, true);
         for (int a = 0; a < 4; a++) {
            if (bitMatrix[MOS[face.ordinal()][a]]) {
               pose.pushPose();
               RenderUtil.rot(pose, ROTMAT[face.ordinal()][a], 0.0F, 0.0F, 1.0F);
               quad(pose, CFRAME, 1.0F, 1, 1, 1, 0.66F, true);
               pose.popPose();
            }
         }
         pose.popPose();
      }
      pose.popPose();
   }

   private static void drawArchitectAxis(PoseStack pose, Vec3 cam, BlockPos pos, Player player, float pt, boolean dx, boolean dy, boolean dz) {
      if (!dx && !dy && !dz) return;
      float r = Mth.sin(player.tickCount / 4.0F + pos.getX()) * 0.2F + 0.3F;
      float g = Mth.sin(player.tickCount / 3.0F + pos.getY()) * 0.2F + 0.3F;
      float b = Mth.sin(player.tickCount / 2.0F + pos.getZ()) * 0.2F + 0.8F;
      pose.pushPose();
      pose.translate(pos.getX() + 0.5 - cam.x, pos.getY() + 0.5 - cam.y, pos.getZ() + 0.5 - cam.z);
      RenderUtil.rot(pose, 90.0F, 1.0F, 0.0F, 0.0F);
      if (dz) {
         pose.pushPose();
         quad(pose, ARROWS, 1.0F, r, g, b, 1.0F, true);
         RenderUtil.rot(pose, 90.0F, 0.0F, 1.0F, 0.0F);
         quad(pose, ARROWS, 1.0F, r, g, b, 1.0F, true);
         pose.popPose();
      }
      if (dx) {
         pose.pushPose();
         RenderUtil.rot(pose, 90.0F, 0.0F, 0.0F, 1.0F);
         quad(pose, ARROWS, 1.0F, r, g, b, 1.0F, true);
         RenderUtil.rot(pose, 90.0F, 0.0F, 1.0F, 0.0F);
         quad(pose, ARROWS, 1.0F, r, g, b, 1.0F, true);
         pose.popPose();
      }
      if (dy) {
         pose.pushPose();
         RenderUtil.rot(pose, 90.0F, 1.0F, 0.0F, 0.0F);
         quad(pose, ARROWS, 1.0F, r, g, b, 1.0F, true);
         RenderUtil.rot(pose, 90.0F, 0.0F, 1.0F, 0.0F);
         quad(pose, ARROWS, 1.0F, r, g, b, 1.0F, true);
         pose.popPose();
      }
      pose.popPose();
   }
}
