package thaumcraft.common.golems.parts;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.PartModel;

/** The uni-wheel turns as GolemLegWheels tracks the distance travelled. */
public class PartModelWheel extends PartModel {
   public PartModelWheel(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
      super(objModel, objTexture, attachPoint);
   }

   @Override
   public void preRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side, Transform t) {
      if (partName.equals("wheel")) {
         float lastRot = GolemLegWheels.ani.getOrDefault(golem.getGolemEntity().getId(), 0.0F);
         t.translate(0.0, -0.375, 0.0);
         t.rotate(lastRot, -1.0F, 0.0F, 0.0F);
      }
   }
}
