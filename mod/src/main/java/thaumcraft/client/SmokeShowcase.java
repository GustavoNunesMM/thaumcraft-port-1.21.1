package thaumcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.phys.Vec3;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.world.CrystalBlock;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;

/** Dev-only: builds a floating display of the milestone 2 blocks for the client smoke test screenshots. */
final class SmokeShowcase {
   private static BlockPos base;
   /** Where the player was, so {@link #restore} leaves the tc_test world as the next run expects it. */
   private static Vec3 savedPos;
   private static float savedYaw, savedPitch;
   private static GameType savedMode;

   private SmokeShowcase() {
   }

   private static void run(Minecraft mc, java.util.function.BiConsumer<ServerLevel, ServerPlayer> task) {
      MinecraftServer server = mc.getSingleplayerServer();
      if (server == null || mc.player == null) return;
      var uuid = mc.player.getUUID();
      server.execute(() -> {
         ServerPlayer p = server.getPlayerList().getPlayer(uuid);
         if (p != null) task.accept(p.serverLevel(), p);
      });
   }

   static void build(Minecraft mc) {
      run(mc, (level, player) -> {
         savedPos = player.position();
         savedYaw = player.getYRot();
         savedPitch = player.getXRot();
         savedMode = player.gameMode.getGameModeForPlayer();
         player.setGameMode(GameType.CREATIVE); // flying, and no fall damage
         level.setDayTime(6000);
         level.setWeatherParameters(6000, 0, false, false);
         base = player.blockPosition().offset(0, 40, 0);
         // Floor: grass with a stone strip, 40 wide, 48 deep.
         for (int x = -20; x <= 20; x++) {
            for (int z = 0; z <= 48; z++) {
               for (int y = -3; y <= 0; y++) level.setBlock(base.offset(x, y, z), (y == 0 ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState(), 2);
               for (int y = 1; y <= 30; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            }
         }
         // Row 1 (z=6): ores, metals, amber, wood blocks.
         Block[] row = {ModBlocks.ORE_AMBER.get(), ModBlocks.ORE_CINNABAR.get(), ModBlocks.ORE_QUARTZ.get(), ModBlocks.DEEPSLATE_ORE_CINNABAR.get(),
            ModBlocks.DEEPSLATE_ORE_QUARTZ.get(), ModBlocks.METAL_THAUMIUM.get(), ModBlocks.METAL_VOID.get(), ModBlocks.METAL_BRASS.get(),
            ModBlocks.AMBER_BLOCK.get(), ModBlocks.AMBER_BRICK.get(), ModBlocks.PLANK_GREATWOOD.get(), ModBlocks.PLANK_SILVERWOOD.get(),
            ModBlocks.LOG_GREATWOOD.get(), ModBlocks.LOG_SILVERWOOD.get(), ModBlocks.LEAVES_GREATWOOD.get(), ModBlocks.LEAVES_SILVERWOOD.get()};
         for (int i = 0; i < row.length; i++) level.setBlock(base.offset(-8 + i, 1, 6), row[i].defaultBlockState(), 2);
         level.setBlock(base.offset(4, 2, 6), ModBlocks.LOG_GREATWOOD.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 2);
         level.setBlock(base.offset(-8, 2, 6), ModBlocks.STAIRS_GREATWOOD.get().defaultBlockState(), 2);
         level.setBlock(base.offset(-7, 2, 6), ModBlocks.SLAB_SILVERWOOD.get().defaultBlockState(), 2);
         // Row 2 (z=3): stone pillars with crystals of each aspect on top, on the side and hanging below.
         var crystals = ModBlocks.crystals();
         for (int i = 0; i < crystals.size(); i++) {
            BlockPos pillar = base.offset(-7 + i * 2, 1, 3);
            level.setBlock(pillar, Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(pillar.above(3), Blocks.STONE.defaultBlockState(), 2);
            CrystalBlock c = crystals.get(i);
            place(level, c, pillar.above(), i % 4);
            place(level, c, pillar.above(2), 3);
            place(level, c, pillar.north(), 2);
         }
         // Plants and saplings (z=1)
         level.setBlock(base.offset(-3, 0, 1), Blocks.SAND.defaultBlockState(), 2);
         level.setBlock(base.offset(-3, 1, 1), ModBlocks.CINDERPEARL.get().defaultBlockState(), 2);
         level.setBlock(base.offset(-1, 1, 1), ModBlocks.SHIMMERLEAF.get().defaultBlockState(), 2);
         level.setBlock(base.offset(1, 1, 1), ModBlocks.VISHROOM.get().defaultBlockState(), 2);
         level.setBlock(base.offset(3, 1, 1), ModBlocks.SAPLING_GREATWOOD.get().defaultBlockState(), 2);
         level.setBlock(base.offset(5, 1, 1), ModBlocks.SAPLING_SILVERWOOD.get().defaultBlockState(), 2);
         // Trees further back, through their configured features (same code as world generation and saplings).
         var features = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
         var gen = level.getChunkSource().getGenerator();
         features.getHolderOrThrow(key("greatwood_tree")).value().place(level, gen, level.random, base.offset(-8, 1, 30));
         features.getHolderOrThrow(key("silverwood_tree")).value().place(level, gen, level.random, base.offset(8, 1, 28));
         // Items for the tooltip check: slot 10 = fire vis crystal.
         player.getInventory().setItem(10, VisCrystalItem.of(Aspect.FIRE, 3));
         player.getInventory().setItem(11, new ItemStack(ModBlocks.CRYSTAL_AQUA.get()));
         player.getInventory().setItem(12, new ItemStack(ModItems.INGOT_THAUMIUM.get()));
         player.getInventory().setItem(13, new ItemStack(ModBlocks.SAPLING_SILVERWOOD.get()));
         player.getInventory().setItem(14, new ItemStack(ModItems.QUICKSILVER.get()));
         player.getInventory().setItem(15, new ItemStack(ModItems.AMBER.get()));
         // Stand at the front edge, looking at the rows (south, slightly down).
         player.teleportTo(level, base.getX() + 0.5, base.getY() + 3, base.getZ() - 4.5, 0.0F, 20.0F);
         player.getAbilities().flying = true;
         player.onUpdateAbilities();
      });
   }

   static void clearItems(Minecraft mc) {
      run(mc, (level, player) -> {
         for (int slot = 10; slot <= 15; slot++) player.getInventory().setItem(slot, ItemStack.EMPTY);
      });
   }

   public static BlockPos tablePos() {
      return base.offset(0, 1, 9);
   }

   /** A research table with tools and paper and a bookshelf (a research aid) next to it; opens it. */
   static void researchTable(Minecraft mc) {
      run(mc, (level, player) -> {
         BlockPos t = tablePos();
         level.setBlockAndUpdate(t, ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
         level.setBlockAndUpdate(t.east(), Blocks.BOOKSHELF.defaultBlockState());
         if (level.getBlockEntity(t) instanceof thaumcraft.common.blocks.crafting.ResearchTableBlockEntity be) {
            be.getItems().setStackInSlot(0, new ItemStack(ModItems.SCRIBING_TOOLS.get()));
            be.getItems().setStackInSlot(1, new ItemStack(net.minecraft.world.item.Items.PAPER, 16));
            player.teleportTo(level, t.getX() + 0.5, t.getY(), t.getZ() - 1.5, 0, 30);
            // A few ticks later, once the client has the new block (as when a player clicks a table).
            thaumcraft.common.lib.ServerTasks.schedule(5, () -> player.openMenu(be, t));
         }
      });
   }

   static void startTheory(Minecraft mc) {
      run(mc, (level, player) -> {
         if (level.getBlockEntity(tablePos()) instanceof thaumcraft.common.blocks.crafting.ResearchTableBlockEntity be) {
            be.startNewTheory(player, java.util.Set.of("Bookshelf"));
            be.data.categoryTotals.put("BASICS", 25);
            be.data.categoryTotals.put("ALCHEMY", 10);
            be.consumePaper();
            be.data.drawCards(2, player);
            be.sync();
         }
      });
   }

   static void removeResearchTable(Minecraft mc) {
      run(mc, (level, player) -> {
         level.removeBlockEntity(tablePos());
         level.setBlockAndUpdate(tablePos(), Blocks.AIR.defaultBlockState());
         level.setBlockAndUpdate(tablePos().east(), Blocks.AIR.defaultBlockState());
         player.teleportTo(level, savedPos.x, savedPos.y, savedPos.z, savedYaw, savedPitch);
      });
   }

   static void arcaneWorkbench(Minecraft mc) {
      run(mc, (level, player) -> {
         BlockPos t = tablePos();
         level.setBlockAndUpdate(t, ModBlocks.ARCANE_WORKBENCH.get().defaultBlockState());
         level.setBlockAndUpdate(t.above(), ModBlocks.ARCANE_WORKBENCH_CHARGER.get().defaultBlockState());
         var ac = thaumcraft.common.world.aura.AuraHandler.getAuraChunk(level, t);
         if (ac != null) ac.setVis(Math.max(ac.getVis(), 100));
         if (level.getBlockEntity(t) instanceof thaumcraft.common.blocks.crafting.ArcaneWorkbenchBlockEntity be) {
            var gold = net.minecraft.world.item.Items.GOLD_INGOT;
            for (int i : new int[]{1, 3, 5, 7}) be.inventory.setItem(i, new ItemStack(gold));
            be.inventory.setItem(4, new ItemStack(net.minecraft.world.item.Items.GLASS_PANE));
            for (int i = 0; i < 6; i++) {
               be.inventory.setItem(9 + i, thaumcraft.common.items.VisCrystalItem.of(thaumcraft.common.menu.ArcaneWorkbenchMenu.CRYSTALS[i], 3));
            }
            player.teleportTo(level, t.getX() + 0.5, t.getY(), t.getZ() - 1.5, 0, 30);
            thaumcraft.common.lib.ServerTasks.schedule(5, () -> player.openMenu(be, t));
         }
      });
   }

   static void removeArcaneWorkbench(Minecraft mc) {
      run(mc, (level, player) -> {
         if (level.getBlockEntity(tablePos()) instanceof thaumcraft.common.blocks.crafting.ArcaneWorkbenchBlockEntity be) be.inventory.clearContent();
         level.setBlockAndUpdate(tablePos().above(), Blocks.AIR.defaultBlockState());
         level.setBlockAndUpdate(tablePos(), Blocks.AIR.defaultBlockState());
         player.teleportTo(level, savedPos.x, savedPos.y, savedPos.z, savedYaw, savedPitch);
      });
   }

   private static BlockState underCrucible = Blocks.AIR.defaultBlockState();

   static void crucible(Minecraft mc) {
      run(mc, (level, player) -> {
         BlockPos t = tablePos();
         underCrucible = level.getBlockState(t.below());
         level.setBlockAndUpdate(t.below(), Blocks.MAGMA_BLOCK.defaultBlockState());
         level.setBlockAndUpdate(t, ModBlocks.CRUCIBLE.get().defaultBlockState());
         if (level.getBlockEntity(t) instanceof thaumcraft.common.blocks.crafting.CrucibleBlockEntity be) {
            be.tank.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000),
               net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            be.heat = 200;
            be.aspects = new thaumcraft.api.aspects.AspectList().add(thaumcraft.api.aspects.Aspect.FIRE, 40)
               .add(thaumcraft.api.aspects.Aspect.WATER, 30).add(thaumcraft.api.aspects.Aspect.MAGIC, 30);
            be.sync();
         }
         var colors = net.minecraft.world.item.DyeColor.values();
         for (int i = 0; i < 4; i++) {
            level.setBlockAndUpdate(t.offset(-2 + i + (i >= 2 ? 1 : 0), 1, 1), ModBlocks.NITOR.get(colors[i * 4 + 1]).get().defaultBlockState());
         }
         player.teleportTo(level, t.getX() + 0.5, t.getY() + 1.2, t.getZ() - 1.8, 0, 40);
      });
   }

   static void removeCrucible(Minecraft mc) {
      run(mc, (level, player) -> {
         BlockPos t = tablePos();
         if (level.getBlockEntity(t) instanceof thaumcraft.common.blocks.crafting.CrucibleBlockEntity be) {
            be.aspects = new thaumcraft.api.aspects.AspectList();
            be.tank.setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
         }
         level.setBlockAndUpdate(t, Blocks.AIR.defaultBlockState());
         level.setBlockAndUpdate(t.below(), underCrucible);
         for (int i = -2; i <= 2; i++) level.setBlockAndUpdate(t.offset(i, 1, 1), Blocks.AIR.defaultBlockState());
         player.teleportTo(level, savedPos.x, savedPos.y, savedPos.z, savedYaw, savedPitch);
      });
   }

   private static final java.util.Map<BlockPos, BlockState> savedEssentia = new java.util.HashMap<>();

   /** Milestone 7 showcase: smeltery + alembics, jars (one labelled) joined by tubes, valve, buffer, centrifuge, bellows. */
   static void essentia(Minecraft mc) {
      run(mc, (level, player) -> {
         BlockPos t = tablePos().offset(-3, 0, 0);
         savedEssentia.clear();
         for (BlockPos p : BlockPos.betweenClosed(t.offset(0, 0, 0), t.offset(7, 3, 2))) savedEssentia.put(p.immutable(), level.getBlockState(p));
         for (BlockPos p : savedEssentia.keySet()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
         BlockState smelter = ModBlocks.SMELTER_BASIC.get().defaultBlockState()
            .setValue(thaumcraft.common.blocks.DeviceBlock.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH)
            .setValue(thaumcraft.common.blocks.DeviceBlock.ENABLED, true);
         level.setBlockAndUpdate(t, smelter);
         level.setBlockAndUpdate(t.east(), ModBlocks.SMELTER_VENT.get().defaultBlockState()
            .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.WEST));
         level.setBlockAndUpdate(t.above(), ModBlocks.ALEMBIC.get().defaultBlockState());
         level.setBlockAndUpdate(t.above(2), ModBlocks.ALEMBIC.get().defaultBlockState());
         if (level.getBlockEntity(t.above()) instanceof thaumcraft.common.blocks.essentia.AlembicBlockEntity a) {
            a.addToContainer(thaumcraft.api.aspects.Aspect.EARTH, 60);
            a.aspectFilter = thaumcraft.api.aspects.Aspect.EARTH;
            a.facing = net.minecraft.core.Direction.NORTH;
            a.sync();
         }
         if (level.getBlockEntity(t) instanceof thaumcraft.common.blocks.essentia.SmelterBlockEntity se) {
            se.setItem(0, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 16));
            se.setItem(1, new ItemStack(net.minecraft.world.item.Items.COAL, 8));
         }
         BlockPos j1 = t.offset(3, 0, 0), j2 = t.offset(5, 0, 0);
         level.setBlockAndUpdate(j1, ModBlocks.JAR_NORMAL.get().defaultBlockState());
         level.setBlockAndUpdate(j2, ModBlocks.JAR_NORMAL.get().defaultBlockState());
         if (level.getBlockEntity(j1) instanceof thaumcraft.common.blocks.essentia.JarBlockEntity j) {
            j.addToContainer(thaumcraft.api.aspects.Aspect.FIRE, 200);
            j.facing = net.minecraft.core.Direction.NORTH;
            j.sync();
         }
         if (level.getBlockEntity(j2) instanceof thaumcraft.common.blocks.essentia.JarBlockEntity j) {
            j.addToContainer(thaumcraft.api.aspects.Aspect.MAGIC, 60);
            j.aspectFilter = thaumcraft.api.aspects.Aspect.MAGIC;
            j.facing = net.minecraft.core.Direction.NORTH;
            j.blocked = true;
            j.sync();
         }
         level.setBlockAndUpdate(j1.above(), ModBlocks.TUBE.get().defaultBlockState());
         level.setBlockAndUpdate(j1.above().east(), ModBlocks.TUBE_VALVE.get().defaultBlockState());
         level.setBlockAndUpdate(j2.above(), ModBlocks.TUBE_BUFFER.get().defaultBlockState());
         level.setBlockAndUpdate(t.offset(7, 0, 0), ModBlocks.JAR_VOID.get().defaultBlockState());
         level.setBlockAndUpdate(t.offset(7, 1, 0), ModBlocks.CENTRIFUGE.get().defaultBlockState());
         level.setBlockAndUpdate(t.offset(1, 0, 1), ModBlocks.BELLOWS.get().defaultBlockState()
            .setValue(thaumcraft.common.blocks.DeviceBlock.FACING, net.minecraft.core.Direction.WEST));
         level.setBlockAndUpdate(t.offset(-1, 0, 0), ModBlocks.TUBE_FILTER.get().defaultBlockState());
         player.teleportTo(level, t.getX() + 3.5, t.getY() + 1.6, t.getZ() - 3.2, 0, 20);
      });
   }

   static void removeEssentia(Minecraft mc) {
      run(mc, (level, player) -> {
         savedEssentia.forEach((p, st) -> {
            if (level.getBlockEntity(p) instanceof net.minecraft.world.Container c) c.clearContent();
            level.removeBlockEntity(p);
            level.setBlock(p, st, 2);
         });
         level.setBlock(tablePos().offset(-4, 0, 0), Blocks.AIR.defaultBlockState(), 2);
         savedEssentia.clear();
         player.teleportTo(level, savedPos.x, savedPos.y, savedPos.z, savedYaw, savedPitch);
      });
   }

   private static final java.util.Map<BlockPos, BlockState> savedInfusion = new java.util.HashMap<>();

   /** Milestone 8 showcase: an arcane infusion altar mid-craft, with candles, a stabilizer and inlay. */
   static void infusion(Minecraft mc) {
      run(mc, (level, player) -> {
         BlockPos m = tablePos().offset(0, 3, 4);
         savedInfusion.clear();
         for (BlockPos p : BlockPos.betweenClosed(m.offset(-4, -3, -4), m.offset(4, 1, 4))) savedInfusion.put(p.immutable(), level.getBlockState(p));
         for (BlockPos p : BlockPos.betweenClosed(m.offset(-4, -2, -4), m.offset(4, 1, 4))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
         for (BlockPos p : BlockPos.betweenClosed(m.offset(-4, -3, -4), m.offset(4, -3, 4))) level.setBlock(p, Blocks.STONE.defaultBlockState(), 2);
         for (int dx : new int[]{-1, 1}) {
            for (int dz : new int[]{-1, 1}) {
               level.setBlockAndUpdate(m.offset(dx, -2, dz), ModBlocks.PILLAR_ARCANE.get().defaultBlockState());
            }
         }
         level.setBlockAndUpdate(m.below(2), ModBlocks.PEDESTAL_ARCANE.get().defaultBlockState());
         level.setBlockAndUpdate(m, ModBlocks.INFUSION_MATRIX.get().defaultBlockState());
         BlockPos[] peds = {m.offset(3, -2, 0), m.offset(-3, -2, 0), m.offset(0, -2, 3), m.offset(0, -2, -3)};
         ItemStack[] items = {new ItemStack(net.minecraft.world.item.Items.DIAMOND), new ItemStack(net.minecraft.world.item.Items.DIAMOND),
            new ItemStack(ModItems.SALIS_MUNDUS.get()), new ItemStack(ModItems.SALIS_MUNDUS.get())};
         for (int i = 0; i < 4; i++) {
            level.setBlockAndUpdate(peds[i], ModBlocks.PEDESTAL_ARCANE.get().defaultBlockState());
            if (level.getBlockEntity(peds[i]) instanceof thaumcraft.common.blocks.infusion.PedestalBlockEntity pe) pe.setItem(0, items[i]);
         }
         if (level.getBlockEntity(m.below(2)) instanceof thaumcraft.common.blocks.infusion.PedestalBlockEntity pe) {
            pe.setItem(0, new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
         }
         for (int dx : new int[]{-2, 2}) {
            for (int dz : new int[]{-2, 2}) {
               level.setBlockAndUpdate(m.offset(dx, -2, dz), ModBlocks.CANDLES.get(net.minecraft.world.item.DyeColor.values()[(dx + 2) + (dz + 2) / 4])
                  .get().defaultBlockState());
            }
         }
         level.setBlockAndUpdate(m.offset(3, -2, 2), ModBlocks.STABILIZER.get().defaultBlockState());
         level.setBlockAndUpdate(m.offset(3, -2, 1), ModBlocks.INLAY.get().defaultBlockState());
         if (level.getBlockEntity(m) instanceof thaumcraft.common.blocks.infusion.InfusionMatrixBlockEntity im) {
            im.active = true;
            im.crafting = true;
            im.sync();
         }
         player.getInventory().selected = 8;
         player.teleportTo(level, m.getX() + 0.5, m.getY() + 1.0, m.getZ() - 7.5, 0, 20);
      });
   }

   static void removeInfusion(Minecraft mc) {
      run(mc, (level, player) -> {
         savedInfusion.forEach((p, st) -> {
            if (level.getBlockEntity(p) instanceof net.minecraft.world.Container c) c.clearContent();
            level.removeBlockEntity(p);
            level.setBlock(p, st, 2);
         });
         savedInfusion.clear();
         player.getInventory().selected = 0;
         player.teleportTo(level, savedPos.x, savedPos.y, savedPos.z, savedYaw, savedPitch);
      });
   }

   static void giveResearch(Minecraft mc, String key) {
      run(mc, (level, player) -> {
         thaumcraft.common.research.ResearchManager.giveRecursiveResearch(player, key);
         thaumcraft.common.research.ResearchManager.sync(player);
      });
   }

   /** Leaves tc_test without research, as the scan checks of the next run expect. */
   static void resetResearch(Minecraft mc) {
      run(mc, (level, player) -> {
         var k = thaumcraft.common.research.ResearchManager.knowledge(player);
         for (String key : java.util.List.copyOf(k.getResearchList())) {
            if (!key.startsWith("!")) k.removeResearch(key);
         }
         thaumcraft.common.research.ResearchManager.sync(player);
      });
   }

   static void restore(Minecraft mc) {
      run(mc, (level, player) -> {
         player.setGameMode(savedMode);
         player.teleportTo(level, savedPos.x, savedPos.y, savedPos.z, savedYaw, savedPitch);
      });
   }

   static void lookAtTrees(Minecraft mc) {
      run(mc, (level, player) -> player.teleportTo(level, base.getX() + 0.5, base.getY() + 12, base.getZ() + 6.5, 0.0F, 10.0F));
   }

   private static void place(ServerLevel level, CrystalBlock c, BlockPos pos, int size) {
      BlockState s = c.worldgenState(level, pos, size);
      level.setBlock(pos, s, 2);
   }

   private static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
      return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name));
   }
}
