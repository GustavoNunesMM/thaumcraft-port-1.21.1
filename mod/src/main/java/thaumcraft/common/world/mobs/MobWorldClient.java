package thaumcraft.common.world.mobs;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.GrassColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.world.MobWorldgen;

/** Client side of the milestone 13 world blocks: the ambient grass is tinted like grass. */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class MobWorldClient {
   private MobWorldClient() {
   }

   @SubscribeEvent
   public static void blockColors(RegisterColorHandlersEvent.Block e) {
      e.register((state, level, pos, tint) -> level != null && pos != null ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor(),
         MobWorldgen.GRASS_AMBIENT.get());
   }

   @SubscribeEvent
   public static void itemColors(RegisterColorHandlersEvent.Item e) {
      e.register((stack, tint) -> GrassColor.getDefaultColor(), MobWorldgen.GRASS_AMBIENT_ITEM.get());
   }
}
