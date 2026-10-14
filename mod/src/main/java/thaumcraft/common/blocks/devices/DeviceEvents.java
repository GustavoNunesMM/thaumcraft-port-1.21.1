package thaumcraft.common.blocks.devices;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.NoteBlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.Thaumcraft;

/** Game events of the devices: note blocks heard by arcane ears (WorldEvents.noteEvent / ServerEvents in 1.12). */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class DeviceEvents {
   private static final Map<Level, Long> MARK = new WeakHashMap<>();

   private DeviceEvents() {
   }

   @SubscribeEvent
   public static void noteEvent(NoteBlockEvent.Play event) {
      if (event.getLevel() instanceof ServerLevel level) {
         ArcaneEarBlockEntity.recordNote(level, event.getPos(), event.getInstrument().ordinal(), event.getVanillaNoteId());
      }
   }

   /** Forgets the notes played before the previous tick (every ear has had a chance to hear them). */
   @SubscribeEvent
   public static void levelTick(LevelTickEvent.Pre event) {
      if (!(event.getLevel() instanceof ServerLevel level)) return;
      List<ArcaneEarBlockEntity.NoteEvent> list = ArcaneEarBlockEntity.NOTE_EVENTS.get(level);
      if (list == null) return;
      long mark = MARK.getOrDefault(level, -1L);
      synchronized (ArcaneEarBlockEntity.class) {
         list.removeIf(e -> e.serial() <= mark);
         MARK.put(level, list.isEmpty() ? mark : list.get(list.size() - 1).serial());
      }
   }
}
