package thaumcraft.api.research;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

/**
 * One page/step of a research entry. A stage is completed in the Thaumonomicon once its requirements are met:
 * items handed in (obtain), items crafted (craft), knowledge spent and other research known.
 */
public record ResearchStage(
   String text,
   List<ResourceLocation> recipes,
   List<ResearchItem> obtain,
   List<ResearchItem> craft,
   List<Knowledge> know,
   List<String> research,
   List<String> researchIcon,
   int warp
) {
   public boolean hasRequirements() {
      return !obtain.isEmpty() || !craft.isEmpty() || !know.isEmpty() || !research.isEmpty();
   }

   @Nullable
   public String researchIcon(int i) {
      return i < researchIcon.size() ? researchIcon.get(i) : null;
   }
}
