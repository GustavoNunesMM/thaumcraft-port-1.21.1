package thaumcraft.api.research;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** An extra page of a completed research entry that appears once some other research is known. */
public record ResearchAddendum(String text, List<ResourceLocation> recipes, List<String> research) {
}
