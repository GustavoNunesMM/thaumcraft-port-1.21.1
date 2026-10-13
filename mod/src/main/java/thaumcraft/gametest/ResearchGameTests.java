package thaumcraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.EnumResearchFlag;
import thaumcraft.api.research.Knowledge;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.common.crafting.SalisMundusRecipe;
import thaumcraft.common.crafting.dust.DustTrigger;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.research.PlayerKnowledge;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.research.ScanningManager;

/** Milestone 4: research data, progression and the salis mundus triggers. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class ResearchGameTests {
   @GameTest(template = "empty")
   public static void researchLoaded(GameTestHelper h) {
      int n = 0;
      for (ResearchEntry ignored : ResearchCategories.allResearch()) n++;
      h.assertTrue(n >= 140, "expected the ~148 original research entries, got " + n);
      ResearchEntry fs = ResearchCategories.getResearch("FIRSTSTEPS");
      h.assertTrue(fs != null && fs.getStages().size() == 3 && fs.getCategory().equals("BASICS"), "FIRSTSTEPS: " + fs);
      h.assertTrue(fs.getParentsClean().contains("!gotthaumonomicon") && fs.getSiblings().contains("KNOWLEDGETYPES"), "FIRSTSTEPS links");
      ResearchEntry farmer = ResearchCategories.getResearch("CRYSTALFARMER");
      h.assertTrue(farmer.getStages().get(0).obtain().size() == 6, "CRYSTALFARMER should ask for 6 vis crystals");
      h.assertTrue(VisCrystalItem.getAspect(farmer.getStages().get(0).obtain().get(0).stack()) == Aspect.AIR, "first crystal is aer");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void scanningOreUnlocksResearch(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      h.setBlock(new BlockPos(2, 1, 2), ModBlocks.ORE_AMBER.get());
      ScanningManager.scanTheThing(player, h.absolutePos(new BlockPos(2, 1, 2)));
      PlayerKnowledge k = ResearchManager.knowledge(player);
      h.assertTrue(k.isResearchComplete("ORE"), "scanning amber ore should complete ORE: " + k.getResearchList());
      h.assertTrue(k.isResearchKnown("!OREAMBER") && k.isResearchKnown("!minecraft:stone") == false, "keys: " + k.getResearchList());
      h.assertTrue(k.isResearchKnown("!thaumcraft:ore_amber"), "generic scan key");
      h.succeed();
   }

   /** Gives the player whatever the current stage asks for, the way a player would. */
   private static void satisfyStage(ServerPlayer player, ResearchStage stage) {
      for (ResearchItem c : stage.craft()) ResearchManager.onItemCrafted(player, c.display().copy());
      for (ResearchItem o : stage.obtain()) player.getInventory().add(o.display().copyWithCount(o.count()));
      for (Knowledge k : stage.know()) ResearchManager.addKnowledge(player, k.type(), k.category(), k.amount() * k.type().getProgression());
      for (String r : stage.research()) ResearchManager.completeResearch(player, r);
   }

   @GameTest(template = "empty")
   public static void firstStepsProgression(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      PlayerKnowledge k = ResearchManager.knowledge(player);
      h.assertFalse(ResearchManager.progressResearch(player, "FIRSTSTEPS"), "FIRSTSTEPS needs the Thaumonomicon first");
      k.addResearch("!gotthaumonomicon");
      h.assertTrue(ResearchManager.progressResearch(player, "FIRSTSTEPS"), "could not start FIRSTSTEPS");
      h.assertTrue(k.getResearchStage("FIRSTSTEPS") == 1, "stage after starting: " + k.getResearchStage("FIRSTSTEPS"));
      h.assertTrue(k.isResearchComplete("KNOWLEDGETYPES"), "sibling KNOWLEDGETYPES should come along");

      ResearchEntry fs = ResearchCategories.getResearch("FIRSTSTEPS");
      for (int stage = 1; stage <= 2; stage++) {
         ResearchStage s = fs.getStages().get(stage - 1);
         if (s.hasRequirements()) {
            h.assertFalse(ResearchManager.tryCompleteStage(player, "FIRSTSTEPS", true, true), "stage " + stage + " completed without its requirements");
         }
         satisfyStage(player, s);
         h.assertTrue(ResearchManager.tryCompleteStage(player, "FIRSTSTEPS", true, true), "stage " + stage + " not completed: " + s);
      }
      h.assertTrue(k.isResearchComplete("FIRSTSTEPS"), "FIRSTSTEPS should be complete, stage " + k.getResearchStage("FIRSTSTEPS"));
      h.assertTrue(k.getKnowledge(EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("BASICS")) == 0,
         "the observation point should have been spent");
      h.assertTrue(k.hasResearchFlag("FIRSTSTEPS", EnumResearchFlag.POPUP), "completion popup flag");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void stageItemsAreHandedIn(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      PlayerKnowledge k = ResearchManager.knowledge(player);
      // Parents of ESSENTIASMELTER: ALUMENTUM, METALLURGY@2
      k.addResearch("ALUMENTUM");
      k.setResearchStage("ALUMENTUM", 3);
      k.addResearch("METALLURGY");
      k.setResearchStage("METALLURGY", 2);
      h.assertTrue(ResearchManager.progressResearch(player, "ESSENTIASMELTER"), "could not start ESSENTIASMELTER");
      ResearchStage s = ResearchCategories.getResearch("ESSENTIASMELTER").getStages().get(0);
      satisfyStage(player, s);
      h.assertTrue(player.getInventory().countItem(ModItems.CRYSTAL_ESSENCE.get()) == 6, "should carry the 6 crystals");
      h.assertTrue(ResearchManager.tryCompleteStage(player, "ESSENTIASMELTER", true, true), "stage not completed");
      h.assertTrue(player.getInventory().countItem(ModItems.CRYSTAL_ESSENCE.get()) == 0, "crystals should have been handed in");
      h.assertTrue(k.getResearchStage("ESSENTIASMELTER") == 2, "stage: " + k.getResearchStage("ESSENTIASMELTER"));
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void giveAndRevokeRecursively(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      PlayerKnowledge k = ResearchManager.knowledge(player);
      ResearchManager.giveRecursiveResearch(player, "UNLOCKALCHEMY");
      h.assertTrue(k.isResearchComplete("UNLOCKALCHEMY") && k.isResearchComplete("FIRSTSTEPS"), "UNLOCKALCHEMY and its parent");
      ResearchManager.revokeRecursiveResearch(player, "FIRSTSTEPS");
      h.assertFalse(k.isResearchKnown("FIRSTSTEPS") || k.isResearchKnown("UNLOCKALCHEMY"), "revoking a parent revokes its children");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void salisMundusRecipe(GameTestHelper h) {
      SalisMundusRecipe r = new SalisMundusRecipe(CraftingBookCategory.MISC);
      List<ItemStack> good = List.of(new ItemStack(Items.FLINT), new ItemStack(Items.BOWL), new ItemStack(Items.REDSTONE),
         VisCrystalItem.of(Aspect.AIR, 1), VisCrystalItem.of(Aspect.FIRE, 1), VisCrystalItem.of(Aspect.WATER, 1));
      CraftingInput in = CraftingInput.of(3, 2, good);
      h.assertTrue(r.matches(in, h.getLevel()), "flint, bowl, redstone and 3 different crystals should match");
      NonNullList<ItemStack> rest = r.getRemainingItems(in);
      h.assertTrue(rest.get(0).is(Items.FLINT) && rest.get(1).is(Items.BOWL) && rest.get(2).isEmpty(), "flint and bowl stay: " + rest);
      List<ItemStack> same = List.of(new ItemStack(Items.FLINT), new ItemStack(Items.BOWL), new ItemStack(Items.REDSTONE),
         VisCrystalItem.of(Aspect.AIR, 1), VisCrystalItem.of(Aspect.AIR, 1), VisCrystalItem.of(Aspect.WATER, 1));
      h.assertFalse(r.matches(CraftingInput.of(3, 2, same), h.getLevel()), "crystals must be of different aspects");
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void dustedBookshelfBecomesThaumonomicon(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      BlockPos rel = new BlockPos(2, 1, 2);
      h.setBlock(rel, Blocks.BOOKSHELF);
      BlockPos pos = h.absolutePos(rel);
      DustTrigger bookshelf = DustTrigger.TRIGGERS.get(0);
      h.assertTrue(bookshelf.getValidFace(h.getLevel(), player, pos, Direction.UP) == null, "needs the dream first");
      ResearchManager.knowledge(player).addResearch("!gotdream");
      DustTrigger.Placement place = bookshelf.getValidFace(h.getLevel(), player, pos, Direction.UP);
      h.assertTrue(place != null, "bookshelf trigger should apply");
      bookshelf.execute(h.getLevel(), player, pos, place, Direction.UP);
      h.runAfterDelay(60, () -> {
         h.assertBlockNotPresent(Blocks.BOOKSHELF, rel);
         h.assertItemEntityPresent(ModItems.THAUMONOMICON.get(), rel, 1.5);
         h.succeed();
      });
   }

   @GameTest(template = "empty")
   public static void oldSavesAndWarp(GameTestHelper h) {
      CompoundTag old = new CompoundTag();
      ListTag research = new ListTag();
      research.add(StringTag.valueOf("!minecraft:stone"));
      old.put("research", research);
      CompoundTag obs = new CompoundTag();
      obs.putInt("BASICS", 40);
      old.put("observation", obs);
      PlayerKnowledge k = PlayerKnowledge.CODEC.parse(NbtOps.INSTANCE, old).getOrThrow();
      h.assertTrue(k.isResearchKnown("!minecraft:stone"), "milestone 1 research keys should load");
      h.assertTrue(k.getKnowledge(EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("BASICS")) == 2,
         "40 raw observation = 2 points");

      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchManager.addWarp(player, 4, PlayerWarp.Type.PERMANENT);
      ResearchManager.addWarp(player, 2, PlayerWarp.Type.TEMPORARY);
      PlayerWarp w = ResearchManager.warp(player);
      h.assertTrue(w.get(PlayerWarp.Type.PERMANENT) == 4 && w.total() == 6 && w.getCounter() == 6, "warp: " + w.total() + " counter " + w.getCounter());
      ResearchManager.addWarp(player, -10, PlayerWarp.Type.TEMPORARY);
      h.assertTrue(w.get(PlayerWarp.Type.TEMPORARY) == 0, "warp never goes negative");
      h.succeed();
   }
}
