package thaumcraft.common.golems.seals;

import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.client.gui.SealBaseContainer;
import thaumcraft.common.golems.client.gui.SealBaseGUI;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.lib.network.FakeNetHandlerPlayServer;
import thaumcraft.common.lib.utils.BlockUtils;

public class SealBreaker extends SealFiltered implements ISealConfigArea, ISealConfigToggles {
   int delay = new Random(System.nanoTime()).nextInt(42);
   HashMap<Integer, Long> cache = new HashMap<>();
   ResourceLocation icon = new ResourceLocation("thaumcraft", "items/seals/seal_breaker");
   protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[]{new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta")};

   @Override
   public String getKey() {
      return "thaumcraft:breaker";
   }

   @Override
   public void tickSeal(World world, ISealEntity seal) {
      if (this.delay % 100 == 0) {
         Iterator<Integer> it = this.cache.keySet().iterator();

         while (it.hasNext()) {
            Task t = TaskHandler.getTask(world.provider.getDimension(), it.next());
            if (t == null) {
               it.remove();
            }
         }
      }

      this.delay++;
      BlockPos p = GolemHelper.getPosInArea(seal, this.delay);
      if (!this.cache.containsValue(p.toLong()) && this.isValidBlock(world, p)) {
         Task task = new Task(seal.getSealPos(), p);
         task.setPriority(seal.getPriority());
         task.setData((int)(world.getBlockState(p).getBlockHardness(world, p) * 10.0F));
         TaskHandler.addTask(world.provider.getDimension(), task);
         this.cache.put(task.getId(), p.toLong());
      }
   }

   private boolean isValidBlock(World world, BlockPos p) {
      IBlockState bs = world.getBlockState(p);
      if (!world.isAirBlock(p) && bs.getBlockHardness(world, p) >= 0.0F) {
         for (ItemStack ts : this.getInv()) {
            if (ts != null && !ts.isEmpty()) {
               ItemStack fs = BlockUtils.getSilkTouchDrop(bs);
               if (fs == null || !fs.isEmpty()) {
                  fs = new ItemStack(bs.getBlock(), 1, !this.getToggles()[0].value ? 32767 : bs.getBlock().getMetaFromState(bs));
               }

               if (!this.getToggles()[0].value) {
                  fs.setItemDamage(32767);
               }

               if (this.isBlacklist()) {
                  if (OreDictionary.itemMatches(fs, ts, this.getToggles()[0].value)) {
                     return false;
                  }
               } else if (!OreDictionary.itemMatches(fs, ts, this.getToggles()[0].value)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean onTaskCompletion(World world, IGolemAPI golem, Task task) {
      IBlockState bs = world.getBlockState(task.getPos());
      if (this.cache.containsKey(task.getId()) && this.isValidBlock(world, task.getPos())) {
         FakePlayer fp = FakePlayerFactory.get((WorldServer)world, new GameProfile((UUID)null, "FakeThaumcraftGolem"));
         fp.connection = new FakeNetHandlerPlayServer(fp.server, new NetworkManager(EnumPacketDirection.CLIENTBOUND), fp);
         fp.setPosition(golem.getGolemEntity().posX, golem.getGolemEntity().posY, golem.getGolemEntity().posZ);
         golem.swingArm();
         boolean silky = this.getToggles().length > 1 && this.getToggles()[1].value;
         int bspd = silky ? 7 : 21;
         if (task.getData() > bspd) {
            float bh = bs.getBlockHardness(world, task.getPos()) * 10.0F;
            task.setLifespan((short)Math.max(task.getLifespan(), 10L));
            task.setData(task.getData() - bspd);
            int progress = (int)(9.0F * (1.0F - task.getData() / bh));
            world.playSound(
               null,
               task.getPos(),
               bs.getBlock().getSoundType().getBreakSound(),
               SoundCategory.BLOCKS,
               (bs.getBlock().getSoundType().getVolume() + 0.7F) / 8.0F,
               bs.getBlock().getSoundType().getPitch() * 0.5F
            );
            BlockUtils.destroyBlockPartially(world, golem.getGolemEntity().getEntityId(), task.getPos(), progress);
            return false;
         }

         BlockUtils.destroyBlockPartially(world, golem.getGolemEntity().getEntityId(), task.getPos(), 10);
         BlockUtils.harvestBlock(world, fp, task.getPos(), true, silky, 0, true);
         golem.addRankXp(1);
         this.cache.remove(task.getId());
      }

      task.setSuspended(true);
      return true;
   }

   @Override
   public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
      if (this.cache.containsKey(task.getId()) && this.isValidBlock(golem.getGolemWorld(), task.getPos())) {
         return true;
      } else {
         task.setSuspended(true);
         return false;
      }
   }

   @Override
   public void onTaskSuspension(World world, Task task) {
      this.cache.remove(task.getId());
   }

   @Override
   public boolean canPlaceAt(World world, BlockPos pos, EnumFacing side) {
      return !world.isAirBlock(pos);
   }

   @Override
   public ResourceLocation getSealIcon() {
      return this.icon;
   }

   @Override
   public void onRemoval(World world, BlockPos pos, EnumFacing side) {
   }

   @Override
   public Object returnContainer(World world, EntityPlayer player, BlockPos pos, EnumFacing side, ISealEntity seal) {
      return new SealBaseContainer(player.inventory, world, seal);
   }

   @SideOnly(Side.CLIENT)
   @Override
   public Object returnGui(World world, EntityPlayer player, BlockPos pos, EnumFacing side, ISealEntity seal) {
      return new SealBaseGUI(player.inventory, world, seal);
   }

   @Override
   public int[] getGuiCategories() {
      return new int[]{2, 1, 3, 0, 4};
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.BREAKER};
   }

   @Override
   public EnumGolemTrait[] getForbiddenTags() {
      return null;
   }

   @Override
   public void onTaskStarted(World world, IGolemAPI golem, Task task) {
   }

   @Override
   public ISealConfigToggles.SealToggle[] getToggles() {
      return this.props;
   }

   @Override
   public void setToggle(int indx, boolean value) {
      this.props[indx].setValue(value);
   }
}
