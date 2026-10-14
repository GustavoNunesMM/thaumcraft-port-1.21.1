package thaumcraft.client.renderers.tile;

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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.client.renderers.RenderUtil;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.devices.BannerBlockEntity;
import thaumcraft.common.blocks.devices.DioptraBlockEntity;
import thaumcraft.common.blocks.devices.HungryChestBlockEntity;
import thaumcraft.common.blocks.devices.JarBrainBlockEntity;
import thaumcraft.common.blocks.devices.MirrorBlockEntity;
import thaumcraft.common.blocks.devices.PatternCrafterBlockEntity;
import thaumcraft.common.blocks.devices.RechargePedestalBlockEntity;
import thaumcraft.common.blocks.devices.ThaumatoriumBlockEntity;

/**
 * The block entity renderers of the devices (TileHungryChestRenderer, TileDioptraRenderer, TileRechargePedestalRenderer,
 * TileThaumatoriumRenderer, TilePatternCrafterRenderer, TileMirrorRenderer, the brain part of TileJarRenderer and
 * TileBannerRenderer), with the GL call sequences translated one to one.
 */
public final class DeviceRenderers {
   private DeviceRenderers() {
   }

   private static ResourceLocation tc(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   public static final ModelLayerLocation CHEST = new ModelLayerLocation(tc("hungry_chest"), "main");
   public static final ModelLayerLocation BRAIN = new ModelLayerLocation(tc("jar_brain"), "main");
   public static final ModelLayerLocation BANNER = new ModelLayerLocation(tc("banner"), "main");
   static final ResourceLocation TEX_CHEST = tc("textures/models/chesthungry.png");
   static final ResourceLocation TEX_GRID = tc("textures/misc/gridblock.png");
   static final ResourceLocation TEX_DIOPTRA_SIDE = tc("textures/models/dioptra_side.png");
   static final ResourceLocation TEX_MODES = tc("textures/blocks/pattern_crafter_modes.png");
   static final ResourceLocation TEX_GEAR = tc("textures/misc/gear_brass.png");
   static final ResourceLocation TEX_PANE = tc("textures/blocks/mirrorpane.png");
   static final ResourceLocation TEX_PANE_TRANS = tc("textures/blocks/mirrorpanetrans.png");
   static final ResourceLocation TEX_BRAIN = tc("textures/models/brain2.png");
   static final ResourceLocation TEX_BRINE = tc("textures/models/jarbrine.png");
   static final ResourceLocation TEX_CULT = tc("textures/models/banner_cultist.png");
   static final ResourceLocation TEX_BLANK = tc("textures/models/banner_blank.png");

   // ------------------------------------------------------------------ models (ModelChest, ModelBrain + jar brine, ModelBanner)

   public static LayerDefinition chestLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(0, -5, -14, 14, 5, 14), PartPose.offset(1, 7, 15));
      root.addOrReplaceChild("knob", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -2, -15, 2, 4, 1), PartPose.offset(8, 7, 15));
      root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(0, 0, 0, 14, 10, 14), PartPose.offset(1, 6, 1));
      return LayerDefinition.create(mesh, 64, 64);
   }

   public static LayerDefinition brainLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("shape1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0, 0, 0, 12, 10, 16), PartPose.offset(-6, 8, -8));
      root.addOrReplaceChild("shape2", CubeListBuilder.create().texOffs(64, 0).mirror().addBox(0, 0, 0, 8, 3, 7), PartPose.offset(-4, 18, 0));
      root.addOrReplaceChild("shape3", CubeListBuilder.create().texOffs(0, 32).mirror().addBox(0, 0, 0, 2, 6, 2),
         PartPose.offsetAndRotation(-1, 18, -2, 0.4089647F, 0, 0));
      root.addOrReplaceChild("brine", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-4, -11, -4, 8, 10, 8), PartPose.ZERO);
      return LayerDefinition.create(mesh, 128, 64);
   }

   public static LayerDefinition bannerLayer() {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      root.addOrReplaceChild("b1", CubeListBuilder.create().texOffs(0, 29).mirror().addBox(-5, -7.5F, -1.5F, 2, 3, 3), PartPose.ZERO);
      root.addOrReplaceChild("b2", CubeListBuilder.create().texOffs(0, 29).mirror().addBox(3, -7.5F, -1.5F, 2, 3, 3), PartPose.ZERO);
      root.addOrReplaceChild("beam", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(-7, -7, -1, 14, 2, 2), PartPose.ZERO);
      root.addOrReplaceChild("banner", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-7, 0, -0.5F, 14, 28, 1), PartPose.offset(0, -5, 0));
      root.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(62, 0).mirror().addBox(0, 0, -1, 2, 31, 2), PartPose.offset(-1, -7, -2));
      return LayerDefinition.create(mesh, 128, 64);
   }

   private static float ticks(float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      return (mc.player != null ? mc.player.tickCount : 0) + partialTick;
   }

   /** One quad with per-vertex colour, in the entity vertex format. */
   static void quad(PoseStack pose, VertexConsumer vc, float[][] v, int[] argb, int light, float nx, float ny, float nz) {
      Matrix4f m = pose.last().pose();
      for (int i = 0; i < 4; i++) {
         vc.addVertex(m, v[i][0], v[i][1], v[i][2]).setColor(argb[i]).setUv(v[i][3], v[i][4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
            .setNormal(pose.last(), nx, ny, nz);
      }
   }

   /** renderQuadCentered with a frame of a horizontal strip texture (frames wide). */
   static void quadCenteredFrame(PoseStack pose, VertexConsumer vc, int frames, int frame, float size, int argb, int light) {
      float h = size / 2.0F;
      float u0 = frame / (float) frames, u1 = (frame + 1) / (float) frames;
      quad(pose, vc, new float[][]{{-h, h, 0, u0, 0}, {h, h, 0, u1, 0}, {h, -h, 0, u1, 1}, {-h, -h, 0, u0, 1}}, new int[]{argb, argb, argb, argb},
         light, 0, 0, -1);
   }

   private static int argb(float a, float r, float g, float b) {
      return ((int) (Mth.clamp(a, 0, 1) * 255) << 24) | ((int) (Mth.clamp(r, 0, 1) * 255) << 16) | ((int) (Mth.clamp(g, 0, 1) * 255) << 8)
         | (int) (Mth.clamp(b, 0, 1) * 255);
   }

   // ------------------------------------------------------------------ hungry chest

   public static class HungryChest implements BlockEntityRenderer<HungryChestBlockEntity> {
      private final ModelPart lid;
      private final ModelPart knob;
      private final ModelPart bottom;

      public HungryChest(BlockEntityRendererProvider.Context ctx) {
         ModelPart root = ctx.bakeLayer(CHEST);
         lid = root.getChild("lid");
         knob = root.getChild("knob");
         bottom = root.getChild("bottom");
      }

      @Override
      public void render(HungryChestBlockEntity chest, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         Direction f = chest.getLevel() != null ? DeviceBlock.getFacing(chest.getBlockState()) : Direction.SOUTH;
         pose.pushPose();
         pose.translate(0.0F, 1.0F, 1.0F);
         pose.scale(1.0F, -1.0F, -1.0F);
         pose.translate(0.5F, 0.5F, 0.5F);
         float angle = switch (f) {
            case NORTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
         };
         RenderUtil.rot(pose, angle, 0, 1, 0);
         pose.translate(-0.5F, -0.5F, -0.5F);
         float open = chest.getOpenNess(partialTick);
         open = 1.0F - open;
         open = 1.0F - open * open * open;
         lid.xRot = -(open * (float) Math.PI / 2.0F);
         knob.xRot = lid.xRot;
         VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEX_CHEST));
         lid.render(pose, vc, light, overlay);
         knob.render(pose, vc, light, overlay);
         bottom.render(pose, vc, light, overlay);
         pose.popPose();
      }
   }

   // ------------------------------------------------------------------ dioptra

   public static class Dioptra implements BlockEntityRenderer<DioptraBlockEntity> {
      public Dioptra(BlockEntityRendererProvider.Context ctx) {
      }

      @Override
      public AABB getRenderBoundingBox(DioptraBlockEntity te) {
         var p = te.getBlockPos();
         return new AABB(p.getX() - 0.3, p.getY() - 0.3, p.getZ() - 0.3, p.getX() + 1.3, p.getY() + 2.3, p.getZ() + 1.3);
      }

      @Override
      public void render(DioptraBlockEntity tco, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         float t = ticks(partialTick);
         float rc = Mth.sin(t / 12.0F) * 0.05F + 0.85F;
         float gc = DeviceBlock.isEnabled(tco.getBlockState()) ? Mth.sin(t / 11.0F) * 0.05F + 0.9F : Mth.sin(t / 11.0F) * 0.05F + 0.45F;
         float bc = Mth.sin(t / 10.0F) * 0.05F + 0.95F;
         byte[] g = tco.gridAmt;
         pose.pushPose();
         pose.translate(0.5, 0.5, 0.5);
         pose.pushPose();
         pose.translate(-0.495, 0.501, -0.495);
         pose.scale(0.99F, 1.0F, 0.99F);
         // GL_ONE blending: additive, so the colour is premultiplied by the 0.9 alpha.
         VertexConsumer vc = buffers.getBuffer(RenderType.eyes(TEX_GRID));
         int c = argb(1.0F, rc * 0.8F * 0.9F, gc * 0.9F, bc * 0.9F);
         int fl = 0xF000F0;
         for (int a = 0; a < 12; a++) {
            for (int b = 0; b < 12; b++) {
               float y00 = g[a + b * 13] / 96.0F, y10 = g[a + 1 + b * 13] / 96.0F, y11 = g[a + 1 + (b + 1) * 13] / 96.0F,
                  y01 = g[a + (b + 1) * 13] / 96.0F;
               float x0 = a / 12.0F, x1 = (a + 1) / 12.0F, z0 = b / 12.0F, z1 = (b + 1) / 12.0F;
               // the grid surface (flipFace: vertex order reversed so it faces up)
               quad(pose, vc, new float[][]{{x0, y01, z1, 0, 0}, {x1, y11, z1, 1, 0}, {x1, y10, z0, 1, 1}, {x0, y00, z0, 0, 1}},
                  new int[]{c, c, c, c}, fl, 0, 1, 0);
               int[] edge = {0, c, c, 0};
               if (a == 0) {
                  quad(pose, vc, new float[][]{{0, 0, z1, 0, 0}, {0, y01, z1, 1, 0}, {0, y00, z0, 1, 1}, {0, 0, z0, 0, 1}},
                     new int[]{0, c, c, 0}, fl, -1, 0, 0);
               }
               if (a == 11) {
                  quad(pose, vc, new float[][]{{1, 0, z0, 0, 1}, {1, y10, z0, 1, 1}, {1, y11, z1, 1, 0}, {1, 0, z1, 0, 0}}, edge, fl, 1, 0, 0);
               }
               if (b == 0) {
                  quad(pose, vc, new float[][]{{x0, y00, 0, 0, 0}, {x1, y10, 0, 1, 0}, {x1, 0, 0, 1, 1}, {x0, 0, 0, 0, 1}},
                     new int[]{c, c, 0, 0}, fl, 0, 0, -1);
               }
               if (b == 11) {
                  quad(pose, vc, new float[][]{{x0, 0, 1, 0, 1}, {x1, 0, 1, 1, 1}, {x1, y11, 1, 1, 0}, {x0, y01, 1, 0, 0}},
                     new int[]{0, 0, c, c}, fl, 0, 0, 1);
               }
            }
         }
         pose.popPose();
         pose.pushPose();
         pose.translate(0.0, 1.0, 0.0);
         RenderUtil.rot(pose, 270.0F, 0, 0, 1);
         VertexConsumer side = buffers.getBuffer(RenderType.eyes(TEX_DIOPTRA_SIDE));
         int sc = argb(1.0F, rc * 0.8F, gc * 0.8F, bc * 0.8F);
         for (int q = 0; q < 4; q++) {
            pose.pushPose();
            RenderUtil.rot(pose, 90.0F * q, 1, 0, 0);
            pose.translate(0.0, 0.0, -0.5);
            quadCenteredFrame(pose, side, 1, 0, 1.0F, sc, fl);
            RenderUtil.rot(pose, 180.0F, 0, 1, 0);
            quadCenteredFrame(pose, side, 1, 0, 1.0F, sc, fl);
            pose.popPose();
         }
         pose.popPose();
         pose.popPose();
      }
   }

   // ------------------------------------------------------------------ recharge pedestal

   public static class RechargePedestal implements BlockEntityRenderer<RechargePedestalBlockEntity> {
      public RechargePedestal(BlockEntityRendererProvider.Context ctx) {
      }

      @Override
      public void render(RechargePedestalBlockEntity ped, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         ItemStack is = ped.getItem();
         if (is.isEmpty()) return;
         float ticks = ticks(partialTick);
         pose.pushPose();
         pose.translate(0.5F, 0.75F, 0.5F);
         pose.scale(1.5F, 1.5F, 1.5F);
         RenderUtil.rot(pose, ticks % 360.0F, 0, 1, 0);
         pose.translate(0.0F, 0.125F, 0.0F);
         Minecraft.getInstance().getItemRenderer().renderStatic(is.copyWithCount(1), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY,
            pose, buffers, ped.getLevel(), 0);
         pose.popPose();
      }
   }

   // ------------------------------------------------------------------ thaumatorium

   public static class Thaumatorium implements BlockEntityRenderer<ThaumatoriumBlockEntity> {
      public Thaumatorium(BlockEntityRendererProvider.Context ctx) {
      }

      @Override
      public void render(ThaumatoriumBlockEntity tile, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         if (tile.getLevel() == null || tile.recipeIds.isEmpty()) return;
         Minecraft mc = Minecraft.getInstance();
         int stack = (mc.player != null ? mc.player.tickCount : 0) / 20 % tile.recipeIds.size();
         RecipeHolder<CrucibleRecipe> recipe = tile.recipe(tile.recipeIds.get(stack));
         if (recipe == null) return;
         Direction facing = DeviceBlock.getFacing(tile.getBlockState());
         pose.pushPose();
         pose.translate(0.5F + facing.getStepX() / 1.99F, 1.125F, 0.5F + facing.getStepZ() / 1.99F);
         switch (facing) {
            case NORTH -> RenderUtil.rot(pose, 180.0F, 0, 1, 0);
            case WEST -> RenderUtil.rot(pose, 270.0F, 0, 1, 0);
            case EAST -> RenderUtil.rot(pose, 90.0F, 0, 1, 0);
            default -> {
            }
         }
         pose.scale(0.75F, 0.75F, 0.75F);
         pose.translate(0.0F, 0.125F, 0.0F);
         ItemStack is = recipe.value().getResultItem(tile.getLevel().registryAccess()).copyWithCount(1);
         mc.getItemRenderer().renderStatic(is, ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, tile.getLevel(), 0);
         pose.popPose();
      }
   }

   // ------------------------------------------------------------------ pattern crafter

   public static class PatternCrafter implements BlockEntityRenderer<PatternCrafterBlockEntity> {
      public PatternCrafter(BlockEntityRendererProvider.Context ctx) {
      }

      @Override
      public void render(PatternCrafterBlockEntity pc, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         Direction f = pc.getLevel() != null ? DeviceBlock.getFacing(pc.getBlockState()) : Direction.SOUTH;
         pose.pushPose();
         pose.translate(0.5F, 0.75F, 0.5F);
         switch (f) {
            case NORTH -> RenderUtil.rot(pose, 180.0F, 0, 1, 0);
            case WEST -> RenderUtil.rot(pose, 270.0F, 0, 1, 0);
            case EAST -> RenderUtil.rot(pose, 90.0F, 0, 1, 0);
            default -> {
            }
         }
         pose.pushPose();
         RenderUtil.rot(pose, 90.0F, 0, 0, 1);
         pose.translate(0.0F, 0.0F, -0.5F);
         quadCenteredFrame(pose, buffers.getBuffer(RenderType.entityTranslucentCull(TEX_MODES)), 10, Math.max(0, Math.min(9, pc.type)), 0.5F,
            0xFFFFFFFF, light);
         pose.popPose();
         VertexConsumer gear = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_GEAR));
         for (int side = -1; side <= 1; side += 2) {
            pose.pushPose();
            pose.translate(0.2F * side, -0.40625F, 0.05F);
            RenderUtil.rot(pose, side * pc.rot % 360.0F, 0, 0, 1);
            pose.scale(0.5F, 0.5F, 1.0F);
            for (float z : new float[]{0.0F, -0.1F}) {
               pose.pushPose();
               pose.translate(0, 0, z);
               quadCenteredFrame(pose, gear, 1, 0, 1.0F, 0xFFFFFFFF, light);
               pose.popPose();
            }
            pose.popPose();
         }
         pose.popPose();
      }
   }

   // ------------------------------------------------------------------ mirrors

   public static class Mirror implements BlockEntityRenderer<MirrorBlockEntity> {
      public Mirror(BlockEntityRendererProvider.Context ctx) {
      }

      /** translateFromOrientation: the plane of the mirror, at off from the block it hangs on. */
      private static void translateFromOrientation(PoseStack pose, Direction d, float off) {
         switch (d) {
            case DOWN -> {
               pose.translate(0, 1, 1);
               RenderUtil.rot(pose, -90.0F, 1, 0, 0);
            }
            case UP -> RenderUtil.rot(pose, 90.0F, 1, 0, 0);
            case NORTH -> pose.translate(0, 0, 1);
            case SOUTH -> {
               pose.translate(1, 0, 0);
               RenderUtil.rot(pose, 180.0F, 0, 1, 0);
            }
            case WEST -> {
               pose.translate(1, 0, 1);
               RenderUtil.rot(pose, 90.0F, 0, 1, 0);
            }
            case EAST -> RenderUtil.rot(pose, -90.0F, 0, 1, 0);
         }
         pose.translate(0.0F, 0.0F, -off);
      }

      @Override
      public void render(MirrorBlockEntity te, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         Direction dir = DeviceBlock.getFacing(te.getBlockState());
         Minecraft mc = Minecraft.getInstance();
         boolean near = mc.player != null && mc.player.distanceToSqr(te.getBlockPos().getCenter()) < 1024.0;
         if (te.linked && near) {
            // The view into the other side: the end portal shader stands in for the 1.12 layered tunnel texture.
            pose.pushPose();
            translateFromOrientation(pose, dir, 0.015F);
            float p = 0.1875F;
            Matrix4f m = pose.last().pose();
            VertexConsumer vc = buffers.getBuffer(RenderType.endPortal());
            vc.addVertex(m, p, 1 - p, 0);
            vc.addVertex(m, 1 - p, 1 - p, 0);
            vc.addVertex(m, 1 - p, p, 0);
            vc.addVertex(m, p, p, 0);
            pose.popPose();
         }
         pose.pushPose();
         translateFromOrientation(pose, dir, 0.02F);
         RenderUtil.rot(pose, 90.0F, 0, 0, 1);
         pose.translate(0.5, -0.5, 0.0);
         VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentCull(te.linked && near ? TEX_PANE_TRANS : TEX_PANE));
         quadCenteredFrame(pose, vc, 1, 0, 1.0F, 0xFFFFFFFF, light);
         pose.popPose();
      }
   }

   // ------------------------------------------------------------------ brain in a jar

   public static class JarBrain implements BlockEntityRenderer<JarBrainBlockEntity> {
      private final ModelPart brain1;
      private final ModelPart brain2;
      private final ModelPart brain3;
      private final ModelPart brine;

      public JarBrain(BlockEntityRendererProvider.Context ctx) {
         ModelPart root = ctx.bakeLayer(BRAIN);
         brain1 = root.getChild("shape1");
         brain2 = root.getChild("shape2");
         brain3 = root.getChild("shape3");
         brine = root.getChild("brine");
      }

      @Override
      public void render(JarBrainBlockEntity te, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         float bob = Mth.sin(ticks(partialTick) / 14.0F) * 0.03F + 0.03F;
         pose.pushPose();
         pose.translate(0.5F, 0.01F, 0.5F);
         RenderUtil.rot(pose, 180.0F, 1, 0, 0);
         pose.pushPose();
         pose.translate(0.0F, -0.8F + bob, 0.0F);
         float f2 = te.rota - te.rotb;
         while (f2 >= (float) Math.PI) f2 -= (float) (Math.PI * 2);
         while (f2 < -(float) Math.PI) f2 += (float) (Math.PI * 2);
         float f3 = te.rotb + f2 * partialTick;
         RenderUtil.rot(pose, f3 * 180.0F / (float) Math.PI, 0, 1, 0);
         RenderUtil.rot(pose, -90.0F, 0, 1, 0);
         pose.scale(0.4F, 0.4F, 0.4F);
         VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_BRAIN));
         brain1.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
         brain2.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
         brain3.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
         pose.popPose();
         brine.render(pose, buffers.getBuffer(RenderType.entityTranslucent(TEX_BRINE)), light, OverlayTexture.NO_OVERLAY);
         pose.popPose();
      }
   }

   // ------------------------------------------------------------------ banners

   public static class Banner implements BlockEntityRenderer<BannerBlockEntity> {
      private final ModelPart b1, b2, beam, banner, pole;

      public Banner(BlockEntityRendererProvider.Context ctx) {
         ModelPart root = ctx.bakeLayer(BANNER);
         b1 = root.getChild("b1");
         b2 = root.getChild("b2");
         beam = root.getChild("beam");
         banner = root.getChild("banner");
         pole = root.getChild("pole");
      }

      @Override
      public AABB getRenderBoundingBox(BannerBlockEntity te) {
         var p = te.getBlockPos();
         return new AABB(p.getX(), p.getY() - 1, p.getZ(), p.getX() + 1, p.getY() + 2, p.getZ() + 1);
      }

      @Override
      public void render(BannerBlockEntity te, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         int color = te.getColor();
         ResourceLocation tex = te.getAspect() == null && color == -1 ? TEX_CULT : TEX_BLANK;
         VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
         pose.pushPose();
         pose.translate(0.5F, 1.5F, 0.5F);
         RenderUtil.rot(pose, 180.0F, 1, 0, 0);
         if (te.getLevel() != null) {
            RenderUtil.rot(pose, 180.0F, 0, 1, 0);
            RenderUtil.rot(pose, te.getBannerFacing() * 360 / 16.0F, 0, 1, 0);
         }
         if (!te.getWall()) pole.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
         else pose.translate(0.0, 1.0, -0.4125);
         beam.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
         int tint = color == -1 ? 0xFFFFFFFF : 0xFF000000 | color;
         b1.render(pose, vc, light, OverlayTexture.NO_OVERLAY, tint);
         b2.render(pose, vc, light, OverlayTexture.NO_OVERLAY, tint);
         var bp = te.getBlockPos();
         float f3 = bp.getX() * 7 + bp.getY() * 9 + bp.getZ() * 13 + ticks(partialTick);
         float rx = 0.02F - Mth.sin(f3 / 11.0F) * 0.02F;
         banner.xRot = rx;
         banner.render(pose, vc, light, OverlayTexture.NO_OVERLAY, tint);
         if (te.getAspect() != null) {
            pose.pushPose();
            pose.translate(0.0F, 0.0F, 0.05001F);
            pose.scale(0.0375F, 0.0375F, 0.0375F);
            RenderUtil.rot(pose, 180.0F, 0, 1, 0);
            RenderUtil.rot(pose, -rx * (180.0F / (float) Math.PI) * 2.0F, 1, 0, 0);
            pose.translate(0.0F, 8.0F, 0.0F);
            pose.scale(16.0F, -16.0F, 16.0F);
            RenderUtil.quadCentered(pose, buffers, te.getAspect().getImage(), 1.0F, 0xBF000000 | te.getAspect().getColor(), light);
            pose.popPose();
         }
         pose.popPose();
      }
   }
}
