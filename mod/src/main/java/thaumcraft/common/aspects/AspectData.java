package thaumcraft.common.aspects;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;

/**
 * Loads aspect definitions from {@code data/<namespace>/thaumcraft/aspects/{items,entities}/*.json}.
 * Replaces the hardcoded ThaumcraftApi.registerObjectTag / registerEntityTag calls of the 1.12 version,
 * so other mods and datapacks can add or override aspects. Files are applied in id order; later entries win.
 */
public class AspectData extends SimpleJsonResourceReloadListener {
   public static final String DIRECTORY = "thaumcraft/aspects";

   /** One entry of an items file. Exactly one of item/tag/block is set. */
   public record ItemEntry(
      Optional<ResourceLocation> item,
      Optional<ResourceLocation> tag,
      Optional<ResourceLocation> block,
      Optional<ResourceLocation> copy,
      AspectList aspects,
      boolean complex
   ) {
      public static final Codec<ItemEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
         ResourceLocation.CODEC.optionalFieldOf("item").forGetter(ItemEntry::item),
         ResourceLocation.CODEC.optionalFieldOf("tag").forGetter(ItemEntry::tag),
         ResourceLocation.CODEC.optionalFieldOf("block").forGetter(ItemEntry::block),
         ResourceLocation.CODEC.optionalFieldOf("copy").forGetter(ItemEntry::copy),
         AspectList.CODEC.optionalFieldOf("aspects", new AspectList()).forGetter(ItemEntry::aspects),
         Codec.BOOL.optionalFieldOf("complex", false).forGetter(ItemEntry::complex)
      ).apply(i, ItemEntry::new));
   }

   public record EntityEntry(ResourceLocation entity, AspectList aspects) {
      public static final Codec<EntityEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
         ResourceLocation.CODEC.fieldOf("entity").forGetter(EntityEntry::entity),
         AspectList.CODEC.fieldOf("aspects").forGetter(EntityEntry::aspects)
      ).apply(i, EntityEntry::new));
   }

   private static <T> Codec<List<T>> file(Codec<T> entry) {
      return entry.listOf().fieldOf("entries").codec();
   }

   private static final Codec<List<ItemEntry>> ITEMS_FILE = file(ItemEntry.CODEC);
   private static final Codec<List<EntityEntry>> ENTITIES_FILE = file(EntityEntry.CODEC);

   public static final AspectData INSTANCE = new AspectData();

   private List<ItemEntry> items = List.of();
   private List<EntityEntry> entities = List.of();

   private AspectData() {
      super(new Gson(), DIRECTORY);
   }

   public List<ItemEntry> items() {
      return items;
   }

   public List<EntityEntry> entities() {
      return entities;
   }

   private static int priority(JsonElement file) {
      return file.isJsonObject() && file.getAsJsonObject().has("priority") ? file.getAsJsonObject().get("priority").getAsInt() : 0;
   }

   @Override
   protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
      List<ItemEntry> newItems = new ArrayList<>();
      List<EntityEntry> newEntities = new ArrayList<>();
      // Later entries override earlier ones. Files apply by "priority" (default 0), then by name, so a file can
      // refine broad tag entries from another one (e.g. Thaumcraft's own logs inside #minecraft:logs).
      files.entrySet().stream().sorted(
         Comparator.<Map.Entry<ResourceLocation, JsonElement>>comparingInt(e -> priority(e.getValue())).thenComparing(Map.Entry::getKey)
      ).forEach(e -> {
         String path = e.getKey().getPath();
         if (path.startsWith("items/")) {
            ITEMS_FILE.parse(JsonOps.INSTANCE, e.getValue())
               .resultOrPartial(err -> Thaumcraft.LOGGER.error("Bad aspect file {}: {}", e.getKey(), err))
               .ifPresent(newItems::addAll);
         } else if (path.startsWith("entities/")) {
            ENTITIES_FILE.parse(JsonOps.INSTANCE, e.getValue())
               .resultOrPartial(err -> Thaumcraft.LOGGER.error("Bad aspect file {}: {}", e.getKey(), err))
               .ifPresent(newEntities::addAll);
         } else {
            Thaumcraft.LOGGER.warn("Ignoring aspect file {} (expected items/ or entities/)", e.getKey());
         }
      });
      items = List.copyOf(newItems);
      entities = List.copyOf(newEntities);
      AspectManager.SERVER.invalidate();
      Thaumcraft.LOGGER.info("Loaded {} item and {} entity aspect entries", items.size(), entities.size());
   }
}
