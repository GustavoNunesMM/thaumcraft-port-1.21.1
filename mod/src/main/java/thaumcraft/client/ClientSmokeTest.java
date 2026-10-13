package thaumcraft.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.registers.ModAttachments;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;

/**
 * Dev-only client smoke test, enabled with -Dthaumcraft.smokeTest=true (see the smokeTest run in build.gradle).
 * Expects the tc_test world (thaumometer in hotbar, sword in the first inventory slot, gold wall ahead).
 * Scans with the thaumometer, checks client sync, shows an aspect tooltip, checks block/item models, builds a showcase
 * of the milestone 2 blocks and trees ({@link SmokeShowcase}), saves screenshots and quits.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ClientSmokeTest {
   static final boolean ENABLED = Boolean.getBoolean("thaumcraft.smokeTest");
   /** Lets the tooltip render as if Shift were held, since the test cannot press keys. */
   static boolean forceShift;

   private static int ticks;
   private static final List<String> results = new ArrayList<>();

   private ClientSmokeTest() {
   }

   private static void check(boolean ok, String what) {
      results.add((ok ? "PASS " : "FAIL ") + what);
      Thaumcraft.LOGGER.info("[smoke] {} {}", ok ? "PASS" : "FAIL", what);
   }

   private static void shot(Minecraft mc, String name) {
      Screenshot.grab(mc.gameDirectory, "smoke_" + name + ".png", mc.getMainRenderTarget(), msg -> {});
   }

   /** Slot whose tooltip is drawn by {@link #renderTooltip}; the cursor can't be moved reliably on Wayland. */
   private static Slot tooltipSlot;

   @SubscribeEvent
   public static void renderTooltip(ScreenEvent.Render.Post event) {
      if (ENABLED && tooltipSlot != null && event.getScreen() instanceof InventoryScreen inv) {
         // Same path as hovering a slot: fires RenderTooltipEvent.GatherComponents, where the aspect row is added.
         event.getGuiGraphics().renderTooltip(
            Minecraft.getInstance().font, tooltipSlot.getItem(), inv.getGuiLeft() + tooltipSlot.x + 8, inv.getGuiTop() + tooltipSlot.y + 8
         );
      }
   }

   private static void checkEssentiaModels(Minecraft mc) {
      net.minecraft.core.BlockPos j = SmokeShowcase.tablePos().offset(0, 0, 0);
      check(mc.level.getBlockEntity(j) instanceof thaumcraft.common.blocks.essentia.JarBlockEntity jar && jar.amount == 200,
         "jar synced to the client with its essentia");
      check(mc.level.getBlockState(j.above().east()).getValue(net.minecraft.world.level.block.PipeBlock.WEST), "valve connects to the tube");
   }

   /** Every Thaumcraft block state and item must have a real model (not the purple-black missing one). */
   private static void checkModels(Minecraft mc) {
      var missing = mc.getModelManager().getMissingModel();
      List<String> bad = new ArrayList<>();
      for (var b : ModBlocks.BLOCKS.getEntries()) {
         for (var state : b.get().getStateDefinition().getPossibleStates()) {
            if (mc.getBlockRenderer().getBlockModel(state) == missing) {
               bad.add(state.toString());
               break;
            }
         }
      }
      for (var i : ModItems.ITEMS.getEntries()) {
         if (mc.getItemRenderer().getModel(new ItemStack(i.get()), mc.level, null, 0) == missing) bad.add(i.getId().toString());
      }
      check(bad.isEmpty(), "no missing models " + bad);
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      if (!ENABLED) return;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null || mc.gameMode == null) return;
      ticks++;
      switch (ticks) {
         case 60 -> {
            check(mc.player.getMainHandItem().is(ModItems.THAUMOMETER.get()), "thaumometer in main hand");
            check(AspectManager.CLIENT.getItemAspects(net.minecraft.world.item.Items.STONE) != null, "client received aspect tables");
            check(thaumcraft.common.world.aura.ClientAura.current.getBase() > 0, "aura synced while holding the thaumometer: "
               + thaumcraft.common.world.aura.ClientAura.current);
            shot(mc, "1_holding_thaumometer");
         }
         case 70 -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         case 100 -> {
            var k = mc.player.getData(ModAttachments.KNOWLEDGE);
            check(k.isResearchKnown("!minecraft:gold_block"), "scan synced to client (knows gold block)");
            check(k.isResearchKnown("!metallum"), "scan taught aspect metallum");
            shot(mc, "2_after_scan");
         }
         case 110 -> mc.setScreen(new InventoryScreen(mc.player));
         case 120 -> {
            if (mc.screen instanceof InventoryScreen inv) {
               Slot slot = inv.getMenu().getSlot(9); // first main-inventory slot: the diamond sword
               ItemStack stack = slot.getItem();
               AspectList a = AspectManager.CLIENT.getObjectAspects(mc.level.registryAccess(), stack);
               check(a != null && !a.isEmpty(), "client aspects for " + stack.getItem() + ": " + a);
               forceShift = true;
               tooltipSlot = slot;
            } else {
               check(false, "inventory screen opened");
            }
         }
         case 140 -> shot(mc, "3_tooltip_with_aspects");
         case 150 -> {
            forceShift = false;
            tooltipSlot = null;
            mc.setScreen(null);
            checkModels(mc);
            mc.player.getInventory().selected = 8; // empty hand: the thaumometer lens would cover the screenshots
            SmokeShowcase.build(mc);
         }
         case 260 -> shot(mc, "4_ores_crystals_wood");
         case 270 -> SmokeShowcase.lookAtTrees(mc);
         case 360 -> shot(mc, "5_greatwood_silverwood");
         // Back in survival first: in creative, InventoryScreen turns itself into the creative inventory.
         case 365 -> {
            mc.player.getInventory().selected = 0;
            SmokeShowcase.restore(mc);
         }
         case 380 -> mc.setScreen(new InventoryScreen(mc.player));
         case 390 -> {
            if (mc.screen instanceof InventoryScreen inv) {
               Slot slot = inv.getMenu().getSlot(10); // the fire vis crystal given by the showcase
               AspectList a = AspectManager.CLIENT.getObjectAspects(mc.level.registryAccess(), slot.getItem());
               check(a != null && a.getAmount(thaumcraft.api.aspects.Aspect.FIRE) == 1, "vis crystal aspects on client: " + a);
               forceShift = true;
               tooltipSlot = slot;
            } else {
               check(false, "inventory screen opened for the vis crystal");
            }
         }
         case 410 -> shot(mc, "6_vis_crystal_tooltip");
         case 420 -> {
            forceShift = false;
            tooltipSlot = null;
            mc.setScreen(null);
            SmokeShowcase.clearItems(mc);
         }
         case 430 -> SmokeShowcase.giveResearch(mc, "FIRSTSTEPS");
         case 450 -> {
            check(thaumcraft.api.research.ResearchCategories.getResearch("FIRSTSTEPS") != null, "client has the research entries");
            check(thaumcraft.common.research.ResearchManager.knowledge(mc.player).isResearchComplete("FIRSTSTEPS"), "research synced to client");
            mc.setScreen(new thaumcraft.client.gui.ResearchBrowserScreen());
         }
         case 470 -> shot(mc, "7_thaumonomicon_browser");
         case 480 -> mc.setScreen(new thaumcraft.client.gui.ResearchPageScreen(
            thaumcraft.api.research.ResearchCategories.getResearch("FIRSTSTEPS"), null, 0, 0));
         case 500 -> shot(mc, "8_research_page");
         case 510 -> mc.setScreen(new thaumcraft.client.gui.ResearchPageScreen(thaumcraft.api.research.ResearchCategories.getResearch("FIRSTSTEPS"),
            net.minecraft.resources.ResourceLocation.parse("thaumcraft:salismundusfake"), 0, 0));
         case 530 -> shot(mc, "9_research_recipe");
         case 540 -> {
            mc.setScreen(null);
            SmokeShowcase.researchTable(mc);
         }
         case 570 -> {
            check(mc.screen instanceof thaumcraft.client.gui.ResearchTableScreen, "research table screen opened: " + mc.screen);
            shot(mc, "10_research_table");
            SmokeShowcase.startTheory(mc);
         }
         case 620 -> shot(mc, "11_theory_cards");
         case 630 -> {
            mc.setScreen(null);
            SmokeShowcase.removeResearchTable(mc);
            SmokeShowcase.arcaneWorkbench(mc);
         }
         case 660 -> {
            check(mc.screen instanceof thaumcraft.client.gui.ArcaneWorkbenchScreen, "arcane workbench screen opened: " + mc.screen);
            if (mc.screen instanceof thaumcraft.client.gui.ArcaneWorkbenchScreen s) {
               check(s.getMenu().currentArcaneRecipe() != null, "client sees the thaumometer arcane recipe");
            }
            shot(mc, "12_arcane_workbench");
         }
         case 670 -> mc.setScreen(new thaumcraft.client.gui.ResearchPageScreen(thaumcraft.api.research.ResearchCategories.getResearch("FIRSTSTEPS"),
            net.minecraft.resources.ResourceLocation.parse("thaumcraft:thaumometer"), 0, 0));
         case 690 -> shot(mc, "13_arcane_recipe_page");
         case 700 -> {
            mc.setScreen(null);
            SmokeShowcase.removeArcaneWorkbench(mc);
            SmokeShowcase.giveResearch(mc, "UNLOCKALCHEMY");
            SmokeShowcase.crucible(mc);
         }
         case 760 -> {
            check(mc.level.getBlockEntity(SmokeShowcase.tablePos()) instanceof thaumcraft.common.blocks.crafting.CrucibleBlockEntity c
               && !c.aspects.isEmpty(), "crucible synced to the client with its essentia");
            shot(mc, "14_crucible_nitor");
         }
         case 770 -> mc.setScreen(new thaumcraft.client.gui.ResearchPageScreen(thaumcraft.api.research.ResearchCategories.getResearch("UNLOCKALCHEMY"),
            net.minecraft.resources.ResourceLocation.parse("thaumcraft:nitor"), 0, 0));
         case 790 -> shot(mc, "15_crucible_recipe_page");
         case 800 -> {
            mc.setScreen(null);
            SmokeShowcase.removeCrucible(mc);
            SmokeShowcase.resetResearch(mc);
         }
         case 810 -> SmokeShowcase.essentia(mc);
         case 900 -> {
            checkEssentiaModels(mc);
            shot(mc, "16_essentia");
         }
         case 905 -> {
            net.minecraft.core.BlockPos sm = SmokeShowcase.tablePos().offset(-3, 0, 0);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(sm), net.minecraft.core.Direction.NORTH, sm, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
         }
         case 925 -> {
            check(mc.screen instanceof thaumcraft.client.gui.SmelterScreen, "smelter screen opened: " + mc.screen);
            shot(mc, "17_smelter_gui");
            mc.setScreen(null);
         }
         case 930 -> SmokeShowcase.removeEssentia(mc);
         case 940 -> {
            SmokeShowcase.infusion(mc);
            mc.player.getInventory().selected = 8; // an empty hand: the thaumometer's lens hides the view
         }
         case 1040 -> {
            check(mc.level.getBlockEntity(SmokeShowcase.tablePos().offset(0, 3, 4)) instanceof thaumcraft.common.blocks.infusion.InfusionMatrixBlockEntity m
               && m.active, "infusion matrix active on the client");
            shot(mc, "18_infusion_altar");
         }
         case 1050 -> {
            mc.player.getInventory().selected = 0;
            SmokeShowcase.removeInfusion(mc);
         }
         case 1070 -> {
            long failed = results.stream().filter(r -> r.startsWith("FAIL")).count();
            Thaumcraft.LOGGER.info("[smoke] DONE {} checks, {} failed", results.size(), failed);
            mc.stop();
         }
         default -> {
         }
      }
   }
}
