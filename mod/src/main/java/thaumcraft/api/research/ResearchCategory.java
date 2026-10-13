package thaumcraft.api.research;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** A tab of the Thaumonomicon: its entries, the aspects that teach it (formula) and the research that unlocks it. */
public class ResearchCategory {
   public final String key;
   /** Research needed to see this category; null = always visible. */
   @Nullable
   public final String researchKey;
   public final AspectList formula;
   public final ResourceLocation icon;
   public final ResourceLocation background;
   @Nullable
   public final ResourceLocation background2;
   public final Map<String, ResearchEntry> research = new LinkedHashMap<>();
   public int minDisplayColumn;
   public int minDisplayRow;
   public int maxDisplayColumn;
   public int maxDisplayRow;

   public ResearchCategory(String key, @Nullable String researchKey, AspectList formula, ResourceLocation icon, ResourceLocation background,
                           @Nullable ResourceLocation background2) {
      this.key = key;
      this.researchKey = researchKey;
      this.formula = formula;
      this.icon = icon;
      this.background = background;
      this.background2 = background2;
   }

   public int applyFormula(AspectList as) {
      return applyFormula(as, 1.0);
   }

   /** sqrt of the aspects weighted by this category's formula, rounded up. */
   public int applyFormula(AspectList as, double mod) {
      double total = 0.0;
      for (Aspect aspect : formula.getAspects()) {
         total += mod * mod * as.getAmount(aspect) * (formula.getAmount(aspect) / 10.0);
      }
      if (total > 0.0) total = Math.sqrt(total);
      return Mth.ceil(total);
   }

   void clearResearch() {
      research.clear();
      minDisplayColumn = minDisplayRow = maxDisplayColumn = maxDisplayRow = 0;
   }
}
