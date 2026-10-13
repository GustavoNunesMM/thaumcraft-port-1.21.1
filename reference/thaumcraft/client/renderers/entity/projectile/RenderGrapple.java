package thaumcraft.client.renderers.entity.projectile;

import com.sasmaster.glelwjgl.java.CoreGLE;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.client.renderers.models.entity.ModelGrappler;
import thaumcraft.common.entities.projectile.EntityGrapple;

public class RenderGrapple extends Render {
   ResourceLocation beam = new ResourceLocation("thaumcraft", "textures/entity/grappler.png");
   ResourceLocation rope = new ResourceLocation("thaumcraft", "textures/misc/rope.png");
   private ModelGrappler model;
   CoreGLE gle = new CoreGLE();
   public ArrayList<Vec3d> points = new ArrayList<>();
   public float length = 1.0F;

   public RenderGrapple(RenderManager rm) {
      super(rm);
      this.shadowSize = 0.0F;
      this.model = new ModelGrappler();
   }

   public void renderEntityAt(Entity entity, double x, double y, double z, float fq, float pticks) {
      Tessellator tessellator = Tessellator.getInstance();
      GL11.glPushMatrix();
      GL11.glTranslated(x, y, z);
      GL11.glPushMatrix();
      this.bindTexture(this.beam);
      GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * pticks - 90.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * pticks, 0.0F, 0.0F, 1.0F);
      this.model.render();
      GL11.glPopMatrix();
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 1);
      GL11.glDisable(2884);
      this.bindTexture(ParticleEngine.particleTexture);
      float f2 = (1 + entity.ticksExisted % 6) / 32.0F;
      float f3 = f2 + 0.03125F;
      float f4 = 0.21875F;
      float f5 = f4 + 0.03125F;
      float f7 = 0.5F;
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.8F);
      GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-this.renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
      float bob = MathHelper.sin(entity.ticksExisted / 5.0F) * 0.2F + 0.2F;
      GL11.glScalef(1.0F + bob, 1.0F + bob, 1.0F + bob);
      tessellator.getBuffer().begin(7, UtilsFX.VERTEXFORMAT_POS_TEX_CO_LM_NO);
      int i = 220;
      int j = i >> 16 & 65535;
      int k = i & 65535;
      tessellator.getBuffer()
         .pos(-f7, -f7, 0.0)
         .tex(f2, f5)
         .color(1.0F, 1.0F, 1.0F, 0.21F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.getBuffer()
         .pos(f7, -f7, 0.0)
         .tex(f3, f5)
         .color(1.0F, 1.0F, 1.0F, 0.21F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.getBuffer()
         .pos(f7, f7, 0.0)
         .tex(f3, f4)
         .color(1.0F, 1.0F, 1.0F, 0.21F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.getBuffer()
         .pos(-f7, f7, 0.0)
         .tex(f2, f4)
         .color(1.0F, 1.0F, 1.0F, 0.21F)
         .lightmap(j, k)
         .normal(0.0F, 1.0F, 0.0F)
         .endVertex();
      tessellator.draw();
      GL11.glEnable(2884);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3042);
      GL11.glDisable(32826);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      this.calcPoints(((EntityGrapple)entity).getThrower(), (EntityGrapple)entity, pticks);
      GL11.glPushMatrix();
      Minecraft.getMinecraft().renderEngine.bindTexture(this.beam);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      if (this.points != null && this.points.size() > 2) {
         double[][] pp = new double[this.points.size()][3];
         float[][] colours = new float[this.points.size()][4];
         double[] radii = new double[this.points.size()];

         for (int a = 0; a < this.points.size(); a++) {
            pp[a][0] = this.points.get(a).x + x;
            pp[a][1] = this.points.get(a).y + y;
            pp[a][2] = this.points.get(a).z + z;
            colours[a][0] = 1.0F;
            colours[a][1] = 1.0F;
            colours[a][2] = 1.0F;
            colours[a][3] = 1.0F;
            radii[a] = 0.025;
         }

         Minecraft.getMinecraft().renderEngine.bindTexture(this.rope);
         this.gle.set_POLYCYL_TESS(4);
         this.gle.gleSetJoinStyle(1042);
         this.gle.glePolyCone(pp.length, pp, colours, radii, 2.0F, this.length - this.points.size());
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3042);
      GL11.glPopMatrix();
   }

   private void calcPoints(EntityLivingBase thrower, EntityGrapple grapple, float pt) {
      if (thrower != null && grapple != null) {
         double tx = thrower.lastTickPosX + (thrower.posX - thrower.lastTickPosX) * pt;
         double ty = thrower.lastTickPosY + (thrower.posY - thrower.lastTickPosY) * pt;
         double tz = thrower.lastTickPosZ + (thrower.posZ - thrower.lastTickPosZ) * pt;
         if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
            double yy = thrower.prevRotationYaw + (double)(thrower.rotationYaw - thrower.prevRotationYaw) * pt;
            double px = -MathHelper.cos((float)((yy - 0.5) / 180.0 * 3.141593F)) * 0.1F * (grapple.hand == EnumHand.MAIN_HAND ? 1 : -1);
            double pz = -MathHelper.sin((float)((yy - 0.5) / 180.0 * 3.141593F)) * 0.1F * (grapple.hand == EnumHand.MAIN_HAND ? 1 : -1);
            Vec3d vl = thrower.getLookVec();
            tx += px + vl.x / 5.0;
            ty += thrower.getEyeHeight() / 2.6 + vl.y / 5.0;
            tz += pz + vl.z / 5.0;
         }

         double gx = grapple.lastTickPosX + (grapple.posX - grapple.lastTickPosX) * pt;
         double gy = grapple.lastTickPosY + (grapple.posY - grapple.lastTickPosY) * pt;
         double gz = grapple.lastTickPosZ + (grapple.posZ - grapple.lastTickPosZ) * pt;
         this.points.clear();
         Vec3d vs = new Vec3d(0.0, 0.0, 0.0);
         Vec3d ve = new Vec3d(tx - gx, ty - gy + thrower.height / 2.0F, tz - gz);
         this.length = (float)(ve.length() * 5.0);
         int steps = (int)this.length;
         this.points.add(vs);

         for (int a = 1; a < steps - 1; a++) {
            float dist = a * (this.length / steps);
            float ss = 1.0F - a / (steps * 1.25F);
            double dx = (tx - gx) / steps * a + MathHelper.sin(dist / 10.0F) * grapple.ampl * ss;
            double dy = (ty - gy + thrower.height / 2.0F) / steps * a + MathHelper.sin(dist / 8.0F) * grapple.ampl * ss;
            double dz = (tz - gz) / steps * a + MathHelper.sin(dist / 6.0F) * grapple.ampl * ss;
            Vec3d vp = new Vec3d(dx, dy, dz);
            this.points.add(vp);
         }

         this.points.add(ve);
         this.points.add(ve);
      }
   }

   public void doRender(Entity entity, double d, double d1, double d2, float f, float f1) {
      this.renderEntityAt(entity, d, d1, d2, f, f1);
   }

   protected ResourceLocation getEntityTexture(Entity entity) {
      return TextureMap.LOCATION_BLOCKS_TEXTURE;
   }
}
