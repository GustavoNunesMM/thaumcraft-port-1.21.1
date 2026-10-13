package thaumcraft.common.lib;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import thaumcraft.Thaumcraft;

/** ServerEvents.addRunnableServer: runs something on the server thread after a number of ticks. */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ServerTasks {
   private record Task(int runAt, Runnable runnable) {}

   private static final List<Task> TASKS = new ArrayList<>();
   private static int tick;

   private ServerTasks() {
   }

   public static synchronized void schedule(int delayTicks, Runnable runnable) {
      TASKS.add(new Task(tick + Math.max(0, delayTicks), runnable));
   }

   @SubscribeEvent
   public static void serverTick(ServerTickEvent.Post event) {
      List<Task> due = new ArrayList<>();
      synchronized (ServerTasks.class) {
         tick++;
         TASKS.removeIf(t -> {
            if (t.runAt() <= tick) {
               due.add(t);
               return true;
            }
            return false;
         });
      }
      for (Task t : due) {
         try {
            t.runnable().run();
         } catch (Exception e) {
            Thaumcraft.LOGGER.error("Scheduled task failed", e);
         }
      }
   }

   @SubscribeEvent
   public static void stopped(ServerStoppedEvent event) {
      synchronized (ServerTasks.class) {
         TASKS.clear();
      }
   }
}
