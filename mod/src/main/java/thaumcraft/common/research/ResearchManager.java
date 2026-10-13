package thaumcraft.common.research;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.EnumResearchFlag;
import thaumcraft.api.research.Knowledge;
import thaumcraft.api.research.ResearchAddendum;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.InventoryHelper;
import thaumcraft.common.network.KnowledgeGainPayload;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.network.SyncKnowledgePayload;
import thaumcraft.common.network.SyncWarpPayload;
import thaumcraft.common.registers.ModAttachments;
import thaumcraft.common.registers.ModSounds;

/** Port of ResearchManager (research progression) and the research parts of CommandThaumcraft and InternalMethodHandler. */
public final class ResearchManager {
   /** Items some research stage wants crafted; crafting one adds "[#]" + its key as research. */
   private static Set<String> craftingReferences = Set.of();
   /** Players whose knowledge changed and must be synced at the end of the tick. */
   private static final Set<ServerPlayer> DIRTY = Collections.newSetFromMap(new ConcurrentHashMap<>());
   /** Set by the Thaumonomicon's "complete stage" request: progress without raising the RESEARCH flag. */
   private static boolean noFlags;

   private ResearchManager() {
   }

   public static PlayerKnowledge knowledge(Player player) {
      return player.getData(ModAttachments.KNOWLEDGE);
   }

   public static PlayerWarp warp(Player player) {
      return player.getData(ModAttachments.WARP);
   }

   // ------------------------------------------------------------------ crafting references

   public static void rebuildCraftingReferences() {
      Set<String> refs = new HashSet<>();
      for (ResearchEntry e : ResearchCategories.allResearch()) {
         for (ResearchStage s : e.getStages()) {
            for (ResearchItem it : s.craft()) refs.add(it.craftKey());
         }
      }
      craftingReferences = Set.copyOf(refs);
   }

   /** CraftingEvents.onCrafting: remembers crafted items that some research asks for. */
   public static void onItemCrafted(ServerPlayer player, ItemStack crafted) {
      if (crafted.isEmpty()) return;
      String key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(crafted.getItem()).toString();
      if (craftingReferences.contains(key)) completeResearch(player, "[#]" + key);
      crafted.getTags().forEach(tag -> {
         String tk = "#" + tag.location();
         if (craftingReferences.contains(tk)) completeResearch(player, "[#]" + tk);
      });
   }

   // ------------------------------------------------------------------ sync

   public static void markDirty(ServerPlayer player) {
      DIRTY.add(player);
   }

   public static void sync(ServerPlayer player) {
      DIRTY.remove(player);
      ModNetwork.sendTo(player, new SyncKnowledgePayload(knowledge(player)));
   }

   /** Called at the end of each server tick. */
   public static void flushDirty() {
      for (ServerPlayer p : Set.copyOf(DIRTY)) {
         if (!p.hasDisconnected()) sync(p);
      }
      DIRTY.clear();
   }

   // ------------------------------------------------------------------ knowledge

   public static boolean addKnowledge(Player player, EnumKnowledgeType type, @Nullable ResearchCategory category, int amount) {
      PlayerKnowledge k = knowledge(player);
      if (!type.hasFields()) category = null;
      int before = k.getKnowledge(type, category);
      if (!k.addKnowledge(type, category, amount)) return false;
      int gained = k.getKnowledge(type, category) - before;
      if (player instanceof ServerPlayer sp) {
         for (int a = 0; a < gained && amount > 0; a++) {
            ModNetwork.sendTo(sp, new KnowledgeGainPayload(type, category == null ? "" : category.key));
         }
         markDirty(sp);
      }
      return true;
   }

   // ------------------------------------------------------------------ queries

   /** ThaumcraftCapabilities.knowsResearch: all known (stage suffix honoured); supports "a&&b" and "a||b". */
   public static boolean knowsResearch(Player player, String... research) {
      PlayerKnowledge k = knowledge(player);
      for (String r : research) {
         if (r.contains("&&")) {
            if (!knowsResearch(player, r.split("&&"))) return false;
         } else if (r.contains("||")) {
            boolean any = false;
            for (String s : r.split("\\|\\|")) any |= knowsResearch(player, s);
            if (!any) return false;
         } else if (!k.isResearchKnown(r)) {
            return false;
         }
      }
      return true;
   }

   /** knowsResearchStrict: like knowsResearch, but entries without a stage suffix must be complete. */
   public static boolean knowsResearchStrict(Player player, String... research) {
      PlayerKnowledge k = knowledge(player);
      for (String r : research) {
         if (r.contains("&&")) {
            if (!knowsResearchStrict(player, r.split("&&"))) return false;
         } else if (r.contains("||")) {
            boolean any = false;
            for (String s : r.split("\\|\\|")) any |= knowsResearchStrict(player, s);
            if (!any) return false;
         } else if (r.contains("@")) {
            if (!k.isResearchKnown(r)) return false;
         } else if (!k.isResearchComplete(r)) {
            return false;
         }
      }
      return true;
   }

   public static boolean doesPlayerHaveRequisites(Player player, String key) {
      ResearchEntry ri = ResearchCategories.getResearch(key);
      if (ri == null || ri.getParents().isEmpty()) return true;
      return knowsResearchStrict(player, ri.getParentsStripped().toArray(new String[0]));
   }

   // ------------------------------------------------------------------ progression

   public static boolean completeResearch(Player player, String key) {
      return completeResearch(player, key, true);
   }

   public static boolean completeResearch(Player player, String key, boolean sync) {
      boolean b = false;
      while (progressResearch(player, key, sync)) b = true;
      return b;
   }

   public static boolean startResearchWithPopup(Player player, String key) {
      boolean b = progressResearch(player, key, true);
      if (b) {
         knowledge(player).setResearchFlag(key, EnumResearchFlag.POPUP);
         knowledge(player).setResearchFlag(key, EnumResearchFlag.RESEARCH);
      }
      return b;
   }

   public static boolean progressResearch(Player player, String key) {
      return progressResearch(player, key, true);
   }

   /** Advances a research by one stage (or simply learns a non-entry key). */
   public static boolean progressResearch(Player player, String key, boolean sync) {
      PlayerKnowledge knowledge = knowledge(player);
      if (knowledge.isResearchComplete(key) || !doesPlayerHaveRequisites(player, key)) return false;
      if (!knowledge.isResearchKnown(key)) knowledge.addResearch(key);
      ResearchEntry re = ResearchCategories.getResearch(key);
      if (re != null) {
         boolean popups = true;
         var stages = re.getStages();
         if (!stages.isEmpty()) {
            int cs = knowledge.getResearchStage(key);
            ResearchStage currentStage = null;
            if (cs > 0) {
               cs = Math.min(cs, stages.size());
               currentStage = stages.get(cs - 1);
            }
            // Stages without requirements are skipped straight over.
            if (stages.size() == 1 && cs == 0 && !stages.get(0).hasRequirements()) {
               cs++;
            } else if (stages.size() > 1 && stages.size() <= cs + 1 && cs < stages.size() && !stages.get(cs).hasRequirements()) {
               cs++;
            }
            knowledge.setResearchStage(key, Math.min(stages.size() + 1, cs + 1));
            popups = cs >= stages.size();
            int warp = currentStage != null ? currentStage.warp() : 0;
            if (popups) {
               cs = Math.min(cs, stages.size());
               currentStage = stages.get(cs - 1);
            }
            if (currentStage != null) {
               warp += currentStage.warp();
               if (warp > 0 && !ModConfig.wussMode() && !player.level().isClientSide) {
                  if (warp > 1) {
                     int w2 = warp / 2;
                     if (warp - w2 > 0) addWarp(player, warp - w2, PlayerWarp.Type.PERMANENT);
                     if (w2 > 0) addWarp(player, w2, PlayerWarp.Type.NORMAL);
                  } else {
                     addWarp(player, warp, PlayerWarp.Type.PERMANENT);
                  }
               }
            }
         }
         if (popups) {
            if (sync) {
               knowledge.setResearchFlag(key, EnumResearchFlag.POPUP);
               if (!noFlags) knowledge.setResearchFlag(key, EnumResearchFlag.RESEARCH);
               else noFlags = false;
               for (ItemStack rs : re.getRewardItem()) {
                  if (!player.getInventory().add(rs.copy())) player.drop(rs.copy(), false);
               }
               for (Knowledge rk : re.getRewardKnow()) {
                  addKnowledge(player, rk.type(), rk.category(), rk.type().getProgression() * rk.amount());
               }
            }
            for (ResearchEntry ri : ResearchCategories.allResearch()) {
               if (ri.getAddenda().isEmpty() || !knowledge.isResearchComplete(ri.getKey())) continue;
               for (ResearchAddendum addendum : ri.getAddenda()) {
                  if (addendum.research().contains(key)) {
                     player.sendSystemMessage(Component.translatable("tc.addaddendum", ri.getLocalizedName()));
                     knowledge.setResearchFlag(ri.getKey(), EnumResearchFlag.PAGE);
                     break;
                  }
               }
            }
         }
      }
      if (re != null) {
         for (String sibling : re.getSiblings()) {
            if (!knowledge.isResearchComplete(sibling) && doesPlayerHaveRequisites(player, sibling)) completeResearch(player, sibling, sync);
         }
      }
      if (sync && player instanceof ServerPlayer sp) {
         markDirty(sp);
         if (re != null) player.giveExperiencePoints(5);
      }
      return true;
   }

   /**
    * PacketSyncProgressToServer: the player asks to complete the current stage of an entry from the Thaumonomicon.
    * Checks (and takes) the stage's requirements, then progresses the research.
    */
   public static boolean tryCompleteStage(ServerPlayer player, String key, boolean checks, boolean flagless) {
      if (checks && !checkAndConsumeRequisites(player, key)) return false;
      if (flagless) noFlags = true;
      boolean b = progressResearch(player, key);
      noFlags = false;
      return b;
   }

   private static boolean checkAndConsumeRequisites(ServerPlayer player, String key) {
      ResearchEntry research = ResearchCategories.getResearch(key);
      if (research == null || research.getStages().isEmpty()) return true;
      PlayerKnowledge knowledge = knowledge(player);
      int currentStage = knowledge.getResearchStage(key) - 1;
      if (currentStage < 0) return false;
      if (currentStage >= research.getStages().size()) return true;
      ResearchStage stage = research.getStages().get(currentStage);
      for (ResearchItem o : stage.obtain()) {
         if (!InventoryHelper.isCarrying(player, o)) return false;
      }
      for (ResearchItem c : stage.craft()) {
         if (!knowledge.isResearchKnown("[#]" + c.craftKey())) return false;
      }
      for (String r : stage.research()) {
         if (!knowsResearchStrict(player, r)) return false;
      }
      for (Knowledge k : stage.know()) {
         if (knowledge.getKnowledge(k.type(), k.category()) < k.amount()) return false;
      }
      for (ResearchItem o : stage.obtain()) InventoryHelper.consume(player, o);
      for (Knowledge k : stage.know()) addKnowledge(player, k.type(), k.category(), -k.amount() * k.type().getProgression());
      return true;
   }

   // ------------------------------------------------------------------ admin (CommandThaumcraft)

   public static void giveRecursiveResearch(Player player, String research) {
      if (research.contains("@")) research = research.substring(0, research.indexOf('@'));
      ResearchEntry res = ResearchCategories.getResearch(research);
      PlayerKnowledge knowledge = knowledge(player);
      if (knowledge.isResearchComplete(research)) return;
      if (res != null) {
         for (String p : res.getParentsStripped()) giveRecursiveResearch(player, p);
         for (ResearchStage page : res.getStages()) {
            for (String gr : page.research()) completeResearch(player, gr);
         }
      }
      completeResearch(player, research);
      for (ResearchEntry ri : ResearchCategories.allResearch()) {
         for (ResearchStage stage : ri.getStages()) {
            if (stage.research().contains(research)) {
               knowledge.setResearchFlag(ri.getKey(), EnumResearchFlag.PAGE);
               break;
            }
         }
      }
      if (res != null) {
         for (String s : res.getSiblings()) giveRecursiveResearch(player, s);
      }
   }

   public static void revokeRecursiveResearch(Player player, String research) {
      if (research.contains("@")) research = research.substring(0, research.indexOf('@'));
      PlayerKnowledge knowledge = knowledge(player);
      if (!knowledge.isResearchComplete(research)) return;
      for (ResearchEntry ri : ResearchCategories.allResearch()) {
         if (knowledge.isResearchComplete(ri.getKey()) && ri.getParentsStripped().contains(research)) revokeRecursiveResearch(player, ri.getKey());
      }
      knowledge.removeResearch(research);
   }

   public static void giveAllResearch(Player player) {
      for (ResearchEntry ri : ResearchCategories.allResearch()) giveRecursiveResearch(player, ri.getKey());
   }

   /** Clears everything and re-adds AUTOUNLOCK research. */
   public static void resetResearch(Player player) {
      knowledge(player).clear();
      addAutoUnlockResearch(player);
   }

   public static void addAutoUnlockResearch(Player player) {
      for (ResearchEntry ri : ResearchCategories.allResearch()) {
         if (ri.hasMeta(ResearchEntry.Meta.AUTOUNLOCK) && !knowledge(player).isResearchKnown(ri.getKey())) {
            completeResearch(player, ri.getKey(), false);
         }
      }
   }

   // ------------------------------------------------------------------ warp

   /** InternalMethodHandler.addWarpToPlayer */
   public static void addWarp(Player player, int amount, PlayerWarp.Type type) {
      if (amount == 0 || player.level().isClientSide) return;
      PlayerWarp pw = warp(player);
      int cur = pw.get(type);
      if (amount < 0 && cur + amount < 0) amount = -cur;
      pw.add(type, amount);
      if (player instanceof ServerPlayer sp) {
         String msg = switch (type) {
            case PERMANENT -> amount > 0 ? "tc.addwarp" : "tc.removewarp";
            case NORMAL -> amount > 0 ? "tc.addwarpsticky" : "tc.removewarpsticky";
            case TEMPORARY -> amount > 0 ? "tc.addwarptemp" : "tc.removewarptemp";
         };
         sp.displayClientMessage(Component.translatable(msg).withStyle(ChatFormatting.DARK_PURPLE), true);
         if (amount > 0 && type != PlayerWarp.Type.TEMPORARY) {
            sp.level().playSound(null, sp.blockPosition(), ModSounds.WHISPERS.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
         }
         ModNetwork.sendTo(sp, new SyncWarpPayload(pw.copy()));
      }
      if (amount > 0) pw.setCounter(pw.total());
      if (type != PlayerWarp.Type.TEMPORARY && knowsResearchStrict(player, "FIRSTSTEPS") && !knowsResearchStrict(player, "WARP")) {
         completeResearch(player, "WARP");
         player.displayClientMessage(Component.translatable("research.WARP.warn"), true);
      }
   }

   // ------------------------------------------------------------------ misc

   /** The compound aspect made of these two primal/compound aspects, if any (used by theorycraft). */
   @Nullable
   public static Aspect getCombinationResult(Aspect a1, Aspect a2) {
      for (Aspect aspect : Aspect.all()) {
         Aspect[] c = aspect.getComponents();
         if (c != null && c.length == 2 && (c[0] == a1 && c[1] == a2 || c[0] == a2 && c[1] == a1)) return aspect;
      }
      return null;
   }
}
