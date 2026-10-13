package thaumcraft.common.golems.parts;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.PartModel;

/** In combat the dart launchers point at the target. */
public class PartModelDarts extends PartModel {
   public PartModelDarts(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
      super(objModel, objTexture, attachPoint);
   }

   @Override
   public float preRenderArmRotationX(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
      return golem.isInCombat() ? 90.0F - golem.getGolemEntity().xRotO + inputRot / 10.0F : inputRot;
   }

   @Override
   public float preRenderArmRotationY(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
      return golem.isInCombat() ? inputRot / 10.0F : inputRot;
   }

   @Override
   public float preRenderArmRotationZ(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
      return golem.isInCombat() ? inputRot / 10.0F : inputRot;
   }
}
