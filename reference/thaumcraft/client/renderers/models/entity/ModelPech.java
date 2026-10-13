package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import thaumcraft.common.entities.monster.EntityPech;

public class ModelPech extends ModelBiped {
   ModelRenderer Jowls;
   ModelRenderer LowerPack;
   ModelRenderer UpperPack;

   public ModelPech() {
      this.textureWidth = 128;
      this.textureHeight = 64;
      this.bipedBody = new ModelRenderer(this, 34, 12);
      this.bipedBody.addBox(-3.0F, 0.0F, 0.0F, 6, 10, 6);
      this.bipedBody.setRotationPoint(0.0F, 9.0F, -3.0F);
      this.bipedBody.setTextureSize(128, 64);
      this.bipedBody.mirror = true;
      this.setRotation(this.bipedBody, 0.3129957F, 0.0F, 0.0F);
      this.bipedRightLeg = new ModelRenderer(this, 35, 1);
      this.bipedRightLeg.mirror = true;
      this.bipedRightLeg.addBox(-2.9F, 0.0F, 0.0F, 3, 6, 3);
      this.bipedRightLeg.setRotationPoint(0.0F, 18.0F, 0.0F);
      this.bipedRightLeg.setTextureSize(128, 64);
      this.bipedRightLeg.mirror = true;
      this.setRotation(this.bipedRightLeg, 0.0F, 0.0F, 0.0F);
      this.bipedRightLeg.mirror = false;
      this.bipedLeftLeg = new ModelRenderer(this, 35, 1);
      this.bipedLeftLeg.addBox(-0.1F, 0.0F, 0.0F, 3, 6, 3);
      this.bipedLeftLeg.setRotationPoint(0.0F, 18.0F, 0.0F);
      this.bipedLeftLeg.setTextureSize(128, 64);
      this.bipedLeftLeg.mirror = true;
      this.setRotation(this.bipedLeftLeg, 0.0F, 0.0F, 0.0F);
      this.bipedHead = new ModelRenderer(this, 2, 11);
      this.bipedHead.addBox(-3.5F, -5.0F, -5.0F, 7, 5, 5);
      this.bipedHead.setRotationPoint(0.0F, 8.0F, 0.0F);
      this.bipedHead.setTextureSize(128, 64);
      this.bipedHead.mirror = true;
      this.setRotation(this.bipedHead, 0.0F, 0.0F, 0.0F);
      this.Jowls = new ModelRenderer(this, 1, 21);
      this.Jowls.addBox(-4.0F, -1.0F, -6.0F, 8, 3, 5);
      this.Jowls.setRotationPoint(0.0F, 8.0F, 0.0F);
      this.Jowls.setTextureSize(128, 64);
      this.Jowls.mirror = true;
      this.setRotation(this.Jowls, 0.0F, 0.0F, 0.0F);
      this.LowerPack = new ModelRenderer(this, 0, 0);
      this.LowerPack.addBox(-5.0F, 0.0F, 0.0F, 10, 5, 5);
      this.LowerPack.setRotationPoint(0.0F, 10.0F, 3.5F);
      this.LowerPack.setTextureSize(128, 64);
      this.LowerPack.mirror = true;
      this.setRotation(this.LowerPack, 0.3013602F, 0.0F, 0.0F);
      this.UpperPack = new ModelRenderer(this, 64, 1);
      this.UpperPack.addBox(-7.5F, -14.0F, 0.0F, 15, 14, 11);
      this.UpperPack.setRotationPoint(0.0F, 10.0F, 3.0F);
      this.UpperPack.setTextureSize(128, 64);
      this.UpperPack.mirror = true;
      this.setRotation(this.UpperPack, 0.4537856F, 0.0F, 0.0F);
      this.bipedRightArm = new ModelRenderer(this, 52, 2);
      this.bipedRightArm.mirror = true;
      this.bipedRightArm.addBox(-2.0F, 0.0F, -1.0F, 2, 6, 2);
      this.bipedRightArm.setRotationPoint(-3.0F, 10.0F, -1.0F);
      this.bipedRightArm.setTextureSize(128, 64);
      this.bipedRightArm.mirror = true;
      this.setRotation(this.bipedRightArm, 0.0F, 0.0F, 0.0F);
      this.bipedRightArm.mirror = false;
      this.bipedLeftArm = new ModelRenderer(this, 52, 2);
      this.bipedLeftArm.addBox(0.0F, 0.0F, -1.0F, 2, 6, 2);
      this.bipedLeftArm.setRotationPoint(3.0F, 10.0F, -1.0F);
      this.bipedLeftArm.setTextureSize(128, 64);
      this.bipedLeftArm.mirror = true;
      this.setRotation(this.bipedLeftArm, 0.0F, 0.0F, 0.0F);
   }

   public void render(Entity par1Entity, float par2, float par3, float par4, float par5, float par6, float par7) {
      this.setRotationAngles(par2, par3, par4, par5, par6, par7, par1Entity);
      this.bipedBody.render(par7);
      this.bipedRightLeg.render(par7);
      this.bipedLeftLeg.render(par7);
      this.bipedHead.render(par7);
      this.Jowls.render(par7);
      this.LowerPack.render(par7);
      this.UpperPack.render(par7);
      this.bipedRightArm.render(par7);
      this.bipedLeftArm.render(par7);
   }

   private void setRotation(ModelRenderer model, float x, float y, float z) {
      model.rotateAngleX = x;
      model.rotateAngleY = y;
      model.rotateAngleZ = z;
   }

   public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
      this.bipedHead.rotateAngleY = par4 / (180.0F / (float)Math.PI);
      this.bipedHead.rotateAngleX = par5 / (180.0F / (float)Math.PI);
      float mumble = 0.0F;
      if (entity instanceof EntityPech) {
         mumble = ((EntityPech)entity).mumble;
      }

      this.Jowls.rotateAngleY = this.bipedHead.rotateAngleY;
      this.Jowls.rotateAngleX = this.bipedHead.rotateAngleX
         + ((float) (Math.PI / 12) + MathHelper.cos(par1 * 0.6662F) * par2 * 0.25F)
         + 0.34906587F * Math.abs(MathHelper.sin(mumble / 8.0F));
      this.bipedRightArm.rotateAngleX = MathHelper.cos(par1 * 0.6662F + (float) Math.PI) * 2.0F * par2 * 0.5F;
      this.bipedLeftArm.rotateAngleX = MathHelper.cos(par1 * 0.6662F) * 2.0F * par2 * 0.5F;
      this.bipedRightArm.rotateAngleZ = 0.0F;
      this.bipedLeftArm.rotateAngleZ = 0.0F;
      this.bipedRightLeg.rotateAngleX = MathHelper.cos(par1 * 0.6662F) * 1.4F * par2;
      this.bipedLeftLeg.rotateAngleX = MathHelper.cos(par1 * 0.6662F + (float) Math.PI) * 1.4F * par2;
      this.bipedRightLeg.rotateAngleY = 0.0F;
      this.bipedLeftLeg.rotateAngleY = 0.0F;
      this.LowerPack.rotateAngleY = MathHelper.cos(par1 * 0.6662F) * 2.0F * par2 * 0.125F;
      this.LowerPack.rotateAngleZ = MathHelper.cos(par1 * 0.6662F) * 2.0F * par2 * 0.125F;
      if (this.isRiding) {
         this.bipedRightArm.rotateAngleX += (float) (-Math.PI / 5);
         this.bipedLeftArm.rotateAngleX += (float) (-Math.PI / 5);
         this.bipedRightLeg.rotateAngleX = (float) (-Math.PI * 2.0 / 5.0);
         this.bipedLeftLeg.rotateAngleX = (float) (-Math.PI * 2.0 / 5.0);
         this.bipedRightLeg.rotateAngleY = (float) (Math.PI / 10);
         this.bipedLeftLeg.rotateAngleY = (float) (-Math.PI / 10);
      }

      this.bipedRightArm.rotateAngleY = 0.0F;
      this.bipedLeftArm.rotateAngleY = 0.0F;
      if (this.swingProgress > -9990.0F) {
         float f6 = this.swingProgress;
         this.bipedRightArm.rotateAngleY = this.bipedRightArm.rotateAngleY + this.bipedBody.rotateAngleY;
         this.bipedLeftArm.rotateAngleY = this.bipedLeftArm.rotateAngleY + this.bipedBody.rotateAngleY;
         this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX + this.bipedBody.rotateAngleY;
         f6 = 1.0F - this.swingProgress;
         f6 *= f6;
         f6 *= f6;
         f6 = 1.0F - f6;
         float f7 = MathHelper.sin(f6 * (float) Math.PI);
         float f8 = MathHelper.sin(this.swingProgress * (float) Math.PI) * -(this.bipedHead.rotateAngleX - 0.7F) * 0.75F;
         this.bipedRightArm.rotateAngleX = (float)(this.bipedRightArm.rotateAngleX - (f7 * 1.2 + f8));
         this.bipedRightArm.rotateAngleY = this.bipedRightArm.rotateAngleY + this.bipedBody.rotateAngleY * 2.0F;
         this.bipedRightArm.rotateAngleZ = MathHelper.sin(this.swingProgress * (float) Math.PI) * -0.4F;
      }

      if (entity.isSneaking()) {
         this.bipedRightArm.rotateAngleX += 0.4F;
         this.bipedLeftArm.rotateAngleX += 0.4F;
      }

      this.bipedRightArm.rotateAngleZ = this.bipedRightArm.rotateAngleZ + (MathHelper.cos(par3 * 0.09F) * 0.05F + 0.05F);
      this.bipedLeftArm.rotateAngleZ = this.bipedLeftArm.rotateAngleZ - (MathHelper.cos(par3 * 0.09F) * 0.05F + 0.05F);
      this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX + MathHelper.sin(par3 * 0.067F) * 0.05F;
      this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX - MathHelper.sin(par3 * 0.067F) * 0.05F;
   }
}
