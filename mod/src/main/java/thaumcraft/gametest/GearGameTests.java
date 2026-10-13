package thaumcraft.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.blocks.infusion.InfusionMatrixBlockEntity;
import thaumcraft.common.crafting.InfusionDataRecipe;
import thaumcraft.common.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.events.GearEvents;
import thaumcraft.common.items.armor.FortressArmorItem;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.lib.CuriosHelper;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraHandler;

/** Milestone 10: tools, armor and baubles. */
@GameTestHolder(Thaumcraft.MODID)
@PrefixGameTestTemplate(false)
public class GearGameTests {
   /** Robes (3 + 2), goggles (5) and the apprentice's ring in a curio slot (5): 15%. */
   @GameTest(template = "empty")
   public static void visDiscountSumsArmorAndCurios(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      h.assertTrue(CasterManager.getTotalVisDiscount(player) == 0.0F, "nothing worn, no discount");
      player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModGear.GOGGLES_ITEM.get()));
      player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModGear.CLOTH_CHEST.get()));
      player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModGear.CLOTH_BOOTS.get()));
      h.assertTrue(Math.abs(CasterManager.getTotalVisDiscount(player) - 0.10F) < 1e-4, "armor: " + CasterManager.getTotalVisDiscount(player));
      h.assertTrue(CuriosHelper.equip(player, "ring", new ItemStack(ModGear.RING_APPRENTICE.get())), "the player has a ring slot");
      h.assertTrue(Math.abs(CasterManager.getTotalVisDiscount(player) - 0.15F) < 1e-4, "armor + ring: " + CasterManager.getTotalVisDiscount(player));
      h.succeed();
   }

   /** Void metal mends a point every second; it weakens what it hits. */
   @GameTest(template = "empty")
   public static void voidGearRepairsItself(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ItemStack sword = new ItemStack(ModGear.VOID_SWORD.get());
      sword.setDamageValue(10);
      ItemStack helm = new ItemStack(ModGear.VOID_HELM.get());
      helm.setDamageValue(5);
      player.setItemSlot(EquipmentSlot.HEAD, helm);
      player.tickCount = 39;
      sword.inventoryTick(h.getLevel(), player, 0, true);
      h.assertTrue(sword.getDamageValue() == 10, "only once a second");
      player.tickCount = 40;
      sword.inventoryTick(h.getLevel(), player, 0, true);
      helm.inventoryTick(h.getLevel(), player, 39, false);
      h.assertTrue(sword.getDamageValue() == 9 && helm.getDamageValue() == 4, "mended: " + sword.getDamageValue() + " " + helm.getDamageValue());
      var target = h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(2, 2, 2));
      sword.getItem().hurtEnemy(sword, target, player);
      h.assertTrue(target.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "the sword weakens");
      h.succeed();
   }

   /** Main hand, armor, curios and infused warp all count (WarpEvents.getWarpFromGear). */
   @GameTest(template = "empty")
   public static void warpFromGear(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModGear.VOID_SWORD.get()));
      ItemStack robe = new ItemStack(ModGear.VOID_ROBE_CHEST.get());
      robe.set(ModDataComponents.WARP.get(), 2);
      player.setItemSlot(EquipmentSlot.CHEST, robe);
      CuriosHelper.equip(player, "charm", new ItemStack(ModGear.CHARM_UNDYING.get()));
      h.assertTrue(GearEvents.getWarpFromGear(player) == 1 + 3 + 2, "warp: " + GearEvents.getWarpFromGear(player));
      h.succeed();
   }

   /** Runic shielding becomes absorption, one point per recharge for one vis each, and is taken back when removed. */
   @GameTest(template = "empty", timeoutTicks = 300)
   public static void runicShieldAbsorbs(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      player.setGameMode(GameType.SURVIVAL);
      BlockPos pos = player.blockPosition();
      AuraHandler.getAuraChunk(h.getLevel(), pos).setVis(100);
      ItemStack chest = new ItemStack(ModGear.THAUMIUM_CHEST.get());
      chest.set(ModGear.RUNIC.get(), 1);
      player.setItemSlot(EquipmentSlot.CHEST, chest);
      ItemStack ring = new ItemStack(ModGear.RING_MUNDANE.get());
      ring.set(ModGear.RUNIC.get(), 1);
      h.assertTrue(CuriosHelper.equip(player, "ring", ring), "ring equipped");
      int[] stage = {0};
      // The level counts the mock player's ticks but, without a ticking connection, never fires PlayerTickEvent.
      h.onEachTick(() -> {
         GearEvents.handleRunicArmor(player);
         if (stage[0] == 0 && player.getAbsorptionAmount() >= 2.0F) {
            stage[0] = 1;
            h.assertTrue(player.getAttributeValue(Attributes.MAX_ABSORPTION) == 2.0, "shield size 2");
            float health = player.getHealth();
            // (out of world damage ignores the fresh player's spawn invulnerability; absorption still takes it)
            player.hurt(player.damageSources().fellOutOfWorld(), 1.0F);
            h.assertTrue(player.getHealth() == health && player.getAbsorptionAmount() == 1.0F, "the shield took the hit");
            chest.remove(ModGear.RUNIC.get());
         }
      });
      h.succeedWhen(() -> {
         h.assertTrue(stage[0] == 1, "shield charged: " + player.getAbsorptionAmount() + " vis " + AuraHandler.getVis(h.getLevel(), pos)
            + " max " + player.getAttributeValue(Attributes.MAX_ABSORPTION) + " runic " + GearEvents.getTotalRunicCharge(player) + " tick "
            + player.tickCount + " t " + h.getLevel().getGameTime());
         h.assertTrue(player.getAttributeValue(Attributes.MAX_ABSORPTION) == 1.0, "shield shrank");
      });
   }

   /** The runic augment infusion matches armor and curios, and asks for one more amber per point already there. */
   @GameTest(template = "empty")
   public static void runicAugmentInfusion(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchManager.giveRecursiveResearch(player, "RUNICSHIELDING");
      List<ItemStack> two = List.of(new ItemStack(ModItems.SALIS_MUNDUS.get()), new ItemStack(ModItems.AMBER.get()));
      ItemStack chest = new ItemStack(ModGear.THAUMIUM_CHEST.get());
      InfusionRecipe r = InfusionMatrixBlockEntity.findMatchingInfusionRecipe(h.getLevel(), two, chest, player);
      h.assertTrue(r instanceof InfusionRunicAugmentRecipe, "runic augment for armor: " + r);
      ItemStack out = r.getRecipeOutput(player, chest, two);
      h.assertTrue(GearEvents.getRunicCharge(out) == 1, "one point of shielding");
      h.assertTrue(r.getAspects(player, chest, two).getAmount(Aspect.PROTECT) == 40, "first augment costs 40 praemunio");
      h.assertTrue(!(InfusionMatrixBlockEntity.findMatchingInfusionRecipe(h.getLevel(), two, out, player) instanceof InfusionRunicAugmentRecipe),
         "the second point needs another amber");
      List<ItemStack> three = List.of(new ItemStack(ModItems.SALIS_MUNDUS.get()), new ItemStack(ModItems.AMBER.get()), new ItemStack(ModItems.AMBER.get()));
      InfusionRecipe r2 = InfusionMatrixBlockEntity.findMatchingInfusionRecipe(h.getLevel(), three, out, player);
      h.assertTrue(r2 instanceof InfusionRunicAugmentRecipe && GearEvents.getRunicCharge(r2.getRecipeOutput(player, out, three)) == 2, "second point");
      h.assertTrue(r2.getAspects(player, out, three).getAmount(Aspect.PROTECT) == 60, "costs double");
      h.assertTrue(InfusionMatrixBlockEntity.findMatchingInfusionRecipe(h.getLevel(), two, new ItemStack(ModGear.RING_MUNDANE.get()), player)
         instanceof InfusionRunicAugmentRecipe, "curios take shielding too");
      h.assertTrue(!(InfusionMatrixBlockEntity.findMatchingInfusionRecipe(h.getLevel(), two, new ItemStack(Items.STICK), player)
         instanceof InfusionRunicAugmentRecipe), "not a stick");
      h.succeed();
   }

   /** The vis amulet is a ticking curio: it feeds aura vis into the traveller boots, which then speed up and soften falls. */
   @GameTest(template = "empty")
   public static void visAmuletChargesBoots(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      AuraHandler.getAuraChunk(h.getLevel(), player.blockPosition()).setVis(100);
      ItemStack boots = new ItemStack(ModGear.TRAVELLER_BOOTS.get());
      player.setItemSlot(EquipmentSlot.FEET, boots);
      ItemStack amulet = new ItemStack(ModGear.AMULET_VIS_CRAFTED.get());
      var curio = top.theillusivec4.curios.api.CuriosApi.getCurio(amulet);
      h.assertTrue(curio.isPresent(), "the amulet is a curio");
      player.tickCount = 5;
      curio.get().curioTick(new top.theillusivec4.curios.api.SlotContext("necklace", player, 0, false, true));
      h.assertTrue(thaumcraft.api.items.RechargeHelper.getCharge(boots) == 1, "one vis into the boots: " + thaumcraft.api.items.RechargeHelper.getCharge(boots));
      h.assertTrue(thaumcraft.common.items.armor.TravellerBootsItem.isActive(boots), "charged boots are active");
      h.succeed();
   }

   /** HelmGoggles: infusing goggles into a fortress helm sets the goggles component (NBT outputs of 1.12). */
   @GameTest(template = "empty")
   public static void fortressGogglesInfusion(GameTestHelper h) {
      ServerPlayer player = h.makeMockServerPlayerInLevel();
      ResearchManager.giveRecursiveResearch(player, "FORTRESSMASK");
      List<ItemStack> comps = List.of(new ItemStack(Items.SLIME_BALL), new ItemStack(ModGear.GOGGLES_ITEM.get()));
      ItemStack helm = new ItemStack(ModGear.FORTRESS_HELM.get());
      helm.setDamageValue(7);
      InfusionRecipe r = InfusionMatrixBlockEntity.findMatchingInfusionRecipe(h.getLevel(), comps, helm, player);
      h.assertTrue(r instanceof InfusionDataRecipe, "helm goggles recipe: " + r);
      ItemStack out = r.getRecipeOutput(player, helm, comps);
      h.assertTrue(FortressArmorItem.hasGoggles(out) && out.getDamageValue() == 7, "goggles on the same helm");
      h.assertTrue(((FortressArmorItem) out.getItem()).showIngamePopups(out, player), "the helm reveals");
      h.succeed();
   }
}
