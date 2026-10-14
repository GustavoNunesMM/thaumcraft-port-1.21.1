package thaumcraft.client;

import java.io.File;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.compat.ShaderCompat;
import thaumcraft.client.fx.DeviceFX;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.fx.FocusFX;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.registers.ModGolems;

/** TEMPORARY dev smoke test for shader-pack rendering (enabled by a run/shader_smoke file). */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class ShaderSmokeTest {
   private static Boolean enabled;
   private static int ticks;
   private static BlockPos base;

   private ShaderSmokeTest() {
   }

   private static void shot(Minecraft mc, String name) {
      String tag = ShaderCompat.isShaderPackInUse() ? "iris" : "vanilla";
      Screenshot.grab(mc.gameDirectory, "shader_" + tag + "_" + name + ".png", mc.getMainRenderTarget(), msg -> {});
      Thaumcraft.LOGGER.info("[shader-smoke] shot {} (iris loaded {}, pack {})", name, ShaderCompat.isIrisLoaded(), ShaderCompat.isShaderPackInUse());
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

   private static void cmd(ServerLevel level, ServerPlayer p, String c) {
      level.getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), c);
   }

   private static String at(BlockPos b) {
      return b.getX() + " " + b.getY() + " " + b.getZ();
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      if (enabled == null) enabled = new File(mc.gameDirectory, "shader_smoke").exists();
      if (!enabled || mc.player == null || mc.level == null || mc.gameMode == null) return;
      ticks++;
      if (ticks == 20) base = mc.player.blockPosition();
      if (base == null) return;
      BlockPos f = base.offset(0, 0, -4);
      if (ticks > 45) {
         // pin the camera (the mouse may move it when the window has focus)
         float pitch = ticks < 270 ? 25.0F : 8.0F;
         mc.player.setYRot(180.0F);
         mc.player.yRotO = 180.0F;
         mc.player.setXRot(pitch);
         mc.player.xRotO = pitch;
      }
      // continuous FX in front of the player
      if (ticks > 60 && ticks < 200) {
         FXDispatcher.drawWispyMotes(f.getX() - 1.5, f.getY() + 1.5, f.getZ(), 0, 0.02, 0, 20, 0.0F);
         DeviceFX.spark(f.getX() + 1.5, f.getY() + 1.5, f.getZ(), 3.0F, 0.6F, 0.2F, 1.0F, 1.0F);
         if (ticks % 2 == 0) FocusFX.arcBolt(f.getX() - 2, f.getY() + 2.5, f.getZ() - 1, f.getX() + 2, f.getY() + 0.5, f.getZ() - 1, 0.5F, 0.6F, 1.0F, 2.0F);
         if (ticks % 10 == 0) FXDispatcher.drawBamf(f.getX(), f.getY() + 1, f.getZ() - 1, true, true, Direction.UP);
      }
      switch (ticks) {
         case 40 -> run(mc, (level, p) -> {
            cmd(level, p, "time set 1000");
            cmd(level, p, "weather clear");
            p.connection.teleport(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 180.0F, 25.0F);
            // seals on the floor ahead (area seal so the corner frames show)
            BlockPos floor = f.below();
            SealHandler.addSealEntity(level, floor, Direction.UP, SealHandler.getSeal("thaumcraft:harvest"), p);
            SealHandler.addSealEntity(level, floor.offset(2, 0, -1), Direction.UP, SealHandler.getSeal("thaumcraft:pickup"), p);
            p.getInventory().setItem(p.getInventory().selected, new ItemStack(ModGolems.GOLEM_BELL.get()));
            cmd(level, p, "summon thaumcraft:golem " + at(f.offset(-2, 0, -2)) + " {NoAI:1b}");
         });
         case 120 -> shot(mc, "1_fx_seals_golem");
         case 125 -> run(mc, (level, p) -> {
            p.connection.teleport(base.getX() + 0.5, base.getY(), base.getZ() + 0.5, 180.0F, 25.0F);
            p.setShiftKeyDown(true);
         });
         case 140 -> shot(mc, "2_seals_sneak");
         case 200 -> run(mc, (level, p) -> {
            p.setShiftKeyDown(false);
            p.getInventory().setItem(p.getInventory().selected, ItemStack.EMPTY);
            cmd(level, p, "kill @e[type=thaumcraft:golem]");
            BlockPos d = base.offset(-3, 0, -5);
            String[] blocks = {"jar_normal", "mirror", "infusion_matrix", "dioptra", "crucible", "recharge_pedestal", "lamp_arcane"};
            for (int i = 0; i < blocks.length; i++) cmd(level, p, "setblock " + at(d.offset(i, 0, 0)) + " thaumcraft:" + blocks[i]);
            cmd(level, p, "setblock " + at(d.offset(0, 0, -2)) + " thaumcraft:eldritch_portal");
         });
         case 260 -> shot(mc, "3_devices");
         case 270 -> run(mc, (level, p) -> {
            for (int x = -3; x < 5; x++) {
               cmd(level, p, "setblock " + at(base.offset(x, 0, -5)) + " minecraft:air");
               cmd(level, p, "setblock " + at(base.offset(x, 0, -7)) + " minecraft:air");
            }
            String[] mobs = {"eldritch_guardian", "cultist_portal_greater", "wisp", "taintacle", "pech", "eldritch_crab", "cultist_knight",
               "flux_rift"};
            for (int i = 0; i < mobs.length; i++) {
               cmd(level, p, "summon thaumcraft:" + mobs[i] + " " + at(base.offset(3 - i, 0, -6 - (i % 2) * 2)) + " {NoAI:1b}");
            }
         });
         case 340 -> shot(mc, "4_mobs");
         case 345 -> run(mc, (level, p) -> cmd(level, p, "time set 18000"));
         case 380 -> shot(mc, "5_mobs_night");
         case 385 -> run(mc, (level, p) -> {
            cmd(level, p, "time set 1000");
            cmd(level, p, "effect give @s thaumcraft:unnatural_hunger 30 0");
         });
         case 420 -> shot(mc, "6_unnatural_hunger");
         case 425 -> run(mc, (level, p) -> {
            cmd(level, p, "effect clear @s");
            cmd(level, p, "effect give @s thaumcraft:blurred_vision 30 0");
         });
         case 460 -> shot(mc, "7_blurred_vision");
         case 465 -> run(mc, (level, p) -> {
            cmd(level, p, "effect clear @s");
            cmd(level, p, "kill @e[type=!minecraft:player]");
         });
         case 480 -> mc.stop();
         default -> {
         }
      }
   }
}
