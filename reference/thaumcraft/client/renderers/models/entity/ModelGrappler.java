package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

public class ModelGrappler extends ModelBase {
   ModelRenderer core;
   ModelRenderer prong1;
   ModelRenderer prong2;
   ModelRenderer prong3;

   public ModelGrappler() {
      this.textureWidth = 64;
      this.textureHeight = 32;
      this.core = new ModelRenderer(this, 0, 0);
      this.core.addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3);
      this.core.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.core.setTextureSize(this.textureWidth, this.textureHeight);
      this.setRotation(this.core, 0.0F, 0.0F, 0.0F);
      this.prong1 = new ModelRenderer(this, 0, 10);
      this.prong1.addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5);
      this.prong1.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.prong1.setTextureSize(this.textureWidth, this.textureHeight);
      this.setRotation(this.prong1, 0.0F, 0.0F, 0.0F);
      this.prong2 = new ModelRenderer(this, 0, 10);
      this.prong2.addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5);
      this.prong2.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.prong2.setTextureSize(this.textureWidth, this.textureHeight);
      this.setRotation(this.prong2, 0.0F, (float) (Math.PI / 2), 0.0F);
      this.prong3 = new ModelRenderer(this, 0, 10);
      this.prong3.addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5);
      this.prong3.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.prong3.setTextureSize(this.textureWidth, this.textureHeight);
      this.setRotation(this.prong3, (float) (Math.PI / 2), (float) (Math.PI / 2), 0.0F);
   }

   public void render() {
      this.core.render(0.0625F);
      this.prong1.render(0.0625F);
      this.prong2.render(0.0625F);
      this.prong3.render(0.0625F);
   }

   private void setRotation(ModelRenderer model, float x, float y, float z) {
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
   }
}
