package thaumcraft.api.research;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import thaumcraft.Thaumcraft;
import thaumcraft.common.research.ResearchManager;

/**
 * Loads research entries from data/&lt;namespace&gt;/thaumcraft/research/*.json (ResearchManager.parseAllResearch).
 * The server keeps the raw files to send them to clients, which parse them the same way.
 */
public class ResearchData extends SimpleJsonResourceReloadListener {
   public static final String DIRECTORY = "thaumcraft/research";
   /** Raw JSON of every research file, by file id, as last loaded (server) or received (client). */
   private static Map<String, String> rawFiles = Map.of();

   private final HolderLookup.Provider registries;

   public ResearchData(HolderLookup.Provider registries) {
      super(new Gson(), DIRECTORY);
      this.registries = registries;
   }

   public static Map<String, String> rawFiles() {
      return rawFiles;
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
      Map<String, String> raw = new LinkedHashMap<>();
      files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> raw.put(e.getKey().toString(), e.getValue().toString()));
      load(raw, registries);
   }

   /** Parses research files and replaces the loaded research. Used by the server reload and by the client sync. */
   public static void load(Map<String, String> files, HolderLookup.Provider registries) {
      List<ResearchEntry> entries = new ArrayList<>();
      files.forEach((id, json) -> {
         int count = 0;
         try {
            for (JsonElement element : JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("entries")) {
               try {
                  entries.add(ResearchEntry.parse(element.getAsJsonObject(), registries));
                  count++;
               } catch (Exception ex) {
                  Thaumcraft.LOGGER.warn("Invalid research entry [{}] in {}: {}", count + 1, id, ex.toString());
               }
            }
         } catch (Exception ex) {
            Thaumcraft.LOGGER.warn("Invalid research file {}: {}", id, ex.toString());
         }
         Thaumcraft.LOGGER.debug("Loaded {} research entries from {}", count, id);
      });
      ResearchCategories.replaceAll(entries);
      rawFiles = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(files));
      ResearchManager.rebuildCraftingReferences();
      Thaumcraft.LOGGER.info("Loaded {} research entries", entries.size());
   }
}
