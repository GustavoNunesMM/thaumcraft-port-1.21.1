package thaumcraft.common.golems.seals;

import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.ISealGui;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.golems.GolemInteractionHelper;
import thaumcraft.common.golems.client.gui.SealBaseContainer;
import thaumcraft.common.golems.client.gui.SealBaseGUI;
import thaumcraft.common.golems.tasks.TaskHandler;
import thaumcraft.common.lib.network.FakeNetHandlerPlayServer;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.CropUtils;

public class SealHarvest implements ISeal, ISealGui, ISealConfigArea, ISealConfigToggles {
   int delay = new Random(System.nanoTime()).nextInt(33);
   int count = 0;
   HashMap<Long, SealHarvest.ReplantInfo> replantTasks = new HashMap<>();
   ResourceLocation icon = new ResourceLocation("thaumcraft", "items/seals/seal_harvest");
   protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[]{
      new ISealConfigToggles.SealToggle(true, "prep", "golem.prop.replant"), new ISealConfigToggles.SealToggle(false, "ppro", "golem.prop.provision")
   };

   @Override
   public String getKey() {
      return "thaumcraft:harvest";
   }

   @Override
   public void tickSeal(World world, ISealEntity seal) {
      if (this.delay % 100 == 0) {
         AxisAlignedBB area = GolemHelper.getBoundsForArea(seal);
         Iterator<Long> rt = this.replantTasks.keySet().iterator();

         while (rt.hasNext()) {
            BlockPos pp = BlockPos.fromLong(rt.next());
            if (!area.contains(new Vec3d(pp.getX() + 0.5, pp.getY() + 0.5, pp.getZ() + 0.5))) {
               if (this.replantTasks.get(rt) != null) {
                  Task tt = TaskHandler.getTask(world.provider.getDimension(), this.replantTasks.get(rt).taskid);
                  if (tt != null) {
                     tt.setSuspended(true);
                  }
               }

               rt.remove();
            }
         }
      }

      if (this.delay++ % 5 == 0) {
         BlockPos p = GolemHelper.getPosInArea(seal, this.count++);
         if (CropUtils.isGrownCrop(world, p)) {
            Task task = new Task(seal.getSealPos(), p);
            task.setPriority(seal.getPriority());
            TaskHandler.addTask(world.provider.getDimension(), task);
         } else if (this.getToggles()[0].value && this.replantTasks.containsKey(p.toLong()) && world.isAirBlock(p)) {
            Task t = TaskHandler.getTask(world.provider.getDimension(), this.replantTasks.get(p.toLong()).taskid);
            if (t == null) {
               Task tt = new Task(seal.getSealPos(), this.replantTasks.get(p.toLong()).pos);
               tt.setPriority(seal.getPriority());
               TaskHandler.addTask(world.provider.getDimension(), tt);
               this.replantTasks.get(p.toLong()).taskid = tt.getId();
            }
         }
      }
   }

   @Override
   public boolean onTaskCompletion(World world, IGolemAPI golem, Task task) {
      if (CropUtils.isGrownCrop(world, task.getPos())) {
         FakePlayer fp = FakePlayerFactory.get((WorldServer)world, new GameProfile((UUID)null, "FakeThaumcraftGolem"));
         fp.connection = new FakeNetHandlerPlayServer(fp.server, new NetworkManager(EnumPacketDirection.CLIENTBOUND), fp);
         fp.setPosition(golem.getGolemEntity().posX, golem.getGolemEntity().posY, golem.getGolemEntity().posZ);
         EnumFacing face = EnumFacing.getDirectionFromEntityLiving(task.getPos(), golem.getGolemEntity());
         IBlockState bs = world.getBlockState(task.getPos());
         if (CropUtils.clickableCrops.contains(bs.getBlock().getTranslationKey() + bs.getBlock().getMetaFromState(bs))) {
            bs.getBlock().onBlockActivated(world, task.getPos(), bs, fp, EnumHand.MAIN_HAND, face, 0.0F, 0.0F, 0.0F);
            golem.addRankXp(1);
            golem.swingArm();
         } else {
            GolemInteractionHelper.golemClick(world, golem, task.getPos(), task.getSealPos().face, ItemStack.EMPTY, false, true);
            if (CropUtils.isGrownCrop(world, task.getPos())) {
               BlockUtils.harvestBlock(world, fp, task.getPos(), true, false, 0, true);
               golem.addRankXp(1);
               golem.swingArm();
               if (this.getToggles()[0].value) {
                  ItemStack seed = ThaumcraftApi.getSeed(bs.getBlock());
                  if (seed != null && !seed.isEmpty()) {
                     IBlockState bb = world.getBlockState(task.getPos().down());
                     EnumFacing rf = null;
                     if (seed.getItem() instanceof IPlantable
                        && bb.getBlock().canSustainPlant(bb, world, task.getPos().down(), EnumFacing.UP, (IPlantable)seed.getItem())) {
                        rf = EnumFacing.DOWN;
                     } else if (!(seed.getItem() instanceof IPlantable) && bs.getBlock() instanceof BlockDirectional) {
                        rf = (EnumFacing)bs.getValue(BlockDirectional.FACING);
                     }

                     if (rf != null) {
                        Task tt = new Task(task.getSealPos(), task.getPos());
                        tt.setPriority(task.getPriority());
                        tt.setLifespan((short)300);
                        this.replantTasks
                           .put(
                              tt.getPos().toLong(),
                              new SealHarvest.ReplantInfo(tt.getPos(), rf, tt.getId(), seed.copy(), bb.getBlock() instanceof BlockFarmland)
                           );
                        TaskHandler.addTask(world.provider.getDimension(), tt);
                     }
                  }
               }
            }
         }
      } else if (this.replantTasks.containsKey(task.getPos().toLong())
         && this.replantTasks.get(task.getPos().toLong()).taskid == task.getId()
         && world.isAirBlock(task.getPos())
         && golem.isCarrying(this.replantTasks.get(task.getPos().toLong()).stack)) {
         FakePlayer fp = FakePlayerFactory.get((WorldServer)world, new GameProfile((UUID)null, "FakeThaumcraftGolem"));
         fp.setPosition(golem.getGolemEntity().posX, golem.getGolemEntity().posY, golem.getGolemEntity().posZ);
         IBlockState bbx = world.getBlockState(task.getPos().down());
         SealHarvest.ReplantInfo ri = this.replantTasks.get(task.getPos().toLong());
         if ((bbx.getBlock() instanceof BlockDirt || bbx.getBlock() instanceof BlockGrass) && ri.farmland) {
            Items.DIAMOND_HOE.onItemUse(fp, world, task.getPos().down(), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
         }

         ItemStack seed = ri.stack.copy();
         seed.setCount(1);
         if (seed.getItem().onItemUse(fp, world, task.getPos().offset(ri.face), EnumHand.MAIN_HAND, ri.face.getOpposite(), 0.5F, 0.5F, 0.5F)
            == EnumActionResult.SUCCESS) {
            world.playBroadcastSound(2001, task.getPos(), Block.getStateId(world.getBlockState(task.getPos())));
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
      if (this.replantTasks.containsKey(task.getPos().toLong()) && this.replantTasks.get(task.getPos().toLong()).taskid == task.getId()) {
         boolean carry = golem.isCarrying(this.replantTasks.get(task.getPos().toLong()).stack);
         if (!carry && this.getToggles()[1].value) {
            ISealEntity se = SealHandler.getSealEntity(golem.getGolemWorld().provider.getDimension(), task.getSealPos());
            if (se != null) {
               GolemHelper.requestProvisioning(golem.getGolemWorld(), se, this.replantTasks.get(task.getPos().toLong()).stack);
            }
         }

         return carry;
      } else {
         return true;
      }
   }

   @Override
   public void onTaskSuspension(World world, Task task) {
   }

   @Override
   public void readCustomNBT(NBTTagCompound nbt) {
      NBTTagList nbttaglist = nbt.getTagList("replant", 10);

      for (int i = 0; i < nbttaglist.tagCount(); i++) {
         NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
         long loc = nbttagcompound1.getLong("taskloc");
         byte face = nbttagcompound1.getByte("taskface");
         boolean farmland = nbttagcompound1.getBoolean("farmland");
         ItemStack stack = new ItemStack(nbttagcompound1);
         this.replantTasks.put(loc, new SealHarvest.ReplantInfo(BlockPos.fromLong(loc), EnumFacing.VALUES[face], 0, stack, farmland));
      }
   }

   @Override
   public void writeCustomNBT(NBTTagCompound nbt) {
      if (this.getToggles()[0].value) {
         NBTTagList nbttaglist = new NBTTagList();

         for (Long key : this.replantTasks.keySet()) {
            SealHarvest.ReplantInfo info = this.replantTasks.get(key);
            NBTTagCompound nbttagcompound1 = new NBTTagCompound();
            nbttagcompound1.setLong("taskloc", info.pos.toLong());
            nbttagcompound1.setByte("taskface", (byte)info.face.ordinal());
            nbttagcompound1.setBoolean("farmland", info.farmland);
            info.stack.writeToNBT(nbttagcompound1);
            nbttaglist.appendTag(nbttagcompound1);
         }

         nbt.setTag("replant", nbttaglist);
      }
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
      return new int[]{2, 3, 0, 4};
   }

   @Override
   public ISealConfigToggles.SealToggle[] getToggles() {
      return this.props;
   }

   @Override
   public void setToggle(int indx, boolean value) {
      this.props[indx].setValue(value);
   }

   @Override
   public EnumGolemTrait[] getRequiredTags() {
      return new EnumGolemTrait[]{EnumGolemTrait.DEFT, EnumGolemTrait.SMART};
   }

   @Override
   public EnumGolemTrait[] getForbiddenTags() {
      return null;
   }

   @Override
   public void onTaskStarted(World world, IGolemAPI golem, Task task) {
   }

   private class ReplantInfo {
      EnumFacing face;
      BlockPos pos;
      int taskid;
      ItemStack stack;
      boolean farmland;

      public ReplantInfo(BlockPos pos, EnumFacing face, int taskid, ItemStack stack, boolean farmland) {
         this.pos = pos;
         this.face = face;
         this.taskid = taskid;
         this.stack = stack;
         this.farmland = farmland;
      }
   }
}
