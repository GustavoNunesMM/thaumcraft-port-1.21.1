package thaumcraft.common.golems.seals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.util.FakePlayer;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemCrops;
import thaumcraft.common.golems.GolemInteractionHelper;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.items.casters.foci.BlockWorks;

/** Harvest: golems harvest ripe crops in the area and (optionally) replant them with seeds they carry. */
public class SealHarvest extends SealBase implements ISealConfigArea, ISealConfigToggles {
   int delay = RAND.nextInt(33);
   int count = 0;
   final HashMap<Long, ReplantInfo> replantTasks = new HashMap<>();
   protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[]{
      new ISealConfigToggles.SealToggle(true, "prep", "golem.prop.replant"), new ISealConfigToggles.SealToggle(false, "ppro", "golem.prop.provision")};

   public SealHarvest() {
      super("seal_harvest");
   }

   @Override
   public String getKey() {
      return "thaumcraft:harvest";
   }

   @Override
   public ISeal newInstance() {
      return new SealHarvest();
   }

   @Override
   public void tickSeal(Level level, ISealEntity seal) {
      if (delay % 100 == 0) {
         AABB area = GolemHelper.getBoundsForArea(seal);
         Iterator<Map.Entry<Long, ReplantInfo>> rt = replantTasks.entrySet().iterator();
         while (rt.hasNext()) {
            Map.Entry<Long, ReplantInfo> e = rt.next();
            if (!area.contains(Vec3.atCenterOf(BlockPos.of(e.getKey())))) {
               Task tt = TaskHandler.getTask(level, e.getValue().taskid);
               if (tt != null) tt.setSuspended(true);
               rt.remove();
            }
         }
      }
      if (delay++ % 5 != 0) return;
      BlockPos p = GolemHelper.getPosInArea(seal, count++);
      if (GolemCrops.isGrownCrop(level, p)) {
         Task task = new Task(seal.getSealPos(), p);
         task.setPriority(seal.getPriority());
         TaskHandler.addTask(level, task);
      } else if (getToggles()[0].value && replantTasks.containsKey(p.asLong()) && level.isEmptyBlock(p)) {
         ReplantInfo ri = replantTasks.get(p.asLong());
         if (TaskHandler.getTask(level, ri.taskid) == null) {
            Task tt = new Task(seal.getSealPos(), ri.pos);
            tt.setPriority(seal.getPriority());
            TaskHandler.addTask(level, tt);
            ri.taskid = tt.getId();
         }
      }
   }

   @Override
   public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
      if (!(level instanceof ServerLevel sl)) return true;
      BlockPos pos = task.getPos();
      if (GolemCrops.isGrownCrop(level, pos)) {
         FakePlayer fp = GolemInteractionHelper.fakePlayer(sl, golem.getGolemEntity());
         BlockState bs = level.getBlockState(pos);
         Direction face = Direction.getNearest(golem.getGolemEntity().getX() - (pos.getX() + 0.5), golem.getGolemEntity().getY() - pos.getY(),
            golem.getGolemEntity().getZ() - (pos.getZ() + 0.5));
         if (GolemCrops.clickableCrops.contains(bs.getBlock())) {
            bs.useWithoutItem(level, fp, new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
            GolemInteractionHelper.dropSomeItems(fp, golem);
            golem.addRankXp(1);
            golem.swingArm();
         } else {
            GolemInteractionHelper.golemClick(level, golem, pos, task.getSealPos().face, ItemStack.EMPTY, false, true);
            if (GolemCrops.isGrownCrop(level, pos)) {
               fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
               BlockWorks.harvestBlock(sl, fp, pos, true, false, 0, true);
               golem.addRankXp(1);
               golem.swingArm();
               if (getToggles()[0].value) {
                  ItemStack seed = GolemCrops.getSeed(bs.getBlock());
                  if (!seed.isEmpty()) {
                     BlockState bb = level.getBlockState(pos.below());
                     Direction rf = null;
                     if (!(bs.getBlock() instanceof HorizontalDirectionalBlock)) {
                        rf = Direction.DOWN;
                     } else {
                        rf = bs.getValue(HorizontalDirectionalBlock.FACING);
                     }
                     Task tt = new Task(task.getSealPos(), pos);
                     tt.setPriority(task.getPriority());
                     tt.setLifespan((short) 300);
                     replantTasks.put(pos.asLong(), new ReplantInfo(pos, rf, tt.getId(), seed.copy(), bb.getBlock() instanceof FarmBlock));
                     TaskHandler.addTask(level, tt);
                  }
               }
            }
         }
      } else if (replantTasks.containsKey(pos.asLong()) && replantTasks.get(pos.asLong()).taskid == task.getId() && level.isEmptyBlock(pos)
         && golem.isCarrying(replantTasks.get(pos.asLong()).stack)) {
         FakePlayer fp = GolemInteractionHelper.fakePlayer(sl, golem.getGolemEntity());
         BlockState below = level.getBlockState(pos.below());
         ReplantInfo ri = replantTasks.get(pos.asLong());
         if ((below.is(BlockTags.DIRT) || below.is(Blocks.GRASS_BLOCK)) && ri.farmland) {
            level.setBlockAndUpdate(pos.below(), Blocks.FARMLAND.defaultBlockState());
         }
         ItemStack seed = ri.stack.copy();
         seed.setCount(1);
         fp.setItemInHand(InteractionHand.MAIN_HAND, seed.copy());
         BlockPos against = pos.relative(ri.face);
         InteractionResult r = fp.getMainHandItem().useOn(new UseOnContext(fp, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(against), ri.face.getOpposite(), against, false)));
         fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
         if (r.consumesAction()) {
            level.levelEvent(2001, pos, Block.getId(level.getBlockState(pos)));
            golem.dropItem(seed);
            golem.addRankXp(1);
            golem.swingArm();
         }
      }
      task.setSuspended(true);
      return true;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      ReplantInfo ri = replantTasks.get(task.getPos().asLong());
      if (ri != null && ri.taskid == task.getId()) {
         boolean carry = golem.isCarrying(ri.stack);
         if (!carry && getToggles()[1].value) {
            ISealEntity se = SealHandler.getSealEntity(golem.getGolemWorld(), task.getSealPos());
            if (se != null) GolemHelper.requestProvisioning(golem.getGolemWorld(), se, ri.stack);
         }
         return carry;
      }
      return true;
   }

   @Override
   public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
      ListTag list = nbt.getList("replant", Tag.TAG_COMPOUND);
      for (int i = 0; i < list.size(); i++) {
         CompoundTag t = list.getCompound(i);
         long loc = t.getLong("taskloc");
         ItemStack stack = ItemStack.parseOptional(registries, t.getCompound("stack"));
         replantTasks.put(loc, new ReplantInfo(BlockPos.of(loc), Direction.from3DDataValue(t.getByte("taskface")), 0, stack, t.getBoolean("farmland")));
      }
   }

   @Override
   public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
      if (!getToggles()[0].value) return;
      ListTag list = new ListTag();
      for (ReplantInfo info : replantTasks.values()) {
         if (info.stack.isEmpty()) continue;
         CompoundTag t = new CompoundTag();
         t.putLong("taskloc", info.pos.asLong());
         t.putByte("taskface", (byte) info.face.get3DDataValue());
         t.putBoolean("farmland", info.farmland);
         t.put("stack", info.stack.save(registries));
         list.add(t);
      }
      nbt.put("replant", list);
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_AREA, CAT_TOGGLES, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public SealToggle[] getToggles() {
      return props;
   }

   @Override
   public void setToggle(int indx, boolean value) {
      props[indx].setValue(value);
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.DEFT, EnumGolemTrait.SMART};
   }

   private static class ReplantInfo {
      final Direction face;
      final BlockPos pos;
      int taskid;
      final ItemStack stack;
      final boolean farmland;

      ReplantInfo(BlockPos pos, Direction face, int taskid, ItemStack stack, boolean farmland) {
         this.pos = pos;
         this.face = face;
         this.taskid = taskid;
         this.stack = stack;
         this.farmland = farmland;
      }
   }
}
