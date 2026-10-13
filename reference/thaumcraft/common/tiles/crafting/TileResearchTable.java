package thaumcraft.common.tiles.crafting;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.items.IScribeTools;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.tiles.TileThaumcraftInventory;

public class TileResearchTable extends TileThaumcraftInventory {
   public ResearchTableData data = null;

   public TileResearchTable() {
      super(2);
      this.syncedSlots = new int[]{0, 1};
   }

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      super.readSyncNBT(nbttagcompound);
      if (nbttagcompound.hasKey("note")) {
         this.data = new ResearchTableData(this);
         this.data.deserialize(nbttagcompound.getCompoundTag("note"));
      } else {
         this.data = null;
      }
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      if (this.data != null) {
         nbttagcompound.setTag("note", this.data.serialize());
      } else {
         nbttagcompound.removeTag("note");
      }

      return super.writeSyncNBT(nbttagcompound);
   }

   protected void setWorldCreate(World worldIn) {
      super.setWorldCreate(worldIn);
      if (!this.hasWorld()) {
         this.setWorld(worldIn);
      }
   }

   public void startNewTheory(EntityPlayer player, Set<String> mutators) {
      this.data = new ResearchTableData(player, this);
      this.data.initialize(player, mutators);
      this.syncTile(false);
      this.markDirty();
   }

   public void finishTheory(EntityPlayer player) {
      Comparator<Entry<String, Integer>> valueComparator = (e1, e2) -> e2.getValue().compareTo(e1.getValue());
      Map<String, Integer> sortedMap = this.data
         .categoryTotals
         .entrySet()
         .stream()
         .sorted(valueComparator)
         .collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
      int i = 0;

      for (String cat : sortedMap.keySet()) {
         int tot = Math.round(sortedMap.get(cat).intValue() / 100.0F * IPlayerKnowledge.EnumKnowledgeType.THEORY.getProgression());
         if (i > this.data.penaltyStart) {
            tot = (int)Math.max(1.0, tot * 0.666666667);
         }

         ResearchCategory rc = ResearchCategories.getResearchCategory(cat);
         ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.THEORY, rc, tot);
         i++;
      }

      this.data = null;
   }

   public Set<String> checkSurroundingAids() {
      HashMap<String, ITheorycraftAid> mutators = new HashMap<>();

      for (int y = -1; y <= 1; y++) {
         for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
               for (String muk : TheorycraftManager.aids.keySet()) {
                  ITheorycraftAid mu = TheorycraftManager.aids.get(muk);
                  IBlockState state = this.world.getBlockState(this.getPos().add(x, y, z));
                  if (mu.getAidObject() instanceof Block) {
                     if (state.getBlock() == (Block)mu.getAidObject()) {
                        mutators.put(muk, mu);
                     }
                  } else if (mu.getAidObject() instanceof ItemStack) {
                     ItemStack is = state.getBlock().getItem(this.getWorld(), this.getPos().add(x, y, z), state);
                     if (is != null && !is.isEmpty() && is.isItemEqualIgnoreDurability((ItemStack)mu.getAidObject())) {
                        mutators.put(muk, mu);
                     }
                  }
               }
            }
         }
      }

      List<Entity> l = EntityUtils.getEntitiesInRange(this.getWorld(), this.getPos(), null, Entity.class, 5.0);
      if (l != null && !l.isEmpty()) {
         for (Entity e : l) {
            for (String mukx : TheorycraftManager.aids.keySet()) {
               ITheorycraftAid mu = TheorycraftManager.aids.get(mukx);
               if (mu.getAidObject() instanceof Class && e.getClass().isAssignableFrom((Class<?>)mu.getAidObject())) {
                  mutators.put(mukx, mu);
               }
            }
         }
      }

      return mutators.keySet();
   }

   public boolean consumeInkFromTable() {
      if (this.getStackInSlot(0).getItem() instanceof IScribeTools && this.getStackInSlot(0).getItemDamage() < this.getStackInSlot(0).getMaxDamage()) {
         this.getStackInSlot(0).setItemDamage(this.getStackInSlot(0).getItemDamage() + 1);
         this.syncTile(false);
         this.markDirty();
         return true;
      } else {
         return false;
      }
   }

   public boolean consumepaperFromTable() {
      if (this.getStackInSlot(1).getItem() == Items.PAPER && this.getStackInSlot(1).getCount() > 0) {
         this.decrStackSize(1, 1);
         this.syncTile(false);
         this.markDirty();
         return true;
      } else {
         return false;
      }
   }

   @Override
   public String getName() {
      return "Research Table";
   }

   @Override
   public boolean isItemValidForSlot(int i, ItemStack itemstack) {
      switch (i) {
         case 0:
            if (itemstack.getItem() instanceof IScribeTools) {
               return true;
            }
            break;
         case 1:
            if (itemstack.getItem() == Items.PAPER && itemstack.getItemDamage() == 0) {
               return true;
            }
      }

      return false;
   }

   @Override
   public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
      super.onDataPacket(net, pkt);
      if (this.world != null && this.world.isRemote) {
         this.syncTile(false);
      }
   }

   public boolean receiveClientEvent(int i, int j) {
      if (i == 1) {
         if (this.world.isRemote) {
            this.world
               .playSound(
                  this.getPos().getX(),
                  this.getPos().getY(),
                  this.getPos().getZ(),
                  SoundsTC.learn,
                  SoundCategory.BLOCKS,
                  1.0F,
                  1.0F,
                  false
               );
         }

         return true;
      } else {
         return super.receiveClientEvent(i, j);
      }
   }
}
