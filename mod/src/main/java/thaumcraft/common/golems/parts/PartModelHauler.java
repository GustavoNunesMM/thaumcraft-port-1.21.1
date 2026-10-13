package thaumcraft.common.golems.parts;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.parts.PartModel;

/** The carry frame; the renderer draws the second carried stack on it (postRenderObjectPart in 1.12). */
public class PartModelHauler extends PartModel {
   public PartModelHauler(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
      super(objModel, objTexture, attachPoint);
   }
}
