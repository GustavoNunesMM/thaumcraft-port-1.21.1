package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

public class ModelArcaneBore extends ModelBase {
   ModelRenderer crystal;
   ModelRenderer leg2;
   ModelRenderer tripod;
   ModelRenderer leg3;
   ModelRenderer leg4;
   ModelRenderer leg1;
   ModelRenderer magbase;
   ModelRenderer base;
   ModelRenderer domebase;
   ModelRenderer dome;
   ModelRenderer tip;

   public ModelArcaneBore() {
      this.textureWidth = 64;
      this.textureHeight = 32;
      this.leg2 = new ModelRenderer(this, 20, 10);
      this.leg2.addBox(-1.0F, 1.0F, -1.0F, 2, 13, 2);
      this.leg2.setRotationPoint(0.0F, 12.0F, 0.0F);
      this.leg2.setTextureSize(64, 32);
      this.setRotation(this.leg2, (float) (Math.PI / 6), 1.570796F, 0.0F);
      this.tripod = new ModelRenderer(this, 13, 0);
      this.tripod.addBox(-1.5F, 0.0F, -1.5F, 3, 2, 3);
      this.tripod.setRotationPoint(0.0F, 12.0F, 0.0F);
      this.tripod.setTextureSize(64, 32);
      this.setRotation(this.tripod, 0.0F, 0.0F, 0.0F);
      this.leg3 = new ModelRenderer(this, 20, 10);
      this.leg3.addBox(-1.0F, 1.0F, -1.0F, 2, 13, 2);
      this.leg3.setRotationPoint(0.0F, 12.0F, 0.0F);
      this.leg3.setTextureSize(64, 32);
      this.setRotation(this.leg3, (float) (Math.PI / 6), 3.141593F, 0.0F);
      this.leg4 = new ModelRenderer(this, 20, 10);
      this.leg4.addBox(-1.0F, 1.0F, -1.0F, 2, 13, 2);
      this.leg4.setRotationPoint(0.0F, 12.0F, 0.0F);
      this.leg4.setTextureSize(64, 32);
      this.setRotation(this.leg4, (float) (Math.PI / 6), (float) (Math.PI * 3.0 / 2.0), 0.0F);
      this.leg1 = new ModelRenderer(this, 20, 10);
      this.leg1.addBox(-1.0F, 1.0F, -1.0F, 2, 13, 2);
      this.leg1.setRotationPoint(0.0F, 12.0F, 0.0F);
      this.leg1.setTextureSize(64, 32);
      this.setRotation(this.leg1, (float) (Math.PI / 6), 0.0F, 0.0F);
      this.base = new ModelRenderer(this, 32, 0);
      this.base.addBox(-3.0F, -6.0F, -3.0F, 6, 6, 6);
      this.base.setRotationPoint(0.0F, 13.0F, 0.0F);
      this.base.setTextureSize(64, 32);
      this.setRotation(this.base, 0.0F, 0.0F, 0.0F);
      this.crystal = new ModelRenderer(this, 32, 25);
      this.crystal.addBox(-1.0F, -4.0F, 5.0F, 2, 2, 2);
      this.crystal.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.crystal.setTextureSize(64, 32);
      this.setRotation(this.crystal, 0.0F, 0.0F, 0.0F);
      this.domebase = new ModelRenderer(this, 32, 19);
      this.domebase.addBox(-2.0F, -5.0F, 3.0F, 4, 4, 1);
      this.domebase.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.domebase.setTextureSize(64, 32);
      this.setRotation(this.domebase, 0.0F, 0.0F, 0.0F);
      this.dome = new ModelRenderer(this, 44, 16);
      this.dome.addBox(-2.0F, -5.0F, 4.0F, 4, 4, 4);
      this.dome.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.dome.setTextureSize(64, 32);
      this.setRotation(this.dome, 0.0F, 0.0F, 0.0F);
      this.magbase = new ModelRenderer(this, 0, 18);
      this.magbase.addBox(-1.0F, -4.0F, -6.0F, 2, 2, 3);
      this.magbase.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.magbase.setTextureSize(64, 32);
      this.magbase.mirror = true;
      this.setRotation(this.magbase, 0.0F, 0.0F, 0.0F);
      this.tip = new ModelRenderer(this, 0, 9);
      this.tip.addBox(-1.5F, 0.0F, -1.5F, 3, 3, 3);
      this.tip.setRotationPoint(0.0F, -3.0F, -6.0F);
      this.tip.setTextureSize(64, 32);
      this.tip.mirror = true;
      this.setRotation(this.tip, -1.570796F, 0.0F, 0.0F);
      this.base.addChild(this.crystal);
      this.base.addChild(this.dome);
      this.base.addChild(this.domebase);
      this.base.addChild(this.magbase);
      this.base.addChild(this.tip);
   }

   public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      super.render(entity, f, f1, f2, f3, f4, f5);
      this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
      this.leg2.render(f5);
      this.tripod.render(f5);
      this.leg3.render(f5);
      this.leg4.render(f5);
      this.leg1.render(f5);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      this.base.render(f5);
      GL11.glDisable(3042);
   }

   private void setRotation(ModelRenderer model, float x, float y, float z) {
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
   }

   public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float headpitch, float headyaw, float scaleFactor, Entity entity) {
      this.base.rotateAngleY = headpitch / (180.0F / (float)Math.PI);
      this.base.rotateAngleX = headyaw / (180.0F / (float)Math.PI);
   }
}
