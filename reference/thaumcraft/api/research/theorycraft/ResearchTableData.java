package thaumcraft.api.research.theorycraft;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;

public class ResearchTableData {
   public TileEntity table;
   public String player;
   public int inspiration;
   public int inspirationStart;
   public int bonusDraws;
   public int placedCards;
   public int aidsChosen;
   public int penaltyStart;
   public ArrayList<Long> savedCards = new ArrayList<>();
   public ArrayList<String> aidCards = new ArrayList<>();
   public TreeMap<String, Integer> categoryTotals = new TreeMap<>();
   public ArrayList<String> categoriesBlocked = new ArrayList<>();
   public ArrayList<ResearchTableData.CardChoice> cardChoices = new ArrayList<>();
   public ResearchTableData.CardChoice lastDraw;

   public ResearchTableData(TileEntity tileResearchTable) {
      this.table = tileResearchTable;
   }

   public ResearchTableData(EntityPlayer player2, TileEntity tileResearchTable) {
      this.player = player2.getName();
      this.table = tileResearchTable;
   }

   public boolean isComplete() {
      return this.inspiration <= 0;
   }

   public boolean hasTotal(String cat) {
      return this.categoryTotals.containsKey(cat);
   }

   public int getTotal(String cat) {
      return this.categoryTotals.containsKey(cat) ? this.categoryTotals.get(cat) : 0;
   }

   public void addTotal(String cat, int amt) {
      int current = this.categoryTotals.containsKey(cat) ? this.categoryTotals.get(cat) : 0;
      current += amt;
      if (current <= 0) {
         this.categoryTotals.remove(cat);
      } else {
         this.categoryTotals.put(cat, current);
      }
   }

   public void addInspiration(int amt) {
      this.inspiration += amt;
      if (this.inspiration > this.inspirationStart) {
         this.inspiration = this.inspirationStart;
      }
   }

   public NBTTagCompound serialize() {
      NBTTagCompound nbt = new NBTTagCompound();
      nbt.setString("player", this.player);
      nbt.setInteger("inspiration", this.inspiration);
      nbt.setInteger("inspirationStart", this.inspirationStart);
      nbt.setInteger("placedCards", this.placedCards);
      nbt.setInteger("bonusDraws", this.bonusDraws);
      nbt.setInteger("aidsChosen", this.aidsChosen);
      nbt.setInteger("penaltyStart", this.penaltyStart);
      NBTTagList savedTag = new NBTTagList();

      for (Long card : this.savedCards) {
         NBTTagCompound gt = new NBTTagCompound();
         gt.setLong("card", card);
         savedTag.appendTag(gt);
      }

      nbt.setTag("savedCards", savedTag);
      NBTTagList categoriesBlockedTag = new NBTTagList();

      for (String category : this.categoriesBlocked) {
         NBTTagCompound gt = new NBTTagCompound();
         gt.setString("category", category);
         categoriesBlockedTag.appendTag(gt);
      }

      nbt.setTag("categoriesBlocked", categoriesBlockedTag);
      NBTTagList categoryTotalsTag = new NBTTagList();

      for (String category : this.categoryTotals.keySet()) {
         NBTTagCompound gt = new NBTTagCompound();
         gt.setString("category", category);
         gt.setInteger("total", this.categoryTotals.get(category));
         categoryTotalsTag.appendTag(gt);
      }

      nbt.setTag("categoryTotals", categoryTotalsTag);
      NBTTagList aidCardsTag = new NBTTagList();

      for (String mc : this.aidCards) {
         NBTTagCompound gt = new NBTTagCompound();
         gt.setString("aidCard", mc);
         aidCardsTag.appendTag(gt);
      }

      nbt.setTag("aidCards", aidCardsTag);
      NBTTagList cardChoicesTag = new NBTTagList();

      for (ResearchTableData.CardChoice mc : this.cardChoices) {
         NBTTagCompound gt = this.serializeCardChoice(mc);
         cardChoicesTag.appendTag(gt);
      }

      nbt.setTag("cardChoices", cardChoicesTag);
      if (this.lastDraw != null) {
         nbt.setTag("lastDraw", this.serializeCardChoice(this.lastDraw));
      }

      return nbt;
   }

   public NBTTagCompound serializeCardChoice(ResearchTableData.CardChoice mc) {
      NBTTagCompound nbt = new NBTTagCompound();
      nbt.setString("cardChoice", mc.key);
      nbt.setBoolean("aid", mc.fromAid);
      nbt.setBoolean("select", mc.selected);

      try {
         nbt.setTag("cardNBT", mc.card.serialize());
      } catch (Exception var4) {
      }

      return nbt;
   }

   public void deserialize(NBTTagCompound nbt) {
      if (nbt != null) {
         this.inspiration = nbt.getInteger("inspiration");
         this.inspirationStart = nbt.getInteger("inspirationStart");
         this.placedCards = nbt.getInteger("placedCards");
         this.bonusDraws = nbt.getInteger("bonusDraws");
         this.aidsChosen = nbt.getInteger("aidsChosen");
         this.penaltyStart = nbt.getInteger("penaltyStart");
         this.player = nbt.getString("player");
         NBTTagList savedTag = nbt.getTagList("savedCards", 10);
         this.savedCards = new ArrayList<>();

         for (int x = 0; x < savedTag.tagCount(); x++) {
            NBTTagCompound nbtdata = savedTag.getCompoundTagAt(x);
            this.savedCards.add(nbtdata.getLong("card"));
         }

         NBTTagList categoriesBlockedTag = nbt.getTagList("categoriesBlocked", 10);
         this.categoriesBlocked = new ArrayList<>();

         for (int x = 0; x < categoriesBlockedTag.tagCount(); x++) {
            NBTTagCompound nbtdata = categoriesBlockedTag.getCompoundTagAt(x);
            this.categoriesBlocked.add(nbtdata.getString("category"));
         }

         NBTTagList categoryTotalsTag = nbt.getTagList("categoryTotals", 10);
         this.categoryTotals = new TreeMap<>();

         for (int x = 0; x < categoryTotalsTag.tagCount(); x++) {
            NBTTagCompound nbtdata = categoryTotalsTag.getCompoundTagAt(x);
            this.categoryTotals.put(nbtdata.getString("category"), nbtdata.getInteger("total"));
         }

         NBTTagList aidCardsTag = nbt.getTagList("aidCards", 10);
         this.aidCards = new ArrayList<>();

         for (int x = 0; x < aidCardsTag.tagCount(); x++) {
            NBTTagCompound nbtdata = aidCardsTag.getCompoundTagAt(x);
            this.aidCards.add(nbtdata.getString("aidCard"));
         }

         EntityPlayer pe = null;
         if (this.table != null && this.table.getWorld() != null && !this.table.getWorld().isRemote) {
            pe = this.table.getWorld().getPlayerEntityByName(this.player);
         }

         NBTTagList cardChoicesTag = nbt.getTagList("cardChoices", 10);
         this.cardChoices = new ArrayList<>();

         for (int x = 0; x < cardChoicesTag.tagCount(); x++) {
            NBTTagCompound nbtdata = cardChoicesTag.getCompoundTagAt(x);
            ResearchTableData.CardChoice cc = this.deserializeCardChoice(nbtdata);
            if (cc != null) {
               this.cardChoices.add(cc);
            }
         }

         this.lastDraw = this.deserializeCardChoice(nbt.getCompoundTag("lastDraw"));
      }
   }

   public ResearchTableData.CardChoice deserializeCardChoice(NBTTagCompound nbt) {
      if (nbt == null) {
         return null;
      } else {
         String key = nbt.getString("cardChoice");
         TheorycraftCard tc = this.generateCardWithNBT(nbt.getString("cardChoice"), nbt.getCompoundTag("cardNBT"));
         return tc == null ? null : new ResearchTableData.CardChoice(key, tc, nbt.getBoolean("aid"), nbt.getBoolean("select"));
      }
   }

   private boolean isCategoryBlocked(String cat) {
      return this.categoriesBlocked.contains(cat);
   }

   public void drawCards(int draw, EntityPlayer pe) {
      if (draw == 3) {
         if (this.bonusDraws > 0) {
            this.bonusDraws--;
         } else {
            draw = 2;
         }
      }

      this.cardChoices.clear();
      this.player = pe.getName();
      ArrayList<String> availCats = this.getAvailableCategories(pe);
      ArrayList<String> drawnCards = new ArrayList<>();
      boolean aidDrawn = false;
      int failsafe = 0;

      while (draw > 0 && failsafe < 10000) {
         failsafe++;
         if (!aidDrawn && !this.aidCards.isEmpty() && pe.getRNG().nextFloat() <= 0.25) {
            int idx = pe.getRNG().nextInt(this.aidCards.size());
            String key = this.aidCards.get(idx);
            TheorycraftCard card = this.generateCard(key, -1L, pe);
            if (card == null || card.getInspirationCost() > this.inspiration || this.isCategoryBlocked(card.getResearchCategory()) || drawnCards.contains(key)) {
               continue;
            }

            drawnCards.add(key);
            this.cardChoices.add(new ResearchTableData.CardChoice(key, card, true, false));
            this.aidCards.remove(idx);
         } else {
            try {
               String[] cards = TheorycraftManager.cards.keySet().toArray(new String[0]);
               int idx = pe.getRNG().nextInt(cards.length);
               TheorycraftCard card = this.generateCard(cards[idx], -1L, pe);
               if (card == null || card.isAidOnly() || card.getInspirationCost() > this.inspiration) {
                  continue;
               }

               if (card.getResearchCategory() != null) {
                  boolean found = false;

                  for (String cn : availCats) {
                     if (cn.equals(card.getResearchCategory())) {
                        found = true;
                        break;
                     }
                  }

                  if (!found) {
                     continue;
                  }
               }

               if (drawnCards.contains(cards[idx])) {
                  continue;
               }

               drawnCards.add(cards[idx]);
               this.cardChoices.add(new ResearchTableData.CardChoice(cards[idx], card, false, false));
            } catch (Exception var13) {
               continue;
            }
         }

         draw--;
      }
   }

   private TheorycraftCard generateCard(String key, long seed, EntityPlayer pe) {
      if (key == null) {
         return null;
      } else {
         Class<TheorycraftCard> tcc = TheorycraftManager.cards.get(key);
         if (tcc == null) {
            return null;
         } else {
            TheorycraftCard tc = null;

            try {
               tc = tcc.newInstance();
               if (seed < 0L) {
                  if (pe != null) {
                     tc.setSeed(pe.getRNG().nextLong());
                  } else {
                     tc.setSeed(System.nanoTime());
                  }
               } else {
                  tc.setSeed(seed);
               }

               if (pe != null && !tc.initialize(pe, this)) {
                  return null;
               }
            } catch (Exception var8) {
            }

            return tc;
         }
      }
   }

   private TheorycraftCard generateCardWithNBT(String key, NBTTagCompound nbt) {
      if (key == null) {
         return null;
      } else {
         Class<TheorycraftCard> tcc = TheorycraftManager.cards.get(key);
         if (tcc == null) {
            return null;
         } else {
            TheorycraftCard tc = null;

            try {
               tc = tcc.newInstance();
               tc.deserialize(nbt);
            } catch (Exception var6) {
            }

            return tc;
         }
      }
   }

   public void initialize(EntityPlayer player1, Set<String> aids) {
      this.inspirationStart = getAvailableInspiration(player1);
      this.inspiration = this.inspirationStart - aids.size();

      for (String muk : aids) {
         ITheorycraftAid mu = TheorycraftManager.aids.get(muk);
         if (mu != null) {
            for (Class clazz : mu.getCards()) {
               this.aidCards.add(clazz.getName());
            }
         }
      }
   }

   public ArrayList<String> getAvailableCategories(EntityPlayer player) {
      ArrayList<String> cats = new ArrayList<>();

      for (String rck : ResearchCategories.researchCategories.keySet()) {
         ResearchCategory rc = ResearchCategories.getResearchCategory(rck);
         if (rc != null && !this.isCategoryBlocked(rck) && (rc.researchKey == null || ThaumcraftCapabilities.knowsResearchStrict(player, rc.researchKey))) {
            cats.add(rck);
         }
      }

      return cats;
   }

   public static int getAvailableInspiration(EntityPlayer player) {
      float tot = 5.0F;
      IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);

      for (String s : knowledge.getResearchList()) {
         if (ThaumcraftCapabilities.knowsResearchStrict(player, s)) {
            ResearchEntry re = ResearchCategories.getResearch(s);
            if (re != null) {
               if (re.hasMeta(ResearchEntry.EnumResearchMeta.SPIKY)) {
                  tot += 0.5F;
               }

               if (re.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN)) {
                  tot += 0.1F;
               }
            }
         }
      }

      return Math.min(15, Math.round(tot));
   }

   public class CardChoice {
      public TheorycraftCard card;
      public String key;
      public boolean fromAid;
      public boolean selected;

      public CardChoice(String key, TheorycraftCard card, boolean aid, boolean selected) {
         this.key = key;
         this.card = card;
         this.fromAid = aid;
         this.selected = selected;
      }

      @Override
      public String toString() {
         return "key:" + this.key + " card:" + this.card.getSeed() + " fromAid:" + this.fromAid + " selected:" + this.selected;
      }
   }
}
