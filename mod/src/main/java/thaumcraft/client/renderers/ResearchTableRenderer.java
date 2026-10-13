package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.crafting.ResearchTableBlockEntity;
import thaumcraft.common.items.ScribingToolsItem;

/** TileResearchTableRenderer + ModelResearchTable: the inkwell and quill when scribing tools are in, a scroll while a theory is open. */
public class ResearchTableRenderer implements BlockEntityRenderer<ResearchTableBlockEntity> {
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "research_table"), "main");
   private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/research_table_model.png");
   private static final ResourceLocation QUILL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/research/quill.png");

   private final ModelPart inkwell;
   private final ModelPart scrollTube;
   private final ModelPart scrollRibbon;

   public ResearchTableRenderer(BlockEntityRendererProvider.Context ctx) {
      ModelPart root = ctx.bakeLayer(LAYER);
      inkwell = root.getChild("inkwell");
      scrollTube = root.getChild("scroll_tube");
      scrollRibbon = root.getChild("scroll_ribbon");
   }

   public static LayerDefinition createLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("inkwell", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(0, 0, 0, 3, 2, 3), PartPose.offset(-6, -2, 3));
      root.addOrReplaceChild("scroll_tube", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-8, -0.5F, 0, 8, 2, 2),
         PartPose.offsetAndRotation(-2, -2, 2, 0, 10.0F, 0));
      root.addOrReplaceChild("scroll_ribbon", CubeListBuilder.create().texOffs(0, 4).mirror().addBox(-4.25F, -0.275F, 0, 1, 2, 2),
         PartPose.offsetAndRotation(-2, -2, 2, 0, 10.0F, 0));
      return LayerDefinition.create(mesh, 64, 32);
   }

   @Override
   public void render(ResearchTableBlockEntity table, float pt, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
      ps.pushPose();
      ps.translate(0.5, 1.0, 0.5);
      ps.mulPose(Axis.XP.rotationDegrees(180));
      switch (table.getBlockState().getValue(HorizontalDirectionalBlock.FACING)) {
         case EAST -> ps.mulPose(Axis.YP.rotationDegrees(90));
         case WEST -> ps.mulPose(Axis.YP.rotationDegrees(270));
         case SOUTH -> ps.mulPose(Axis.YP.rotationDegrees(180));
         default -> {
         }
      }
      VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEX));
      if (table.data != null) {
         scrollTube.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
         ps.pushPose();
         ps.scale(1.2F, 1.2F, 1.2F);
         scrollRibbon.render(ps, vc, light, OverlayTexture.NO_OVERLAY, 0xFF000000 | Aspect.ALCHEMY.getColor());
         ps.popPose();
      }
      if (table.getItems().getStackInSlot(0).getItem() instanceof ScribingToolsItem) {
         inkwell.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
         ps.pushPose();
         ps.mulPose(Axis.XP.rotationDegrees(180));
         ps.translate(-0.5, 0.1, 0.125);
         ps.mulPose(Axis.YP.rotationDegrees(60));
         ps.scale(0.5F, 0.5F, 0.5F);
         quill(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(QUILL)), light);
         ps.popPose();
      }
      ps.popPose();
   }

   /** UtilsFX.renderItemIn2D for the quill: a flat, two-sided 1x1 sprite standing upright. */
   private static void quill(PoseStack ps, VertexConsumer vc, int light) {
      Matrix4f m = ps.last().pose();
      var pose = ps.last();
      vc.addVertex(m, 0, 0, 0).setColor(-1).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
      vc.addVertex(m, 1, 0, 0).setColor(-1).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
      vc.addVertex(m, 1, 1, 0).setColor(-1).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
      vc.addVertex(m, 0, 1, 0).setColor(-1).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 0, 1);
   }
}
