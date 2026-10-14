package thaumcraft.common.blocks.devices;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TileMirror: a mirror linked to another mirror of the same kind (possibly in another dimension). Items thrown into
 * a magic mirror are queued in the other one and dropped out of it one at a time. Using it builds up instability,
 * which turns into flux.
 */
public class MirrorBlockEntity extends TCBlockEntity {
   public boolean linked = false;
   @Nullable
   public GlobalPos link = null;
   public int instability;
   protected int count = 0;
   protected int inc = 40;
   private List<ItemStack> outputStacks = new ArrayList<>();

   public MirrorBlockEntity(BlockPos pos, BlockState state) {
      this(ModDevices.MIRROR_BE.get(), pos, state);
   }

   protected MirrorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   protected int instabilityLimit() {
      return 128;
   }

   @Nullable
   public GlobalPos here() {
      return level == null ? null : GlobalPos.of(level.dimension(), worldPosition);
   }

   @Nullable
   protected ServerLevel targetLevel() {
      if (link == null || !(level instanceof ServerLevel sl)) return null;
      return sl.getServer().getLevel(link.dimension());
   }

   /** The mirror at the other end, if its chunk is loaded and it is one of ours. */
   @Nullable
   protected MirrorBlockEntity target() {
      ServerLevel tl = targetLevel();
      if (tl == null || !tl.isLoaded(link.pos())) return null;
      return tl.getBlockEntity(link.pos()) instanceof MirrorBlockEntity m && m.getClass() == getClass() ? m : null;
   }

   private void unlink() {
      linked = false;
      sync();
   }

   public void restoreLink() {
      if (!isDestinationValid()) return;
      MirrorBlockEntity tm = target();
      if (tm != null) {
         tm.linked = true;
         tm.link = here();
         tm.onLinked(this);
         tm.sync();
         linked = true;
         onLinked(tm);
         sync();
      }
   }

   protected void onLinked(MirrorBlockEntity other) {
   }

   public void invalidateLink() {
      MirrorBlockEntity tm = target();
      if (tm != null) {
         tm.linked = false;
         tm.onUnlinked();
         tm.sync();
      }
   }

   protected void onUnlinked() {
   }

   public boolean isLinkValid() {
      if (!linked) return false;
      if (targetLevel() == null) return false;
      MirrorBlockEntity tm = target();
      if (tm == null) {
         unlink();
         return false;
      }
      if (!tm.linked || !here().equals(tm.link)) {
         unlink();
         return false;
      }
      return true;
   }

   public boolean isLinkValidSimple() {
      if (!linked) return false;
      MirrorBlockEntity tm = target();
      return tm != null && tm.linked && here().equals(tm.link);
   }

   public boolean isDestinationValid() {
      if (targetLevel() == null) return false;
      MirrorBlockEntity tm = target();
      if (tm == null) {
         unlink();
         return false;
      }
      return !tm.isLinkValid();
   }

   public boolean transport(ItemEntity ie) {
      ItemStack items = ie.getItem();
      if (!linked || !isLinkValid()) return false;
      MirrorBlockEntity target = target();
      if (target == null) return false;
      target.addStack(items.copy());
      addInstability(items.getCount());
      ie.discard();
      setChanged();
      level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
      return true;
   }

   public boolean transportDirect(ItemStack items) {
      if (items.isEmpty()) return false;
      addStack(items.copy());
      return true;
   }

   public void addStack(ItemStack stack) {
      outputStacks.add(stack);
      setChanged();
   }

   private void eject() {
      if (outputStacks.isEmpty() || count <= 20) return;
      int i = level.random.nextInt(outputStacks.size());
      ItemStack s = outputStacks.get(i);
      if (!s.isEmpty()) {
         if (spawnItem(s.copyWithCount(1))) {
            s.shrink(1);
            addInstability(1);
            level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
            if (s.isEmpty()) outputStacks.remove(i);
         }
      } else {
         outputStacks.remove(i);
      }
      setChanged();
   }

   public boolean spawnItem(ItemStack stack) {
      Direction face = DeviceBlock.getFacing(getBlockState());
      ItemEntity ie = new ItemEntity(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.25, worldPosition.getZ() + 0.5, stack);
      ie.setDeltaMovement(face.getStepX() * 0.15F, face.getStepY() * 0.15F, face.getStepZ() * 0.15F);
      ie.setPortalCooldown(20);
      return level.addFreshEntity(ie);
   }

   protected void addInstability(int amt) {
      instability += amt;
      setChanged();
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, MirrorBlockEntity be) {
      be.eject();
      be.checkInstability();
      if (be.count++ % be.inc == 0) {
         if (!be.isLinkValidSimple()) {
            if (be.inc < 600) be.inc += 20;
            be.restoreLink();
         } else {
            be.inc = 40;
         }
      }
   }

   private void checkInstability() {
      if (instability > instabilityLimit()) {
         AuraHandler.addFlux(level, worldPosition, 1.0F);
         instability -= instabilityLimit();
         setChanged();
      }
      if (instability > 0 && count % 100 == 0) instability--;
   }

   @Override
   public boolean triggerEvent(int id, int param) {
      if (id == 1) {
         if (level.isClientSide) {
            Direction face = DeviceBlock.getFacing(getBlockState());
            var r = level.random;
            double xx = worldPosition.getX() + 0.33 + r.nextFloat() * 0.33F - face.getStepX() / 2.0;
            double yy = worldPosition.getY() + 0.33 + r.nextFloat() * 0.33F - face.getStepY() / 2.0;
            double zz = worldPosition.getZ() + 0.33 + r.nextFloat() * 0.33F - face.getStepZ() / 2.0;
            thaumcraft.client.fx.FXDispatcher.drawWispyMotes(xx, yy, zz, face.getStepX() / 50.0 + r.nextGaussian() * 0.01,
               face.getStepY() / 50.0 + r.nextGaussian() * 0.01, face.getStepZ() / 50.0 + r.nextGaussian() * 0.01, Mth.nextInt(r, 10, 30),
               r.nextFloat() / 3.0F, 0.0F, r.nextFloat() / 2.0F, (float) (r.nextGaussian() * 0.01));
         }
         return true;
      }
      return super.triggerEvent(id, param);
   }

   // ---------------------------------------------------------------- item components: the link travels with the item

   @Override
   protected void applyImplicitComponents(DataComponentInput input) {
      super.applyImplicitComponents(input);
      GlobalPos gp = input.get(ModDevices.MIRROR_LINK.get());
      if (gp != null) link = gp;
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder builder) {
      super.collectImplicitComponents(builder);
      if (linked && link != null) builder.set(ModDevices.MIRROR_LINK.get(), link);
   }

   @Override
   public void removeComponentsFromTag(CompoundTag tag) {
      tag.remove("link");
      tag.remove("linked");
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      linked = tag.getBoolean("linked");
      link = tag.contains("link") ? GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get("link")).result().orElse(null) : null;
      instability = tag.getInt("instability");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putBoolean("linked", linked);
      if (link != null) GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, link).result().ifPresent(t -> tag.put("link", t));
      tag.putInt("instability", instability);
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      outputStacks = new ArrayList<>();
      ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
      for (int i = 0; i < list.size(); i++) {
         ItemStack s = ItemStack.parseOptional(registries, list.getCompound(i));
         if (!s.isEmpty()) outputStacks.add(s);
      }
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      ListTag list = new ListTag();
      for (ItemStack s : outputStacks) {
         if (!s.isEmpty()) list.add(s.save(registries));
      }
      tag.put("Items", list);
   }
}
