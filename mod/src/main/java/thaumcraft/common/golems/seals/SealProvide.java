package thaumcraft.common.golems.seals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.ProvisionRequest;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemEntity;
import thaumcraft.common.golems.GolemInvHelper;
import thaumcraft.common.golems.tasks.TaskHandler;

/**
 * Provide: answers provisioning requests within 64 blocks with items from this inventory. Task data 0 fetches the
 * items here; data 1 (an entity) or 2 (a block face) delivers them.
 */
public class SealProvide extends SealFiltered implements ISealConfigToggles {
   int delay = RAND.nextInt(88);
   protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[]{
      new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"), new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
      new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"), new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod"),
      new ISealConfigToggles.SealToggle(false, "psing", "golem.prop.single"), new ISealConfigToggles.SealToggle(false, "pleave", "golem.prop.leave")};

   public SealProvide() {
      super("seal_provider");
   }

   @Override
   public String getKey() {
      return "thaumcraft:provider";
   }

   @Override
   public ISeal newInstance() {
      return new SealProvide();
   }

   @Override
   public int getFilterSize() {
      return 9;
   }

   @Override
   public void tickSeal(Level level, ISealEntity seal) {
      ArrayList<ProvisionRequest> requests = GolemHelper.provisionRequests.get(level.dimension());
      if (delay % 100 == 0 && requests != null) {
         Iterator<ProvisionRequest> it = requests.iterator();
         while (it.hasNext()) {
            ProvisionRequest pr = it.next();
            if (pr.isInvalid() || pr.getLinkedTask() == null || pr.getLinkedTask().isSuspended() || pr.getLinkedTask().isCompleted()
               || pr.getTimeout() < System.currentTimeMillis()) {
               it.remove();
            }
         }
      }
      if (delay++ % 20 != 0) return;
      IItemHandler inv = GolemInvHelper.getItemHandlerAt(level, seal.getSealPos().pos, seal.getSealPos().face);
      if (inv == null || requests == null) return;
      BlockPos sp = seal.getSealPos().pos;
      ListIterator<ProvisionRequest> it = requests.listIterator();
      while (it.hasNext()) {
         ProvisionRequest pr = it.next();
         if (pr.isInvalid()) {
            it.remove();
            continue;
         }
         if (pr.getLinkedTask() != null) continue;
         boolean near = pr.getSeal() != null && pr.getSeal().getSealPos().pos.distSqr(sp) < 4096.0
            || pr.getEntity() != null && sp.distToCenterSqr(pr.getEntity().getX(), pr.getEntity().getY(), pr.getEntity().getZ()) < 4096.0
            || pr.getPos() != null && sp.distSqr(pr.getPos()) < 4096.0;
         if (!near) continue;
         NonNullList<ItemStack> stacks = NonNullList.withSize(1, pr.getStack());
         if (!GolemInvHelper.findFirstMatchFromFilter(getInv(), getSizes(), blacklist, stacks, invFilter(props)).isEmpty()
            && GolemInvHelper.countTotalItemsIn(inv, pr.getStack(), GolemInvHelper.InvFilter.STRICT) > (props[5].value ? 1 : 0)) {
            Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
            task.setPriority(pr.getSeal() != null ? pr.getSeal().getPriority() : 5);
            task.setLifespan((short) (pr.getSeal() != null ? 10 : 31000));
            TaskHandler.addTask(level, task);
            pr.setLinkedTask(task);
            task.setLinkedProvision(pr);
            break;
         }
      }
   }

   public boolean matchesFilters(ItemStack stack) {
      return GolemInvHelper.matchesFilters(getInv(), blacklist, stack, invFilter(props));
   }

   @Override
   public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
      ProvisionRequest linked = task.getLinkedProvision();
      if (linked != null) {
         if (task.getData() == 0) {
            IItemHandler inv = GolemInvHelper.getItemHandlerAt(level, task.getSealPos().pos, task.getSealPos().face);
            if (inv != null) {
               ItemStack stack = linked.getStack().copy();
               if (props[4].value) stack.setCount(1);
               int sa;
               if (!stack.isEmpty() && props[5].value && (sa = GolemInvHelper.countTotalItemsIn(inv, stack, GolemInvHelper.InvFilter.STRICT)) <= stack.getCount()) {
                  stack.setCount(sa - 1);
               }
               if (!stack.isEmpty()) {
                  int limit = golem.canCarryAmount(stack);
                  if (limit > 0) {
                     ItemStack s = golem.holdItem(GolemInvHelper.removeStackFrom(inv, GolemInvHelper.copyLimitedStack(stack, limit),
                        GolemInvHelper.InvFilter.STRICT, false));
                     Direction face = task.getSealPos().face;
                     if (!s.isEmpty()) GolemInvHelper.ejectStackAt(level, task.getSealPos().pos.relative(face), face.getOpposite(), s);
                     golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, 0.125F,
                        ((level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                     golem.addRankXp(1);
                     golem.swingArm();
                     if (linked.getEntity() != null || linked.getPos() != null) {
                        Task task2 = linked.getEntity() != null ? new Task(task.getSealPos(), linked.getEntity()) : new Task(task.getSealPos(), linked.getPos());
                        task2.setPriority(task.getPriority());
                        task2.setData(linked.getEntity() != null ? 1 : 2);
                        task2.setLifespan((short) 31000);
                        TaskHandler.addTask(level, task2);
                        linked.setLinkedTask(task2);
                        task2.setLinkedProvision(linked);
                     }
                  }
               }
            }
         } else {
            ItemStack cs = linked.getStack();
            ItemStack sx = golem.dropItem(cs);
            if (sx.getCount() < cs.getCount()) {
               ItemStack ps = cs.copy();
               ps.setCount(cs.getCount() - sx.getCount());
               if (task.getData() == 1) {
                  GolemHelper.requestProvisioning(level, linked.getEntity(), ps);
               } else {
                  GolemHelper.requestProvisioning(level, linked.getPos(), linked.getSide(), ps);
               }
            }
            if (task.getData() == 1) {
               GolemInvHelper.dropItemAtEntity(level, sx, linked.getEntity());
            } else {
               ItemStack back = GolemInvHelper.ejectStackAt(level, linked.getPos().relative(linked.getSide()), linked.getSide().getOpposite(), sx, true);
               if (!back.isEmpty()) golem.holdItem(back);
            }
            golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, 0.125F, (level.random.nextFloat() - level.random.nextFloat()) * 0.7F + 1.0F);
            golem.swingArm();
            linked.setInvalid(true);
         }
      }
      task.setSuspended(true);
      return true;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      ProvisionRequest pr = task.getLinkedProvision();
      if (pr == null) return false;
      GolemEntity g = (GolemEntity) golem;
      boolean b = pr.getSeal() != null && g.isWithinRestriction(pr.getSeal().getSealPos().pos)
         || pr.getEntity() != null && g.isWithinRestriction(pr.getEntity().blockPosition()) || pr.getPos() != null && g.isWithinRestriction(pr.getPos());
      if (!b || !areGolemTagsValidForTask(pr.getSeal(), golem)) return false;
      return task.getData() == 0 ? !golem.isCarrying(pr.getStack()) && golem.canCarry(pr.getStack(), true) : golem.isCarrying(pr.getStack());
   }

   private boolean areGolemTagsValidForTask(ISealEntity se, IGolemAPI golem) {
      if (se == null) return true;
      if (se.isLocked() && golem instanceof OwnableEntity o && (o.getOwnerUUID() == null || !o.getOwnerUUID().toString().equals(se.getOwner()))) {
         return false;
      }
      if (se.getSeal().getRequiredTags() != null && !golem.getProperties().getTraits().containsAll(Arrays.asList(se.getSeal().getRequiredTags()))) {
         return false;
      }
      if (se.getSeal().getForbiddenTags() != null) {
         for (EnumGolemTrait tag : se.getSeal().getForbiddenTags()) {
            if (golem.getProperties().getTraits().contains(tag)) return false;
         }
      }
      return true;
   }

   @Override
   public void onTaskSuspension(Level level, Task task) {
      if (task.getLinkedProvision() != null) task.getLinkedProvision().setLinkedTask(null);
      task.setLinkedProvision(null);
   }

   @Override
   public boolean canPlaceAt(Level level, BlockPos pos, Direction side) {
      return GolemInvHelper.getItemHandlerAt(level, pos, side) != null;
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_FILTER, CAT_TOGGLES, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public EnumGolemTrait[] getForbiddenTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.CLUMSY};
   }

   @Override
   public SealToggle[] getToggles() {
      return props;
   }

   @Override
   public void setToggle(int indx, boolean value) {
      props[indx].setValue(value);
   }
}
