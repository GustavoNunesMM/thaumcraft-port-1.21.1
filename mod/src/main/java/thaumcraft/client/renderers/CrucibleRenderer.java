package thaumcraft.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Matrix4f;
import thaumcraft.common.blocks.crafting.CrucibleBlockEntity;

/** TileCrucibleRenderer: the water surface, tinted towards purple as essentia builds up. */
public class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity> {
   public CrucibleRenderer(BlockEntityRendererProvider.Context ctx) {
   }

   @Override
   public void render(CrucibleBlockEntity cr, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
      if (cr.tank.getFluidAmount() <= 0) return;
      IClientFluidTypeExtensions water = IClientFluidTypeExtensions.of(Fluids.WATER);
      TextureAtlasSprite icon = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(water.getStillTexture());
      float recolor = cr.aspects.visSize() / 500.0F;
      if (recolor > 0.0F) recolor = 0.5F + recolor / 2.0F;
      if (recolor > 1.0F) recolor = 1.0F;
      // The 1.12 water texture was already blue; the 1.21 one is gray and takes the water tint.
      int tint = 0x3F76E4;
      float r = (tint >> 16 & 255) / 255.0F * (1.0F - recolor / 3.0F);
      float g = (tint >> 8 & 255) / 255.0F * (1.0F - recolor);
      float b = (tint & 255) / 255.0F * (1.0F - recolor / 2.0F);
      float h = cr.getFluidHeight();
      VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
      Matrix4f m = pose.last().pose();
      float u0 = icon.getU0(), u1 = icon.getU1(), v0 = icon.getV0(), v1 = icon.getV1();
      vc.addVertex(m, 0, h, 0).setColor(r, g, b, 1.0F).setUv(u0, v0).setLight(light).setNormal(pose.last(), 0, 1, 0);
      vc.addVertex(m, 0, h, 1).setColor(r, g, b, 1.0F).setUv(u0, v1).setLight(light).setNormal(pose.last(), 0, 1, 0);
      vc.addVertex(m, 1, h, 1).setColor(r, g, b, 1.0F).setUv(u1, v1).setLight(light).setNormal(pose.last(), 0, 1, 0);
      vc.addVertex(m, 1, h, 0).setColor(r, g, b, 1.0F).setUv(u1, v0).setLight(light).setNormal(pose.last(), 0, 1, 0);
   }
}
