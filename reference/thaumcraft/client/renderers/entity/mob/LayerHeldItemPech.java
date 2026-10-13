package thaumcraft.client.renderers.entity.mob;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LayerHeldItemPech extends LayerHeldItem {
   public LayerHeldItemPech(RenderLivingBase<?> livingEntityRendererIn) {
      super(livingEntityRendererIn);
   }

   public void doRenderLayer(
      EntityLivingBase entitylivingbaseIn,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float ageInTicks,
      float netHeadYaw,
      float headPitch,
      float scale
   ) {
      boolean flag = entitylivingbaseIn.getPrimaryHand() == EnumHandSide.RIGHT;
      ItemStack itemstack = flag ? entitylivingbaseIn.getHeldItemOffhand() : entitylivingbaseIn.getHeldItemMainhand();
      ItemStack itemstack1 = flag ? entitylivingbaseIn.getHeldItemMainhand() : entitylivingbaseIn.getHeldItemOffhand();
      if (itemstack != null && !itemstack.isEmpty() || itemstack1 != null && !itemstack1.isEmpty()) {
         GlStateManager.pushMatrix();
         if (this.livingEntityRenderer.getMainModel().isChild) {
            float f = 0.5F;
            GlStateManager.translate(0.0F, 0.625F, 0.0F);
            GlStateManager.rotate(-20.0F, -1.0F, 0.0F, 0.0F);
            GlStateManager.scale(f, f, f);
         }

         this.renderHeldItem(entitylivingbaseIn, itemstack1, TransformType.THIRD_PERSON_RIGHT_HAND, EnumHandSide.RIGHT);
         this.renderHeldItem(entitylivingbaseIn, itemstack, TransformType.THIRD_PERSON_LEFT_HAND, EnumHandSide.LEFT);
         GlStateManager.popMatrix();
      }
   }

   private void renderHeldItem(EntityLivingBase entity, ItemStack stack, TransformType p_188358_3_, EnumHandSide handSide) {
      if (stack != null && !stack.isEmpty()) {
         GlStateManager.pushMatrix();
         ((ModelBiped)this.livingEntityRenderer.getMainModel()).postRenderArm(0.0625F, handSide);
         GlStateManager.translate(0.0F, -0.1F, 0.0625F);
         if (stack.getItem() instanceof ItemBow) {
            GlStateManager.translate(-0.075F, -0.1, 0.0);
         }

         if (entity.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
         }

         GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
         GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
         boolean flag = handSide == EnumHandSide.LEFT;
         GlStateManager.translate(flag ? -0.0625F : 0.0625F, 0.125F, -0.625F);
         Minecraft.getMinecraft().getItemRenderer().renderItemSide(entity, stack, p_188358_3_, flag);
         GlStateManager.popMatrix();
      }
   }

   public boolean shouldCombineTextures() {
      return false;
   }
}
