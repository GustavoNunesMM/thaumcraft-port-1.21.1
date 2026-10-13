package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

public class ModelRendererTaintSeed extends ModelRenderer {
   private int textureOffsetX;
   private int textureOffsetY;
   private boolean compiled;
   private int displayList;
   private ModelBase baseModel;
   static int q = 0;

   public ModelRendererTaintSeed(ModelBase par1ModelBase) {
      super(par1ModelBase);
   }

   public ModelRendererTaintSeed(ModelBase par1ModelBase, int par2, int par3) {
      this(par1ModelBase);
      this.setTextureOffset(par2, par3);
   }

   @SideOnly(Side.CLIENT)
   public void render(float par1, float tt, float height) {
      float qq = (float)(Math.PI * (q / 7.0F));
      float scale = height - (float)Math.sin(qq);
      float pulse = (float)Math.sin(tt / 12.0F - qq) * 0.33F;
      pulse *= pulse;
      q++;
      if (q > 7) {
         q = 0;
      }

      if (!this.isHidden && this.showModel) {
         if (!this.compiled) {
            this.compileDisplayList(par1);
         }

         GL11.glTranslatef(this.offsetX, this.offsetY, this.offsetZ);
         if (this.rotateAngleX == 0.0F && this.rotateAngleY == 0.0F && this.rotateAngleZ == 0.0F) {
            if (this.rotationPointX == 0.0F && this.rotationPointY == 0.0F && this.rotationPointZ == 0.0F) {
               GL11.glCallList(this.displayList);
               if (this.childModels != null) {
                  for (int i = 0; i < this.childModels.size(); i++) {
                     GL11.glPushMatrix();
                     GL11.glScalef(scale + pulse, scale * 0.9F, scale + pulse);
                     ((ModelRendererTaintSeed)this.childModels.get(i)).render(par1, tt, height);
                     GL11.glPopMatrix();
                  }
               }
            } else {
               GL11.glTranslatef(this.rotationPointX * par1, this.rotationPointY * par1, this.rotationPointZ * par1);
               GL11.glCallList(this.displayList);
               if (this.childModels != null) {
                  for (int i = 0; i < this.childModels.size(); i++) {
                     GL11.glPushMatrix();
                     GL11.glScalef(scale + pulse, scale * 0.9F, scale + pulse);
                     ((ModelRendererTaintSeed)this.childModels.get(i)).render(par1, tt, height);
                     GL11.glPopMatrix();
                  }
               }

               GL11.glTranslatef(-this.rotationPointX * par1, -this.rotationPointY * par1, -this.rotationPointZ * par1);
            }
         } else {
            GL11.glPushMatrix();
            GL11.glTranslatef(this.rotationPointX * par1, this.rotationPointY * par1, this.rotationPointZ * par1);
            if (this.rotateAngleZ != 0.0F) {
               GL11.glRotatef(this.rotateAngleZ * (180.0F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
            }

            if (this.rotateAngleY != 0.0F) {
               GL11.glRotatef(this.rotateAngleY * (180.0F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
            }

            if (this.rotateAngleX != 0.0F) {
               GL11.glRotatef(this.rotateAngleX * (180.0F / (float)Math.PI), 1.0F, 0.0F, 0.0F);
            }

            GL11.glCallList(this.displayList);
            if (this.childModels != null) {
               for (int i = 0; i < this.childModels.size(); i++) {
                  GL11.glPushMatrix();
                  GL11.glScalef(scale + pulse, scale * 0.9F, scale + pulse);
                  ((ModelRendererTaintSeed)this.childModels.get(i)).render(par1, tt, height);
                  GL11.glPopMatrix();
               }
            }

            GL11.glPopMatrix();
         }

         GL11.glTranslatef(-this.offsetX, -this.offsetY, -this.offsetZ);
      }
   }

   @SideOnly(Side.CLIENT)
   private void compileDisplayList(float par1) {
      this.displayList = GLAllocation.generateDisplayLists(1);
      GL11.glNewList(this.displayList, 4864);
      Tessellator tessellator = Tessellator.getInstance();

      for (int i = 0; i < this.cubeList.size(); i++) {
         ((ModelBox)this.cubeList.get(i)).render(tessellator.getBuffer(), par1);
      }

      GL11.glEndList();
      this.compiled = true;
   }
}
