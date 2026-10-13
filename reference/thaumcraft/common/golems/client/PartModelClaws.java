package thaumcraft.common.golems.client;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.PartModel;

public class PartModelClaws extends PartModel {
   float f = 0.0F;

   public PartModelClaws(ResourceLocation objModel, ResourceLocation objTexture, PartModel.EnumAttachPoint attachPoint) {
      super(objModel, objTexture, attachPoint);
   }

   @Override
   public void preRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, PartModel.EnumLimbSide side) {
      if (partName.startsWith("claw")) {
         this.f = 0.0F;
         this.f = golem.getGolemEntity().getSwingProgress(partialTicks) * 4.1F;
         this.f = this.f * this.f;
         GlStateManager.translate(0.0, -0.2, 0.0);
         GlStateManager.rotate(this.f, partName.endsWith("1") ? 1.0F : -1.0F, 0.0F, 0.0F);
      }
   }
}
