package thaumcraft.client;

import java.util.function.BiConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.blocks.essentia.JarBlockEntity;
import thaumcraft.common.lib.CuriosHelper;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModGear;

/**
 * Dev-only client check of the milestone 10 rendering, enabled with the TC_GEAR_SMOKE environment variable
 * (TC_GEAR_SMOKE=1 ./gradlew runClient -PtcQuickPlay=tc_test): dresses the player in each armor set, looks at it from the
 * front and saves screenshots (run/screenshots/gear_*.png), then shows the goggles popup over a jar and quits.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class GearSmokeTest {
   static final boolean ENABLED = System.getenv("TC_GEAR_SMOKE") != null;
   private static int ticks;

   private GearSmokeTest() {
   }

   private static void run(Minecraft mc, BiConsumer<ServerLevel, ServerPlayer> task) {
      MinecraftServer server = mc.getSingleplayerServer();
      if (server == null || mc.player == null) return;
      var uuid = mc.player.getUUID();
      server.execute(() -> {
         ServerPlayer p = server.getPlayerList().getPlayer(uuid);
         if (p != null) task.accept(p.serverLevel(), p);
      });
   }

   private static void wear(Minecraft mc, ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
      run(mc, (level, p) -> {
         p.setItemSlot(EquipmentSlot.HEAD, head);
         p.setItemSlot(EquipmentSlot.CHEST, chest);
         p.setItemSlot(EquipmentSlot.LEGS, legs);
         p.setItemSlot(EquipmentSlot.FEET, feet);
      });
   }

   private static void shot(Minecraft mc, String name) {
      Screenshot.grab(mc.gameDirectory, "gear_" + name + ".png", mc.getMainRenderTarget(), msg -> {});
      Thaumcraft.LOGGER.info("[gear-smoke] screenshot {}", name);
   }

   @SubscribeEvent
   public static void tick(ClientTickEvent.Post event) {
      if (!ENABLED) return;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) return;
      ticks++;
      switch (ticks) {
         case 40 -> {
            ItemStack helm = new ItemStack(ModGear.FORTRESS_HELM.get());
            helm.set(ModGear.MASK.get(), 0);
            helm.set(ModGear.GOGGLES.get(), Unit.INSTANCE);
            wear(mc, helm, new ItemStack(ModGear.FORTRESS_CHEST.get()), new ItemStack(ModGear.FORTRESS_LEGS.get()),
               new ItemStack(ModGear.TRAVELLER_BOOTS.get()));
            run(mc, (level, p) -> CuriosHelper.equip(p, "necklace", new ItemStack(ModGear.AMULET_VIS_CRAFTED.get())));
            mc.player.setXRot(0.0F);
            mc.player.getInventory().selected = 8;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
         }
         case 80 -> shot(mc, "1_fortress");
         case 90 -> wear(mc, new ItemStack(ModGear.VOID_ROBE_HELM.get()), new ItemStack(ModGear.VOID_ROBE_CHEST.get()),
            new ItemStack(ModGear.VOID_ROBE_LEGS.get()), new ItemStack(ModGear.CRIMSON_BOOTS.get()));
         case 130 -> shot(mc, "2_void_robe");
         case 140 -> wear(mc, new ItemStack(ModGear.CRIMSON_PRAETOR_HELM.get()), new ItemStack(ModGear.CRIMSON_PRAETOR_CHEST.get()),
            new ItemStack(ModGear.CRIMSON_PRAETOR_LEGS.get()), new ItemStack(ModGear.CRIMSON_BOOTS.get()));
         case 180 -> shot(mc, "3_praetor");
         case 190 -> wear(mc, new ItemStack(ModGear.CRIMSON_PLATE_HELM.get()), new ItemStack(ModGear.CRIMSON_PLATE_CHEST.get()),
            new ItemStack(ModGear.CRIMSON_PLATE_LEGS.get()), new ItemStack(ModGear.VOID_BOOTS.get()));
         case 230 -> shot(mc, "4_crimson_plate");
         case 240 -> {
            ItemStack chest = new ItemStack(ModGear.CLOTH_CHEST.get());
            chest.set(net.minecraft.core.component.DataComponents.DYED_COLOR, new DyedItemColor(0x3050C0, true));
            wear(mc, ItemStack.EMPTY, chest, new ItemStack(ModGear.CLOTH_LEGS.get()), new ItemStack(ModGear.THAUMIUM_BOOTS.get()));
            run(mc, (level, p) -> CuriosHelper.equip(p, "head", new ItemStack(ModGear.GOGGLES_ITEM.get())));
         }
         case 280 -> shot(mc, "5_robes_goggles_curio");
         case 290 -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            run(mc, (level, p) -> {
               BlockPos jar = p.blockPosition().relative(p.getDirection(), 2);
               level.setBlockAndUpdate(jar.below(), Blocks.STONE.defaultBlockState());
               level.setBlockAndUpdate(jar, ModBlocks.JAR_NORMAL.get().defaultBlockState());
               if (level.getBlockEntity(jar) instanceof JarBlockEntity j) j.addToContainer(Aspect.FIRE, 32);
            });
            mc.player.setXRot(30.0F);
         }
         case 330 -> shot(mc, "6_goggles_popup");
         case 340 -> {
            Thaumcraft.LOGGER.info("[gear-smoke] DONE");
            mc.stop();
         }
         default -> {
         }
      }
   }
}
