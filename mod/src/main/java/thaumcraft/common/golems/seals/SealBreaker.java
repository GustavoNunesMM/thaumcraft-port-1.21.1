package thaumcraft.common.golems.seals;

import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemBlockUtils;
import thaumcraft.common.golems.GolemInteractionHelper;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.items.casters.foci.BlockWorks;

/** Block breaker: breakers grind down the blocks in the area that pass the filter; harder blocks take longer. */
public class SealBreaker extends SealFiltered implements ISealConfigArea, ISealConfigToggles {
   int delay = RAND.nextInt(42);
   final HashMap<Integer, Long> cache = new HashMap<>();
   protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[]{new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta")};

   public SealBreaker() {
      this("seal_breaker");
   }

   protected SealBreaker(String icon) {
      super(icon);
   }

   @Override
   public String getKey() {
      return "thaumcraft:breaker";
   }

   @Override
   public ISeal newInstance() {
      return new SealBreaker();
   }

   @Override
   public void tickSeal(Level level, ISealEntity seal) {
      if (delay % 100 == 0) cache.keySet().removeIf(id -> TaskHandler.getTask(level, id) == null);
      delay++;
      BlockPos p = GolemHelper.getPosInArea(seal, delay);
      if (!cache.containsValue(p.asLong()) && isValidBlock(level, p)) {
         Task task = new Task(seal.getSealPos(), p);
         task.setPriority(seal.getPriority());
         task.setData((int) (level.getBlockState(p).getDestroySpeed(level, p) * 10.0F));
         TaskHandler.addTask(level, task);
         cache.put(task.getId(), p.asLong());
      }
   }

   /**
    * Not air nor unbreakable, and passing the filter (compared as the block's item). 1.12 rejected a block when any
    * whitelist entry differed from it, so only single-entry whitelists worked; here any matching entry lets it pass.
    */
   private boolean isValidBlock(Level level, BlockPos p) {
      BlockState bs = level.getBlockState(p);
      if (level.isEmptyBlock(p) || bs.getDestroySpeed(level, p) < 0.0F) return false;
      ItemStack fs = new ItemStack(bs.getBlock());
      boolean any = false;
      boolean matched = false;
      for (ItemStack ts : getInv()) {
         if (ts.isEmpty()) continue;
         any = true;
         if (!fs.isEmpty() && ts.is(fs.getItem())) matched = true;
      }
      if (!any) return true;
      return isBlacklist() != matched;
   }

   @Override
   public boolean onTaskCompletion(Level level, IGolemAPI golem, Task task) {
      BlockState bs = level.getBlockState(task.getPos());
      if (cache.containsKey(task.getId()) && isValidBlock(level, task.getPos()) && level instanceof ServerLevel sl) {
         golem.swingArm();
         boolean silky = getToggles().length > 1 && getToggles()[1].value;
         int bspd = silky ? 7 : 21;
         if (task.getData() > bspd) {
            float bh = bs.getDestroySpeed(level, task.getPos()) * 10.0F;
            task.setLifespan((short) Math.max(task.getLifespan(), 10L));
            task.setData(task.getData() - bspd);
            int progress = (int) (9.0F * (1.0F - task.getData() / bh));
            SoundType st = bs.getSoundType(level, task.getPos(), golem.getGolemEntity());
            level.playSound(null, task.getPos(), st.getBreakSound(), SoundSource.BLOCKS, (st.getVolume() + 0.7F) / 8.0F, st.getPitch() * 0.5F);
            GolemBlockUtils.destroyBlockPartially(level, golem.getGolemEntity().getId(), task.getPos(), progress);
            return false;
         }
         GolemBlockUtils.destroyBlockPartially(level, golem.getGolemEntity().getId(), task.getPos(), -1);
         BlockWorks.harvestBlock(sl, GolemInteractionHelper.fakePlayer(sl, golem.getGolemEntity()), task.getPos(), true, silky, 0, true);
         golem.addRankXp(1);
         cache.remove(task.getId());
      }
      task.setSuspended(true);
      return true;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      if (cache.containsKey(task.getId()) && isValidBlock(golem.getGolemWorld(), task.getPos())) return true;
      task.setSuspended(true);
      return false;
   }

   @Override
   public void onTaskSuspension(Level level, Task task) {
      cache.remove(task.getId());
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{CAT_AREA, CAT_FILTER, CAT_TOGGLES, CAT_PRIORITY, CAT_TAGS};
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.BREAKER};
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
