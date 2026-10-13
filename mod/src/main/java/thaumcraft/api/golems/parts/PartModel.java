package thaumcraft.api.golems.parts;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;

/**
 * The OBJ model of a golem part and where it attaches. Object groups whose name starts with "bm" use the golem's
 * material texture. The render hooks receive a {@link Transform} (the client renderer wraps its pose stack), so this
 * class stays loadable on a dedicated server.
 */
public class PartModel {
   private final ResourceLocation objModel;
   @Nullable
   private final ResourceLocation texture;
   private final EnumAttachPoint attachPoint;

   public PartModel(ResourceLocation objModel, @Nullable ResourceLocation objTexture, EnumAttachPoint attachPoint) {
      this.objModel = objModel;
      this.texture = objTexture;
      this.attachPoint = attachPoint;
   }

   public ResourceLocation getObjModel() {
      return objModel;
   }

   @Nullable
   public ResourceLocation getTexture() {
      return texture;
   }

   public EnumAttachPoint getAttachPoint() {
      return attachPoint;
   }

   public boolean useMaterialTextureForObjectPart(String partName) {
      return partName.startsWith("bm");
   }

   public void preRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side, Transform t) {
   }

   public void postRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side, Transform t) {
   }

   public float preRenderArmRotationX(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
      return inputRot;
   }

   public float preRenderArmRotationY(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
      return inputRot;
   }

   public float preRenderArmRotationZ(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
      return inputRot;
   }

   /** GlStateManager.translate/rotate on the current render matrix. */
   public interface Transform {
      void translate(double x, double y, double z);

      /** Degrees around the (x, y, z) axis, as glRotatef. */
      void rotate(float degrees, float x, float y, float z);
   }

   public enum EnumAttachPoint {
      ARMS, LEGS, BODY, HEAD
   }

   public enum EnumLimbSide {
      LEFT, RIGHT, MIDDLE
   }
}
