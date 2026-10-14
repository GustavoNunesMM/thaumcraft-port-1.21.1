package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.devices.ArcaneEarBlockEntity;
import thaumcraft.common.blocks.devices.DioptraBlockEntity;
import thaumcraft.common.blocks.devices.EverfullUrnBlockEntity;
import thaumcraft.common.blocks.devices.GrowthLampBlockEntity;
import thaumcraft.common.blocks.devices.HungryChestBlockEntity;
import thaumcraft.common.blocks.devices.InfernalFurnaceBlockEntity;
import thaumcraft.common.blocks.devices.JarBrainBlockEntity;
import thaumcraft.common.blocks.devices.MirrorBlockEntity;
import thaumcraft.common.blocks.devices.PatternCrafterBlockEntity;
import thaumcraft.common.blocks.devices.PotionSprayerBlockEntity;
import thaumcraft.common.blocks.devices.RechargePedestalBlockEntity;
import thaumcraft.common.blocks.devices.SpaBlockEntity;
import thaumcraft.common.blocks.devices.ThaumatoriumBlockEntity;
import thaumcraft.common.blocks.devices.VisBatteryBlock;
import thaumcraft.common.blocks.devices.VisGeneratorBlockEntity;
import thaumcraft.common.blocks.essentia.JarBlockEntity;
import thaumcraft.common.crafting.dust.DustTrigger;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraHandler;

/** Devices milestone: lamps, levitator, hungry chest, ears, relay, urn, spa, furnace, thaumatorium, sprayer, mirrors... */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class DeviceGameTests {
   private static void vis(GameTestHelper h, BlockPos rel, float vis) {
      AuraHandler.getAuraChunk(h.getLevel(), h.absolutePos(rel)).setVis(vis);
   }

   @SuppressWarnings("unchecked")
   private static <T> T be(GameTestHelper h, BlockPos rel) {
      return (T) h.getLevel().getBlockEntity(h.absolutePos(rel));
   }

   /** A lit lamp of growth spends its charges ticking the crops below it. */
   @GameTest(template = "empty", timeoutTicks = 1200)
   public static void lampOfGrowthGrowsCrops(GameTestHelper h) {
      for (int x = 1; x <= 3; x++) {
         for (int z = 1; z <= 3; z++) {
            h.setBlock(new BlockPos(x, 0, z), x == 2 && z == 2 ? Blocks.WATER.defaultBlockState()
               : Blocks.FARMLAND.defaultBlockState().setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7)); // wet farmland: dry crops grow too slowly to test reliably
            if (x != 2 || z != 2) h.setBlock(new BlockPos(x, 1, z), Blocks.WHEAT);
         }
      }
      h.setBlock(new BlockPos(2, 4, 2), Blocks.STONE);
      h.setBlock(new BlockPos(2, 3, 2), ModDevices.LAMP_GROWTH.get().defaultBlockState().setValue(DeviceBlock.FACING, Direction.UP));
      GrowthLampBlockEntity lamp = be(h, new BlockPos(2, 3, 2));
      lamp.charges = 200;
      h.succeedWhen(() -> {
         h.assertTrue(lamp.charges < 200, "the lamp spends charges: " + lamp.charges);
         int grown = 0;
         for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
               BlockState s = h.getBlockState(new BlockPos(x, 1, z));
               if (s.is(Blocks.WHEAT) && s.getValue(CropBlock.AGE) > 0) grown++;
            }
         }
         h.assertTrue(grown > 0, "some wheat grew; charges " + lamp.charges + " light " + h.getLevel().getRawBrightness(h.absolutePos(new BlockPos(1, 1, 1)), 0)
            + " " + h.getBlockState(new BlockPos(1, 1, 1)) + " " + h.getBlockState(new BlockPos(1, 0, 1)));
      });
   }

   @GameTest(template = "empty", timeoutTicks = 200)
   public static void levitatorLiftsItems(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      vis(h, rel, 100);
      h.setBlock(rel, ModDevices.LEVITATOR.get().defaultBlockState().setValue(DeviceBlock.FACING, Direction.UP));
      ItemEntity item = h.spawnItem(Items.STONE, 2.5F, 2.2F, 2.5F);
      double start = item.getY();
      h.succeedWhen(() -> h.assertTrue(item.getY() > start + 1.5, "the item rises: " + item.getY()));
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void hungryChestEatsItems(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      h.setBlock(rel, ModDevices.HUNGRY_CHEST.get());
      h.spawnItem(Items.DIAMOND, 2.5F, 2.0F, 2.5F);
      HungryChestBlockEntity chest = be(h, rel);
      h.succeedWhen(() -> h.assertTrue(chest.getItem(0).is(Items.DIAMOND), "the diamond is in the chest"));
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void arcaneEarHearsNoteBlocks(GameTestHelper h) {
      h.setBlock(new BlockPos(3, 1, 2), Blocks.STONE);
      h.setBlock(new BlockPos(3, 2, 2), ModDevices.ARCANE_EAR.get().defaultBlockState().setValue(DeviceBlock.FACING, Direction.UP)
         .setValue(DeviceBlock.ENABLED, false));
      ArcaneEarBlockEntity ear = be(h, new BlockPos(3, 2, 2));
      ear.updateTone();
      h.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
      BlockPos nb = new BlockPos(1, 2, 2);
      h.setBlock(nb, Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.INSTRUMENT, NoteBlockInstrument.BASEDRUM));
      h.assertTrue(ear.tone == NoteBlockInstrument.BASEDRUM.ordinal(), "the ear takes the tone of stone: " + ear.tone);
      h.runAfterDelay(2, () -> h.getLevel().blockEvent(h.absolutePos(nb), Blocks.NOTE_BLOCK, 0, 0));
      h.succeedWhen(() -> h.assertTrue(DeviceBlock.isEnabled(h.getBlockState(new BlockPos(3, 2, 2))), "the ear gives a signal"));
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void redstoneRelayPassesSignal(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      h.setBlock(rel.below(), Blocks.STONE);
      h.setBlock(rel, ModDevices.REDSTONE_RELAY.get().defaultBlockState().setValue(DeviceBlock.HORIZONTAL_FACING, Direction.NORTH));
      h.setBlock(new BlockPos(2, 1, 1), Blocks.REDSTONE_BLOCK);
      h.succeedWhen(() -> {
         h.assertTrue(DeviceBlock.isEnabled(h.getBlockState(rel)), "the relay turns on");
         h.assertTrue(h.getLevel().getSignal(h.absolutePos(rel), Direction.NORTH) == 15, "and outputs 15 behind it: "
            + h.getLevel().getSignal(h.absolutePos(rel), Direction.NORTH) + " " + h.getBlockState(rel) + " " + h.getLevel().getBlockEntity(h.absolutePos(rel)));
      });
   }

   @GameTest(template = "empty", timeoutTicks = 500)
   public static void everfullUrnFillsCauldrons(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      vis(h, rel, 100);
      h.setBlock(rel, ModDevices.EVERFULL_URN.get());
      h.setBlock(new BlockPos(3, 1, 2), Blocks.CAULDRON);
      EverfullUrnBlockEntity urn = be(h, rel);
      urn.tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
      h.succeedWhen(() -> h.assertBlockPresent(Blocks.WATER_CAULDRON, new BlockPos(3, 1, 2)));
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void visBatteryReleasesVisWhenPowered(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      vis(h, rel, 0);
      h.setBlock(rel, ModDevices.VIS_BATTERY.get().defaultBlockState().setValue(VisBatteryBlock.CHARGE, 5));
      h.setBlock(new BlockPos(3, 1, 2), Blocks.REDSTONE_BLOCK);
      h.succeedWhen(() -> {
         h.assertTrue(h.getBlockState(rel).getValue(VisBatteryBlock.CHARGE) == 0, "the battery empties");
         h.assertTrue(AuraHandler.getVis(h.getLevel(), h.absolutePos(rel)) >= 5, "into the aura");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void rechargePedestalChargesItems(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      vis(h, rel, 100);
      h.setBlock(rel, ModDevices.RECHARGE_PEDESTAL.get());
      RechargePedestalBlockEntity ped = be(h, rel);
      ped.setItem(0, new ItemStack(ModGear.TRAVELLER_BOOTS.get()));
      h.succeedWhen(() -> h.assertTrue(RechargeHelper.getCharge(ped.getItem()) >= 10, "charged: " + RechargeHelper.getCharge(ped.getItem())));
   }

   @GameTest(template = "empty", timeoutTicks = 60)
   public static void dioptraShowsVis(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      vis(h, rel, 250);
      h.setBlock(rel, ModDevices.DIOPTRA.get());
      DioptraBlockEntity d = be(h, rel);
      h.succeedWhen(() -> h.assertTrue(d.gridAmt[84] >= 30 && d.gridAmt[84] <= 32, "the centre shows its chunk's vis: " + d.gridAmt[84]));
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void visGeneratorMakesEnergy(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      vis(h, rel, 100);
      h.setBlock(rel, ModDevices.VIS_GENERATOR.get().defaultBlockState().setValue(DeviceBlock.FACING, Direction.UP).setValue(DeviceBlock.ENABLED, true));
      VisGeneratorBlockEntity g = be(h, rel);
      h.succeedWhen(() -> h.assertTrue(g.getEnergyStored() == 1000, "1 vis = 1000 FE: " + g.getEnergyStored()));
   }

   @GameTest(template = "empty", timeoutTicks = 200)
   public static void spaPoursItsFluid(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      h.setBlock(rel, ModDevices.SPA.get());
      SpaBlockEntity spa = be(h, rel);
      spa.toggleMix();
      spa.tank.fill(new FluidStack(Fluids.WATER, 2000), IFluidHandler.FluidAction.EXECUTE);
      h.succeedWhen(() -> {
         h.assertBlockPresent(Blocks.WATER, rel.above());
         h.assertTrue(spa.tank.getFluidAmount() <= 1000, "it used 1000 mB");
      });
   }

   /** Iron ore thrown in comes out of the front as iron ingots. */
   @GameTest(template = "empty", timeoutTicks = 400)
   public static void infernalFurnaceSmelts(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      h.setBlock(rel, ModDevices.INFERNAL_FURNACE.get().defaultBlockState().setValue(DeviceBlock.HORIZONTAL_FACING, Direction.NORTH));
      h.setBlock(new BlockPos(2, 1, 3), Blocks.CHEST);
      InfernalFurnaceBlockEntity f = be(h, rel);
      ItemStack left = f.addItemsToInventory(new ItemStack(Items.RAW_IRON, 2));
      h.assertTrue(left.isEmpty(), "the furnace takes the ore");
      ChestBlockEntity out = be(h, new BlockPos(2, 1, 3));
      h.succeedWhen(() -> {
         int ingots = 0;
         for (int i = 0; i < out.getContainerSize(); i++) if (out.getItem(i).is(Items.IRON_INGOT)) ingots += out.getItem(i).getCount();
         h.assertTrue(ingots == 2, "two iron ingots out of the front: " + ingots);
      });
   }

   /** Salis mundus on the nether brick / obsidian / lava structure turns it into an infernal furnace. */
   @GameTest(template = "empty", timeoutTicks = 200)
   public static void salisMundusMakesTheInfernalFurnace(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchManager.giveRecursiveResearch(player, "INFERNALFURNACE");
      for (int x = 1; x <= 3; x++) {
         for (int y = 1; y <= 3; y++) {
            for (int z = 1; z <= 3; z++) {
               h.setBlock(new BlockPos(x, y, z), x != 2 && z != 2 ? Blocks.NETHER_BRICKS : Blocks.OBSIDIAN);
            }
         }
      }
      h.setBlock(new BlockPos(2, 2, 2), Blocks.LAVA);
      BlockPos bars = h.absolutePos(new BlockPos(2, 2, 3));
      h.getLevel().setBlock(bars, net.minecraft.world.level.block.Block.updateFromNeighbourShapes(Blocks.IRON_BARS.defaultBlockState(), h.getLevel(), bars), 3);
      h.setBlock(new BlockPos(2, 3, 2), Blocks.AIR);
      BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
      DustTrigger t = DustTrigger.TRIGGERS.stream().filter(x -> x.getValidFace(h.getLevel(), player, pos, Direction.UP) != null).findFirst()
         .orElseThrow(() -> new IllegalStateException("no trigger for the furnace"));
      t.execute(h.getLevel(), player, pos, t.getValidFace(h.getLevel(), player, pos, Direction.UP), Direction.UP);
      h.runAfterDelay(70, () -> {
         h.assertBlockPresent(ModDevices.INFERNAL_FURNACE.get(), new BlockPos(2, 2, 2));
         h.assertBlockPresent(ModDevices.PLACEHOLDER_BRICK.get(), new BlockPos(1, 1, 1));
         h.assertBlockPresent(ModDevices.PLACEHOLDER_OBSIDIAN.get(), new BlockPos(2, 1, 1));
         h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 2, 3));
         h.assertTrue(DeviceBlock.getFacing(h.getBlockState(new BlockPos(2, 2, 2))) == Direction.NORTH, "the opening is its front");
         h.succeed();
      });
   }

   /** An iron ingot and instrumentum piped in from a jar make brass. */
   @GameTest(template = "empty", timeoutTicks = 600)
   public static void thaumatoriumCraftsBrass(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 2, 2);
      h.setBlock(new BlockPos(2, 0, 2), ModBlocks.NITOR.get(net.minecraft.world.item.DyeColor.YELLOW).get());
      h.setBlock(new BlockPos(2, 1, 2), ModBlocks.CRUCIBLE.get());
      h.setBlock(rel, ModDevices.THAUMATORIUM.get().defaultBlockState().setValue(DeviceBlock.HORIZONTAL_FACING, Direction.NORTH));
      h.setBlock(rel.above(), ModDevices.THAUMATORIUM_TOP.get().defaultBlockState().setValue(DeviceBlock.HORIZONTAL_FACING, Direction.NORTH));
      h.setBlock(new BlockPos(3, 1, 2), ModBlocks.JAR_NORMAL.get());
      ((JarBlockEntity) be(h, new BlockPos(3, 1, 2))).addToContainer(Aspect.TOOL, 20);
      h.setBlock(new BlockPos(3, 2, 2), ModBlocks.TUBE.get());
      h.setBlock(new BlockPos(2, 2, 1), Blocks.CHEST);
      ThaumatoriumBlockEntity t = be(h, rel);
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "brassingot");
      h.assertTrue(t.recipe(id) != null, "the brass recipe exists");
      t.recipeIds.add(id);
      t.recipeEssentia.add(t.recipe(id).value().getAspects(h.getLevel()).copy());
      t.recipePlayer.add("");
      t.setItem(0, new ItemStack(Items.IRON_INGOT, 2));
      ChestBlockEntity out = be(h, new BlockPos(2, 2, 1));
      h.succeedWhen(() -> {
         int brass = 0;
         for (int i = 0; i < out.getContainerSize(); i++) {
            if (out.getItem(i).is(thaumcraft.common.registers.ModItems.INGOT_BRASS.get())) brass += out.getItem(i).getCount();
         }
         h.assertTrue(brass >= 1, "brass comes out of the front: essentia " + t.essentia + ", suction " + t.currentSuction);
      });
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void thaumatoriumNeedsItsCrucible(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 2, 2);
      h.setBlock(new BlockPos(2, 1, 2), ModBlocks.CRUCIBLE.get());
      h.setBlock(rel, ModDevices.THAUMATORIUM.get().defaultBlockState().setValue(DeviceBlock.HORIZONTAL_FACING, Direction.NORTH));
      h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
      h.succeedWhen(() -> h.assertBlockPresent(ModDevices.METAL_ALCHEMICAL.get(), rel));
   }

   @GameTest(template = "empty", timeoutTicks = 200)
   public static void potionSprayerSpraysOnPulse(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      h.setBlock(rel, ModDevices.POTION_SPRAYER.get().defaultBlockState().setValue(DeviceBlock.FACING, Direction.UP));
      PotionSprayerBlockEntity s = be(h, rel);
      s.setItem(0, PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS));
      h.assertTrue(PotionSprayerBlockEntity.isValidPotion(s.getItem(0)), "swiftness is sprayable");
      s.charges = 1;
      Pig pig = h.spawn(EntityType.PIG, new BlockPos(2, 3, 2));
      h.setBlock(new BlockPos(3, 1, 2), Blocks.REDSTONE_BLOCK);
      h.succeedWhen(() -> h.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SPEED), "the pig is sped up"));
   }

   @GameTest(template = "empty", timeoutTicks = 200)
   public static void patternCrafterCraftsFromAbove(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 2, 2);
      vis(h, rel, 100);
      h.setBlock(rel.below(), Blocks.CHEST);
      h.setBlock(rel, ModDevices.PATTERN_CRAFTER.get().defaultBlockState().setValue(DeviceBlock.HORIZONTAL_FACING, Direction.NORTH));
      h.setBlock(rel.above(), Blocks.CHEST);
      ((ChestBlockEntity) be(h, rel.above())).setItem(0, new ItemStack(Items.IRON_INGOT, 9));
      PatternCrafterBlockEntity pc = be(h, rel);
      h.assertTrue(pc.type == 0, "the 3x3 pattern first");
      ChestBlockEntity below = be(h, rel.below());
      h.succeedWhen(() -> h.assertTrue(below.getItem(0).is(Items.IRON_BLOCK), "an iron block below: " + below.getItem(0)));
   }

   /** Two linked magic mirrors: an item thrown into one drops out of the other. */
   @GameTest(template = "empty", timeoutTicks = 200)
   public static void mirrorsTransportItems(GameTestHelper h) {
      BlockPos a = new BlockPos(1, 1, 1), b = new BlockPos(3, 1, 3);
      h.setBlock(a, ModDevices.MIRROR.get().defaultBlockState().setValue(DeviceBlock.FACING, Direction.UP));
      h.setBlock(b, ModDevices.MIRROR.get().defaultBlockState().setValue(DeviceBlock.FACING, Direction.UP));
      MirrorBlockEntity ma = be(h, a);
      ma.link = GlobalPos.of(h.getLevel().dimension(), h.absolutePos(b));
      ma.restoreLink();
      MirrorBlockEntity mb = be(h, b);
      h.assertTrue(ma.linked && mb.linked && ma.isLinkValid(), "the mirrors link up");
      ItemEntity item = h.spawnItem(Items.EMERALD, 1.5F, 1.1F, 1.5F);
      h.succeedWhen(() -> {
         h.assertTrue(!item.isAlive(), "the item went in");
         var near = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(b)).inflate(2.0));
         h.assertTrue(near.stream().anyMatch(e -> e.getItem().is(Items.EMERALD)), "and came out of the other mirror");
      });
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void brainInAJarEatsExperience(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      h.setBlock(rel, ModDevices.JAR_BRAIN.get());
      JarBrainBlockEntity jar = be(h, rel);
      h.getLevel().addFreshEntity(new ExperienceOrb(h.getLevel(), h.absolutePos(rel).getX() + 0.5, h.absolutePos(rel).getY() + 0.5,
         h.absolutePos(rel).getZ() + 0.5, 7));
      h.succeedWhen(() -> h.assertTrue(jar.xp == 7, "the brain stores the xp: " + jar.xp));
   }

   @GameTest(template = "empty", timeoutTicks = 100)
   public static void lootUrnsDropLoot(GameTestHelper h) {
      BlockPos rel = new BlockPos(2, 1, 2);
      h.setBlock(rel, ModDevices.LOOT_URN_RARE.get());
      h.getLevel().destroyBlock(h.absolutePos(rel), true);
      h.succeedWhen(() -> {
         var items = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(rel)).inflate(2.0));
         h.assertTrue(items.size() >= 3, "a rare urn gives at least 3 things: " + items.size());
      });
   }
}
