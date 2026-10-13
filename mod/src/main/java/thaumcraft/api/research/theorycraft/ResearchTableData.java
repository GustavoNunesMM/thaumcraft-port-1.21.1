package thaumcraft.api.research.theorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.common.research.ResearchManager;

/**
 * The theory being written at a research table: inspiration left, the totals gathered per category, blocked
 * categories, the current card choices and the cards already played.
 */
public class ResearchTableData {
   @Nullable
   public final BlockEntity table;
   public String player = "";
   public int inspiration;
   public int inspirationStart;
   public int bonusDraws;
   public int placedCards;
   public int aidsChosen;
   public int penaltyStart;
   public List<Long> savedCards = new ArrayList<>();
   public List<String> aidCards = new ArrayList<>();
   public TreeMap<String, Integer> categoryTotals = new TreeMap<>();
   public List<String> categoriesBlocked = new ArrayList<>();
   public List<CardChoice> cardChoices = new ArrayList<>();
   @Nullable
   public CardChoice lastDraw;

   public ResearchTableData(@Nullable BlockEntity table) {
      this.table = table;
   }

   public ResearchTableData(Player player, @Nullable BlockEntity table) {
      this(table);
      this.player = player.getGameProfile().getName();
   }

   public static final class CardChoice {
      public final TheorycraftCard card;
      public final String key;
      public final boolean fromAid;
      public boolean selected;

      public CardChoice(String key, TheorycraftCard card, boolean fromAid, boolean selected) {
         this.key = key;
         this.card = card;
         this.fromAid = fromAid;
         this.selected = selected;
      }
   }

   public boolean isComplete() {
      return inspiration <= 0;
   }

   public boolean hasTotal(String cat) {
      return categoryTotals.containsKey(cat);
   }

   public int getTotal(String cat) {
      return categoryTotals.getOrDefault(cat, 0);
   }

   public void addTotal(String cat, int amt) {
      int current = getTotal(cat) + amt;
      if (current <= 0) categoryTotals.remove(cat);
      else categoryTotals.put(cat, current);
   }

   public void addInspiration(int amt) {
      inspiration = Math.min(inspirationStart, inspiration + amt);
   }

   private boolean isCategoryBlocked(@Nullable String cat) {
      return cat != null && categoriesBlocked.contains(cat);
   }

   /** Draws 2 cards (3 if a bonus draw is available and asked for), a quarter of the time one from the aids. */
   public void drawCards(int draw, Player pe) {
      if (draw == 3) {
         if (bonusDraws > 0) bonusDraws--;
         else draw = 2;
      }
      cardChoices.clear();
      player = pe.getGameProfile().getName();
      List<String> availCats = getAvailableCategories(pe);
      List<String> drawn = new ArrayList<>();
      boolean aidDrawn = false;
      int failsafe = 0;
      String[] all = TheorycraftManager.CARDS.keySet().toArray(new String[0]);
      while (draw > 0 && failsafe++ < 10000) {
         if (!aidDrawn && !aidCards.isEmpty() && pe.getRandom().nextFloat() <= 0.25F) {
            int idx = pe.getRandom().nextInt(aidCards.size());
            String key = aidCards.get(idx);
            TheorycraftCard card = generateCard(key, -1L, pe);
            if (card == null || card.getInspirationCost() > inspiration || isCategoryBlocked(card.getResearchCategory()) || drawn.contains(key)) continue;
            drawn.add(key);
            cardChoices.add(new CardChoice(key, card, true, false));
            aidCards.remove(idx);
         } else {
            String key = all[pe.getRandom().nextInt(all.length)];
            TheorycraftCard card = generateCard(key, -1L, pe);
            if (card == null || card.isAidOnly() || card.getInspirationCost() > inspiration) continue;
            if (card.getResearchCategory() != null && !availCats.contains(card.getResearchCategory())) continue;
            if (drawn.contains(key)) continue;
            drawn.add(key);
            cardChoices.add(new CardChoice(key, card, false, false));
         }
         draw--;
      }
   }

   @Nullable
   private TheorycraftCard generateCard(@Nullable String key, long seed, @Nullable Player pe) {
      Supplier<TheorycraftCard> s = key == null ? null : TheorycraftManager.CARDS.get(key);
      if (s == null) return null;
      TheorycraftCard tc = s.get();
      tc.setSeed(seed >= 0 ? seed : pe != null ? pe.getRandom().nextLong() : System.nanoTime());
      try {
         if (pe != null && !tc.initialize(pe, this)) return null;
      } catch (RuntimeException e) {
         return null;
      }
      return tc;
   }

   @Nullable
   private TheorycraftCard generateCardWithNBT(String key, CompoundTag nbt) {
      Supplier<TheorycraftCard> s = TheorycraftManager.CARDS.get(key);
      if (s == null) return null;
      TheorycraftCard tc = s.get();
      tc.deserialize(nbt);
      return tc;
   }

   public void initialize(Player player, Set<String> aids) {
      inspirationStart = getAvailableInspiration(player);
      inspiration = inspirationStart - aids.size();
      for (String muk : aids) {
         ITheorycraftAid mu = TheorycraftManager.AIDS.get(muk);
         if (mu == null) continue;
         for (Supplier<TheorycraftCard> c : mu.getCards()) aidCards.add(TheorycraftManager.keyOf(c.get()));
      }
   }

   public List<String> getAvailableCategories(Player player) {
      List<String> cats = new ArrayList<>();
      for (ResearchCategory rc : ResearchCategories.CATEGORIES.values()) {
         if (!isCategoryBlocked(rc.key) && (rc.researchKey == null || ResearchManager.knowsResearchStrict(player, rc.researchKey))) cats.add(rc.key);
      }
      return cats;
   }

   /** 5 plus half a point per completed SPIKY (milestone) entry and a tenth per HIDDEN one, at most 15. */
   public static int getAvailableInspiration(Player player) {
      float tot = 5.0F;
      for (String s : ResearchManager.knowledge(player).getResearchList()) {
         if (!ResearchManager.knowsResearchStrict(player, s)) continue;
         ResearchEntry re = ResearchCategories.getResearch(s);
         if (re != null) {
            if (re.hasMeta(ResearchEntry.Meta.SPIKY)) tot += 0.5F;
            if (re.hasMeta(ResearchEntry.Meta.HIDDEN)) tot += 0.1F;
         }
      }
      return Math.min(15, Math.round(tot));
   }

   // ------------------------------------------------------------------ NBT

   public CompoundTag serialize() {
      CompoundTag nbt = new CompoundTag();
      nbt.putString("player", player);
      nbt.putInt("inspiration", inspiration);
      nbt.putInt("inspirationStart", inspirationStart);
      nbt.putInt("placedCards", placedCards);
      nbt.putInt("bonusDraws", bonusDraws);
      nbt.putInt("aidsChosen", aidsChosen);
      nbt.putInt("penaltyStart", penaltyStart);
      ListTag saved = new ListTag();
      for (Long card : savedCards) {
         CompoundTag t = new CompoundTag();
         t.putLong("card", card);
         saved.add(t);
      }
      nbt.put("savedCards", saved);
      ListTag blocked = new ListTag();
      for (String c : categoriesBlocked) {
         CompoundTag t = new CompoundTag();
         t.putString("category", c);
         blocked.add(t);
      }
      nbt.put("categoriesBlocked", blocked);
      ListTag totals = new ListTag();
      for (Map.Entry<String, Integer> e : categoryTotals.entrySet()) {
         CompoundTag t = new CompoundTag();
         t.putString("category", e.getKey());
         t.putInt("total", e.getValue());
         totals.add(t);
      }
      nbt.put("categoryTotals", totals);
      ListTag aids = new ListTag();
      for (String a : aidCards) {
         CompoundTag t = new CompoundTag();
         t.putString("aidCard", a);
         aids.add(t);
      }
      nbt.put("aidCards", aids);
      ListTag choices = new ListTag();
      for (CardChoice cc : cardChoices) choices.add(serializeCardChoice(cc));
      nbt.put("cardChoices", choices);
      if (lastDraw != null) nbt.put("lastDraw", serializeCardChoice(lastDraw));
      return nbt;
   }

   private CompoundTag serializeCardChoice(CardChoice mc) {
      CompoundTag nbt = new CompoundTag();
      nbt.putString("cardChoice", mc.key);
      nbt.putBoolean("aid", mc.fromAid);
      nbt.putBoolean("select", mc.selected);
      nbt.put("cardNBT", mc.card.serialize());
      return nbt;
   }

   public void deserialize(CompoundTag nbt) {
      inspiration = nbt.getInt("inspiration");
      inspirationStart = nbt.getInt("inspirationStart");
      placedCards = nbt.getInt("placedCards");
      bonusDraws = nbt.getInt("bonusDraws");
      aidsChosen = nbt.getInt("aidsChosen");
      penaltyStart = nbt.getInt("penaltyStart");
      player = nbt.getString("player");
      savedCards = new ArrayList<>();
      for (Tag t : nbt.getList("savedCards", Tag.TAG_COMPOUND)) savedCards.add(((CompoundTag) t).getLong("card"));
      categoriesBlocked = new ArrayList<>();
      for (Tag t : nbt.getList("categoriesBlocked", Tag.TAG_COMPOUND)) categoriesBlocked.add(((CompoundTag) t).getString("category"));
      categoryTotals = new TreeMap<>();
      for (Tag t : nbt.getList("categoryTotals", Tag.TAG_COMPOUND)) {
         categoryTotals.put(((CompoundTag) t).getString("category"), ((CompoundTag) t).getInt("total"));
      }
      aidCards = new ArrayList<>();
      for (Tag t : nbt.getList("aidCards", Tag.TAG_COMPOUND)) aidCards.add(((CompoundTag) t).getString("aidCard"));
      cardChoices = new ArrayList<>();
      for (Tag t : nbt.getList("cardChoices", Tag.TAG_COMPOUND)) {
         CardChoice cc = deserializeCardChoice((CompoundTag) t);
         if (cc != null) cardChoices.add(cc);
      }
      lastDraw = nbt.contains("lastDraw") ? deserializeCardChoice(nbt.getCompound("lastDraw")) : null;
   }

   @Nullable
   private CardChoice deserializeCardChoice(CompoundTag nbt) {
      String key = nbt.getString("cardChoice");
      TheorycraftCard tc = generateCardWithNBT(key, nbt.getCompound("cardNBT"));
      return tc == null ? null : new CardChoice(key, tc, nbt.getBoolean("aid"), nbt.getBoolean("select"));
   }
}
