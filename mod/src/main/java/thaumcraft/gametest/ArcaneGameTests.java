package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.crafting.ArcaneWorkbenchBlockEntity;
import thaumcraft.common.crafting.dust.DustTrigger;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.menu.ArcaneWorkbenchMenu;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModRecipes;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

/** Milestone 5: arcane workbench and arcane recipes. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class ArcaneGameTests {
   private static final BlockPos REL = new BlockPos(2, 1, 2);

   private static ArcaneWorkbenchBlockEntity workbench(GameTestHelper h, float vis) {
      h.setBlock(REL, ModBlocks.ARCANE_WORKBENCH.get());
      AuraChunk ac = AuraHandler.getAuraChunk(h.getLevel(), h.absolutePos(REL));
      h.assertTrue(ac != null, "no aura");
      ac.setVis(vis);
      return (ArcaneWorkbenchBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(REL));
   }

   /** Thaumometer: gold ingots around a glass pane + one of each primal crystal, 20 vis, FIRSTSTEPS@2. */
   private static void fillThaumometer(ArcaneWorkbenchBlockEntity be) {
      be.inventory.setItem(1, new ItemStack(Items.GOLD_INGOT));
      be.inventory.setItem(3, new ItemStack(Items.GOLD_INGOT));
      be.inventory.setItem(4, new ItemStack(Items.GLASS_PANE));
      be.inventory.setItem(5, new ItemStack(Items.GOLD_INGOT));
      be.inventory.setItem(7, new ItemStack(Items.GOLD_INGOT));
      for (int i = 0; i < 6; i++) be.inventory.setItem(9 + i, VisCrystalItem.of(ArcaneWorkbenchMenu.CRYSTALS[i], 2));
   }

   @GameTest(template = "empty")
   public static void arcaneRecipesLoad(GameTestHelper h) {
      var recipes = h.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.ARCANE_CRAFTING.get());
      h.assertTrue(recipes.stream().anyMatch(r -> r.id().getPath().equals("thaumometer")), "thaumometer arcane recipe missing: " + recipes.size());
      h.assertTrue(h.getLevel().getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "missing")).isEmpty(), "sanity");
      h.succeed();
   }

   @GameTest(template = "empty", timeoutTicks = 120)
   public static void dustedCraftingTableBecomesWorkbench(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      h.setBlock(REL, Blocks.CRAFTING_TABLE);
      BlockPos pos = h.absolutePos(REL);
      DustTrigger trigger = DustTrigger.TRIGGERS.stream()
         .filter(t -> {
            ResearchManager.giveRecursiveResearch(player, "FIRSTSTEPS");
            return t.getValidFace(h.getLevel(), player, pos, Direction.UP) != null;
         }).findFirst().orElseThrow();
      trigger.execute(h.getLevel(), player, pos, trigger.getValidFace(h.getLevel(), player, pos, Direction.UP), Direction.UP);
      h.runAfterDelay(60, () -> {
         h.assertBlockPresent(ModBlocks.ARCANE_WORKBENCH.get(), REL);
         h.succeed();
      });
   }

   @GameTest(template = "empty")
   public static void craftsThaumometer(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchManager.giveRecursiveResearch(player, "FIRSTSTEPS");
      ArcaneWorkbenchBlockEntity be = workbench(h, 100);
      fillThaumometer(be);
      ArcaneWorkbenchMenu menu = new ArcaneWorkbenchMenu(1, player.getInventory(), be);
      ItemStack out = menu.getSlot(0).getItem();
      h.assertTrue(out.is(ModItems.THAUMOMETER.get()), "expected a thaumometer, got " + out);
      menu.clicked(0, 0, ClickType.PICKUP, player);
      h.assertTrue(menu.getCarried().is(ModItems.THAUMOMETER.get()), "the thaumometer should be taken");
      h.assertTrue(be.inventory.getItem(4).isEmpty() && be.inventory.getItem(1).isEmpty(), "the grid is used up");
      for (int i = 0; i < 6; i++) h.assertTrue(be.inventory.getItem(9 + i).getCount() == 1, "one crystal of each is used");
      float vis = AuraHandler.getVis(h.getLevel(), h.absolutePos(REL));
      h.assertTrue(Math.abs(vis - 80) < 0.01, "20 vis drained, left " + vis);
      h.assertTrue(menu.getSlot(0).getItem().isEmpty(), "nothing left to craft");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void needsResearchVisAndCrystals(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ArcaneWorkbenchBlockEntity be = workbench(h, 100);
      fillThaumometer(be);
      ArcaneWorkbenchMenu menu = new ArcaneWorkbenchMenu(1, player.getInventory(), be);
      h.assertTrue(menu.getSlot(0).getItem().isEmpty(), "unknown research: no result");
      ResearchManager.giveRecursiveResearch(player, "FIRSTSTEPS");
      AuraHandler.getAuraChunk(h.getLevel(), h.absolutePos(REL)).setVis(10);
      menu.slotsChanged(be.inventory);
      h.assertTrue(menu.getSlot(0).getItem().isEmpty(), "10 vis isn't enough");
      AuraHandler.getAuraChunk(h.getLevel(), h.absolutePos(REL)).setVis(100);
      be.inventory.setItem(9, ItemStack.EMPTY);
      h.assertTrue(menu.getSlot(0).getItem().isEmpty(), "no aer crystal: no result");
      be.inventory.setItem(9, VisCrystalItem.of(Aspect.AIR, 1));
      h.assertTrue(menu.getSlot(0).getItem().is(ModItems.THAUMOMETER.get()), "everything is there now");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void normalCraftingWorks(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ArcaneWorkbenchBlockEntity be = workbench(h, 0);
      be.inventory.setItem(4, new ItemStack(Items.OAK_LOG));
      ArcaneWorkbenchMenu menu = new ArcaneWorkbenchMenu(1, player.getInventory(), be);
      h.assertTrue(menu.getSlot(0).getItem().is(Items.OAK_PLANKS) && menu.getSlot(0).getItem().getCount() == 4, "logs make planks");
      menu.quickMoveStack(player, 0);
      h.assertTrue(player.getInventory().countItem(Items.OAK_PLANKS) == 4, "shift-click crafts into the inventory");
      h.assertTrue(be.inventory.getItem(4).isEmpty(), "log used");
      h.succeed();
   }

   @GameTest(template = "empty")
   public static void chargerReadsNineChunks(GameTestHelper h) {
      ArcaneWorkbenchBlockEntity be = workbench(h, 50);
      int alone = be.getAura();
      h.assertTrue(alone == 50, "one chunk: " + alone);
      h.setBlock(REL.above(), ModBlocks.ARCANE_WORKBENCH_CHARGER.get());
      int charged = be.getAura();
      h.assertTrue(charged > alone, "the charger adds the chunks around: " + charged);
      h.setBlock(REL, Blocks.AIR);
      h.assertBlockNotPresent(ModBlocks.ARCANE_WORKBENCH_CHARGER.get(), REL.above());
      h.succeed();
   }
}
