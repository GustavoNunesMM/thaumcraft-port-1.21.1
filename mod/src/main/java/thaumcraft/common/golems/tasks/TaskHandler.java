package thaumcraft.common.golems.tasks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemEntity;
import thaumcraft.common.golems.seals.SealHandler;

/** The open golem tasks of each dimension (server side). */
public class TaskHandler {
   static final int TASK_LIMIT = 10000;
   public static final ConcurrentHashMap<ResourceKey<Level>, ConcurrentHashMap<Integer, Task>> tasks = new ConcurrentHashMap<>();

   public static void addTask(Level level, Task ticket) {
      ConcurrentHashMap<Integer, Task> dc = getTasks(level);
      if (dc.size() > TASK_LIMIT) {
         Iterator<Task> i = dc.values().iterator();
         if (i.hasNext()) {
            i.next();
            i.remove();
         }
      }
      dc.put(ticket.getId(), ticket);
   }

   @Nullable
   public static Task getTask(Level level, int id) {
      return getTasks(level).get(id);
   }

   public static ConcurrentHashMap<Integer, Task> getTasks(Level level) {
      return tasks.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>());
   }

   private static ArrayList<Task> sorted(Level level, @Nullable UUID uuid, Entity golem, int type) {
      ArrayList<Task> out = new ArrayList<>();
      outer:
      for (Task ticket : getTasks(level).values()) {
         if (ticket.isReserved() || ticket.getType() != type || uuid != null && ticket.getGolemUUID() != null && !uuid.equals(ticket.getGolemUUID())) {
            continue;
         }
         if (type == 1 && (ticket.getEntity() == null || !ticket.getEntity().isAlive())) {
            ticket.setSuspended(true);
            continue;
         }
         if (out.isEmpty()) {
            out.add(ticket);
            continue;
         }
         double d = ticket.getPos().distToCenterSqr(golem.getX(), golem.getY(), golem.getZ()) - ticket.getPriority() * 256;
         for (int a = 0; a < out.size(); a++) {
            double d1 = out.get(a).getPos().distToCenterSqr(golem.getX(), golem.getY(), golem.getZ()) - out.get(a).getPriority() * 256;
            if (d < d1) {
               out.add(a, ticket);
               continue outer;
            }
         }
         out.add(ticket);
      }
      return out;
   }

   /** Unreserved block tasks, nearest (and highest priority) first. */
   public static ArrayList<Task> getBlockTasksSorted(Level level, @Nullable UUID uuid, Entity golem) {
      return sorted(level, uuid, golem, 0);
   }

   public static ArrayList<Task> getEntityTasksSorted(Level level, @Nullable UUID uuid, Entity golem) {
      return sorted(level, uuid, golem, 1);
   }

   public static void completeTask(Task task, GolemEntity golem) {
      if (!task.isCompleted() && !task.isSuspended()) {
         ISealEntity se = SealHandler.getSealEntity(golem.level(), task.getSealPos());
         if (se != null) {
            task.setCompletion(se.getSeal().onTaskCompletion(golem.level(), golem, task));
         } else {
            task.setCompletion(true);
         }
      }
   }

   public static void clearSuspendedOrExpiredTasks(Level level) {
      ConcurrentHashMap<Integer, Task> temp = new ConcurrentHashMap<>();
      for (Task ticket : getTasks(level).values()) {
         if (!ticket.isSuspended() && ticket.getLifespan() > 0L) {
            ticket.setLifespan((short) (ticket.getLifespan() - 1L));
            temp.put(ticket.getId(), ticket);
         } else {
            ISealEntity sEnt = SealHandler.getSealEntity(level, ticket.getSealPos());
            if (sEnt != null) sEnt.getSeal().onTaskSuspension(level, ticket);
         }
      }
      tasks.put(level.dimension(), temp);
   }
}
