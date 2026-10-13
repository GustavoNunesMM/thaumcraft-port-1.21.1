package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.math.MathHelper;
import thaumcraft.common.entities.construct.EntityTurretCrossbow;

public class ModelCrossbow extends ModelBase {
   ModelRenderer crossl3;
   ModelRenderer crossr3;
   ModelRenderer loadbarcross;
   ModelRenderer loadbarl;
   ModelRenderer loadbarr;
   ModelRenderer barrel;
   ModelRenderer basebarcross;
   ModelRenderer ammobox;
   ModelRenderer crossbow;
   ModelRenderer basebarr;
   ModelRenderer basebarl;
   ModelRenderer crossl1;
   ModelRenderer crossl2;
   ModelRenderer crossr1;
   ModelRenderer crossr2;
   ModelRenderer tripod;
   ModelRenderer leg3;
   ModelRenderer leg4;
   ModelRenderer leg1;
   ModelRenderer leg2;

   public ModelCrossbow() {
      this.textureWidth = 64;
      this.textureHeight = 32;
      this.crossbow = new ModelRenderer(this, 28, 14);
      this.crossbow.addBox(-2.0F, 0.0F, -7.0F, 4, 2, 14);
      this.crossbow.setRotationPoint(0.0F, 10.0F, 0.0F);
      this.crossbow.setTextureSize(64, 32);
      this.crossbow.mirror = true;
      this.setRotation(this.crossbow, 0.0F, 0.0F, 0.0F);
      this.basebarr = new ModelRenderer(this, 40, 23);
      this.basebarr.addBox(-1.0F, 0.0F, 7.0F, 1, 2, 5);
      this.basebarr.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.basebarr.setTextureSize(64, 32);
      this.basebarr.mirror = true;
      this.setRotation(this.basebarr, 0.0F, -0.1396263F, 0.0F);
      this.basebarl = new ModelRenderer(this, 40, 23);
      this.basebarl.addBox(0.0F, 0.0F, 7.0F, 1, 2, 5);
      this.basebarl.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.basebarl.setTextureSize(64, 32);
      this.basebarl.mirror = true;
      this.setRotation(this.basebarl, 0.0F, 0.1396263F, 0.0F);
      this.barrel = new ModelRenderer(this, 20, 28);
      this.barrel.addBox(-1.0F, -1.0F, -8.0F, 2, 2, 2);
      this.barrel.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.barrel.setTextureSize(64, 32);
      this.barrel.mirror = true;
      this.setRotation(this.barrel, 0.0F, 0.0F, 0.0F);
      this.basebarcross = new ModelRenderer(this, 0, 13);
      this.basebarcross.addBox(-2.0F, 0.5F, 10.0F, 4, 1, 1);
      this.basebarcross.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.basebarcross.setTextureSize(64, 32);
      this.basebarcross.mirror = true;
      this.setRotation(this.basebarcross, 0.0F, 0.0F, 0.0F);
      this.ammobox = new ModelRenderer(this, 38, 0);
      this.ammobox.addBox(-2.0F, -5.0F, -6.0F, 4, 5, 9);
      this.ammobox.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.ammobox.setTextureSize(64, 32);
      this.ammobox.mirror = true;
      this.setRotation(this.ammobox, 0.0F, 0.0F, 0.0F);
      this.loadbarcross = new ModelRenderer(this, 0, 13);
      this.loadbarcross.addBox(-2.0F, -8.5F, -0.5F, 4, 1, 1);
      this.loadbarcross.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.loadbarcross.setTextureSize(64, 32);
      this.loadbarcross.mirror = true;
      this.setRotation(this.loadbarcross, -0.5585054F, 0.0F, 0.0F);
      this.loadbarl = new ModelRenderer(this, 0, 15);
      this.loadbarl.addBox(2.0F, -9.0F, -1.0F, 1, 11, 2);
      this.loadbarl.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.loadbarl.setTextureSize(64, 32);
      this.loadbarl.mirror = true;
      this.setRotation(this.loadbarl, -0.5585054F, 0.0F, 0.0F);
      this.loadbarr = new ModelRenderer(this, 0, 15);
      this.loadbarr.addBox(-3.0F, -9.0F, -1.0F, 1, 11, 2);
      this.loadbarr.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.loadbarr.setTextureSize(64, 32);
      this.loadbarr.mirror = true;
      this.setRotation(this.loadbarr, -0.5585054F, 0.0F, 0.0F);
      this.crossl1 = new ModelRenderer(this, 0, 0);
      this.crossl1.addBox(0.0F, 0.0F, -6.0F, 5, 2, 1);
      this.crossl1.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.crossl1.setTextureSize(64, 32);
      this.crossl1.mirror = true;
      this.setRotation(this.crossl1, 0.0F, -0.2443461F, 0.0F);
      this.crossl2 = new ModelRenderer(this, 0, 0);
      this.crossl2.addBox(4.0F, 0.0F, -5.0F, 3, 2, 1);
      this.crossl2.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.crossl2.setTextureSize(64, 32);
      this.crossl2.mirror = true;
      this.setRotation(this.crossl2, 0.0F, -0.2443461F, 0.0F);
      this.crossl3 = new ModelRenderer(this, 0, 0);
      this.crossl3.addBox(6.0F, 0.0F, -4.0F, 2, 2, 1);
      this.crossl3.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.crossl3.setTextureSize(64, 32);
      this.crossl3.mirror = true;
      this.setRotation(this.crossl3, 0.0F, -0.2443461F, 0.0F);
      this.crossr1 = new ModelRenderer(this, 0, 0);
      this.crossr1.addBox(-5.0F, 0.0F, -6.0F, 5, 2, 1);
      this.crossr1.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.crossr1.setTextureSize(64, 32);
      this.crossr1.mirror = true;
      this.setRotation(this.crossr1, 0.0F, 0.2443461F, 0.0F);
      this.crossr2 = new ModelRenderer(this, 0, 0);
      this.crossr2.addBox(-7.0F, 0.0F, -5.0F, 3, 2, 1);
      this.crossr2.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.crossr2.setTextureSize(64, 32);
      this.crossr2.mirror = true;
      this.setRotation(this.crossr2, 0.0F, 0.2443461F, 0.0F);
      this.crossr3 = new ModelRenderer(this, 0, 0);
      this.crossr3.addBox(-8.0F, 0.0F, -4.0F, 2, 2, 1);
      this.crossr3.setRotationPoint(0.0F, 0.0F, 0.0F);
      this.crossr3.setTextureSize(64, 32);
      this.crossr3.mirror = true;
      this.setRotation(this.crossr3, 0.0F, 0.2443461F, 0.0F);
      this.leg2 = new ModelRenderer(this, 20, 10);
      this.leg2.addBox(-1.0F, 1.0F, -1.0F, 2, 13, 2);
      this.leg2.setRotationPoint(0.0F, 12.0F, 0.0F);
      this.leg2.setTextureSize(64, 32);
      this.leg2.mirror = true;
      this.setRotation(this.leg2, (float) (Math.PI / 6), 1.570796F, 0.0F);
      this.tripod = new ModelRenderer(this, 13, 0);
      this.tripod.addBox(-1.5F, 0.0F, -1.5F, 3, 2, 3);
      this.tripod.setRotationPoint(0.0F, 12.0F, 0.0F);
      this.tripod.setTextureSize(64, 32);
      this.tripod.mirror = true;
      this.setRotation(this.tripod, 0.0F, 0.0F, 0.0F);
      this.leg3 = new ModelRenderer(this, 20, 10);
      this.leg3.addBox(-1.0F, 1.0F, -1.0F, 2, 13, 2);
      this.leg3.setRotationPoint(0.0F, 12.0F, 0.0F);
      this.leg3.setTextureSize(64, 32);
      this.leg3.mirror = true;
      this.setRotation(this.leg3, (float) (Math.PI / 6), 3.141593F, 0.0F);
      this.leg4 = new ModelRenderer(this, 20, 10);
      this.leg4.addBox(-1.0F, 1.0F, -1.0F, 2, 13, 2);
      this.leg4.setRotationPoint(0.0F, 12.0F, 0.0F);
      this.leg4.setTextureSize(64, 32);
      this.leg4.mirror = true;
      this.setRotation(this.leg4, (float) (Math.PI / 6), (float) (Math.PI * 3.0 / 2.0), 0.0F);
      this.leg1 = new ModelRenderer(this, 20, 10);
      this.leg1.addBox(-1.0F, 1.0F, -1.0F, 2, 13, 2);
      this.leg1.setRotationPoint(0.0F, 12.0F, 0.0F);
      this.leg1.setTextureSize(64, 32);
      this.leg1.mirror = true;
      this.setRotation(this.leg1, (float) (Math.PI / 6), 0.0F, 0.0F);
      this.crossbow.addChild(this.ammobox);
      this.crossbow.addChild(this.barrel);
      this.crossbow.addChild(this.basebarcross);
      this.crossbow.addChild(this.basebarr);
      this.crossbow.addChild(this.basebarl);
      this.crossbow.addChild(this.loadbarcross);
      this.crossbow.addChild(this.loadbarl);
      this.crossbow.addChild(this.loadbarr);
      this.crossbow.addChild(this.crossl1);
      this.crossbow.addChild(this.crossl2);
      this.crossbow.addChild(this.crossl3);
      this.crossbow.addChild(this.crossr1);
      this.crossbow.addChild(this.crossr2);
      this.crossbow.addChild(this.crossr3);
   }

   public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
      super.render(entity, f, f1, f2, f3, f4, f5);
      this.setRotationAngles(f, f1, f2, f3, f4, f5, entity);
      this.crossbow.render(f5);
      this.leg2.render(f5);
      this.tripod.render(f5);
      this.leg3.render(f5);
      this.leg4.render(f5);
      this.leg1.render(f5);
   }

   private void setRotation(ModelRenderer model, float x, float y, float z) {
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
   }

   public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float headpitch, float headyaw, float scaleFactor, Entity entity) {
      this.crossbow.rotateAngleY = headpitch / (180.0F / (float)Math.PI);
      this.crossbow.rotateAngleX = headyaw / (180.0F / (float)Math.PI);
      if (this.swingProgress > -9990.0F) {
         float f6 = this.swingProgress;
         this.crossl1.rotateAngleY = this.crossl2.rotateAngleY = this.crossl3.rotateAngleY = -0.2F
            + MathHelper.sin(MathHelper.sqrt(f6) * (float) Math.PI * 2.0F) * 0.2F;
         this.crossr1.rotateAngleY = this.crossr2.rotateAngleY = this.crossr3.rotateAngleY = 0.2F
            - MathHelper.sin(MathHelper.sqrt(f6) * (float) Math.PI * 2.0F) * 0.2F;
      }

      float lp = ((EntityTurretCrossbow)entity).loadProgressForRender;
      this.loadbarcross.rotateAngleX = this.loadbarl.rotateAngleX = this.loadbarr.rotateAngleX = -0.5F
         + MathHelper.sin(MathHelper.sqrt(lp) * (float) Math.PI * 2.0F) * 0.5F;
      if (entity.isRiding() && entity.getRidingEntity() != null && entity.getRidingEntity() instanceof EntityMinecart) {
         this.leg1.offsetY = -0.5F;
         this.leg2.offsetY = -0.5F;
         this.leg3.offsetY = -0.5F;
         this.leg4.offsetY = -0.5F;
         this.leg1.rotateAngleX = 0.1F;
         this.leg2.rotateAngleX = 0.1F;
         this.leg3.rotateAngleX = 0.1F;
         this.leg4.rotateAngleX = 0.1F;
      } else {
         this.leg1.offsetY = 0.0F;
         this.leg2.offsetY = 0.0F;
         this.leg3.offsetY = 0.0F;
         this.leg4.offsetY = 0.0F;
         this.leg1.rotateAngleX = 0.5F;
         this.leg2.rotateAngleX = 0.5F;
         this.leg3.rotateAngleX = 0.5F;
         this.leg4.rotateAngleX = 0.5F;
      }
   }
}
