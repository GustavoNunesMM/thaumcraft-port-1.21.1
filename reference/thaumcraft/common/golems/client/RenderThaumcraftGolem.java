package thaumcraft.common.golems.client;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import net.minecraftforge.client.event.RenderLivingEvent.Pre;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.ISealDisplayer;
import thaumcraft.api.golems.parts.PartModel;
import thaumcraft.client.lib.obj.AdvancedModelLoader;
import thaumcraft.client.lib.obj.IModelCustom;
import thaumcraft.common.golems.EntityThaumcraftGolem;
import thaumcraft.common.lib.utils.EntityUtils;

@SideOnly(Side.CLIENT)
public class RenderThaumcraftGolem extends RenderBiped {
   private static final Logger logger = LogManager.getLogger();
   private HashMap<String, IModelCustom> models = new HashMap<>();
   private HashMap<Integer, HashMap<PartModel.EnumAttachPoint, ArrayList<PartModel>>> partsCache = new HashMap<>();
   private IModelCustom baseModel;
   float swingProgress = 0.0F;

   public RenderThaumcraftGolem(RenderManager p_i46127_1_) {
      super(p_i46127_1_, new ModelBiped(), 0.3F);
      this.layerRenderers.clear();
      this.baseModel = AdvancedModelLoader.loadModel(new ResourceLocation("thaumcraft", "models/obj/golem_base.obj"));
   }

   private void renderModel(EntityThaumcraftGolem entity, float p1, float p2, float p3, float p4, float p5, float p6, float partialTicks) {
      boolean flag = !entity.isInvisible();
      boolean flag1 = !flag && !entity.isInvisibleToPlayer(Minecraft.getMinecraft().player);
      if (flag || flag1) {
         if (flag1) {
            GlStateManager.pushMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.15F);
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.alphaFunc(516, 0.003921569F);
         }

         this.renderParts(entity, p1, p2, p3, p4, p5, p6, partialTicks);
         if (flag1) {
            GlStateManager.disableBlend();
            GlStateManager.alphaFunc(516, 0.1F);
            GlStateManager.popMatrix();
            GlStateManager.depthMask(true);
         }
      }

      EntityPlayer player = Minecraft.getMinecraft().player;
      if (player.isSneaking()
         && (
            player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem() instanceof ISealDisplayer
               || player.getHeldItemOffhand() != null && player.getHeldItemOffhand().getItem() instanceof ISealDisplayer
         )
         && !EntityUtils.canEntityBeSeen(player, entity)) {
         GlStateManager.pushMatrix();
         GlStateManager.color(0.25F, 0.25F, 0.25F, 0.25F);
         GlStateManager.depthMask(false);
         GL11.glDisable(2929);
         GlStateManager.enableBlend();
         GlStateManager.blendFunc(770, 771);
         GlStateManager.alphaFunc(516, 0.003921569F);
         this.renderParts(entity, p1, p2, p3, p4, p5, p6, partialTicks);
         GlStateManager.disableBlend();
         GlStateManager.alphaFunc(516, 0.1F);
         GlStateManager.popMatrix();
         GL11.glEnable(2929);
         GlStateManager.depthMask(true);
      }
   }

   private void renderParts(
      EntityThaumcraftGolem entity, float limbSwing, float prevLimbSwing, float rotFloat, float headPitch, float headYaw, float scaleFactor, float partialTicks
   ) {
      ResourceLocation matTexture = entity.getProperties().getMaterial().texture;
      boolean holding = !entity.getHeldItemMainhand().isEmpty();
      boolean aflag = entity.getProperties().hasTrait(EnumGolemTrait.WHEELED) || entity.getProperties().hasTrait(EnumGolemTrait.FLYER);
      Vec3d v1 = new Vec3d(entity.posX, 0.0, entity.posZ);
      Vec3d v2 = new Vec3d(entity.prevPosX, 0.0, entity.prevPosZ);
      double speed = v1.squareDistanceTo(v2);
      if (entity.redrawParts || !this.partsCache.containsKey(entity.getEntityId())) {
         entity.redrawParts = false;
         this.createPartsCache(entity);
      }

      float f1 = 0.0F;
      float bry = 0.0F;
      float rx = (float)Math.toDegrees(MathHelper.sin(rotFloat * 0.067F) * 0.03F);
      float rz = (float)Math.toDegrees(MathHelper.cos(rotFloat * 0.09F) * 0.05F + 0.05F);
      float rrx = 0.0F;
      float rry = 0.0F;
      float rrz = 0.0F;
      float rlx = 0.0F;
      float rly = 0.0F;
      float rlz = 0.0F;
      if (holding) {
         rrx = 90.0F - rz / 2.0F;
         rrz = -2.0F;
         rlx = 90.0F - rz / 2.0F;
         rlz = 2.0F;
      } else {
         if (aflag) {
            rrx = rx * 2.0F;
            rlx = -rx * 2.0F;
         } else {
            f1 = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * prevLimbSwing * 0.5F;
            rrx = (float)(Math.toDegrees(f1) + rx);
            f1 = MathHelper.cos(limbSwing * 0.6662F) * 2.0F * prevLimbSwing * 0.5F;
            rlx = (float)(Math.toDegrees(f1) - rx);
         }

         rrz += rz + 2.0F;
         rlz -= rz + 2.0F;
      }

      if (this.swingProgress > 0.0F) {
         float wiggle = -MathHelper.sin(MathHelper.sqrt(this.swingProgress) * (float) Math.PI * 2.0F) * 0.2F;
         bry = (float)Math.toDegrees(wiggle);
         rrz = -((float)Math.toDegrees(MathHelper.sin(wiggle) * 3.0F));
         rrx = (float)Math.toDegrees(-MathHelper.cos(wiggle) * 5.0F);
         rry += bry;
      }

      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GlStateManager.rotate(bry, 0.0F, 1.0F, 0.0F);
      float lean = 25.0F;
      if (aflag) {
         lean = 75.0F;
      }

      GlStateManager.rotate((float)(speed * lean), -1.0F, 0.0F, 0.0F);
      GlStateManager.rotate((float)(speed * lean * 0.06 * (entity.rotationYaw - entity.prevRotationYaw)), 0.0F, 0.0F, -1.0F);
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0, 0.5, 0.0);
      this.bindTexture(matTexture);
      this.baseModel.renderPart("chest");
      this.baseModel.renderPart("waist");
      if (entity.getGolemColor() > 0) {
         Color c = new Color(EnumDyeColor.byMetadata(entity.getGolemColor() - 1).getColorValue());
         GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F);
         this.baseModel.renderPart("flag");
         GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      }

      for (PartModel part : this.partsCache.get(entity.getEntityId()).get(PartModel.EnumAttachPoint.BODY)) {
         this.renderPart(entity, part.getObjModel().toString(), part, matTexture, partialTicks, PartModel.EnumLimbSide.MIDDLE);
      }

      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0, 0.75, -0.03125);
      GlStateManager.rotate(headPitch, 0.0F, -1.0F, 0.0F);
      GlStateManager.rotate(headYaw, -1.0F, 0.0F, 0.0F);

      for (PartModel part : this.partsCache.get(entity.getEntityId()).get(PartModel.EnumAttachPoint.HEAD)) {
         this.renderPart(entity, part.getObjModel().toString(), part, matTexture, partialTicks, PartModel.EnumLimbSide.MIDDLE);
      }

      this.bindTexture(matTexture);
      this.baseModel.renderPart("head");
      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.20625, 0.6875, 0.0);
      Iterator var40 = this.partsCache.get(entity.getEntityId()).get(PartModel.EnumAttachPoint.ARMS).iterator();
      if (var40.hasNext()) {
         PartModel part = (PartModel)var40.next();
         rrx = part.preRenderArmRotationX(entity, partialTicks, PartModel.EnumLimbSide.RIGHT, rrx);
         rry = part.preRenderArmRotationY(entity, partialTicks, PartModel.EnumLimbSide.RIGHT, rry);
         rrz = part.preRenderArmRotationZ(entity, partialTicks, PartModel.EnumLimbSide.RIGHT, rrz);
      }

      GlStateManager.rotate(rrx, 1.0F, 0.0F, 0.0F);
      GlStateManager.rotate(rry, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(rrz, 0.0F, 0.0F, 1.0F);
      this.bindTexture(matTexture);
      this.baseModel.renderPart("arm");

      for (PartModel part : this.partsCache.get(entity.getEntityId()).get(PartModel.EnumAttachPoint.ARMS)) {
         this.renderPart(entity, part.getObjModel().toString(), part, matTexture, partialTicks, PartModel.EnumLimbSide.RIGHT);
      }

      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.translate(-0.20625, 0.6875, 0.0);
      var40 = this.partsCache.get(entity.getEntityId()).get(PartModel.EnumAttachPoint.ARMS).iterator();
      if (var40.hasNext()) {
         PartModel part = (PartModel)var40.next();
         rlx = part.preRenderArmRotationX(entity, partialTicks, PartModel.EnumLimbSide.LEFT, rlx);
         rly = part.preRenderArmRotationY(entity, partialTicks, PartModel.EnumLimbSide.LEFT, rly);
         rlz = part.preRenderArmRotationZ(entity, partialTicks, PartModel.EnumLimbSide.LEFT, rlz);
      }

      GlStateManager.rotate(rlx, 1.0F, 0.0F, 0.0F);
      GlStateManager.rotate(rly + 180.0F, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate(rlz, 0.0F, 0.0F, -1.0F);
      this.bindTexture(matTexture);
      this.baseModel.renderPart("arm");

      for (PartModel part : this.partsCache.get(entity.getEntityId()).get(PartModel.EnumAttachPoint.ARMS)) {
         this.renderPart(entity, part.getObjModel().toString(), part, matTexture, partialTicks, PartModel.EnumLimbSide.LEFT);
      }

      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.09375, 0.375, 0.0);
      f1 = MathHelper.cos(limbSwing * 0.6662F) * prevLimbSwing;
      GlStateManager.rotate((float)Math.toDegrees(f1), 1.0F, 0.0F, 0.0F);

      for (PartModel part : this.partsCache.get(entity.getEntityId()).get(PartModel.EnumAttachPoint.LEGS)) {
         this.renderPart(entity, part.getObjModel().toString(), part, matTexture, partialTicks, PartModel.EnumLimbSide.RIGHT);
      }

      GlStateManager.popMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.translate(-0.09375, 0.375, 0.0);
      f1 = MathHelper.cos(limbSwing * 0.6662F + (float) Math.PI) * prevLimbSwing;
      GlStateManager.rotate((float)Math.toDegrees(f1), 1.0F, 0.0F, 0.0F);

      for (PartModel part : this.partsCache.get(entity.getEntityId()).get(PartModel.EnumAttachPoint.LEGS)) {
         this.renderPart(entity, part.getObjModel().toString(), part, matTexture, partialTicks, PartModel.EnumLimbSide.LEFT);
      }

      GlStateManager.popMatrix();
      GL11.glDisable(3042);
      GlStateManager.pushMatrix();
      GlStateManager.translate(0.0, 0.625, 0.0);
      GlStateManager.rotate(90.0F - rz * 0.5F, 1.0F, 0.0F, 0.0F);
      this.drawHeldItem(entity);
      GlStateManager.popMatrix();
   }

   private void drawHeldItem(EntityThaumcraftGolem entity) {
      ItemStack itemstack = entity.getHeldItemMainhand();
      if (itemstack != null && !itemstack.isEmpty()) {
         GlStateManager.pushMatrix();
         Item item = itemstack.getItem();
         Minecraft minecraft = Minecraft.getMinecraft();
         GlStateManager.rotate(90.0F, -1.0F, 0.0F, 0.0F);
         GlStateManager.scale(0.375, 0.375, 0.375);
         GlStateManager.translate(0.0F, 0.25F, -1.5F);
         if (!(item instanceof ItemBlock)) {
            GlStateManager.translate(0.0F, -0.6F, 0.0F);
         }

         minecraft.getItemRenderer().renderItem(entity, itemstack, TransformType.HEAD);
         GlStateManager.popMatrix();
      }
   }

   private void renderPart(
      EntityThaumcraftGolem golem, String partName, PartModel part, ResourceLocation matTexture, float partialTicks, PartModel.EnumLimbSide side
   ) {
      IModelCustom model = this.models.get(partName);
      if (model == null) {
         model = AdvancedModelLoader.loadModel(part.getObjModel());
         if (model == null) {
            return;
         }

         this.models.put(partName, model);
      }

      for (String op : model.getPartNames()) {
         GL11.glPushMatrix();
         if (part.useMaterialTextureForObjectPart(op)) {
            this.bindTexture(matTexture);
         } else {
            this.bindTexture(part.getTexture());
         }

         part.preRenderObjectPart(op, golem, partialTicks, side);
         model.renderPart(op);
         part.postRenderObjectPart(op, golem, partialTicks, side);
         GL11.glPopMatrix();
      }
   }

   private void doRender(EntityThaumcraftGolem entity, double x, double y, double z, float entityYaw, float partialTicks) {
      if (!MinecraftForge.EVENT_BUS.post(new Pre(entity, this, x, y, z))) {
         GlStateManager.pushMatrix();
         GlStateManager.disableCull();
         this.swingProgress = this.getSwingProgress(entity, partialTicks);

         try {
            float f2 = this.interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
            float f3 = this.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
            float f4 = f3 - f2;
            if (entity.isRiding() && entity.getRidingEntity() instanceof EntityLivingBase) {
               EntityLivingBase entitylivingbase1 = (EntityLivingBase)entity.getRidingEntity();
               f2 = this.interpolateRotation(entitylivingbase1.prevRenderYawOffset, entitylivingbase1.renderYawOffset, partialTicks);
               f4 = f3 - f2;
               float f5 = MathHelper.wrapDegrees(f4);
               if (f5 < -85.0F) {
                  f5 = -85.0F;
               }

               if (f5 >= 85.0F) {
                  f5 = 85.0F;
               }

               f2 = f3 - f5;
               if (f5 * f5 > 2500.0F) {
                  f2 += f5 * 0.2F;
               }
            }

            float f9 = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
            this.renderLivingAt(entity, x, y, z);
            float f5x = this.handleRotationFloat(entity, partialTicks);
            this.applyRotations(entity, f5x, f2, partialTicks);
            GlStateManager.enableRescaleNormal();
            this.preRenderCallback(entity, partialTicks);
            float f7 = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
            float f8 = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
            if (f7 > 1.0F) {
               f7 = 1.0F;
            }

            GlStateManager.enableAlpha();
            if (this.renderOutlines) {
               boolean flag = this.setScoreTeamColor(entity);
               this.renderModel(entity, f8, f7, f5x, f4, f9, 0.0625F, partialTicks);
               if (flag) {
                  this.unsetScoreTeamColor();
               }
            } else {
               boolean flag = this.setDoRenderBrightness(entity, partialTicks);
               this.renderModel(entity, f8, f7, f5x, f4, f9, 0.0625F, partialTicks);
               if (flag) {
                  this.unsetBrightness();
               }

               GlStateManager.depthMask(true);
               this.renderLayers(entity, f8, f7, partialTicks, f5x, f4, f9, 0.0625F);
            }

            GlStateManager.disableRescaleNormal();
         } catch (Exception var18) {
            logger.error("Couldn't render entity", var18);
         }

         GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
         GlStateManager.enableTexture2D();
         GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
         GlStateManager.enableCull();
         GlStateManager.popMatrix();
         if (!this.renderOutlines) {
            this.renderName(entity, x, y, z);
         }

         MinecraftForge.EVENT_BUS.post(new Post(entity, this, x, y, z));
         this.renderLeash(entity, x, y, z, entityYaw, partialTicks);
      }
   }

   private void createPartsCache(EntityThaumcraftGolem golem) {
      HashMap<PartModel.EnumAttachPoint, ArrayList<PartModel>> pl = new HashMap<>();
      pl.put(PartModel.EnumAttachPoint.BODY, new ArrayList<>());
      pl.put(PartModel.EnumAttachPoint.HEAD, new ArrayList<>());
      pl.put(PartModel.EnumAttachPoint.ARMS, new ArrayList<>());
      pl.put(PartModel.EnumAttachPoint.LEGS, new ArrayList<>());
      IGolemProperties props = golem.getProperties();
      if (props.getHead().model != null) {
         pl.get(props.getHead().model.getAttachPoint()).add(props.getHead().model);
      }

      if (props.getArms().model != null) {
         pl.get(props.getArms().model.getAttachPoint()).add(props.getArms().model);
      }

      if (props.getLegs().model != null) {
         pl.get(props.getLegs().model.getAttachPoint()).add(props.getLegs().model);
      }

      if (props.getAddon().model != null) {
         pl.get(props.getAddon().model.getAttachPoint()).add(props.getAddon().model);
      }

      this.partsCache.put(golem.getEntityId(), pl);
   }

   public void doRender(EntityLiving entity, double x, double y, double z, float entityYaw, float partialTicks) {
      this.doRender((EntityThaumcraftGolem)entity, x, y, z, entityYaw, partialTicks);
   }

   protected ResourceLocation getEntityTexture(EntityLiving entity) {
      return null;
   }
}
