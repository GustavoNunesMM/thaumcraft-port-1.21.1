package thaumcraft.common.golems.parts;

import java.util.HashMap;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.PartModel;

/** The grinders spin, faster while the golem swings. */
public class PartModelBreakers extends PartModel {
   private final HashMap<Integer, float[]> ani = new HashMap<>();

   public PartModelBreakers(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
      super(objModel, objTexture, attachPoint);
   }

   @Override
   public void preRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side, Transform t) {
      if (partName.equals("grinder")) {
         int id = golem.getGolemEntity().getId();
         float[] last = ani.getOrDefault(id, new float[2]);
         float f = Math.max(last[0], golem.getGolemEntity().getAttackAnim(partialTicks) * 20.0F);
         float rot = last[1] + f;
         ani.put(id, new float[]{f * 0.99F, rot});
         t.translate(0.0, -0.34, 0.0);
         t.rotate((golem.getGolemEntity().tickCount + partialTicks) / 2.0F + rot + (side == EnumLimbSide.LEFT ? 22 : 0),
            side == EnumLimbSide.LEFT ? -1.0F : 1.0F, 0.0F, 0.0F);
      }
   }
}
