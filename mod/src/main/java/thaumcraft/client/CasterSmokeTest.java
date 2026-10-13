package thaumcraft.client;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.client.gui.FocalManipulatorScreen;
import thaumcraft.client.gui.FocusRadialScreen;
import thaumcraft.common.blocks.crafting.FocalManipulatorBlockEntity;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.items.casters.CasterItem;
import thaumcraft.common.items.casters.FocusItem;
import thaumcraft.common.items.casters.FocusPouchItem;
import thaumcraft.common.menu.FocusPouchMenu;
import thaumcraft.common.registers.ModCasters;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Dev-only client smoke test of milestone 9, run when the file "caster_smoke" exists in the game directory
 * (./gradlew runClient -PtcQuickPlay=tc_test). Screenshots the caster HUD, the focus selector, a cast, the focal
 * manipulator, the focus pouch, the plan preview and the focus entities, then quits.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class CasterSmokeTest {
   private static Boolean enabled;
   private static int ticks;
   private static BlockPos base;

   private CasterSmokeTest() {
   }

   private static void shot(Minecraft mc, String name) {
      Screenshot.grab(mc.gameDirectory, "caster_" + name + ".png", mc.getMainRenderTarget(), msg -> {});
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

   private static FocusPackage pack(String... keys) {
      FocusPackage p = new FocusPackage();
      int c = 0;
      for (String k : keys) {
         FocusNode n = (FocusNode) FocusEngine.getElement(k);
         p.addNode(n);
         c += n.getComplexity();
      }
      p.setComplexity(c);
      return p;
   }

   private static ItemStack focus(String name, FocusPackage p) {
      ItemStack f = new ItemStack(ModCasters.FOCUS_2.get());
      f.set(DataComponents.CUSTOM_NAME, Component.literal(name));
      FocusItem.setPackage(f, p);
      return f;
   }

   private static ItemStack caster(ItemStack focus) {
      ItemStack c = new ItemStack(ModItems.CASTER_BASIC.get());
      ((CasterItem) c.getItem()).setFocus(c, focus);
      return c;
   }

   private static void setup(ServerLevel level, ServerPlayer player) {
      player.setGameMode(GameType.SURVIVAL);
      player.getAbilities().mayfly = true;
      player.getAbilities().flying = true;
      player.onUpdateAbilities();
      ResearchManager.giveAllResearch(player);
      level.setDayTime(6000);
      level.setWeatherParameters(6000, 0, false, false);
      base = player.blockPosition().offset(0, 30, 0);
      for (int x = -8; x <= 8; x++) {
         for (int z = -4; z <= 12; z++) {
            level.setBlock(base.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 2);
            for (int y = 1; y <= 8; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
         }
      }
      player.teleportTo(level, base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5, 0.0F, 15.0F);
      player.getInventory().clearContent();
      player.getInventory().setItem(0, caster(focus("Burning Touch", pack("ROOT", "thaumcraft.TOUCH", "thaumcraft.FIRE"))));
      player.getInventory().setItem(1, focus("Frost Bolt", pack("ROOT", "thaumcraft.BOLT", "thaumcraft.FROST")));
      ItemStack pouch = new ItemStack(ModCasters.FOCUS_POUCH.get());
      NonNullList<ItemStack> inv = NonNullList.withSize(FocusPouchItem.SIZE, ItemStack.EMPTY);
      inv.set(0, focus("Earth Orb", pack("ROOT", "thaumcraft.PROJECTILE", "thaumcraft.EARTH")));
      inv.set(1, focus("Healing Cloud", pack("ROOT", "thaumcraft.CLOUD", "thaumcraft.HEAL")));
      inv.set(2, focus("Curse Bat", pack("ROOT", "thaumcraft.SPELLBAT", "thaumcraft.CURSE")));
      FocusPouchItem.setInventory(pouch, inv);
      player.getInventory().setItem(2, pouch);
      player.getInventory().setItem(3, caster(focus("Planner", pack("ROOT", "thaumcraft.PLAN", "thaumcraft.BREAK"))));
      for (Aspect a : new Aspect[]{Aspect.AVERSION, Aspect.FIRE, Aspect.ENERGY, Aspect.COLD, Aspect.DEATH}) player.getInventory().add(VisCrystalItem.of(a, 4));
      player.getInventory().selected = 0;
      player.experienceLevel = 30;
      level.setBlock(base.offset(3, 1, 3), ModCasters.WAND_WORKBENCH.get().defaultBlockState(), 3);
      if (level.getBlockEntity(base.offset(3, 1, 3)) instanceof FocalManipulatorBlockEntity be) {
         be.inventory.setItem(0, new ItemStack(ModCasters.FOCUS_2.get()));
      }
      AuraHandler.getAuraChunk(level, base).setVis(300);
      Cow cow = EntityType.COW.create(level);
      cow.moveTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 3.5, 180.0F, 0.0F);
      cow.setNoAi(true);
      level.addFreshEntity(cow);
   }

   private static void click(Minecraft mc, double x, double y, int button) {
      if (mc.screen != null) {
         mc.screen.mouseClicked(x, y, button);
         mc.screen.mouseReleased(x, y, button);
      }
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (enabled == null) enabled = new File(mc.gameDirectory, "caster_smoke").exists();
      if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null) return;
      ticks++;
      switch (ticks) {
         case 40 -> run(mc, CasterSmokeTest::setup);
         case 100 -> shot(mc, "1_hud");
         case 105 -> {
            FocusRadialScreen.smokeHold = true;
            FocusRadialScreen.open();
         }
         case 125 -> shot(mc, "2_radial");
         case 130 -> {
            FocusRadialScreen.smokeHold = false;
            mc.setScreen(null);
         }
         case 135 -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         case 137 -> shot(mc, "3_cast_fire");
         case 150 -> run(mc, (level, p) -> {
            if (level.getBlockEntity(base.offset(3, 1, 3)) instanceof FocalManipulatorBlockEntity be) p.openMenu(be, be.getBlockPos());
         });
         case 170 -> {
            if (mc.screen instanceof FocalManipulatorScreen s) {
               int gx = s.getGuiLeft(), gy = s.getGuiTop();
               click(mc, gx + 38, gy + 42, 0);
            }
         }
         case 175 -> {
            if (mc.screen instanceof FocalManipulatorScreen s) click(mc, s.getGuiLeft() + 38, s.getGuiTop() + 42 + 48, 0);
         }
         case 190 -> shot(mc, "4_manipulator");
         case 195 -> {
            if (mc.screen instanceof FocalManipulatorScreen s) click(mc, s.getGuiLeft() + 242, s.getGuiTop() + 18, 0);
         }
         case 200 -> mc.setScreen(null);
         case 240 -> shot(mc, "5_manipulator_crafting");
         case 250 -> run(mc, (level, p) -> {
            p.getInventory().selected = 2;
            p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new FocusPouchMenu(id, inv, InteractionHand.MAIN_HAND, 2),
               Component.translatable("item.thaumcraft.focus_pouch")), buf -> {
               buf.writeBoolean(true);
               buf.writeVarInt(2);
            });
         });
         case 270 -> shot(mc, "6_pouch");
         case 275 -> {
            mc.setScreen(null);
            mc.player.getInventory().selected = 3;
            mc.player.setXRot(40.0F);
            run(mc, (level, p) -> {
               p.getInventory().selected = 3;
               p.setXRot(40.0F);
            });
         }
         case 300 -> shot(mc, "7_plan_preview");
         case 310 -> run(mc, (level, p) -> {
            p.setXRot(10.0F);
            FocusEngine.castFocusPackage(p, pack("ROOT", "thaumcraft.SPELLBAT", "thaumcraft.CURSE"));
            FocusEngine.castFocusPackage(p, pack("ROOT", "thaumcraft.CLOUD", "thaumcraft.HEAL"));
            FocusEngine.castFocusPackage(p, pack("ROOT", "thaumcraft.MINE", "thaumcraft.FIRE"));
            FocusEngine.castFocusPackage(p, pack("ROOT", "thaumcraft.BOLT", "thaumcraft.FROST"));
         });
         case 312 -> {
            mc.player.setXRot(10.0F);
            mc.player.getInventory().selected = 5;
         }
         case 314 -> shot(mc, "8_bolt");
         case 360 -> shot(mc, "9_entities");
         case 380 -> mc.stop();
         default -> {
         }
      }
   }
}
