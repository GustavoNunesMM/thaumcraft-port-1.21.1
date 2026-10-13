package thaumcraft.common.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.EnumResearchFlag;
import thaumcraft.api.research.EnumResearchStatus;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;

/**
 * Per-player research state (the 1.12 IPlayerKnowledge capability, now a data attachment): known research and the
 * stage reached in each, notification flags, and raw knowledge points per type and category.
 *
 * <p>Research keys also cover things that are not Thaumonomicon entries: scanned objects ("!minecraft:stone"),
 * aspects ("!terra"), crafted items ("[#]minecraft:stick"), milestones ("m_deepdown", "!gotdream")...
 */
public class PlayerKnowledge {
   private record Entry(String key, Optional<Integer> stage, List<EnumResearchFlag> flags) {
      static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
         Codec.STRING.fieldOf("key").forGetter(Entry::key),
         Codec.INT.optionalFieldOf("stage").forGetter(Entry::stage),
         Codec.STRING.xmap(EnumResearchFlag::valueOf, EnumResearchFlag::name).listOf().optionalFieldOf("flags", List.of()).forGetter(Entry::flags)
      ).apply(i, Entry::new));
   }

   public static final Codec<PlayerKnowledge> CODEC = RecordCodecBuilder.create(i -> i.group(
      Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(PlayerKnowledge::entries),
      Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("knowledge", Map.of()).forGetter(k -> k.knowledge),
      // Milestone 1 format: plain research keys and observation points per category.
      Codec.STRING.listOf().optionalFieldOf("research", List.of()).forGetter(k -> List.of()),
      Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("observation", Map.of()).forGetter(k -> Map.of())
   ).apply(i, PlayerKnowledge::new));

   public static final StreamCodec<ByteBuf, PlayerKnowledge> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

   private final Set<String> research = new LinkedHashSet<>();
   private final Map<String, Integer> stages = new HashMap<>();
   private final Map<String, EnumSet<EnumResearchFlag>> flags = new HashMap<>();
   private final Map<String, Integer> knowledge = new HashMap<>();

   public PlayerKnowledge() {
   }

   private PlayerKnowledge(List<Entry> entries, Map<String, Integer> knowledge, List<String> legacyResearch, Map<String, Integer> legacyObs) {
      for (Entry e : entries) {
         research.add(e.key());
         e.stage().filter(s -> s > 0).ifPresent(s -> stages.put(e.key(), s));
         if (!e.flags().isEmpty()) flags.put(e.key(), EnumSet.copyOf(e.flags()));
      }
      this.knowledge.putAll(knowledge);
      research.addAll(legacyResearch);
      legacyObs.forEach((cat, amount) -> this.knowledge.merge(EnumKnowledgeType.OBSERVATION.getAbbreviation() + "_" + cat, amount, Integer::sum));
   }

   private List<Entry> entries() {
      List<Entry> out = new ArrayList<>();
      for (String key : research) {
         EnumSet<EnumResearchFlag> f = flags.get(key);
         out.add(new Entry(key, Optional.ofNullable(stages.get(key)), f == null ? List.of() : List.copyOf(f)));
      }
      return out;
   }

   public void clear() {
      research.clear();
      stages.clear();
      flags.clear();
      knowledge.clear();
   }

   public PlayerKnowledge copy() {
      PlayerKnowledge k = new PlayerKnowledge();
      k.research.addAll(research);
      k.stages.putAll(stages);
      flags.forEach((key, f) -> k.flags.put(key, EnumSet.copyOf(f)));
      k.knowledge.putAll(knowledge);
      return k;
   }

   // ------------------------------------------------------------------ research

   public EnumResearchStatus getResearchStatus(String res) {
      if (!isResearchKnown(res)) return EnumResearchStatus.UNKNOWN;
      ResearchEntry entry = ResearchCategories.getResearch(res);
      return entry != null && !entry.getStages().isEmpty() && getResearchStage(res) <= entry.getStages().size()
         ? EnumResearchStatus.IN_PROGRESS : EnumResearchStatus.COMPLETE;
   }

   /** Known at all; "KEY@n" means known and at stage n or later. "" is always known. */
   public boolean isResearchKnown(@Nullable String res) {
      if (res == null) return false;
      if (res.isEmpty()) return true;
      String[] ss = res.split("@");
      if (ss.length > 1 && getResearchStage(ss[0]) < Mth.getInt(ss[1], 0)) return false;
      return research.contains(ss[0]);
   }

   public boolean isResearchComplete(String res) {
      return getResearchStatus(res) == EnumResearchStatus.COMPLETE;
   }

   /** -1 if unknown, 0 if known without stages progressed. */
   public int getResearchStage(@Nullable String res) {
      if (res == null || !research.contains(res)) return -1;
      return stages.getOrDefault(res, 0);
   }

   public boolean setResearchStage(String res, int stage) {
      if (res == null || !research.contains(res) || stage <= 0) return false;
      stages.put(res, stage);
      return true;
   }

   /** Returns true if the key was not known before. */
   public boolean addResearch(String res) {
      if (isResearchKnown(res)) return false;
      research.add(res);
      return true;
   }

   public boolean removeResearch(String res) {
      if (!isResearchKnown(res)) return false;
      research.remove(res);
      stages.remove(res);
      flags.remove(res);
      return true;
   }

   public Set<String> getResearchList() {
      return Collections.unmodifiableSet(research);
   }

   public boolean setResearchFlag(String res, EnumResearchFlag flag) {
      return flags.computeIfAbsent(res, k -> EnumSet.noneOf(EnumResearchFlag.class)).add(flag);
   }

   public boolean clearResearchFlag(String res, EnumResearchFlag flag) {
      EnumSet<EnumResearchFlag> f = flags.get(res);
      if (f == null) return false;
      boolean b = f.remove(flag);
      if (f.isEmpty()) flags.remove(res);
      return b;
   }

   public boolean hasResearchFlag(String res, EnumResearchFlag flag) {
      EnumSet<EnumResearchFlag> f = flags.get(res);
      return f != null && f.contains(flag);
   }

   // ------------------------------------------------------------------ knowledge

   private static String key(EnumKnowledgeType type, @Nullable ResearchCategory category) {
      return type.getAbbreviation() + "_" + (category == null ? "" : category.key);
   }

   /** Adds raw points (negative to spend); fails if it would go below zero. */
   public boolean addKnowledge(EnumKnowledgeType type, @Nullable ResearchCategory category, int amount) {
      int c = getKnowledgeRaw(type, category);
      if (c + amount < 0) return false;
      knowledge.put(key(type, category), c + amount);
      return true;
   }

   /** Whole knowledge points (raw / progression). */
   public int getKnowledge(EnumKnowledgeType type, @Nullable ResearchCategory category) {
      return Math.floorDiv(getKnowledgeRaw(type, category), type.getProgression());
   }

   public int getKnowledgeRaw(EnumKnowledgeType type, @Nullable ResearchCategory category) {
      return knowledge.getOrDefault(key(type, category), 0);
   }
}
