package thaumcraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.GolemAddon;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.api.golems.parts.GolemHead;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.api.golems.parts.GolemMaterial;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.SealPos;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.crafting.dust.DustTrigger;
import thaumcraft.common.golems.GolemEntity;
import thaumcraft.common.golems.GolemPlacerItem;
import thaumcraft.common.golems.GolemProperties;
import thaumcraft.common.golems.seals.SealEntity;
import thaumcraft.common.golems.seals.SealFill;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.golems.seals.SealHarvest;
import thaumcraft.common.golems.seals.SealPickup;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModGolems;
import thaumcraft.common.research.ResearchManager;

/** Milestone 11: golem parts and placer, the golem press multiblock, seals, tasks and golem logistics. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class GolemGameTests {
   private static IGolemProperties design(int mat, int head, int arms, int legs, int addon) {
      IGolemProperties p = GolemProperties.fromLong(0L);
      p.setMaterial(GolemMaterial.getMaterials()[mat]);
      p.setHead(GolemHead.getHeads()[head]);
      p.setArms(GolemArm.getArms()[arms]);
      p.setLegs(GolemLeg.getLegs()[legs]);
      p.setAddon(GolemAddon.getAddons()[addon]);
      return p;
   }

   /** The parts survive the long encoding, and the traits come from all of them. */
   @GameTest(template = "empty")
   public static void golemPropertiesRoundTrip(GameTestHelper h) {
      h.assertTrue(GolemMaterial.getMaterials().length == 6 && GolemHead.getHeads().length == 5 && GolemArm.getArms().length == 5
         && GolemLeg.getLegs().length == 4 && GolemAddon.getAddons().length == 4, "all 1.12 parts registered");
      IGolemProperties p = design(1, 3, 2, 2, 3);
      p.setRank(4);
      IGolemProperties q = GolemProperties.fromLong(p.toLong());
      h.assertTrue(q.getMaterial().key.equals("IRON") && q.getHead().key.equals("SCOUT") && q.getArms().key.equals("CLAWS")
         && q.getLegs().key.equals("CLIMBER") && q.getAddon().key.equals("HAULER") && q.getRank() == 4, "parts and rank decoded");
      for (EnumGolemTrait t : new EnumGolemTrait[]{EnumGolemTrait.HEAVY, EnumGolemTrait.FIREPROOF, EnumGolemTrait.SCOUT, EnumGolemTrait.FIGHTER,
         EnumGolemTrait.CLIMBER, EnumGolemTrait.HAULER}) {
         h.assertTrue(q.hasTrait(t), "has trait " + t);
      }
      h.assertTrue(q.generateComponents().length > 0, "the design lists its components");
      h.succeed();
   }

   /** A golem placer puts down an owned golem with the design's stats, and picking it up gives the same placer back. */
   @GameTest(template = "empty")
   public static void golemPlacerSpawnsGolem(GameTestHelper h) {
      IGolemProperties p = design(1, 0, 0, 0, 1);
      ItemStack placer = GolemPlacerItem.of(p);
      GolemEntity golem = GolemPlacerItem.spawn(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2)), placer, null);
      h.assertTrue(golem != null && golem.isAlive(), "golem spawned");
      h.assertTrue(golem.getProperties().toLong() == p.toLong(), "golem has the design");
      // IRON: 10 + 20 health; armor 8, ARMORED plating: max(8 * 1.5, 9) = 12
      h.assertTrue(golem.getMaxHealth() == 30.0F, "health from the material: " + golem.getMaxHealth());
      h.assertTrue(golem.getArmorValue() == 12, "armor from material and plating: " + golem.getArmorValue());
      h.assertTrue(golem.getAttributeValue(Attributes.ATTACK_DAMAGE) == 0.0, "not a fighter");
      h.assertTrue(golem.getRestrictCenter().equals(h.absolutePos(new BlockPos(2, 2, 2))), "home where it was placed");
      ItemStack back = golem.toPlacer();
      h.assertTrue(back.is(ModGolems.GOLEM_PLACER.get()) && GolemPlacerItem.getProperties(back).toLong() == p.toLong(), "placer round trip");
      CompoundTag tag = new CompoundTag();
      golem.saveWithoutId(tag);
      GolemEntity copy = ModGolems.GOLEM.get().create(h.getLevel());
      copy.load(tag);
      h.assertTrue(copy.getProperties().toLong() == p.toLong() && copy.getMaxHealth() == 30.0F, "golem saves and loads");
      golem.discard();
      h.succeed();
   }

   /** Salis mundus on bars over a cauldron, an anvil, an upward piston and a stone table makes the golem press. */
   @GameTest(template = "empty", timeoutTicks = 200)
   public static void salisMundusMakesTheGolemPress(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchManager.giveRecursiveResearch(player, "MINDCLOCKWORK");
      h.setBlock(new BlockPos(1, 2, 1), Blocks.CAULDRON);
      h.setBlock(new BlockPos(1, 2, 2), Blocks.ANVIL);
      h.setBlock(new BlockPos(2, 2, 1), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP));
      h.setBlock(new BlockPos(2, 2, 2), ModBlocks.TABLE_STONE.get());
      h.setBlock(new BlockPos(2, 3, 1), Blocks.IRON_BARS);
      // Clicked on the anvil (the cauldron alone would make a crucible).
      BlockPos pos = h.absolutePos(new BlockPos(1, 2, 2));
      DustTrigger t = DustTrigger.TRIGGERS.stream().filter(x -> x instanceof ModGolems.PressTrigger).findFirst()
         .orElseThrow(() -> new IllegalStateException("no trigger for the golem press"));
      h.assertTrue(t.getValidFace(h.getLevel(), player, pos, Direction.UP) != null, "the structure fits the press blueprint");
      t.execute(h.getLevel(), player, pos, t.getValidFace(h.getLevel(), player, pos, Direction.UP), Direction.UP);
      h.runAfterDelay(70, () -> {
         h.assertBlockPresent(ModGolems.GOLEM_BUILDER.get(), new BlockPos(2, 2, 1));
         h.assertBlockPresent(ModGolems.PLACEHOLDER_CAULDRON.get(), new BlockPos(1, 2, 1));
         h.assertBlockPresent(ModGolems.PLACEHOLDER_ANVIL.get(), new BlockPos(1, 2, 2));
         h.assertBlockPresent(ModGolems.PLACEHOLDER_TABLE.get(), new BlockPos(2, 2, 2));
         h.assertBlockPresent(ModGolems.PLACEHOLDER_BARS.get(), new BlockPos(2, 3, 1));
         // Breaking a part takes the press apart again.
         h.destroyBlock(new BlockPos(1, 2, 2));
         h.assertBlockPresent(Blocks.CAULDRON, new BlockPos(1, 2, 1));
         h.assertBlockPresent(Blocks.PISTON, new BlockPos(2, 2, 1));
         h.assertBlockPresent(ModBlocks.TABLE_STONE.get(), new BlockPos(2, 2, 2));
         h.assertBlockPresent(Blocks.IRON_BARS, new BlockPos(2, 3, 1));
         h.succeed();
      });
   }

   /** A seal item places its seal on the clicked face; the seal keeps its settings through NBT; removing it drops the item. */
   @GameTest(template = "empty")
   public static void sealsArePlacedSavedAndRemoved(GameTestHelper h) {
      ServerLevel level = h.getLevel();
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      BlockPos ground = h.absolutePos(new BlockPos(2, 1, 2));
      player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModGolems.SEALS.get("harvest").get(), 2));
      ItemStack held = player.getMainHandItem();
      held.getItem().onItemUseFirst(held, new UseOnContext(player, InteractionHand.MAIN_HAND,
         new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false)));
      ISealEntity se = SealHandler.getSealEntity(level, new SealPos(ground, Direction.UP));
      try {
         h.assertTrue(se != null && se.getSeal() instanceof SealHarvest, "harvest seal placed");
         h.assertTrue(se.getOwner().equals(player.getUUID().toString()), "owned by the player");
         se.setPriority((byte) 3);
         se.setColor((byte) 5);
         se.setArea(new BlockPos(4, 2, 5));
         ((ISealConfigToggles) se.getSeal()).setToggle(0, false);
         SealEntity copy = new SealEntity();
         copy.readNBT(se.writeNBT(level.registryAccess()), level.registryAccess());
         h.assertTrue(copy.getSeal() instanceof SealHarvest && copy.getPriority() == 3 && copy.getColor() == 5
            && copy.getArea().equals(new BlockPos(4, 2, 5)) && !((ISealConfigToggles) copy.getSeal()).getToggles()[0].getValue(), "seal NBT round trip");
         h.assertFalse(SealHandler.addSealEntity(level, ground, Direction.UP, new SealHarvest(), player), "one seal per face");
         SealHandler.removeSealEntity(level, se.getSealPos(), false);
         h.assertTrue(SealHandler.getSealEntity(level, new SealPos(ground, Direction.UP)) == null, "seal removed");
         h.assertTrue(!level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(ground).inflate(2),
            e -> e.getItem().is(ModGolems.SEALS.get("harvest").get())).isEmpty(), "the seal item drops");
      } finally {
         SealHandler.removeSealEntity(level, new SealPos(ground, Direction.UP), true);
      }
      h.succeed();
   }

   /**
    * The basic logistics loop: a collect seal on the floor makes a task for a dropped item; a golem fetches it and,
    * holding it, answers the store seal on a chest.
    */
   @GameTest(template = "empty", timeoutTicks = 1200)
   public static void golemCarriesItemsFromCollectToStoreSeal(GameTestHelper h) {
      ServerLevel level = h.getLevel();
      h.setBlock(new BlockPos(4, 2, 4), Blocks.CHEST);
      BlockPos chest = h.absolutePos(new BlockPos(4, 2, 4));
      BlockPos ground = h.absolutePos(new BlockPos(1, 1, 3));
      SealEntity pickup = new SealEntity(new SealPos(ground, Direction.UP), new SealPickup());
      SealEntity fill = new SealEntity(new SealPos(chest, Direction.UP), new SealFill());
      SealHandler.addSealEntity(level, pickup);
      SealHandler.addSealEntity(level, fill);
      GolemEntity golem = GolemPlacerItem.spawn(level, h.absolutePos(new BlockPos(1, 2, 1)), GolemPlacerItem.of(GolemProperties.fromLong(0L)), null);
      h.assertTrue(golem != null, "golem spawned");
      ItemEntity item = new ItemEntity(level, ground.getX() + 0.5, ground.getY() + 1.1, ground.getZ() + 0.5, new ItemStack(Items.COBBLESTONE, 5));
      item.setDeltaMovement(Vec3.ZERO);
      item.setNoPickUpDelay();
      level.addFreshEntity(item);
      Runnable cleanup = () -> {
         SealHandler.removeSealEntity(level, pickup.getSealPos(), true);
         SealHandler.removeSealEntity(level, fill.getSealPos(), true);
         golem.discard();
      };
      h.runAfterDelay(1150, cleanup);
      h.succeedWhen(() -> {
         ChestBlockEntity be = (ChestBlockEntity) level.getBlockEntity(chest);
         int n = 0;
         for (int i = 0; i < be.getContainerSize(); i++) if (be.getItem(i).is(Items.COBBLESTONE)) n += be.getItem(i).getCount();
         h.assertTrue(n == 5, "cobblestone stored by the golem: " + n);
         cleanup.run();
      });
   }

   /** Tasks expire, suspended tasks are cleared, and a seal's tasks are suspended when it goes. */
   @GameTest(template = "empty")
   public static void tasksAreSuspendedWithTheirSeal(GameTestHelper h) {
      ServerLevel level = h.getLevel();
      BlockPos ground = h.absolutePos(new BlockPos(2, 1, 2));
      SealEntity seal = new SealEntity(new SealPos(ground, Direction.UP), new SealFill());
      SealHandler.addSealEntity(level, seal);
      Task task = new Task(seal.getSealPos(), ground);
      TaskHandler.addTask(level, task);
      h.assertTrue(TaskHandler.getTask(level, task.getId()) == task, "task added");
      SealHandler.removeSealEntity(level, seal.getSealPos(), true);
      h.assertTrue(task.isSuspended(), "removing the seal suspends its tasks");
      TaskHandler.clearSuspendedOrExpiredTasks(level);
      h.assertTrue(TaskHandler.getTask(level, task.getId()) == null, "suspended tasks are cleared");
      h.succeed();
   }
}
