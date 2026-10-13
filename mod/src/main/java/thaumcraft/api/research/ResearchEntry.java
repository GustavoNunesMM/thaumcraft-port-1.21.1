package thaumcraft.api.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;

/** A Thaumonomicon entry, parsed from research JSON like ResearchManager.parseResearchJson. */
public class ResearchEntry {
   public enum Meta {
      ROUND, SPIKY, REVERSE, HIDDEN, AUTOUNLOCK, HEX
   }

   /** An icon: an item, a texture, or a focus effect ("focus:thaumcraft.AIR"). */
   public record Icon(@Nullable ItemStack stack, @Nullable ResourceLocation texture, @Nullable String focus) {}

   private final String key;
   private final String category;
   private final String name;
   private final List<String> parents;
   private final List<String> siblings;
   private final int displayColumn;
   private final int displayRow;
   private final List<Icon> icons;
   private final Set<Meta> meta;
   private final List<ItemStack> rewardItem;
   private final List<Knowledge> rewardKnow;
   private final List<ResearchStage> stages;
   private final List<ResearchAddendum> addenda;

   private ResearchEntry(String key, String category, String name, List<String> parents, List<String> siblings, int col, int row,
                         List<Icon> icons, Set<Meta> meta, List<ItemStack> rewardItem, List<Knowledge> rewardKnow,
                         List<ResearchStage> stages, List<ResearchAddendum> addenda) {
      this.key = key;
      this.category = category;
      this.name = name;
      this.parents = parents;
      this.siblings = siblings;
      this.displayColumn = col;
      this.displayRow = row;
      this.icons = icons;
      this.meta = meta;
      this.rewardItem = rewardItem;
      this.rewardKnow = rewardKnow;
      this.stages = stages;
      this.addenda = addenda;
   }

   public String getKey() {
      return key;
   }

   public String getCategory() {
      return category;
   }

   public String getName() {
      return name;
   }

   public Component getLocalizedName() {
      return Component.translatable(name);
   }

   /** Raw parents: "~" hides the line in the GUI, "@n" needs only stage n. */
   public List<String> getParents() {
      return parents;
   }

   /** Parents without the "~" prefix (stage suffix kept). */
   public List<String> getParentsStripped() {
      return parents.stream().map(p -> p.startsWith("~") ? p.substring(1) : p).toList();
   }

   /** Parents without prefix or stage suffix. */
   public List<String> getParentsClean() {
      return getParentsStripped().stream().map(p -> p.contains("@") ? p.substring(0, p.indexOf('@')) : p).toList();
   }

   public List<String> getSiblings() {
      return siblings;
   }

   public int getDisplayColumn() {
      return displayColumn;
   }

   public int getDisplayRow() {
      return displayRow;
   }

   public List<Icon> getIcons() {
      return icons;
   }

   public boolean hasMeta(Meta m) {
      return meta.contains(m);
   }

   public List<ItemStack> getRewardItem() {
      return rewardItem;
   }

   public List<Knowledge> getRewardKnow() {
      return rewardKnow;
   }

   public List<ResearchStage> getStages() {
      return stages;
   }

   public List<ResearchAddendum> getAddenda() {
      return addenda;
   }

   @Override
   public String toString() {
      return "ResearchEntry[" + key + "]";
   }

   // ------------------------------------------------------------------ parsing

   private static List<String> strings(JsonObject obj, String field) {
      List<String> out = new ArrayList<>();
      if (obj.has(field)) {
         for (JsonElement e : obj.getAsJsonArray(field)) out.add(e.getAsString());
      }
      return out;
   }

   private static List<ResearchItem> items(String key, List<String> list, HolderLookup.Provider reg) {
      List<ResearchItem> out = new ArrayList<>();
      for (String s : list) {
         ResearchItem it = ResearchItem.parse(s, reg);
         if (it != null) out.add(it);
         else Thaumcraft.LOGGER.debug("Research {}: unknown item {}", key, s);
      }
      return out;
   }

   private static List<Knowledge> knowledge(List<String> list) {
      List<Knowledge> out = new ArrayList<>();
      for (String s : list) {
         Knowledge k = Knowledge.parse(s);
         if (k != null) out.add(k);
      }
      return out;
   }

   private static List<ResourceLocation> recipes(JsonObject obj) {
      return strings(obj, "recipes").stream().map(ResourceLocation::parse).toList();
   }

   public static ResearchEntry parse(JsonObject obj, HolderLookup.Provider reg) {
      String key = obj.get("key").getAsString();
      String name = obj.get("name").getAsString();
      String category = obj.get("category").getAsString();

      List<Icon> icons = new ArrayList<>();
      for (String s : strings(obj, "icons")) {
         if (s.startsWith("focus")) icons.add(new Icon(null, null, s));
         else if (s.contains("textures/")) icons.add(new Icon(null, ResourceLocation.parse(s), null));
         else {
            ResearchItem it = ResearchItem.parse(s, reg);
            if (it != null) icons.add(new Icon(it.display(), null, null));
         }
      }
      Set<Meta> meta = EnumSet.noneOf(Meta.class);
      for (String s : strings(obj, "meta")) meta.add(Meta.valueOf(s.toUpperCase(Locale.ROOT)));
      int col = 0, row = 0;
      if (obj.has("location")) {
         JsonArray loc = obj.getAsJsonArray("location");
         col = loc.get(0).getAsInt();
         row = loc.get(1).getAsInt();
      }
      List<ItemStack> reward = items(key, strings(obj, "reward_item"), reg).stream().map(ResearchItem::display).toList();

      List<ResearchStage> stages = new ArrayList<>();
      for (JsonElement e : obj.getAsJsonArray("stages")) {
         JsonObject so = e.getAsJsonObject();
         List<String> research = new ArrayList<>();
         List<String> researchIcon = new ArrayList<>();
         for (String r : strings(so, "required_research")) {
            String[] ss = r.split(";");
            research.add(ss[0]);
            researchIcon.add(ss.length > 1 ? ss[1] : null);
         }
         stages.add(new ResearchStage(so.get("text").getAsString(), recipes(so),
            items(key, strings(so, "required_item"), reg), items(key, strings(so, "required_craft"), reg),
            knowledge(strings(so, "required_knowledge")), research, researchIcon, so.has("warp") ? so.get("warp").getAsInt() : 0));
      }
      List<ResearchAddendum> addenda = new ArrayList<>();
      if (obj.has("addenda")) {
         for (JsonElement e : obj.getAsJsonArray("addenda")) {
            JsonObject ao = e.getAsJsonObject();
            addenda.add(new ResearchAddendum(ao.get("text").getAsString(), recipes(ao), strings(ao, "required_research")));
         }
      }
      return new ResearchEntry(key, category, name, strings(obj, "parents"), strings(obj, "siblings"), col, row, icons, meta, reward,
         knowledge(strings(obj, "reward_knowledge")), stages, addenda);
   }
}
