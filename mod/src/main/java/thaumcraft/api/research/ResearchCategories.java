package thaumcraft.api.research;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** The research categories (ConfigResearch.initCategories) and lookup of entries across them. */
public final class ResearchCategories {
   public static final Map<String, ResearchCategory> CATEGORIES = new LinkedHashMap<>();
   private static final ResourceLocation BACK_OVER = tex("textures/gui/gui_research_back_over.png");

   static {
      register("BASICS", null, new AspectList().add(Aspect.PLANT, 5).add(Aspect.ORDER, 5).add(Aspect.ENTROPY, 5).add(Aspect.AIR, 5)
         .add(Aspect.FIRE, 5).add(Aspect.EARTH, 3).add(Aspect.WATER, 5), "textures/items/thaumonomicon_cheat.png", 1);
      register("AUROMANCY", "UNLOCKAUROMANCY", new AspectList().add(Aspect.AURA, 20).add(Aspect.MAGIC, 20).add(Aspect.FLUX, 15)
         .add(Aspect.CRYSTAL, 5).add(Aspect.COLD, 5).add(Aspect.AIR, 5), "textures/research/cat_auromancy.png", 2);
      register("ALCHEMY", "UNLOCKALCHEMY", new AspectList().add(Aspect.ALCHEMY, 30).add(Aspect.FLUX, 10).add(Aspect.MAGIC, 10)
         .add(Aspect.LIFE, 5).add(Aspect.AVERSION, 5).add(Aspect.DESIRE, 5).add(Aspect.WATER, 5), "textures/research/cat_alchemy.png", 3);
      register("ARTIFICE", "UNLOCKARTIFICE", new AspectList().add(Aspect.MECHANISM, 10).add(Aspect.CRAFT, 10).add(Aspect.METAL, 10)
         .add(Aspect.TOOL, 10).add(Aspect.ENERGY, 10).add(Aspect.LIGHT, 5).add(Aspect.FLIGHT, 5).add(Aspect.TRAP, 5).add(Aspect.FIRE, 5),
         "textures/research/cat_artifice.png", 4);
      register("INFUSION", "UNLOCKINFUSION", new AspectList().add(Aspect.MAGIC, 30).add(Aspect.PROTECT, 10).add(Aspect.TOOL, 10)
         .add(Aspect.FLUX, 5).add(Aspect.CRAFT, 5).add(Aspect.SOUL, 5).add(Aspect.EARTH, 3), "textures/research/cat_infusion.png", 7);
      register("GOLEMANCY", "UNLOCKGOLEMANCY", new AspectList().add(Aspect.MAN, 20).add(Aspect.MOTION, 10).add(Aspect.MIND, 10)
         .add(Aspect.MECHANISM, 10).add(Aspect.EXCHANGE, 5).add(Aspect.SENSES, 5).add(Aspect.BEAST, 5).add(Aspect.ORDER, 5),
         "textures/research/cat_golemancy.png", 5);
      register("ELDRITCH", "UNLOCKELDRITCH", new AspectList().add(Aspect.ELDRITCH, 20).add(Aspect.DARKNESS, 10).add(Aspect.MAGIC, 5)
         .add(Aspect.MIND, 5).add(Aspect.VOID, 5).add(Aspect.DEATH, 5).add(Aspect.UNDEAD, 5).add(Aspect.ENTROPY, 5),
         "textures/research/cat_eldritch.png", 6);
   }

   private static ResourceLocation tex(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   private static void register(String key, @Nullable String research, AspectList formula, String icon, int back) {
      CATEGORIES.put(key, new ResearchCategory(key, research, formula, tex(icon), tex("textures/gui/gui_research_back_" + back + ".png"), BACK_OVER));
   }

   private ResearchCategories() {
   }

   @Nullable
   public static ResearchCategory getResearchCategory(String key) {
      return CATEGORIES.get(key);
   }

   @Nullable
   public static ResearchEntry getResearch(String key) {
      for (ResearchCategory cat : CATEGORIES.values()) {
         ResearchEntry e = cat.research.get(key);
         if (e != null) return e;
      }
      return null;
   }

   public static Iterable<ResearchEntry> allResearch() {
      return () -> CATEGORIES.values().stream().flatMap(c -> c.research.values().stream()).iterator();
   }

   /** Replaces every category's entries (on datapack reload, or when the client receives them). */
   public static void replaceAll(Iterable<ResearchEntry> entries) {
      CATEGORIES.values().forEach(ResearchCategory::clearResearch);
      for (ResearchEntry ri : entries) {
         ResearchCategory rl = CATEGORIES.get(ri.getCategory());
         if (rl == null || rl.research.containsKey(ri.getKey())) {
            Thaumcraft.LOGGER.warn("Could not add invalid research entry {}", ri.getKey());
            continue;
         }
         ResearchEntry overlap = rl.research.values().stream()
            .filter(rr -> rr.getDisplayColumn() == ri.getDisplayColumn() && rr.getDisplayRow() == ri.getDisplayRow()).findFirst().orElse(null);
         if (overlap != null) {
            Thaumcraft.LOGGER.warn("Research [{}] not added as it overlaps with existing research [{}] at {},{}",
               ri.getKey(), overlap.getKey(), ri.getDisplayColumn(), ri.getDisplayRow());
            continue;
         }
         rl.research.put(ri.getKey(), ri);
         rl.minDisplayColumn = Math.min(rl.minDisplayColumn, ri.getDisplayColumn());
         rl.minDisplayRow = Math.min(rl.minDisplayRow, ri.getDisplayRow());
         rl.maxDisplayColumn = Math.max(rl.maxDisplayColumn, ri.getDisplayColumn());
         rl.maxDisplayRow = Math.max(rl.maxDisplayRow, ri.getDisplayRow());
      }
   }
}
