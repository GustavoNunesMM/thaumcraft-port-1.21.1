package thaumcraft.gametest;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.common.blocks.crafting.ResearchTableBlockEntity;
import thaumcraft.common.menu.ResearchTableMenu;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.research.ResearchManager;

/** Milestone 4c: research table and theorycraft. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class TheorycraftGameTests {
   private static ResearchTableBlockEntity tableWithTools(GameTestHelper h, ServerPlayer player, BlockPos rel) {
      h.setBlock(rel, ModBlocks.TABLE_WOOD.get());
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SCRIBING_TOOLS.get()));
      BlockPos abs = h.absolutePos(rel);
      h.getLevel().getBlockState(abs).useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND,
         new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false));
      h.assertBlockPresent(ModBlocks.RESEARCH_TABLE.get(), rel);
      return (ResearchTableBlockEntity) h.getLevel().getBlockEntity(abs);
   }

   @GameTest(template = "empty")
   public static void scribingToolsMakeResearchTable(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchTableBlockEntity be = tableWithTools(h, player, new BlockPos(2, 1, 2));
      h.assertTrue(be.getItems().getStackInSlot(0).is(ModItems.SCRIBING_TOOLS.get()), "the scribing tools should go into the table");
      h.assertTrue(player.getMainHandItem().isEmpty(), "the tools leave the hand");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void theoryGivesTheoryKnowledge(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchTableBlockEntity be = tableWithTools(h, player, new BlockPos(2, 1, 2));
      be.getItems().setStackInSlot(1, new ItemStack(Items.PAPER, 8));
      be.startNewTheory(player, Set.of());
      h.assertTrue(be.data != null && be.data.inspiration == ResearchTableData.getAvailableInspiration(player), "theory started");
      ResearchTableMenu menu = new ResearchTableMenu(0, player.getInventory(), be);
      // With nothing written yet only Experimentation can be drawn (as in the original); give the theory a start.
      be.data.categoryTotals.put("BASICS", 10);
      menu.clickMenuButton(player, 2);
      h.assertTrue(be.data.cardChoices.size() == 2, "two cards drawn: " + be.data.cardChoices.size());
      h.assertTrue(be.getItems().getStackInSlot(1).getCount() == 7, "drawing uses a sheet of paper");

      // Write the rest of the theory by hand and complete it.
      be.data.cardChoices.clear();
      be.data.categoryTotals.clear();
      be.data.categoryTotals.put("ALCHEMY", 100);
      be.data.categoryTotals.put("BASICS", 50);
      be.data.inspiration = 0;
      menu.clickMenuButton(player, 7);
      h.assertTrue(be.data == null, "completing clears the theory");
      var k = ResearchManager.knowledge(player);
      h.assertTrue(k.getKnowledge(EnumKnowledgeType.THEORY, ResearchCategories.getResearchCategory("ALCHEMY")) == 1, "100% alchemy = 1 theory point");
      h.assertTrue(k.getKnowledgeRaw(EnumKnowledgeType.THEORY, ResearchCategories.getResearchCategory("BASICS")) == 10,
         "50% basics = 16 raw, but past the penalty point only 2/3 of it");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void playingACardSpendsInspirationAndInk(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchTableBlockEntity be = tableWithTools(h, player, new BlockPos(2, 1, 2));
      be.startNewTheory(player, Set.of());
      int before = be.data.inspiration;
      TheorycraftCard card = TheorycraftManager.CARDS.get("Experimentation").get();
      be.data.cardChoices.add(new ResearchTableData.CardChoice("Experimentation", card, false, false));
      ResearchTableMenu menu = new ResearchTableMenu(0, player.getInventory(), be);
      h.assertTrue(menu.clickMenuButton(player, 4), "playing Experimentation should work");
      h.assertTrue(be.data.inspiration == before - 2, "Experimentation costs 2 inspiration");
      h.assertTrue(be.getItems().getStackInSlot(0).getDamageValue() == 1, "playing a card uses ink");
      h.assertTrue(be.data.categoryTotals.containsKey("BASICS"), "Experimentation adds basics");
      menu.clickMenuButton(player, 1);
      h.assertTrue(be.data.lastDraw != null && be.data.cardChoices.isEmpty(), "confirming moves the card to the played pile");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void bookshelfIsAResearchAid(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchTableBlockEntity be = tableWithTools(h, player, new BlockPos(2, 1, 2));
      h.setBlock(new BlockPos(3, 1, 2), Blocks.BOOKSHELF);
      Set<String> aids = be.checkSurroundingAids();
      h.assertTrue(aids.contains("Bookshelf"), "bookshelf should be found: " + aids);
      be.startNewTheory(player, Set.of("Bookshelf"));
      h.assertTrue(be.data.inspiration == be.data.inspirationStart - 1, "each aid costs an inspiration");
      h.assertTrue(be.data.aidCards.size() == 6, "bookshelf adds 6 cards: " + be.data.aidCards);
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void everyCardWorks(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      player.giveExperienceLevels(30);
      ResearchTableData data = new ResearchTableData(player, null);
      data.inspirationStart = data.inspiration = 15;
      data.categoryTotals.put("BASICS", 30);
      data.categoryTotals.put("ALCHEMY", 20);
      data.categoryTotals.put("ARTIFICE", 10);
      int played = 0;
      for (var e : TheorycraftManager.CARDS.entrySet()) {
         TheorycraftCard card = e.getValue().get();
         card.setSeed(1234);
         if (!card.initialize(player, data)) continue;
         CompoundTag saved = card.serialize();
         TheorycraftCard copy = e.getValue().get();
         copy.deserialize(saved);
         h.assertTrue(copy.getLocalizedName() != null && copy.getLocalizedText() != null, e.getKey() + " has no text");
         if (card.getRequiredItems() == null) {
            card.activate(player, data);
            played++;
         }
      }
      h.assertTrue(played > 10, "only " + played + " cards could be played");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void curiosGiveKnowledge(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.CURIOS.get(1).get()));
      ModItems.CURIOS.get(1).get().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
      var k = ResearchManager.knowledge(player);
      h.assertTrue(k.getKnowledgeRaw(EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory("ALCHEMY")) >= 8,
         "a preserved curio teaches alchemy");
      h.succeed();
   }
}
