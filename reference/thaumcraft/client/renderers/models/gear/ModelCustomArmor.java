package thaumcraft.client.renderers.models.gear;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelBiped.ArmPose;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import thaumcraft.client.lib.UtilsFX;

public class ModelCustomArmor extends ModelBiped {
   public ModelCustomArmor(float f, int i, int j, int k) {
      super(f, i, j, k);
   }

   public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
      if (entityIn instanceof EntityLivingBase) {
         this.swingProgress = ((EntityLivingBase)entityIn).getSwingProgress(UtilsFX.sysPartialTicks);
      }

      if (entityIn instanceof EntityArmorStand) {
         this.setRotationAnglesStand(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
      } else if (!(entityIn instanceof EntitySkeleton) && !(entityIn instanceof EntityZombie)) {
         boolean flag = entityIn instanceof EntityLivingBase && ((EntityLivingBase)entityIn).getTicksElytraFlying() > 4;
         this.bipedHead.rotateAngleY = netHeadYaw * (float) (Math.PI / 180.0);
         if (flag) {
            this.bipedHead.rotateAngleX = (float) (-Math.PI / 4);
         } else {
            this.bipedHead.rotateAngleX = headPitch * (float) (Math.PI / 180.0);
         }

         this.bipedBody.rotateAngleY = 0.0F;
         this.bipedRightArm.rotationPointZ = 0.0F;
         this.bipedRightArm.rotationPointX = -5.0F;
         this.bipedLeftArm.rotationPointZ = 0.0F;
         this.bipedLeftArm.rotationPointX = 5.0F;
         float f = 1.0F;
         if (flag) {
            f = (float)(
               entityIn.motionX * entityIn.motionX
                  + entityIn.motionY * entityIn.motionY
                  + entityIn.motionZ * entityIn.motionZ
            );
            f /= 0.2F;
            f = f * f * f;
         }

         if (f < 1.0F) {
            f = 1.0F;
         }

         this.bipedRightArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F / f;
         this.bipedLeftArm.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F / f;
         this.bipedRightArm.rotateAngleZ = 0.0F;
         this.bipedLeftArm.rotateAngleZ = 0.0F;
         this.bipedRightLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount / f;
         this.bipedLeftLeg.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount / f;
         this.bipedRightLeg.rotateAngleY = 0.0F;
         this.bipedLeftLeg.rotateAngleY = 0.0F;
         this.bipedRightLeg.rotateAngleZ = 0.0F;
         this.bipedLeftLeg.rotateAngleZ = 0.0F;
         if (this.isRiding) {
            this.bipedRightArm.rotateAngleX += (float) (-Math.PI / 5);
            this.bipedLeftArm.rotateAngleX += (float) (-Math.PI / 5);
            this.bipedRightLeg.rotateAngleX = -1.4137167F;
            this.bipedRightLeg.rotateAngleY = (float) (Math.PI / 10);
            this.bipedRightLeg.rotateAngleZ = 0.07853982F;
            this.bipedLeftLeg.rotateAngleX = -1.4137167F;
            this.bipedLeftLeg.rotateAngleY = (float) (-Math.PI / 10);
            this.bipedLeftLeg.rotateAngleZ = -0.07853982F;
         }

         this.bipedRightArm.rotateAngleY = 0.0F;
         this.bipedRightArm.rotateAngleZ = 0.0F;
         switch (this.leftArmPose) {
            case EMPTY:
               this.bipedLeftArm.rotateAngleY = 0.0F;
               break;
            case BLOCK:
               this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX * 0.5F - 0.9424779F;
               this.bipedLeftArm.rotateAngleY = (float) (Math.PI / 6);
               break;
            case ITEM:
               this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX * 0.5F - (float) (Math.PI / 10);
               this.bipedLeftArm.rotateAngleY = 0.0F;
         }

         switch (this.rightArmPose) {
            case EMPTY:
               this.bipedRightArm.rotateAngleY = 0.0F;
               break;
            case BLOCK:
               this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX * 0.5F - 0.9424779F;
               this.bipedRightArm.rotateAngleY = (float) (-Math.PI / 6);
               break;
            case ITEM:
               this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX * 0.5F - (float) (Math.PI / 10);
               this.bipedRightArm.rotateAngleY = 0.0F;
         }

         if (this.swingProgress > 0.0F) {
            EnumHandSide enumhandside = this.getMainHand(entityIn);
            ModelRenderer modelrenderer = this.getArmForSide(enumhandside);
            float f1 = this.swingProgress;
            this.bipedBody.rotateAngleY = MathHelper.sin(MathHelper.sqrt(f1) * (float) (Math.PI * 2)) * 0.2F;
            if (enumhandside == EnumHandSide.LEFT) {
               this.bipedBody.rotateAngleY *= -1.0F;
            }

            this.bipedRightArm.rotationPointZ = MathHelper.sin(this.bipedBody.rotateAngleY) * 5.0F;
            this.bipedRightArm.rotationPointX = -MathHelper.cos(this.bipedBody.rotateAngleY) * 5.0F;
            this.bipedLeftArm.rotationPointZ = -MathHelper.sin(this.bipedBody.rotateAngleY) * 5.0F;
            this.bipedLeftArm.rotationPointX = MathHelper.cos(this.bipedBody.rotateAngleY) * 5.0F;
            this.bipedRightArm.rotateAngleY = this.bipedRightArm.rotateAngleY + this.bipedBody.rotateAngleY;
            this.bipedLeftArm.rotateAngleY = this.bipedLeftArm.rotateAngleY + this.bipedBody.rotateAngleY;
            this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX + this.bipedBody.rotateAngleY;
            f1 = 1.0F - this.swingProgress;
            f1 *= f1;
            f1 *= f1;
            f1 = 1.0F - f1;
            float f2 = MathHelper.sin(f1 * (float) Math.PI);
            float f3 = MathHelper.sin(this.swingProgress * (float) Math.PI) * -(this.bipedHead.rotateAngleX - 0.7F) * 0.75F;
            modelrenderer.rotateAngleX = (float)(modelrenderer.rotateAngleX - (f2 * 1.2 + f3));
            modelrenderer.rotateAngleY = modelrenderer.rotateAngleY + this.bipedBody.rotateAngleY * 2.0F;
            modelrenderer.rotateAngleZ = modelrenderer.rotateAngleZ + MathHelper.sin(this.swingProgress * (float) Math.PI) * -0.4F;
         }

         if (this.isSneak) {
            this.bipedBody.rotateAngleX = 0.5F;
            this.bipedRightArm.rotateAngleX += 0.4F;
            this.bipedLeftArm.rotateAngleX += 0.4F;
            this.bipedRightLeg.rotationPointZ = 4.0F;
            this.bipedLeftLeg.rotationPointZ = 4.0F;
            this.bipedRightLeg.rotationPointY = 13.0F;
            this.bipedLeftLeg.rotationPointY = 13.0F;
            this.bipedHead.rotationPointY = 4.5F;
            this.bipedBody.rotationPointY = 4.5F;
            this.bipedRightArm.rotationPointY = 5.0F;
            this.bipedLeftArm.rotationPointY = 5.0F;
         } else {
            this.bipedBody.rotateAngleX = 0.0F;
            this.bipedRightLeg.rotationPointZ = 0.1F;
            this.bipedLeftLeg.rotationPointZ = 0.1F;
            this.bipedRightLeg.rotationPointY = 12.0F;
            this.bipedLeftLeg.rotationPointY = 12.0F;
            this.bipedHead.rotationPointY = 0.0F;
            this.bipedBody.rotationPointY = 0.0F;
            this.bipedRightArm.rotationPointY = 2.0F;
            this.bipedLeftArm.rotationPointY = 2.0F;
         }

         this.bipedRightArm.rotateAngleZ = this.bipedRightArm.rotateAngleZ + (MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F);
         this.bipedLeftArm.rotateAngleZ = this.bipedLeftArm.rotateAngleZ - (MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F);
         this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX + MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
         this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX - MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
         if (this.rightArmPose == ArmPose.BOW_AND_ARROW) {
            this.bipedRightArm.rotateAngleY = -0.1F + this.bipedHead.rotateAngleY;
            this.bipedLeftArm.rotateAngleY = 0.1F + this.bipedHead.rotateAngleY + 0.4F;
            this.bipedRightArm.rotateAngleX = (float) (-Math.PI / 2) + this.bipedHead.rotateAngleX;
            this.bipedLeftArm.rotateAngleX = (float) (-Math.PI / 2) + this.bipedHead.rotateAngleX;
         } else if (this.leftArmPose == ArmPose.BOW_AND_ARROW) {
            this.bipedRightArm.rotateAngleY = -0.1F + this.bipedHead.rotateAngleY - 0.4F;
            this.bipedLeftArm.rotateAngleY = 0.1F + this.bipedHead.rotateAngleY;
            this.bipedRightArm.rotateAngleX = (float) (-Math.PI / 2) + this.bipedHead.rotateAngleX;
            this.bipedLeftArm.rotateAngleX = (float) (-Math.PI / 2) + this.bipedHead.rotateAngleX;
         }

         copyModelAngles(this.bipedHead, this.bipedHeadwear);
      } else {
         this.setRotationAnglesZombie(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
      }
   }

   public void setRotationAnglesZombie(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn
   ) {
      super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entityIn);
      boolean flag = entityIn instanceof EntityZombie && ((EntityZombie)entityIn).isArmsRaised();
      float f = MathHelper.sin(this.swingProgress * (float) Math.PI);
      float f1 = MathHelper.sin((1.0F - (1.0F - this.swingProgress) * (1.0F - this.swingProgress)) * (float) Math.PI);
      this.bipedRightArm.rotateAngleZ = 0.0F;
      this.bipedLeftArm.rotateAngleZ = 0.0F;
      this.bipedRightArm.rotateAngleY = -(0.1F - f * 0.6F);
      this.bipedLeftArm.rotateAngleY = 0.1F - f * 0.6F;
      float f2 = (float) -Math.PI / (flag ? 1.5F : 2.25F);
      this.bipedRightArm.rotateAngleX = f2;
      this.bipedLeftArm.rotateAngleX = f2;
      this.bipedRightArm.rotateAngleX += f * 1.2F - f1 * 0.4F;
      this.bipedLeftArm.rotateAngleX += f * 1.2F - f1 * 0.4F;
      this.bipedRightArm.rotateAngleZ = this.bipedRightArm.rotateAngleZ + (MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F);
      this.bipedLeftArm.rotateAngleZ = this.bipedLeftArm.rotateAngleZ - (MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F);
      this.bipedRightArm.rotateAngleX = this.bipedRightArm.rotateAngleX + MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
      this.bipedLeftArm.rotateAngleX = this.bipedLeftArm.rotateAngleX - MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
   }

   public void setRotationAnglesStand(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn
   ) {
      if (entityIn instanceof EntityArmorStand) {
         EntityArmorStand entityarmorstand = (EntityArmorStand)entityIn;
         this.bipedHead.rotateAngleX = (float) (Math.PI / 180.0) * entityarmorstand.getHeadRotation().getX();
         this.bipedHead.rotateAngleY = (float) (Math.PI / 180.0) * entityarmorstand.getHeadRotation().getY();
         this.bipedHead.rotateAngleZ = (float) (Math.PI / 180.0) * entityarmorstand.getHeadRotation().getZ();
         this.bipedHead.setRotationPoint(0.0F, 1.0F, 0.0F);
         this.bipedBody.rotateAngleX = (float) (Math.PI / 180.0) * entityarmorstand.getBodyRotation().getX();
         this.bipedBody.rotateAngleY = (float) (Math.PI / 180.0) * entityarmorstand.getBodyRotation().getY();
         this.bipedBody.rotateAngleZ = (float) (Math.PI / 180.0) * entityarmorstand.getBodyRotation().getZ();
         this.bipedLeftArm.rotateAngleX = (float) (Math.PI / 180.0) * entityarmorstand.getLeftArmRotation().getX();
         this.bipedLeftArm.rotateAngleY = (float) (Math.PI / 180.0) * entityarmorstand.getLeftArmRotation().getY();
         this.bipedLeftArm.rotateAngleZ = (float) (Math.PI / 180.0) * entityarmorstand.getLeftArmRotation().getZ();
         this.bipedRightArm.rotateAngleX = (float) (Math.PI / 180.0) * entityarmorstand.getRightArmRotation().getX();
         this.bipedRightArm.rotateAngleY = (float) (Math.PI / 180.0) * entityarmorstand.getRightArmRotation().getY();
         this.bipedRightArm.rotateAngleZ = (float) (Math.PI / 180.0) * entityarmorstand.getRightArmRotation().getZ();
         this.bipedLeftLeg.rotateAngleX = (float) (Math.PI / 180.0) * entityarmorstand.getLeftLegRotation().getX();
         this.bipedLeftLeg.rotateAngleY = (float) (Math.PI / 180.0) * entityarmorstand.getLeftLegRotation().getY();
         this.bipedLeftLeg.rotateAngleZ = (float) (Math.PI / 180.0) * entityarmorstand.getLeftLegRotation().getZ();
         this.bipedLeftLeg.setRotationPoint(1.9F, 11.0F, 0.0F);
         this.bipedRightLeg.rotateAngleX = (float) (Math.PI / 180.0) * entityarmorstand.getRightLegRotation().getX();
         this.bipedRightLeg.rotateAngleY = (float) (Math.PI / 180.0) * entityarmorstand.getRightLegRotation().getY();
         this.bipedRightLeg.rotateAngleZ = (float) (Math.PI / 180.0) * entityarmorstand.getRightLegRotation().getZ();
         this.bipedRightLeg.setRotationPoint(-1.9F, 11.0F, 0.0F);
         copyModelAngles(this.bipedHead, this.bipedHeadwear);
      }
   }
}
