package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.devices.BellowsBlockEntity;
import thaumcraft.common.blocks.essentia.AlembicBlockEntity;
import thaumcraft.common.blocks.essentia.CentrifugeBlockEntity;
import thaumcraft.common.blocks.essentia.JarBlockEntity;
import thaumcraft.common.blocks.essentia.TubeBufferBlockEntity;
import thaumcraft.common.blocks.essentia.TubeOnewayBlockEntity;
import thaumcraft.common.blocks.essentia.TubeValveBlockEntity;

/** TileJarRenderer, TileTubeValveRenderer, TileTubeOnewayRenderer, TileTubeBufferRenderer, TileCentrifugeRenderer, TileAlembicRenderer, TileBellowsRenderer. */
public final class EssentiaRenderers {
   private EssentiaRenderers() {
   }

   private static ResourceLocation tc(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   public static final ModelLayerLocation JAR = new ModelLayerLocation(tc("jar"), "main");
   public static final ModelLayerLocation VALVE = new ModelLayerLocation(tc("tube_valve"), "main");
   public static final ModelLayerLocation CENTRIFUGE = new ModelLayerLocation(tc("centrifuge"), "main");
   public static final ModelLayerLocation BELLOWS = new ModelLayerLocation(tc("bellows"), "main");
   static final ResourceLocation TEX_LABEL = tc("textures/models/label.png");
   static final ResourceLocation TEX_BRINE = tc("textures/models/jarbrine.png");
   static final ResourceLocation TEX_VALVE = tc("textures/models/valve.png");
   static final ResourceLocation TEX_CENTRIFUGE = tc("textures/models/centrifuge.png");
   static final ResourceLocation TEX_BELLOWS = tc("textures/blocks/bellows.png");
   static final ResourceLocation GLOW = tc("blocks/animatedglow");

   // ------------------------------------------------------------------ models (ModelJar, ModelTubeValve, ModelCentrifuge, ModelBellows)

   public static LayerDefinition jarLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(32, 24).mirror().addBox(-3, 0, -3, 6, 2, 6), PartPose.offset(0, -14, 0));
      root.addOrReplaceChild("lid_extension", CubeListBuilder.create().texOffs(0, 23).mirror().addBox(-2, -16, -2, 4, 2, 4), PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 32);
   }

   public static LayerDefinition valveLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("rod", CubeListBuilder.create().texOffs(0, 10).mirror().addBox(-1, 2, -1, 2, 2, 2), PartPose.ZERO);
      root.addOrReplaceChild("ring", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-2, 4, -2, 4, 1, 4), PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 32);
   }

   public static LayerDefinition centrifugeLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("crossbar", CubeListBuilder.create().texOffs(16, 0).mirror().addBox(-4, -1, -1, 8, 2, 2), PartPose.ZERO);
      root.addOrReplaceChild("dingus1", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(4, -3, -2, 4, 6, 4), PartPose.ZERO);
      root.addOrReplaceChild("dingus2", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-8, -3, -2, 4, 6, 4), PartPose.ZERO);
      root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.5F, -4, -1.5F, 3, 8, 3), PartPose.ZERO);
      root.addOrReplaceChild("top", CubeListBuilder.create().texOffs(20, 16).mirror().addBox(-4, -8, -4, 8, 4, 8), PartPose.ZERO);
      root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(20, 16).mirror().addBox(-4, 4, -4, 8, 4, 8), PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 32);
   }

   public static LayerDefinition bellowsLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-6, 0, -6, 12, 2, 12), PartPose.offset(0, 22, 0));
      root.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-6, -1, -6, 12, 2, 12), PartPose.offset(0, 16, 0));
      root.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-6, 0, -6, 12, 2, 12), PartPose.offset(0, 8, 0));
      root.addOrReplaceChild("bag", CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-10, -12.03333F, -10, 20, 24, 20), PartPose.offset(0, 0.5F, 0));
      root.addOrReplaceChild("nozzle", CubeListBuilder.create().texOffs(0, 36).mirror().addBox(-2, -2, 0, 4, 4, 2), PartPose.offset(0, 16, 6));
      return LayerDefinition.create(mesh, 128, 128);
   }

   // ------------------------------------------------------------------ jar

   public static class Jar implements BlockEntityRenderer<JarBlockEntity> {
      private final ModelPart lid;
      private final ModelPart lidExtension;

      public Jar(BlockEntityRendererProvider.Context ctx) {
         ModelPart root = ctx.bakeLayer(JAR);
         lid = root.getChild("lid");
         lidExtension = root.getChild("lid_extension");
      }

      @Override
      public void render(JarBlockEntity tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         pose.pushPose();
         pose.translate(0.5, 0.01, 0.5);
         RenderUtil.rot(pose, 180, 1, 0, 0);
         VertexConsumer brine = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_BRINE));
         if (tile.blocked) {
            pose.pushPose();
            pose.scale(1.001F, 1.001F, 1.001F);
            lid.render(pose, brine, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
         }
         if (tile.getLevel() != null && ThaumcraftApiHelper.getConnectableTile(tile.getLevel(), tile.getBlockPos(), Direction.UP) != null) {
            pose.pushPose();
            pose.scale(0.9F, 1.0F, 0.9F);
            lidExtension.render(pose, brine, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
         }
         pose.popPose();
         if (tile.aspectFilter != null) {
            pose.pushPose();
            pose.translate(0.5, 0.0, 0.5);
            RenderUtil.rot(pose, 180.0F - tile.facing.toYRot(), 0, 1, 0);
            float crooked = (tile.aspectFilter.getTag().hashCode() + tile.getBlockPos().getX() + tile.facing.get3DDataValue()) % 4 - 2;
            pose.translate(0.0, 0.41, -0.3155);
            RenderUtil.rot(pose, crooked, 0, 0, 1);
            RenderUtil.quadCentered(pose, buffers, TEX_LABEL, 0.5F, 0xFFFFFFFF, light);
            pose.translate(0.0, 0.0, -0.001);
            RenderUtil.labelIcon(pose, buffers, tile.aspectFilter, 0.336F, light);
            pose.popPose();
         }
         if (tile.amount > 0) renderLiquid(tile, pose, buffers, light);
      }

      private static void renderLiquid(JarBlockEntity te, PoseStack pose, MultiBufferSource buffers, int light) {
         float level = te.amount / (float) JarBlockEntity.CAPACITY * 0.625F;
         int color = te.aspect != null ? te.aspect.getColor() : 0;
         TextureAtlasSprite icon = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(GLOW);
         cube(pose, buffers.getBuffer(RenderType.translucent()), icon, 0.25F, 0.0625F, 0.25F, 0.75F, 0.0625F + level, 0.75F, color, 0xF000F0);
      }
   }

   /** A coloured, full-bright cube with the given sprite on every face (RenderCubes in the original). */
   static void cube(PoseStack pose, VertexConsumer vc, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int light) {
      Matrix4f m = pose.last().pose();
      float r = (rgb >> 16 & 255) / 255.0F, g = (rgb >> 8 & 255) / 255.0F, b = (rgb & 255) / 255.0F;
      float u0 = s.getU0(), u1 = s.getU1(), v0 = s.getV0(), v1 = s.getV1();
      float[][] faces = {
         {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0},
         {x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0},
         {x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, 0, 0, -1},
         {x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1, 0, 0, 1},
         {x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, -1, 0, 0},
         {x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, 1, 0, 0}};
      for (float[] f : faces) {
         vc.addVertex(m, f[0], f[1], f[2]).setColor(r, g, b, 1.0F).setUv(u0, v0).setLight(light).setNormal(pose.last(), f[12], f[13], f[14]);
         vc.addVertex(m, f[3], f[4], f[5]).setColor(r, g, b, 1.0F).setUv(u1, v0).setLight(light).setNormal(pose.last(), f[12], f[13], f[14]);
         vc.addVertex(m, f[6], f[7], f[8]).setColor(r, g, b, 1.0F).setUv(u1, v1).setLight(light).setNormal(pose.last(), f[12], f[13], f[14]);
         vc.addVertex(m, f[9], f[10], f[11]).setColor(r, g, b, 1.0F).setUv(u0, v1).setLight(light).setNormal(pose.last(), f[12], f[13], f[14]);
      }
   }

   // ------------------------------------------------------------------ tubes

   public static class Valve implements BlockEntityRenderer<TubeValveBlockEntity> {
      private final ModelPart rod;
      private final ModelPart ring;

      public Valve(BlockEntityRendererProvider.Context ctx) {
         ModelPart root = ctx.bakeLayer(VALVE);
         rod = root.getChild("rod");
         ring = root.getChild("ring");
      }

      @Override
      public void render(TubeValveBlockEntity valve, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_VALVE));
         pose.pushPose();
         pose.translate(0.5, 0.5, 0.5);
         RenderUtil.pointUpTo(pose, valve.facing);
         RenderUtil.rot(pose, -valve.rotation * 1.5F, 0, 1, 0);
         pose.translate(0.0, -0.03F - valve.rotation / 360.0F * 0.09F, 0.0);
         ring.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
         pose.scale(0.75F, 1.0F, 0.75F);
         rod.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
         pose.popPose();
      }
   }

   public static class Oneway implements BlockEntityRenderer<TubeOnewayBlockEntity> {
      private final ModelPart rod;

      public Oneway(BlockEntityRendererProvider.Context ctx) {
         rod = ctx.bakeLayer(VALVE).getChild("rod");
      }

      @Override
      public void render(TubeOnewayBlockEntity tube, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         if (tube.getLevel() != null && ThaumcraftApiHelper.getConnectableTile(tube.getLevel(), tube.getBlockPos(), tube.facing.getOpposite()) == null) return;
         pose.pushPose();
         pose.translate(0.5, 0.5, 0.5);
         RenderUtil.pointUpTo(pose, tube.facing);
         pose.scale(2.0F, 2.0F, 2.0F);
         pose.translate(0.0, -0.32F, 0.0);
         rod.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_VALVE)), light, OverlayTexture.NO_OVERLAY, 0xFF737FFF);
         pose.popPose();
      }
   }

   public static class Buffer implements BlockEntityRenderer<TubeBufferBlockEntity> {
      private final ModelPart rod;

      public Buffer(BlockEntityRendererProvider.Context ctx) {
         rod = ctx.bakeLayer(VALVE).getChild("rod");
      }

      @Override
      public void render(TubeBufferBlockEntity buffer, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         if (buffer.getLevel() == null) return;
         for (Direction dir : Direction.values()) {
            int i = dir.get3DDataValue();
            if (buffer.chokedSides[i] == 0 || !buffer.openSides[i]
               || ThaumcraftApiHelper.getConnectableTile(buffer.getLevel(), buffer.getBlockPos(), dir) == null) {
               continue;
            }
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            RenderUtil.pointUpTo(pose, dir.getOpposite());
            pose.scale(2.0F, 1.0F, 2.0F);
            pose.translate(0.0, -0.5, 0.0);
            int color = buffer.chokedSides[i] == 2 ? 0xFFFF4D4D : 0xFF4D4DFF;
            rod.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_VALVE)), light, OverlayTexture.NO_OVERLAY, color);
            pose.popPose();
         }
      }
   }

   // ------------------------------------------------------------------ centrifuge, alembic, bellows

   public static class Centrifuge implements BlockEntityRenderer<CentrifugeBlockEntity> {
      private final ModelPart root;

      public Centrifuge(BlockEntityRendererProvider.Context ctx) {
         root = ctx.bakeLayer(CENTRIFUGE);
      }

      @Override
      public void render(CentrifugeBlockEntity cf, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_CENTRIFUGE));
         pose.pushPose();
         pose.translate(0.5, 0.5, 0.5);
         root.getChild("top").render(pose, vc, light, overlay);
         root.getChild("bottom").render(pose, vc, light, overlay);
         RenderUtil.rot(pose, cf.rotation, 0, 1, 0);
         for (String part : new String[]{"crossbar", "dingus1", "dingus2", "core"}) root.getChild(part).render(pose, vc, light, overlay);
         pose.popPose();
      }
   }

   public static class Alembic implements BlockEntityRenderer<AlembicBlockEntity> {
      public Alembic(BlockEntityRendererProvider.Context ctx) {
      }

      @Override
      public void render(AlembicBlockEntity tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         if (tile.aspectFilter == null || !tile.facing.getAxis().isHorizontal()) return;
         pose.pushPose();
         pose.translate(0.5, 0.5, 0.5);
         RenderUtil.rot(pose, 180.0F - tile.facing.toYRot(), 0, 1, 0);
         pose.translate(0.0, 0.0, -0.376);
         RenderUtil.quadCentered(pose, buffers, TEX_LABEL, 0.44F, 0xFFFFFFFF, light);
         pose.translate(0.0, 0.0, -0.001);
         RenderUtil.labelIcon(pose, buffers, tile.aspectFilter, 0.32F, light);
         pose.popPose();
      }
   }

   public static class Bellows implements BlockEntityRenderer<BellowsBlockEntity> {
      private final ModelPart root;

      public Bellows(BlockEntityRendererProvider.Context ctx) {
         root = ctx.bakeLayer(BELLOWS);
      }

      @Override
      public void render(BellowsBlockEntity bellows, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         float scale = bellows.inflation;
         Direction dir = DeviceBlock.getFacing(bellows.getBlockState());
         float tscale = 0.125F + scale * 0.875F;
         VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_BELLOWS));
         pose.pushPose();
         translateFromOrientation(pose, dir);
         pose.translate(0.0, 1.0, 0.0);
         pose.pushPose();
         pose.scale(0.5F, (scale + 0.1F) / 2.0F, 0.5F);
         root.getChild("bag").render(pose, vc, light, overlay);
         pose.popPose();
         pose.translate(0.0, -1.0, 0.0);
         pose.pushPose();
         pose.translate(0.0, -tscale / 2.0F + 0.5F, 0.0);
         root.getChild("top").render(pose, vc, light, overlay);
         pose.popPose();
         pose.pushPose();
         pose.translate(0.0, tscale / 2.0F - 0.5F, 0.0);
         root.getChild("bottom").render(pose, vc, light, overlay);
         pose.popPose();
         root.getChild("middle").render(pose, vc, light, overlay);
         root.getChild("nozzle").render(pose, vc, light, overlay);
         pose.popPose();
      }

      private static void translateFromOrientation(PoseStack pose, Direction d) {
         pose.translate(0.5, -0.5, 0.5);
         switch (d) {
            case DOWN -> {
               pose.translate(0.0, 1.0, -1.0);
               RenderUtil.rot(pose, 90.0F, 1, 0, 0);
            }
            case UP -> {
               pose.translate(0.0, 1.0, 1.0);
               RenderUtil.rot(pose, 270.0F, 1, 0, 0);
            }
            case NORTH -> RenderUtil.rot(pose, 180.0F, 0, 1, 0);
            case WEST -> RenderUtil.rot(pose, 270.0F, 0, 1, 0);
            case EAST -> RenderUtil.rot(pose, 90.0F, 0, 1, 0);
            default -> {
            }
         }
      }
   }
}
