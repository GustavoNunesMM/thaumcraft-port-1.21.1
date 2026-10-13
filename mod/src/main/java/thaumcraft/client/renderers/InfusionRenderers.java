package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.infusion.InfusionMatrixBlockEntity;
import thaumcraft.common.blocks.infusion.PedestalBlockEntity;
import thaumcraft.common.registers.ModBlocks;

/** TilePedestalRenderer and TileInfusionMatrixRenderer. */
public final class InfusionRenderers {
   private InfusionRenderers() {
   }

   private static ResourceLocation tc(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   public static final ModelLayerLocation CUBE = new ModelLayerLocation(tc("matrix_cube"), "main");
   public static final ModelLayerLocation CUBE_OVER = new ModelLayerLocation(tc("matrix_cube"), "over");
   static final ResourceLocation TEX_NORMAL = tc("textures/blocks/infuser_normal.png");
   static final ResourceLocation TEX_ANCIENT = tc("textures/blocks/infuser_ancient.png");
   static final ResourceLocation TEX_ELDRITCH = tc("textures/blocks/infuser_eldritch.png");

   /** ModelCube(shift): a 16-pixel cube, texture 64x64, at v = shift. */
   public static LayerDefinition cubeLayer(int shift) {
      MeshDefinition mesh = new MeshDefinition();
      mesh.getRoot().addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, shift).mirror().addBox(-8, -8, -8, 16, 16, 16), PartPose.ZERO);
      return LayerDefinition.create(mesh, 64, 64);
   }

   public static class Pedestal implements BlockEntityRenderer<PedestalBlockEntity> {
      public Pedestal(BlockEntityRendererProvider.Context ctx) {
      }

      @Override
      public void render(PedestalBlockEntity ped, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         ItemStack is = ped.getItem();
         if (is.isEmpty()) return;
         Minecraft mc = Minecraft.getInstance();
         float ticks = (mc.player != null ? mc.player.tickCount : 0) + partialTick;
         pose.pushPose();
         pose.translate(0.5, 0.75 + 0.25 + Mth.sin(ticks / 10.0F) * 0.03, 0.5);
         pose.scale(0.6F, 0.6F, 0.6F);
         RenderUtil.rot(pose, ticks % 360.0F, 0, 1, 0);
         mc.getItemRenderer().renderStatic(is.copyWithCount(1), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, ped.getLevel(), 0);
         pose.popPose();
      }
   }

   public static class Matrix implements BlockEntityRenderer<InfusionMatrixBlockEntity> {
      private final ModelPart cube;
      private final ModelPart cubeOver;

      public Matrix(BlockEntityRendererProvider.Context ctx) {
         cube = ctx.bakeLayer(CUBE).getChild("cube");
         cubeOver = ctx.bakeLayer(CUBE_OVER).getChild("cube");
      }

      @Override
      public void render(InfusionMatrixBlockEntity is, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         Minecraft mc = Minecraft.getInstance();
         float ticks = (mc.player != null ? mc.player.tickCount : 0) + partialTick;
         ResourceLocation t = TEX_NORMAL;
         pose.pushPose();
         pose.translate(0.5, 0.5, 0.5);
         if (is.getLevel() != null) {
            RenderUtil.rot(pose, ticks % 360.0F * is.startUp, 0, 1, 0);
            RenderUtil.rot(pose, 35.0F * is.startUp, 1, 0, 0);
            RenderUtil.rot(pose, 45.0F * is.startUp, 0, 0, 1);
            BlockState bs = is.getLevel().getBlockState(is.getBlockPos().offset(-1, -2, -1));
            if (bs.is(ModBlocks.PILLAR_ANCIENT.get())) t = TEX_ANCIENT;
            if (bs.is(ModBlocks.PILLAR_ELDRITCH.get())) t = TEX_ELDRITCH;
         }
         float inst = is.stability;
         float startup = is.startUp;
         float instability = Math.min(6.0F, 1.0F + (inst < 0.0F ? -inst * 0.66F : 1.0F) * (Math.min(is.craftCount, 50) / 50.0F));
         for (int pass = 0; pass < (is.active ? 2 : 1); pass++) {
            // One buffer at a time: asking for another render type ends the previous batch.
            VertexConsumer vc = buffers.getBuffer(pass == 0 ? RenderType.entityCutoutNoCull(t) : RenderType.entityTranslucentEmissive(t));
            for (int a = 0; a < 2; a++) {
               for (int b = 0; b < 2; b++) {
                  for (int c = 0; c < 2; c++) {
                     float b1 = 0, b2 = 0, b3 = 0;
                     if (is.active) {
                        b1 = Mth.sin((ticks + a * 10) / 15.0F) * 0.01F * startup * instability;
                        b2 = Mth.sin((ticks + b * 10) / 14.0F) * 0.01F * startup * instability;
                        b3 = Mth.sin((ticks + c * 10) / 13.0F) * 0.01F * startup * instability;
                     }
                     pose.pushPose();
                     pose.translate(b1 + (a == 0 ? -1 : 1) * 0.25F, b2 + (b == 0 ? -1 : 1) * 0.25F, b3 + (c == 0 ? -1 : 1) * 0.25F);
                     if (a > 0) RenderUtil.rot(pose, 90.0F, a, 0, 0);
                     if (b > 0) RenderUtil.rot(pose, 90.0F, 0, b, 0);
                     if (c > 0) RenderUtil.rot(pose, 90.0F, 0, 0, c);
                     pose.scale(0.45F, 0.45F, 0.45F);
                     if (pass == 0) {
                        cube.render(pose, vc, light, overlay);
                     } else {
                        float alpha = (Mth.sin((ticks + a * 2 + b * 3 + c * 4) / 4.0F) * 0.1F + 0.2F) * startup;
                        int argb = (int) (Mth.clamp(alpha, 0, 1) * 255) << 24 | 0xCC1AFF;
                        cubeOver.render(pose, vc, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, argb);
                     }
                     pose.popPose();
                  }
               }
            }
         }
         pose.popPose();
         if (is.crafting) drawHalo(pose, buffers, is.craftCount);
      }

      /** drawHalo: violet rays bursting out while crafting. */
      private static void drawHalo(PoseStack pose, MultiBufferSource buffers, int count) {
         pose.pushPose();
         pose.translate(0.5, 0.5, 0.5);
         VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
         float f1 = count / 500.0F;
         Random random = new Random(245L);
         int q = 20;
         for (int i = 0; i < q; i++) {
            RenderUtil.rot(pose, random.nextFloat() * 360.0F, 1, 0, 0);
            RenderUtil.rot(pose, random.nextFloat() * 360.0F, 0, 1, 0);
            RenderUtil.rot(pose, random.nextFloat() * 360.0F, 0, 0, 1);
            RenderUtil.rot(pose, random.nextFloat() * 360.0F, 1, 0, 0);
            RenderUtil.rot(pose, random.nextFloat() * 360.0F, 0, 1, 0);
            RenderUtil.rot(pose, random.nextFloat() * 360.0F + f1 * 360.0F, 0, 0, 1);
            float fa = random.nextFloat() * 20.0F + 5.0F;
            float f4 = random.nextFloat() * 2.0F + 1.0F;
            fa /= 20.0F / (Math.min(count, 50) / 50.0F);
            f4 /= 20.0F / (Math.min(count, 50) / 50.0F);
            Matrix4f m = pose.last().pose();
            int a0 = (int) (255.0F * (1.0F - f1));
            vc.addVertex(m, 0, 0, 0).setColor(255, 255, 255, Math.max(0, a0));
            vc.addVertex(m, -0.866F * f4, fa, -0.5F * f4).setColor(255, 0, 255, 0);
            vc.addVertex(m, 0.866F * f4, fa, -0.5F * f4).setColor(255, 0, 255, 0);
            vc.addVertex(m, 0, 0, 0).setColor(255, 255, 255, Math.max(0, a0));
            vc.addVertex(m, 0, 0, 0).setColor(255, 255, 255, Math.max(0, a0));
            vc.addVertex(m, 0.866F * f4, fa, -0.5F * f4).setColor(255, 0, 255, 0);
            vc.addVertex(m, 0, fa, 1.0F * f4).setColor(255, 0, 255, 0);
            vc.addVertex(m, 0, 0, 0).setColor(255, 255, 255, Math.max(0, a0));
            vc.addVertex(m, 0, 0, 0).setColor(255, 255, 255, Math.max(0, a0));
            vc.addVertex(m, 0, fa, 1.0F * f4).setColor(255, 0, 255, 0);
            vc.addVertex(m, -0.866F * f4, fa, -0.5F * f4).setColor(255, 0, 255, 0);
            vc.addVertex(m, 0, 0, 0).setColor(255, 255, 255, Math.max(0, a0));
         }
         pose.popPose();
      }
   }
}
