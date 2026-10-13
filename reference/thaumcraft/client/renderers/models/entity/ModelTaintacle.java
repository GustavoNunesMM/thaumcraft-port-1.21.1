package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeed;
import thaumcraft.common.entities.monster.tainted.EntityTaintacle;

public class ModelTaintacle extends ModelBase {
   public ModelRenderer tentacle = new ModelRendererTaintacle(this);
   public ModelRenderer[] tents;
   public ModelRenderer orb = new ModelRendererTaintacle(this);
   private int length = 10;
   private boolean seed = false;

   public ModelTaintacle(int length, boolean seed) {
      this.seed = seed;
      int var3 = 0;
      this.length = length;
      this.textureHeight = 64;
      this.textureWidth = 64;
      this.tentacle = new ModelRendererTaintacle(this, 0, 0);
      this.tentacle.addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8);
      this.tentacle.rotationPointX = 0.0F;
      this.tentacle.rotationPointZ = 0.0F;
      this.tentacle.rotationPointY = 12.0F;
      this.tents = new ModelRendererTaintacle[length];

      for (int k = 0; k < length - 1; k++) {
         this.tents[k] = new ModelRendererTaintacle(this, 0, 16);
         this.tents[k].addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8);
         this.tents[k].rotationPointY = -8.0F;
         if (k == 0) {
            this.tentacle.addChild(this.tents[k]);
         } else {
            this.tents[k - 1].addChild(this.tents[k]);
         }
      }

      if (!seed) {
         this.orb = new ModelRendererTaintacle(this, 0, 56);
         this.orb.addBox(-2.0F, -2.0F, -2.0F, 4, 4, 4);
         this.orb.rotationPointY = -8.0F;
         this.tents[length - 2].addChild(this.orb);
         this.tents[length - 1] = new ModelRendererTaintacle(this, 0, 32);
         this.tents[length - 1].addBox(-6.0F, -6.0F, -6.0F, 12, 12, 12);
         this.tents[length - 1].rotationPointY = -8.0F;
         this.tents[length - 2].addChild(this.tents[length - 1]);
      }
   }

   public void setRotationAngles(float par1, float par2, float par3, float par4, float par5, float par6, Entity entity) {
      float flail = 0.0F;
      float ht = 0.0F;
      int at = 0;
      if (entity instanceof EntityTaintacle) {
         EntityTaintacle tentacle = (EntityTaintacle)entity;
         flail = tentacle.flailIntensity;
         ht = tentacle.hurtTime;
         float mod = par6 * 0.2F;
         float fs = flail > 1.0F ? 3.0F : 1.0F + (flail > 1.0F ? mod : -mod);
         float fi = flail + (!(ht > 0.0F) && at <= 0 ? -mod : mod);
         this.tentacle.rotateAngleX = 0.0F;

         for (int k = 0; k < this.length - 1; k++) {
            this.tents[k].rotateAngleX = 0.15F * fi * MathHelper.sin(par3 * 0.1F * fs - k / 2.0F);
            this.tents[k].rotateAngleZ = 0.1F / fi * MathHelper.sin(par3 * 0.15F - k / 2.0F);
         }
      }

      if (entity instanceof EntityTaintSeed) {
         EntityTaintSeed seed = (EntityTaintSeed)entity;
         ht = seed.hurtTime / 200.0F;
         flail = 0.1F;
         float mod = par6 * 0.2F;
         if (flail > 1.0F) {
            float var10000 = 3.0F;
         } else {
            float var25 = 1.0F + (flail > 1.0F ? mod : -mod);
         }

         float fi = flail + (!(ht > 0.0F) && at <= 0 ? -mod : mod);
         fi *= 3.0F;
         this.tentacle.rotateAngleX = 0.0F;

         for (int k = 0; k < this.length - 1; k++) {
            this.tents[k].rotateAngleX = 0.2F + 0.01F * k * k + ht + seed.attackAnim;
            this.tents[k].rotateAngleZ = 0.1F / fi * MathHelper.sin(par3 * 0.05F - k / 2.0F) / 5.0F;
         }
      }
   }

   public void render(Entity par1Entity, float par2, float par3, float par4, float par5, float par6, float par7) {
      this.setRotationAngles(par2, par3, par4, par5, par6, par7, par1Entity);
      GL11.glPushMatrix();
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      if (par1Entity instanceof EntityTaintSeed) {
         GL11.glTranslatef(0.0F, 1.0F, -0.2F);
         GL11.glScalef(par1Entity.width * 0.6F, par1Entity.height, par1Entity.width * 0.6F);
         ((ModelRendererTaintacle)this.tentacle).render(par7, this.seed ? 0.82F : 0.85F);
      } else {
         float height = 0.0F;
         float hc = par1Entity.height * 10.0F;
         if (par1Entity.ticksExisted < hc) {
            height = (hc - par1Entity.ticksExisted) / hc * par1Entity.height;
         }

         GL11.glTranslatef(0.0F, (par1Entity.height == 3.0F ? 0.6F : 1.2F) + height, 0.0F);
         GL11.glScalef(par1Entity.height / 3.0F, par1Entity.height / 3.0F, par1Entity.height / 3.0F);
         ((ModelRendererTaintacle)this.tentacle).render(par7, 0.88F);
      }

      GL11.glDisable(3042);
      GL11.glPopMatrix();
   }
}
