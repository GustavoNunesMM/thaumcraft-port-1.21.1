package thaumcraft.common.research.theorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.blocks.crafting.ResearchTableBlockEntity;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;

/** All the research table cards (thaumcraft.api.research.theorycraft.Card* and common.lib.research.theorycraft.Card*). */
public final class Cards {
   private Cards() {
   }

   // ------------------------------------------------------------------ helpers

   static MutableComponent cat(String cat) {
      return Component.translatable("tc.research_category." + cat).withStyle(ChatFormatting.BOLD);
   }

   static MutableComponent catTitle(String cat) {
      return Component.translatable("tc.research_category." + cat).withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.BOLD);
   }

   static MutableComponent bold(String s) {
      return Component.literal(s).withStyle(ChatFormatting.BOLD);
   }

   static String randomCategory(Player p) {
      String[] s = ResearchCategories.CATEGORIES.keySet().toArray(new String[0]);
      return s[p.getRandom().nextInt(s.length)];
   }

   static int rnd(Player p, int min, int max) {
      return Mth.nextInt(p.getRandom(), min, max);
   }

   static List<Aspect> compounds() {
      List<Aspect> out = new ArrayList<>();
      for (Aspect a : Aspect.all()) {
         if (a.getComponents() != null && a.getComponents().length == 2) out.add(a);
      }
      return out;
   }

   static void warp(Player p, int amount, PlayerWarp.Type type) {
      ResearchManager.addWarp(p, amount, type);
   }

   /** An item by id; EMPTY if it isn't ported yet. */
   static ItemStack item(String id) {
      Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
      return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
   }

   /** ItemPhial.makeFilledPhial: a phial of 10 of an aspect (EMPTY until phials are ported). */
   static ItemStack phial(Aspect a) {
      ItemStack s = item("thaumcraft:phial");
      if (!s.isEmpty()) s.set(thaumcraft.common.registers.ModDataComponents.ESSENTIA.get(), new AspectList().add(a, 10));
      return s;
   }

   static int visSize(Player p, ItemStack stack) {
      AspectList al = AspectManager.get(p.level()).getObjectAspects(p.registryAccess(), stack);
      return al == null ? 0 : al.visSize();
   }

   static CompoundTag putStack(CompoundTag nbt, String key, ItemStack stack, Player p) {
      if (!stack.isEmpty()) nbt.put(key, stack.save(p.registryAccess()));
      return nbt;
   }

   // ------------------------------------------------------------------ basic deck

   /** Converts an observation point of a category into theory for it (and a little basics). */
   public static class Analyze extends TheorycraftCard {
      String cat = null;

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         if (cat != null) n.putString("cat", cat);
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         cat = n.contains("cat") ? n.getString("cat") : null;
      }

      @Override
      public String getResearchCategory() {
         return cat;
      }

      /** Port fix: the original checked the knowledge of a not-yet-chosen category, so the card never showed up. */
      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         Random r = new Random(getSeed());
         List<String> cats = new ArrayList<>();
         for (ResearchCategory rc : ResearchCategories.CATEGORIES.values()) {
            if (!rc.key.equals("BASICS") && ResearchManager.knowledge(player).getKnowledge(EnumKnowledgeType.OBSERVATION, rc) > 0) cats.add(rc.key);
         }
         if (!cats.isEmpty()) cat = cats.get(r.nextInt(cats.size()));
         return cat != null;
      }

      @Override
      public int getInspirationCost() {
         return 2;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.analyze.name", catTitle(cat));
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.analyze.text", cat(cat), cat("BASICS"));
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         ResearchCategory rc = ResearchCategories.getResearchCategory(cat);
         if (ResearchManager.knowledge(player).getKnowledge(EnumKnowledgeType.OBSERVATION, rc) < 1) return false;
         data.addTotal("BASICS", 5);
         ResearchManager.knowledge(player).addKnowledge(EnumKnowledgeType.OBSERVATION, rc, -EnumKnowledgeType.OBSERVATION.getProgression());
         data.addTotal(cat, rnd(player, 25, 50));
         return true;
      }
   }

   public static class Balance extends TheorycraftCard {
      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.balance.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.balance.text");
      }

      private static int[] totals(ResearchTableData data) {
         int total = 0, size = 0;
         for (String c : data.categoryTotals.keySet()) {
            if (!data.categoriesBlocked.contains(c)) {
               total += data.categoryTotals.get(c);
               size++;
            }
         }
         return new int[]{total, size};
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         int[] t = totals(data);
         return data.categoriesBlocked.size() < data.categoryTotals.size() - 1 && t[0] >= t[1];
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         int[] t = totals(data);
         if (!(data.categoriesBlocked.size() < data.categoryTotals.size() - 1 && t[0] >= t[1])) return false;
         for (String category : List.copyOf(data.categoryTotals.keySet())) {
            if (!data.categoriesBlocked.contains(category)) data.categoryTotals.put(category, t[0] / t[1]);
         }
         data.addTotal("BASICS", 5);
         data.penaltyStart++;
         return true;
      }
   }

   public static class Experimentation extends TheorycraftCard {
      @Override
      public int getInspirationCost() {
         return 2;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.experimentation.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.experimentation.text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal(randomCategory(player), rnd(player, 15, 30));
         data.addTotal("BASICS", rnd(player, 1, 10));
         return true;
      }
   }

   public static class Inspired extends TheorycraftCard {
      String cat = null;
      int amt;

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         if (cat != null) n.putString("cat", cat);
         n.putInt("amt", amt);
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         cat = n.getString("cat");
         amt = n.getInt("amt");
      }

      @Override
      public String getResearchCategory() {
         return cat;
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         if (data.categoryTotals.isEmpty()) return false;
         int hVal = 0;
         String hKey = "";
         for (String category : data.categoryTotals.keySet()) {
            int q = data.getTotal(category);
            if (q > hVal) {
               hVal = q;
               hKey = category;
            }
         }
         cat = hKey;
         amt = 10 + hVal / 2;
         return true;
      }

      @Override
      public int getInspirationCost() {
         return 2;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.inspired.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.inspired.text", amt, cat(cat));
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal(cat, amt);
         return true;
      }
   }

   /** Aid only (bookshelf): moves the lowest category's total into the highest. */
   public static class Notation extends TheorycraftCard {
      String cat1, cat2;

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         if (cat1 != null) n.putString("cat1", cat1);
         if (cat2 != null) n.putString("cat2", cat2);
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         cat1 = n.getString("cat1");
         cat2 = n.getString("cat2");
      }

      @Override
      public boolean isAidOnly() {
         return true;
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.notation.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.notation.text", cat(cat1), cat(cat2));
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         if (data.categoryTotals.size() < 2) return false;
         int lVal = Integer.MAX_VALUE, hVal = 0;
         String lKey = "", hKey = "";
         for (String category : data.categoryTotals.keySet()) {
            int q = data.getTotal(category);
            if (q < lVal) {
               lVal = q;
               lKey = category;
            }
            if (q > hVal) {
               hVal = q;
               hKey = category;
            }
         }
         if (hKey.equals(lKey) || lVal <= 0) return false;
         cat1 = lKey;
         cat2 = hKey;
         return true;
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         if (cat1 == null || cat2 == null) return false;
         int lVal = data.getTotal(cat1);
         data.addTotal(cat1, -lVal);
         data.addTotal(cat2, lVal / 2 + rnd(player, 0, lVal / 2));
         return true;
      }
   }

   public static class Ponder extends TheorycraftCard {
      @Override
      public int getInspirationCost() {
         return 2;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.ponder.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.ponder.text");
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         return data.categoriesBlocked.size() < data.categoryTotals.size();
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         int a = 25;
         int tries = 0;
         while (a > 0 && tries++ < 1000) {
            for (String category : List.copyOf(data.categoryTotals.keySet())) {
               if (data.categoriesBlocked.contains(category)) {
                  if (data.categoryTotals.size() <= 1) return false;
               } else {
                  data.addTotal(category, 1);
                  if (--a <= 0) break;
               }
            }
         }
         data.addTotal("BASICS", 5);
         data.bonusDraws++;
         return a != 20;
      }
   }

   public static class Reject extends TheorycraftCard {
      String cat1;

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         if (cat1 != null) n.putString("cat", cat1);
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         cat1 = n.getString("cat");
      }

      @Override
      public int getInspirationCost() {
         return 0;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.reject.name", catTitle(cat1));
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.reject.text", cat(cat1));
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         List<String> s = new ArrayList<>();
         for (String c : data.categoryTotals.keySet()) {
            if (!data.categoriesBlocked.contains(c)) s.add(c);
         }
         if (s.isEmpty()) return false;
         cat1 = s.get(new Random(getSeed()).nextInt(s.size()));
         return true;
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         if (cat1 == null) return false;
         data.addTotal("BASICS", 5);
         data.categoriesBlocked.add(cat1);
         return true;
      }
   }

   public static class Rethink extends TheorycraftCard {
      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         int a = 0;
         for (String category : data.categoryTotals.keySet()) a += data.getTotal(category);
         return a >= 10;
      }

      @Override
      public int getInspirationCost() {
         return -1;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.rethink.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.rethink.text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         if (!initialize(player, data)) return false;
         int a = 0;
         for (String category : data.categoryTotals.keySet()) a += data.getTotal(category);
         a = Math.min(a, 10);
         int tries = 0;
         while (a > 0 && tries++ < 1000) {
            for (String category : List.copyOf(data.categoryTotals.keySet())) {
               data.addTotal(category, -1);
               if (--a <= 0 || !data.hasTotal(category)) break;
            }
         }
         data.bonusDraws++;
         data.addTotal("BASICS", rnd(player, 1, 10));
         return true;
      }
   }

   /** Aid only (bookshelf). */
   public static class Study extends TheorycraftCard {
      String cat = "BASICS";

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         n.putString("cat", cat);
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         cat = n.getString("cat");
      }

      @Override
      public String getResearchCategory() {
         return cat;
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         List<String> list = data.getAvailableCategories(player);
         if (list.isEmpty()) return false;
         cat = list.get(new Random(getSeed()).nextInt(list.size()));
         return true;
      }

      @Override
      public boolean isAidOnly() {
         return true;
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.study.name", catTitle(cat));
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.study.text", cat(cat));
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal(cat, rnd(player, 15, 25));
         return true;
      }
   }

   // ------------------------------------------------------------------ simple category cards

   /** A card worth a fixed amount in its category, maybe with a bonus draw (Calibrate, Focus, Measure...). */
   static class Simple extends TheorycraftCard {
      final String key, category;
      final int amount;
      final boolean bonusDraw;

      Simple(String key, String category, int amount, boolean bonusDraw) {
         this.key = key;
         this.category = category;
         this.amount = amount;
         this.bonusDraw = bonusDraw;
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return category;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card." + key + ".name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card." + key + ".text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal(category, amount);
         if (bonusDraw) data.bonusDraws++;
         return true;
      }
   }

   public static class Calibrate extends Simple {
      public Calibrate() {
         super("calibrate", "ARTIFICE", 15, true);
      }
   }

   public static class Focus extends Simple {
      public Focus() {
         super("focus", "AUROMANCY", 15, true);
      }
   }

   public static class Measure extends Simple {
      public Measure() {
         super("measure", "INFUSION", 15, true);
      }
   }

   public static class Awareness extends Simple {
      public Awareness() {
         super("awareness", "AUROMANCY", 20, false);
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal(category, 20);
         if (player.getRandom().nextFloat() < 0.33F) {
            data.addTotal("ELDRITCH", rnd(player, 1, 5));
            warp(player, 1, PlayerWarp.Type.NORMAL);
         }
         return true;
      }
   }

   public static class Sculpting extends Simple {
      public Sculpting() {
         super("sculpting", "GOLEMANCY", 20, true);
      }

      @Override
      public ItemStack[] getRequiredItems() {
         return new ItemStack[]{new ItemStack(Items.CLAY_BALL)};
      }

      @Override
      public boolean[] getRequiredItemsConsumed() {
         return new boolean[]{true};
      }
   }

   public static class Spellbinding extends Simple {
      public Spellbinding() {
         super("spellbinding", "AUROMANCY", 0, false);
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         return player.experienceLevel > 0;
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         if (player.experienceLevel <= 0) return false;
         int l = Math.min(5, player.experienceLevel);
         data.addTotal(category, l * 5);
         player.giveExperienceLevels(-l);
         return true;
      }
   }

   public static class Scripting extends Simple {
      public Scripting() {
         super("scripting", "GOLEMANCY", 25, false);
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         if (data.table instanceof ResearchTableBlockEntity t && t.hasInk() && t.hasPaper()) {
            t.consumeInk();
            t.consumePaper();
            data.addTotal(category, 25);
            return true;
         }
         return false;
      }
   }

   public static class Synergy extends Simple {
      public Synergy() {
         super("synergy", "GOLEMANCY", 30, false);
      }

      private static int tot(ResearchTableData data) {
         return data.getTotal("ARTIFICE") + data.getTotal("ALCHEMY") + data.getTotal("INFUSION");
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         return tot(data) >= 15;
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         if (tot(data) < 15) return false;
         int tot = 15;
         int tries = 0;
         while (tot > 0 && tries++ < 1000) {
            for (String category : new String[]{"ARTIFICE", "ALCHEMY", "INFUSION"}) {
               if (data.getTotal(category) > 0) {
                  data.addTotal(category, -1);
                  if (--tot <= 0) break;
               }
            }
         }
         data.addTotal("GOLEMANCY", 30);
         data.penaltyStart++;
         return true;
      }
   }

   // ------------------------------------------------------------------ item cards

   /** Needs a vis crystal of a random compound aspect (kept). */
   public static class Concentrate extends TheorycraftCard {
      Aspect aspect;

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         if (aspect != null) n.putString("aspect", aspect.getTag());
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         aspect = Aspect.getAspect(n.getString("aspect"));
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         List<Aspect> c = compounds();
         aspect = c.get(new Random(getSeed()).nextInt(c.size()));
         return true;
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return "ALCHEMY";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.concentrate.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.concentrate.text", bold(aspect.getName()));
      }

      @Override
      public ItemStack[] getRequiredItems() {
         return new ItemStack[]{VisCrystalItem.of(aspect, 1)};
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal("ALCHEMY", 15);
         data.bonusDraws++;
         if (player.getRandom().nextFloat() < 0.33F) data.addInspiration(1);
         return true;
      }
   }

   public static class Reactions extends TheorycraftCard {
      Aspect aspect1, aspect2;

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         if (aspect1 != null) n.putString("aspect1", aspect1.getTag());
         if (aspect2 != null) n.putString("aspect2", aspect2.getTag());
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         aspect1 = Aspect.getAspect(n.getString("aspect1"));
         aspect2 = Aspect.getAspect(n.getString("aspect2"));
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         Random r = new Random(getSeed());
         List<Aspect> c = compounds();
         int num = Mth.nextInt(net.minecraft.util.RandomSource.create(r.nextLong()), 0, c.size() - 1);
         int num2 = num;
         while (num2 == num) num2 = r.nextInt(c.size());
         aspect1 = c.get(num);
         aspect2 = c.get(num2);
         return true;
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return "ALCHEMY";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.reactions.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.reactions.text", bold(aspect1.getName()), bold(aspect2.getName()));
      }

      @Override
      public ItemStack[] getRequiredItems() {
         return new ItemStack[]{VisCrystalItem.of(aspect1, 1), VisCrystalItem.of(aspect2, 1)};
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal("ALCHEMY", 25);
         if (player.getRandom().nextFloat() < 0.33F) data.addInspiration(1);
         return true;
      }
   }

   /** Combines the crystals of a compound's two components into the compound's crystal. */
   public static class Synthesis extends TheorycraftCard {
      Aspect aspect1, aspect2, aspect3;

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         if (aspect3 != null) n.putString("aspect", aspect3.getTag());
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         aspect3 = Aspect.getAspect(n.getString("aspect"));
         if (aspect3 != null) {
            aspect1 = aspect3.getComponents()[0];
            aspect2 = aspect3.getComponents()[1];
         }
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         List<Aspect> c = compounds();
         aspect3 = c.get(new Random(getSeed()).nextInt(c.size()));
         aspect1 = aspect3.getComponents()[0];
         aspect2 = aspect3.getComponents()[1];
         return true;
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return "ALCHEMY";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.synthesis.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.synthesis.text", bold(aspect1.getName()), bold(aspect2.getName()));
      }

      @Override
      public ItemStack[] getRequiredItems() {
         return new ItemStack[]{VisCrystalItem.of(aspect1, 1), VisCrystalItem.of(aspect2, 1)};
      }

      @Override
      public boolean[] getRequiredItemsConsumed() {
         return new boolean[]{true, true};
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal("ALCHEMY", 40);
         if (player.getRandom().nextFloat() < 0.33F) data.addInspiration(1);
         ItemStack res = VisCrystalItem.of(aspect3, 1);
         if (!player.getInventory().add(res)) player.drop(res, true);
         return true;
      }
   }

   /** Needs a phial of a compound aspect (kept). Phials arrive with the essentia milestone. */
   public static class Channel extends TheorycraftCard {
      Aspect aspect;

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         if (aspect != null) n.putString("aspect", aspect.getTag());
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         aspect = Aspect.getAspect(n.getString("aspect"));
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         List<Aspect> c = compounds();
         aspect = c.get(new Random(getSeed()).nextInt(c.size()));
         return !phial(aspect).isEmpty();
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return "INFUSION";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.channel.name", Component.literal(aspect.getName()).withStyle(ChatFormatting.DARK_BLUE));
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.channel.text", bold(aspect.getName()));
      }

      @Override
      public ItemStack[] getRequiredItems() {
         return new ItemStack[]{phial(aspect)};
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal("INFUSION", 25);
         return true;
      }
   }

   /** Infuse: an item and a phial, both used up; worth more for items with more aspects. */
   public static class Infuse extends TheorycraftCard {
      static final String[] OPTIONS = {"thaumcraft:alumentum", "thaumcraft:nitor_yellow", "thaumcraft:amber", "thaumcraft:brain", "thaumcraft:fabric",
         "thaumcraft:salis_mundus", "thaumcraft:ingot_thaumium", "thaumcraft:ingot_brass", "thaumcraft:quicksilver", "minecraft:gold_ingot",
         "minecraft:iron_ingot", "minecraft:diamond", "minecraft:emerald", "minecraft:blaze_rod", "minecraft:leather", "minecraft:white_wool",
         "minecraft:brick", "minecraft:arrow", "minecraft:egg", "minecraft:feather", "minecraft:glowstone_dust", "minecraft:redstone",
         "minecraft:ghast_tear", "minecraft:gunpowder", "minecraft:bow", "minecraft:golden_sword", "minecraft:iron_sword", "minecraft:iron_pickaxe",
         "minecraft:golden_pickaxe", "minecraft:quartz", "minecraft:apple"};
      Aspect aspect;
      ItemStack stack = ItemStack.EMPTY;
      int val = 10;

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         if (aspect != null) n.putString("aspect", aspect.getTag());
         n.putString("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
         n.putInt("val", val);
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         aspect = Aspect.getAspect(n.getString("aspect"));
         stack = item(n.getString("item"));
         val = n.getInt("val");
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         Random r = new Random(getSeed());
         List<Aspect> c = compounds();
         aspect = c.get(r.nextInt(c.size()));
         stack = item(OPTIONS[r.nextInt(OPTIONS.length)]);
         val = 10 + (int) (Math.sqrt(visSize(player, stack)) * 1.5);
         return aspect != null && !stack.isEmpty() && !phial(aspect).isEmpty();
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return "INFUSION";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.infuse.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.infuse.text", bold(aspect.getName()), stack.getHoverName(), val);
      }

      @Override
      public ItemStack[] getRequiredItems() {
         return new ItemStack[]{stack, phial(aspect)};
      }

      @Override
      public boolean[] getRequiredItemsConsumed() {
         return new boolean[]{true, true};
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal("INFUSION", val);
         return true;
      }
   }

   /** MindOverMatter (item used up) and Tinker (item kept, worth twice as much): mechanical things. */
   public static class MindOverMatter extends TheorycraftCard {
      static final String[] OPTIONS = {"thaumcraft:vis_resonator", "thaumcraft:thaumometer", "minecraft:anvil", "minecraft:activator_rail",
         "minecraft:dispenser", "minecraft:dropper", "minecraft:enchanting_table", "minecraft:ender_chest", "minecraft:jukebox",
         "minecraft:daylight_detector", "minecraft:piston", "minecraft:hopper", "minecraft:sticky_piston", "minecraft:map", "minecraft:compass",
         "minecraft:tnt_minecart", "minecraft:comparator", "minecraft:clock"};
      ItemStack stack = ItemStack.EMPTY;
      int val;

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         n.putString("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
         n.putInt("val", val);
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         stack = item(n.getString("item"));
         val = n.getInt("val");
      }

      int base() {
         return 10;
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         int start = new Random(getSeed()).nextInt(OPTIONS.length);
         for (int i = 0; i < OPTIONS.length && stack.isEmpty(); i++) stack = item(OPTIONS[(start + i) % OPTIONS.length]);
         val = base() + (int) Math.sqrt(visSize(player, stack));
         return !stack.isEmpty();
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return "ARTIFICE";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.mindmatter.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.mindmatter.text", val);
      }

      @Override
      public ItemStack[] getRequiredItems() {
         return new ItemStack[]{stack};
      }

      @Override
      public boolean[] getRequiredItemsConsumed() {
         return new boolean[]{true};
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal("ARTIFICE", val);
         return true;
      }
   }

   public static class Tinker extends MindOverMatter {
      @Override
      int base() {
         return 0;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.tinker.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.tinker.text", val * 2, val * 2 + 10);
      }

      @Override
      public boolean[] getRequiredItemsConsumed() {
         return new boolean[]{false};
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         int q = val * 2;
         data.addTotal("ARTIFICE", rnd(player, q, q + 10));
         return true;
      }
   }

   /** Uses up a curio from the inventory; its kind decides the category. */
   public static class Curio extends TheorycraftCard {
      ItemStack curio = ItemStack.EMPTY;

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         n.putString("item", BuiltInRegistries.ITEM.getKey(curio.getItem()).toString());
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         curio = item(n.getString("item"));
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.curio.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.curio.text");
      }

      @Override
      public ItemStack[] getRequiredItems() {
         return new ItemStack[]{curio};
      }

      @Override
      public boolean[] getRequiredItemsConsumed() {
         return new boolean[]{true};
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         List<ItemStack> curios = new ArrayList<>();
         for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof thaumcraft.common.items.CurioItem) curios.add(s.copyWithCount(1));
         }
         if (!curios.isEmpty()) curio = curios.get(new Random(getSeed()).nextInt(curios.size()));
         return !curio.isEmpty();
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal("BASICS", 5);
         data.addTotal(randomCategory(player), 5);
         String type = curio.getItem() instanceof thaumcraft.common.items.CurioItem c ? c.variant : "";
         switch (type) {
            case "arcane" -> data.addTotal("AUROMANCY", rnd(player, 25, 35));
            case "preserved" -> data.addTotal("ALCHEMY", rnd(player, 25, 35));
            case "ancient" -> data.addTotal("GOLEMANCY", rnd(player, 25, 35));
            case "eldritch" -> data.addTotal("ELDRITCH", rnd(player, 25, 35));
            case "knowledge" -> data.addTotal("INFUSION", rnd(player, 25, 35));
            case "twisted" -> data.addTotal("ARTIFICE", rnd(player, 25, 35));
            case "rites" -> {
               data.addTotal("ELDRITCH", rnd(player, 15, 20));
               data.addTotal("AUROMANCY", rnd(player, 10, 15));
            }
            default -> data.addTotal("BASICS", rnd(player, 25, 35));
         }
         if (player.getRandom().nextBoolean()) data.bonusDraws++;
         if (player.getRandom().nextBoolean()) data.bonusDraws++;
         return true;
      }
   }

   /** Two different celestial notes (used up), for those who know CELESTIALSCANNING. */
   public static class Celestial extends TheorycraftCard {
      int md1, md2;
      String cat = "BASICS";

      @Override
      public CompoundTag serialize() {
         CompoundTag n = super.serialize();
         n.putInt("md1", md1);
         n.putInt("md2", md2);
         n.putString("cat", cat);
         return n;
      }

      @Override
      public void deserialize(CompoundTag n) {
         super.deserialize(n);
         md1 = n.getInt("md1");
         md2 = n.getInt("md2");
         cat = n.getString("cat");
      }

      @Override
      public String getResearchCategory() {
         return cat;
      }

      @Override
      public boolean initialize(Player player, ResearchTableData data) {
         if (data.categoryTotals.isEmpty() || !ResearchManager.knowsResearch(player, "CELESTIALSCANNING")) return false;
         Random r = new Random(getSeed());
         md1 = r.nextInt(13);
         md2 = md1;
         while (md1 == md2) md2 = r.nextInt(13);
         int hVal = 0;
         String hKey = "";
         for (String category : data.categoryTotals.keySet()) {
            int q = data.getTotal(category);
            if (q > hVal) {
               hVal = q;
               hKey = category;
            }
         }
         cat = hKey;
         return true;
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.celestial.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.celestial.text", cat(cat));
      }

      @Override
      public ItemStack[] getRequiredItems() {
         return new ItemStack[]{thaumcraft.common.items.CelestialNotesItem.of(md1), thaumcraft.common.items.CelestialNotesItem.of(md2)};
      }

      @Override
      public boolean[] getRequiredItemsConsumed() {
         return new boolean[]{true, true};
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal(cat, rnd(player, 25, 50));
         boolean sun = md1 == 0 || md2 == 0;
         boolean moon = md1 > 4 || md2 > 4;
         boolean stars = md1 > 0 && md1 < 5 || md2 > 0 && md2 < 5;
         if (stars) {
            int amt = rnd(player, 0, 5);
            data.addTotal("ELDRITCH", amt * 2);
            warp(player, amt, PlayerWarp.Type.TEMPORARY);
         }
         if (sun) data.penaltyStart++;
         if (moon) data.bonusDraws++;
         return true;
      }
   }

   // ------------------------------------------------------------------ aid cards

   /** Aid only (beacon): gives inspiration back, a bonus draw and delays the penalty. */
   public static class Beacon extends TheorycraftCard {
      @Override
      public int getInspirationCost() {
         return -2;
      }

      @Override
      public boolean isAidOnly() {
         return true;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.beacon.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.beacon.text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.bonusDraws++;
         data.penaltyStart++;
         return true;
      }
   }

   public static class DragonEgg extends TheorycraftCard {
      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public boolean isAidOnly() {
         return true;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.dragonegg.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.dragonegg.text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         for (int a = 0; a < 10; a++) data.addTotal(randomCategory(player), rnd(player, 2, 5));
         return true;
      }
   }

   public static class Enchantment extends TheorycraftCard {
      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public boolean isAidOnly() {
         return true;
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.enchantment.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.enchantment.text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         if (player.experienceLevel < 5) return false;
         player.giveExperienceLevels(-5);
         data.addTotal("INFUSION", rnd(player, 15, 20));
         data.addTotal("AUROMANCY", rnd(player, 15, 20));
         return true;
      }
   }

   public static class DarkWhispers extends TheorycraftCard {
      @Override
      public boolean isAidOnly() {
         return true;
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return "ELDRITCH";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.darkwhisper.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.darkwhisper.text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         int l = player.experienceLevel;
         player.giveExperienceLevels(-(10 + l));
         if (l > 0) {
            for (String k : ResearchCategories.CATEGORIES.keySet()) {
               if (!player.getRandom().nextBoolean()) data.addTotal(k, rnd(player, 0, Math.max(1, (int) Math.sqrt(l))));
            }
         }
         data.addTotal("ELDRITCH", rnd(player, Math.max(1, l / 5), Math.max(5, l / 2)));
         warp(player, Math.max(1, (int) Math.sqrt(l)), PlayerWarp.Type.NORMAL);
         if (player.getRandom().nextBoolean()) data.bonusDraws++;
         return true;
      }
   }

   public static class Glyphs extends TheorycraftCard {
      @Override
      public boolean isAidOnly() {
         return true;
      }

      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return "ELDRITCH";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.glyph.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.glyph.text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal(randomCategory(player), rnd(player, 10, 20));
         data.addTotal("ELDRITCH", rnd(player, 10, 20));
         warp(player, 5, PlayerWarp.Type.TEMPORARY);
         return true;
      }
   }

   public static class Portal extends TheorycraftCard {
      @Override
      public boolean isAidOnly() {
         return true;
      }

      @Override
      public int getInspirationCost() {
         return -1;
      }

      @Override
      public String getResearchCategory() {
         return "ELDRITCH";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.portal.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.portal.text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal(randomCategory(player), rnd(player, 5, 10));
         data.addTotal(randomCategory(player), rnd(player, 5, 10));
         data.addTotal("ELDRITCH", rnd(player, 5, 10));
         data.bonusDraws += 2;
         warp(player, 5, PlayerWarp.Type.TEMPORARY);
         warp(player, 1, PlayerWarp.Type.NORMAL);
         return true;
      }
   }

   public static class Realization extends TheorycraftCard {
      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return "ELDRITCH";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.realization.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.realization.text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal(randomCategory(player), rnd(player, 5, 10));
         data.addTotal(randomCategory(player), rnd(player, 5, 10));
         data.addTotal("ELDRITCH", 15);
         warp(player, 5, PlayerWarp.Type.TEMPORARY);
         if (player.getRandom().nextBoolean()) warp(player, 1, PlayerWarp.Type.NORMAL);
         return true;
      }
   }

   public static class Revelation extends TheorycraftCard {
      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return "ELDRITCH";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.revelation.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.revelation.text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal(randomCategory(player), rnd(player, 5, 10));
         data.addTotal("ELDRITCH", 30);
         warp(player, 5, PlayerWarp.Type.TEMPORARY);
         warp(player, 1, PlayerWarp.Type.NORMAL);
         data.penaltyStart++;
         return true;
      }
   }

   public static class Truth extends TheorycraftCard {
      @Override
      public int getInspirationCost() {
         return 1;
      }

      @Override
      public String getResearchCategory() {
         return "ELDRITCH";
      }

      @Override
      public Component getLocalizedName() {
         return Component.translatable("card.truth.name");
      }

      @Override
      public Component getLocalizedText() {
         return Component.translatable("card.truth.text");
      }

      @Override
      public boolean activate(Player player, ResearchTableData data) {
         data.addTotal("ELDRITCH", rnd(player, 10, 25));
         data.bonusDraws++;
         warp(player, 3, PlayerWarp.Type.TEMPORARY);
         return true;
      }
   }

   // ------------------------------------------------------------------ registration (ConfigResearch.initTheorycraft)

   private static void card(String key, Supplier<TheorycraftCard> s) {
      TheorycraftManager.registerCard(key, s);
   }

   public static void register() {
      card("Study", Study::new);
      card("Analyze", Analyze::new);
      card("Balance", Balance::new);
      card("Notation", Notation::new);
      card("Ponder", Ponder::new);
      card("Rethink", Rethink::new);
      card("Reject", Reject::new);
      card("Experimentation", Experimentation::new);
      card("Curio", Curio::new);
      card("Inspired", Inspired::new);
      card("Enchantment", Enchantment::new);
      card("Beacon", Beacon::new);
      card("Celestial", Celestial::new);
      card("Concentrate", Concentrate::new);
      card("Reactions", Reactions::new);
      card("Synthesis", Synthesis::new);
      card("Calibrate", Calibrate::new);
      card("MindOverMatter", MindOverMatter::new);
      card("Tinker", Tinker::new);
      card("Measure", Measure::new);
      card("Channel", Channel::new);
      card("Infuse", Infuse::new);
      card("Focus", Focus::new);
      card("Awareness", Awareness::new);
      card("Spellbinding", Spellbinding::new);
      card("Sculpting", Sculpting::new);
      card("Scripting", Scripting::new);
      card("Synergy", Synergy::new);
      card("DarkWhispers", DarkWhispers::new);
      card("Glyphs", Glyphs::new);
      card("Portal", Portal::new);
      card("Revelation", Revelation::new);
      card("Realization", Realization::new);
      // Aid cards the original never registered (so they could not be drawn); registered here, aid-only.
      card("Truth", () -> new Truth() {
         @Override
         public boolean isAidOnly() {
            return true;
         }
      });
      card("DragonEgg", DragonEgg::new);
      Aids.register();
   }

   @Nullable
   static Supplier<TheorycraftCard> supplier(String key) {
      return TheorycraftManager.CARDS.get(key);
   }
}
