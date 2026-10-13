package thaumcraft.common.blocks.crafting;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.ItemStackHandler;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.common.items.ScribingToolsItem;
import thaumcraft.common.menu.ResearchTableMenu;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.research.ResearchManager;

/** TileResearchTable: scribing tools (slot 0) and paper (slot 1), and the theory being written. */
public class ResearchTableBlockEntity extends BlockEntity implements MenuProvider {
   private final ItemStackHandler items = new ItemStackHandler(2) {
      @Override
      public boolean isItemValid(int slot, ItemStack stack) {
         return slot == 0 ? stack.getItem() instanceof ScribingToolsItem : stack.is(Items.PAPER);
      }

      @Override
      protected void onContentsChanged(int slot) {
         sync();
      }
   };
   @Nullable
   public ResearchTableData data;

   public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.RESEARCH_TABLE.get(), pos, state);
   }

   public ItemStackHandler getItems() {
      return items;
   }

   // ------------------------------------------------------------------ theory

   public void startNewTheory(Player player, Set<String> aids) {
      data = new ResearchTableData(player, this);
      data.initialize(player, aids);
      sync();
   }

   /** Turns the theory's category totals into theory knowledge; past the penalty point only 2/3 of it. */
   public void finishTheory(Player player) {
      if (data == null) return;
      Map<String, Integer> sorted = new LinkedHashMap<>();
      data.categoryTotals.entrySet().stream().sorted((a, b) -> b.getValue().compareTo(a.getValue()))
         .forEach(e -> sorted.put(e.getKey(), e.getValue()));
      int i = 0;
      for (Map.Entry<String, Integer> e : sorted.entrySet()) {
         int tot = Math.round(e.getValue() / 100.0F * EnumKnowledgeType.THEORY.getProgression());
         if (i > data.penaltyStart) tot = (int) Math.max(1.0, tot * 0.666666667);
         ResearchManager.addKnowledge(player, EnumKnowledgeType.THEORY, ResearchCategories.getResearchCategory(e.getKey()), tot);
         i++;
      }
      data = null;
      sync();
   }

   /** Research aids within 4 blocks horizontally and 1 vertically (blocks), or 5 blocks (creatures). */
   public Set<String> checkSurroundingAids() {
      Map<String, ITheorycraftAid> found = new HashMap<>();
      if (level == null) return found.keySet();
      for (int y = -1; y <= 1; y++) {
         for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
               BlockPos p = worldPosition.offset(x, y, z);
               BlockState state = level.getBlockState(p);
               for (Map.Entry<String, ITheorycraftAid> e : TheorycraftManager.AIDS.entrySet()) {
                  Object o = e.getValue().getAidObject();
                  if (o instanceof Block b && state.is(b)) found.put(e.getKey(), e.getValue());
                  else if (o instanceof ItemStack is && !is.isEmpty() && state.getBlock().asItem() == is.getItem()) found.put(e.getKey(), e.getValue());
               }
            }
         }
      }
      List<Entity> near = level.getEntities((Entity) null, new AABB(worldPosition).inflate(5.0));
      for (Entity ent : near) {
         for (Map.Entry<String, ITheorycraftAid> e : TheorycraftManager.AIDS.entrySet()) {
            if (e.getValue().getAidObject() instanceof Class<?> c && c.isInstance(ent)) found.put(e.getKey(), e.getValue());
         }
      }
      return found.keySet();
   }

   public boolean hasInk() {
      ItemStack s = items.getStackInSlot(0);
      return s.getItem() instanceof ScribingToolsItem && s.getDamageValue() < s.getMaxDamage();
   }

   public boolean hasPaper() {
      return items.getStackInSlot(1).is(Items.PAPER) && !items.getStackInSlot(1).isEmpty();
   }

   public boolean consumeInk() {
      if (!hasInk()) return false;
      ItemStack s = items.getStackInSlot(0).copy();
      s.setDamageValue(s.getDamageValue() + 1);
      items.setStackInSlot(0, s);
      return true;
   }

   public boolean consumePaper() {
      if (!hasPaper()) return false;
      items.extractItem(1, 1, false);
      return true;
   }

   // ------------------------------------------------------------------ sync and save

   public void sync() {
      setChanged();
      if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
   }

   @Override
   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.put("Items", items.serializeNBT(registries));
      if (data != null) tag.put("note", data.serialize());
   }

   @Override
   protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      items.deserializeNBT(registries, tag.getCompound("Items"));
      if (tag.contains("note")) {
         data = new ResearchTableData(this);
         data.deserialize(tag.getCompound("note"));
      } else {
         data = null;
      }
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      CompoundTag tag = new CompoundTag();
      saveAdditional(tag, registries);
      return tag;
   }

   @Override
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public Component getDisplayName() {
      return Component.translatable("block.thaumcraft.research_table");
   }

   @Override
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new ResearchTableMenu(id, inv, this);
   }
}
