package thaumcraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.common.blocks.crafting.FocalManipulatorBlockEntity;
import thaumcraft.common.blocks.crafting.FocusElementNode;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.items.casters.CasterItem;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.items.casters.FocusItem;
import thaumcraft.common.items.casters.FocusPouchItem;
import thaumcraft.common.registers.ModCasters;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraHandler;

/** Milestone 9: casters and foci. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class CasterGameTests {
   private static final BlockPos REL = new BlockPos(2, 1, 2);

   /** A focus package made of these elements in a row (the root first). */
   private static FocusPackage pack(String... keys) {
      FocusPackage p = new FocusPackage();
      int complexity = 0;
      for (String k : keys) {
         FocusNode n = (FocusNode) FocusEngine.getElement(k);
         p.addNode(n);
         complexity += n.getComplexity();
      }
      p.setComplexity(complexity);
      return p;
   }

   private static ItemStack focus(String name, FocusPackage p) {
      ItemStack f = new ItemStack(ModCasters.FOCUS_1.get());
      f.set(DataComponents.CUSTOM_NAME, Component.literal(name));
      FocusItem.setPackage(f, p);
      return f;
   }

   private static FocusElementNode node(int id, String key, int parent, int... children) {
      FocusElementNode n = new FocusElementNode();
      n.id = id;
      n.parent = parent;
      n.children = children;
      if (key != null) {
         n.node = (FocusNode) FocusEngine.getElement(key);
         n.target = n.node.canSupply(FocusNode.EnumSupplyType.TARGET);
         n.trajectory = n.node.canSupply(FocusNode.EnumSupplyType.TRAJECTORY);
      }
      return n;
   }

   /** A caster with a touch + fire focus, used on a cow in front of the player: the cow burns and the aura pays. */
   @GameTest(template = "empty", timeoutTicks = 100)
   public static void touchFireHitsEntity(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      Vec3 p = h.absoluteVec(new Vec3(0.5, 1, 2.5));
      player.moveTo(p.x, p.y, p.z, -90.0F, 0.0F);
      player.setYHeadRot(-90.0F);
      Cow cow = h.spawn(EntityType.COW, new BlockPos(3, 1, 2));
      cow.setNoAi(true);
      AuraHandler.getAuraChunk(h.getLevel(), player.blockPosition()).setVis(100);
      ItemStack caster = new ItemStack(ModItems.CASTER_BASIC.get());
      ((CasterItem) caster.getItem()).setFocus(caster, focus("Fire", pack("ROOT", "thaumcraft.TOUCH", "thaumcraft.FIRE")));
      player.setItemInHand(InteractionHand.MAIN_HAND, caster);
      CasterManager.setCooldown(player, 0);
      float before = cow.getHealth();
      var result = caster.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
      h.assertTrue(result.getResult().consumesAction(), "casting should succeed: " + result.getResult());
      h.assertTrue(cow.getHealth() < before, "the cow should be hurt: " + cow.getHealth());
      h.assertTrue(cow.isOnFire(), "the cow should burn");
      float vis = AuraHandler.getVis(h.getLevel(), player.blockPosition());
      h.assertTrue(Math.abs(vis - (100 - 0.8F)) < 0.01F, "a focus of complexity 4 costs 0.8 vis: " + vis);
      h.assertTrue(CasterManager.isOnCooldown(player), "casting starts the cooldown");
      h.assertFalse(caster.getItem().use(h.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "no recast while cooling down");
      CasterManager.setCooldown(player, 0);
      h.succeed();
   }

   /** The manipulator's tree becomes a package; repeated elements cost 50% more complexity each. */
   @GameTest(template = "empty", timeoutTicks = 40)
   public static void focusComplexityAndVisCost(GameTestHelper h) {
      h.setBlock(REL, ModCasters.WAND_WORKBENCH.get());
      FocalManipulatorBlockEntity be = (FocalManipulatorBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(REL));
      be.data.put(0, node(0, "ROOT", -1, 1));
      be.data.put(1, node(1, "thaumcraft.TOUCH", 0, 2));
      be.data.put(2, node(2, "thaumcraft.SPLITTARGET", 1, 3, 4));
      be.data.put(3, node(3, "thaumcraft.FIRE", 2));
      be.data.put(4, node(4, "thaumcraft.FIRE", 2));
      FocusPackage core = be.generateFocus();
      // root 0 + touch 2 + split 4 + fire 2 + second fire 2 * 1.5
      h.assertTrue(core.getComplexity() == 11, "complexity 11, got " + core.getComplexity());
      h.assertTrue(core.nodes.size() == 3 && core.nodes.get(2) instanceof FocusModSplit, "root, touch, split: " + core.nodes);
      FocusModSplit split = (FocusModSplit) core.nodes.get(2);
      h.assertTrue(split.getSplitPackages().size() == 2, "two branches");
      ItemStack f = focus("Split", core);
      FocusPackage back = FocusItem.getPackage(f);
      h.assertTrue(back != null && back.getComplexity() == 11 && ((FocusModSplit) back.nodes.get(2)).getSplitPackages().size() == 2,
         "the package survives the item component");
      FocusItem fi = (FocusItem) f.getItem();
      h.assertTrue(Math.abs(fi.getVisCost(f) - 2.2F) < 0.001F, "vis cost is complexity / 5: " + fi.getVisCost(f));
      h.assertTrue(fi.getActivationTime(f) == 5, "cooldown: " + fi.getActivationTime(f));
      ItemStack big = focus("Big", pack("ROOT", "thaumcraft.TOUCH", "thaumcraft.FIRE"));
      FocusPackage bp = FocusItem.getPackage(big);
      bp.setComplexity(40);
      FocusItem.setPackage(big, bp);
      h.assertTrue(fi.getActivationTime(big) == 8 * 10, "cooldown of complexity 40: " + fi.getActivationTime(big));
      h.succeed();
   }

   /** Starting the craft takes xp and crystals, then the table draws vis and imprints the package into the focus. */
   @GameTest(template = "empty", timeoutTicks = 300)
   public static void manipulatorBuildsPackage(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
      ResearchManager.giveRecursiveResearch(player, "BASEAUROMANCY");
      h.setBlock(REL, ModCasters.WAND_WORKBENCH.get());
      AuraHandler.getAuraChunk(h.getLevel(), h.absolutePos(REL)).setVis(400);
      FocalManipulatorBlockEntity be = (FocalManipulatorBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(REL));
      be.inventory.setItem(0, new ItemStack(ModCasters.FOCUS_1.get()));
      be.data.put(0, node(0, "ROOT", -1, 1));
      be.data.put(1, node(1, "thaumcraft.TOUCH", 0, 2));
      be.data.put(2, node(2, "thaumcraft.FIRE", 1));
      be.focusName = "Burner";
      h.assertFalse(be.startCraft(player), "no crystals, no craft");
      player.experienceLevel = 10;
      player.getInventory().add(VisCrystalItem.of(Aspect.AVERSION, 1));
      player.getInventory().add(VisCrystalItem.of(Aspect.FIRE, 2));
      h.assertTrue(be.startCraft(player), "the craft should start");
      h.assertTrue(player.experienceLevel == 8, "costs sqrt(4) = 2 levels: " + player.experienceLevel);
      h.assertTrue(FocalManipulatorBlockEntity.countCrystals(player, Aspect.AVERSION) == 0
         && FocalManipulatorBlockEntity.countCrystals(player, Aspect.FIRE) == 1, "one crystal per aspect used");
      h.assertTrue(be.vis == 4 * 10 + 15 / 5, "vis to draw: " + be.vis);
      h.succeedWhen(() -> {
         ItemStack f = be.getFocus();
         FocusPackage p = FocusItem.getPackage(f);
         h.assertTrue(p != null, "the focus gets a package (vis left " + be.vis + ")");
         h.assertTrue(FocusEngine.doesPackageContainElement(p, "thaumcraft.TOUCH") && FocusEngine.doesPackageContainElement(p, "thaumcraft.FIRE"),
            "touch and fire imprinted");
         h.assertTrue(p.getComplexity() == 4, "complexity " + p.getComplexity());
         h.assertTrue(f.getHoverName().getString().equals("Burner"), "named after the field: " + f.getHoverName().getString());
         h.assertTrue(AuraHandler.getVis(h.getLevel(), h.absolutePos(REL)) <= 400 - 43 + 0.01F, "vis drawn from the aura");
      });
   }

   /** F cycles the foci carried in the inventory and in a pouch; the removed focus goes into the pouch. */
   @GameTest(template = "empty", timeoutTicks = 40)
   public static void focusChangeFromInventoryAndPouch(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ItemStack caster = new ItemStack(ModItems.CASTER_BASIC.get());
      CasterItem ci = (CasterItem) caster.getItem();
      player.setItemInHand(InteractionHand.MAIN_HAND, caster);
      ItemStack a = focus("A", pack("ROOT", "thaumcraft.TOUCH", "thaumcraft.FIRE"));
      ItemStack b = focus("B", pack("ROOT", "thaumcraft.BOLT", "thaumcraft.FROST"));
      player.getInventory().setItem(5, a);
      ItemStack pouch = new ItemStack(ModCasters.FOCUS_POUCH.get());
      NonNullList<ItemStack> inv = NonNullList.withSize(FocusPouchItem.SIZE, ItemStack.EMPTY);
      inv.set(0, b);
      FocusPouchItem.setInventory(pouch, inv);
      player.getInventory().setItem(6, pouch);

      CasterManager.changeFocus(caster, h.getLevel(), player, "");
      h.assertTrue("A".equals(ci.getFocusStack(caster).getHoverName().getString()), "first focus A fitted");
      h.assertTrue(player.getInventory().getItem(5).isEmpty(), "A left the inventory");

      CasterManager.changeFocus(caster, h.getLevel(), player, FocusItem.getSortingHelper(ci.getFocusStack(caster)));
      h.assertTrue("B".equals(ci.getFocusStack(caster).getHoverName().getString()), "next focus B fitted from the pouch");
      NonNullList<ItemStack> pinv = FocusPouchItem.getInventory(player.getInventory().getItem(6));
      h.assertTrue("A".equals(pinv.get(0).getHoverName().getString()), "A went into the pouch: " + pinv.get(0));

      CasterManager.changeFocus(caster, h.getLevel(), player, "REMOVE");
      h.assertTrue(ci.getFocus(caster) == null, "focus removed");
      pinv = FocusPouchItem.getInventory(player.getInventory().getItem(6));
      long inPouch = pinv.stream().filter(s -> s.getItem() instanceof FocusItem).count();
      h.assertTrue(inPouch == 2, "both foci in the pouch: " + inPouch);
      h.assertTrue(caster.get(ModDataComponents.CASTER_FOCUS.get()) == null, "no focus component left");
      h.succeed();
   }

   /** The plan focus' G toggle grows the area along the chosen axis. */
   @GameTest(template = "empty", timeoutTicks = 40)
   public static void planAreaToggle(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ItemStack caster = new ItemStack(ModItems.CASTER_BASIC.get());
      ((CasterItem) caster.getItem()).setFocus(caster, focus("Plan", pack("ROOT", "thaumcraft.PLAN", "thaumcraft.BREAK")));
      h.assertTrue(CasterManager.getAreaX(caster) == 3 && CasterManager.getAreaDim(caster) == 0, "default area 3, all axes");
      CasterManager.toggleMisc(caster, h.getLevel(), player, 0);
      h.assertTrue(CasterManager.getAreaX(caster) == 0 && CasterManager.getAreaY(caster) == 0, "wraps to 0");
      CasterManager.toggleMisc(caster, h.getLevel(), player, 1);
      CasterManager.toggleMisc(caster, h.getLevel(), player, 0);
      h.assertTrue(CasterManager.getAreaDim(caster) == 1 && CasterManager.getAreaX(caster) == 1 && CasterManager.getAreaY(caster) == 0,
         "axis X only grows x");
      for (int x = 0; x <= 4; x++) h.setBlock(new BlockPos(x, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
      List<BlockPos> blocks = ((CasterItem) caster.getItem()).getArchitectBlocks(caster, h.getLevel(), h.absolutePos(new BlockPos(2, 1, 2)),
         net.minecraft.core.Direction.UP, player);
      h.assertTrue(blocks != null && blocks.size() == 3, "the plan previews the 3 blocks in a row along x: " + blocks);
      h.succeed();
   }
}
