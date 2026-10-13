package thaumcraft.client.golems;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import thaumcraft.client.renderers.RenderUtil;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.golems.GolemBuilderBlockEntity;

/** TileGolemBuilderRenderer: the press OBJ with its moving ram, and a pool of lava in the cauldron. */
public class GolemBuilderRenderer implements BlockEntityRenderer<GolemBuilderBlockEntity> {
   private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath("thaumcraft", "models/block/golembuilder.obj");
   private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/blocks/golembuilder.png");

   public GolemBuilderRenderer(BlockEntityRendererProvider.Context ctx) {
   }

   @Override
   public void render(GolemBuilderBlockEntity tile, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
      ObjModel model = ObjModel.get(MODEL);
      pose.pushPose();
      pose.translate(0.5F, 0.0F, 0.5F);
      switch (DeviceBlock.getFacing(tile.getBlockState())) {
         case SOUTH -> RenderUtil.rot(pose, 180.0F, 0.0F, 1.0F, 0.0F);
         case WEST -> RenderUtil.rot(pose, 90.0F, 0.0F, 1.0F, 0.0F);
         case EAST -> RenderUtil.rot(pose, 270.0F, 0.0F, 1.0F, 0.0F);
         default -> {
         }
      }
      VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEX));
      model.renderAllExcept("press", pose, vc, light, overlay, -1, false);
      pose.pushPose();
      double s = Math.sin(Math.toRadians(tile.press)) * 0.625;
      pose.translate(0.0, -s, 0.0);
      model.renderPart("press", pose, vc, light, overlay, -1, false);
      pose.popPose();
      pose.translate(-0.3125F, 0.625F, 1.3125F);
      RenderUtil.rot(pose, 90.0F, -1.0F, 0.0F, 0.0F);
      TextureAtlasSprite lava = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.LAVA.defaultBlockState());
      VertexConsumer lv = buffers.getBuffer(RenderType.entityTranslucent(lava.atlasLocation()));
      Matrix4f m = pose.last().pose();
      float sz = 0.625F;
      int full = 0xF000F0;
      lv.addVertex(m, 0, sz, 0).setColor(-1).setUv(lava.getU0(), lava.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(0, 0, 1);
      lv.addVertex(m, sz, sz, 0).setColor(-1).setUv(lava.getU1(), lava.getV1()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(0, 0, 1);
      lv.addVertex(m, sz, 0, 0).setColor(-1).setUv(lava.getU1(), lava.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(0, 0, 1);
      lv.addVertex(m, 0, 0, 0).setColor(-1).setUv(lava.getU0(), lava.getV0()).setOverlay(OverlayTexture.NO_OVERLAY).setLight(full).setNormal(0, 0, 1);
      pose.popPose();
   }

   @Override
   public AABB getRenderBoundingBox(GolemBuilderBlockEntity be) {
      var p = be.getBlockPos();
      return new AABB(p.getX() - 1, p.getY(), p.getZ() - 1, p.getX() + 2, p.getY() + 2, p.getZ() + 2);
   }
}
