package thaumcraft.common.golems.seals;

import java.util.HashMap;
import java.util.Iterator;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemEntity;
import thaumcraft.common.golems.GolemInvHelper;
import thaumcraft.common.golems.tasks.TaskHandler;

/** Collect: golems pick up the items lying in the area. */
public class SealPickup extends SealFiltered implements ISealConfigArea {
   int delay = RAND.nextInt(100);
   HashMap<Integer, Integer> itemEntities = new HashMap<>();
   protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[]{
      new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"), new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
      new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"), new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod")};

   public SealPickup() {
      this("seal_pickup");
   }

   protected SealPickup(String icon) {
      super(icon);
   }

   @Override
   public String getKey() {
      return "thaumcraft:pickup";
   }

   @Override
   public ISeal newInstance() {
      return new SealPickup();
   }

   @Override
   public void tickSeal(Level level, ISealEntity seal) {
      if (delay++ % 5 != 0) return;
      AABB area = GolemHelper.getBoundsForArea(seal);
      for (ItemEntity ent : level.getEntitiesOfClass(ItemEntity.class, area)) {
         if (ent.onGround() && !ent.hasPickUpDelay() && !ent.getItem().isEmpty() && !itemEntities.containsValue(ent.getId())) {
            ItemStack stack = GolemInvHelper.findFirstMatchFromFilter(filter, filterSize, isBlacklist(), NonNullList.withSize(1, ent.getItem()),
               invFilter(props));
            if (!stack.isEmpty()) {
               Task task = new Task(seal.getSealPos(), ent);
               task.setPriority(seal.getPriority());
               itemEntities.put(task.getId(), ent.getId());
               TaskHandler.addTask(level, task);
               break;
            }
         }
      }
      if (delay % 100 != 0) {
         Iterator<Integer> it = itemEntities.values().iterator();
         while (it.hasNext()) {
            Entity ex = level.getEntity(it.next());
            if (ex == null || !ex.isAlive()) it.remove();
         }
      }
   }

   @Override
   public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
      ItemEntity ei = getItemEntity(level, task);
      if (ei != null && !ei.getItem().isEmpty()) {
         ItemStack stack = GolemInvHelper.findFirstMatchFromFilter(filter, filterSize, isBlacklist(), NonNullList.withSize(1, ei.getItem()),
            invFilter(props));
         if (!stack.isEmpty()) {
            ItemStack is = golem.holdItem(ei.getItem().copy());
            if (!is.isEmpty()) {
               ei.setItem(is);
            } else {
               ei.discard();
            }
            golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, 0.125F, ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            golem.swingArm();
         }
      }
      task.setSuspended(true);
      itemEntities.remove(task.getId());
      GolemEntity g = (GolemEntity) golem;
      for (Task ticket : TaskHandler.getEntityTasksSorted(level, null, g)) {
         if (itemEntities.containsKey(ticket.getId()) && ticket.canGolemPerformTask(golem) && g.isWithinRestriction(ticket.getEntity().blockPosition())) {
            g.setTask(ticket);
            ticket.setReserved(true);
            if (GolemEntity.showEmotes) level.broadcastEntityEvent(g, (byte) 5);
            break;
         }
      }
      return true;
   }

   protected ItemEntity getItemEntity(Level level, Task task) {
      Integer ei = itemEntities.get(task.getId());
      if (ei != null && level.getEntity(ei) instanceof ItemEntity item) return item;
      return null;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      ItemEntity ei = getItemEntity(golem.getGolemWorld(), task);
      if (ei == null) return false;
      if (!ei.isAlive()) {
         task.setSuspended(true);
         return false;
      }
      return golem.canCarry(ei.getItem(), true);
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_AREA, CAT_FILTER, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public EnumGolemTrait[] getForbiddenTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.CLUMSY};
   }
}
