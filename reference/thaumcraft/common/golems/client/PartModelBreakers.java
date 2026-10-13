package thaumcraft.common.golems.client;

import java.util.HashMap;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.PartModel;

public class PartModelBreakers extends PartModel {
   private HashMap<Integer, Float[]> ani = new HashMap<>();

   public PartModelBreakers(ResourceLocation objModel, ResourceLocation objTexture, PartModel.EnumAttachPoint attachPoint) {
      super(objModel, objTexture, attachPoint);
   }

   @Override
   public void preRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, PartModel.EnumLimbSide side) {
      if (partName.equals("grinder")) {
         float lastSpeed = 0.0F;
         float lastRot = 0.0F;
         if (this.ani.containsKey(golem.getGolemEntity().getEntityId())) {
            lastSpeed = this.ani.get(golem.getGolemEntity().getEntityId())[0];
            lastRot = this.ani.get(golem.getGolemEntity().getEntityId())[1];
         }

         float f = Math.max(lastSpeed, golem.getGolemEntity().getSwingProgress(partialTicks) * 20.0F);
         float rot = lastRot + f;
         lastSpeed = f * 0.99F;
         this.ani.put(golem.getGolemEntity().getEntityId(), new Float[]{lastSpeed, rot});
         GlStateManager.translate(0.0, -0.34, 0.0);
         GlStateManager.rotate(
            (golem.getGolemEntity().ticksExisted + partialTicks) / 2.0F + rot + (side == PartModel.EnumLimbSide.LEFT ? 22 : 0),
            side == PartModel.EnumLimbSide.LEFT ? -1.0F : 1.0F,
            0.0F,
            0.0F
         );
      }
   }
}
