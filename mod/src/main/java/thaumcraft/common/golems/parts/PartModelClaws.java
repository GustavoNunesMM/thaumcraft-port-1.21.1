package thaumcraft.common.golems.parts;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.PartModel;

/** The claws snap shut as the golem swings. */
public class PartModelClaws extends PartModel {
   public PartModelClaws(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
      super(objModel, objTexture, attachPoint);
   }

   @Override
   public void preRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side, Transform t) {
      if (partName.startsWith("claw")) {
         float f = golem.getGolemEntity().getAttackAnim(partialTicks) * 4.1F;
         f = f * f;
         t.translate(0.0, -0.2, 0.0);
         t.rotate(f, partName.endsWith("1") ? 1.0F : -1.0F, 0.0F, 0.0F);
      }
   }
}
