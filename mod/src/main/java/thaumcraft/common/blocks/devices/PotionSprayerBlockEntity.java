package thaumcraft.common.blocks.devices;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.menu.PotionSprayerMenu;
import thaumcraft.common.registers.ModDevices;

/**
 * TilePotionSprayer: holds a potion; sucks in the potion's aspects (at most 10 kinds) through tubes to brew up to 8
 * charges. Each redstone pulse sprays one charge on the living things in the 3x3x3 area two blocks in front.
 */
public class PotionSprayerBlockEntity extends DeviceInventoryBlockEntity implements IAspectContainer, IEssentiaTransport, MenuProvider {
   public AspectList recipe = new AspectList();
   public AspectList recipeProgress = new AspectList();
   public int charges = 0;
   public int color = 0;
   private int counter = 0;
   private boolean activated = false;
   public int venting = 0;
   @Nullable
   private Aspect currentSuction = null;

   public PotionSprayerBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.POTION_SPRAYER_BE.get(), pos, state, 1);
   }

   private Direction facing() {
      return DeviceBlock.getFacing(getBlockState());
   }

   /** SlotPotion.isValidPotion. */
   public static boolean isValidPotion(ItemStack stack) {
      if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) return false;
      PotionContents pc = stack.get(DataComponents.POTION_CONTENTS);
      if (pc == null || pc.potion().isEmpty()) return false;
      Holder<Potion> p = pc.potion().get();
      return !p.is(Potions.WATER) && !p.is(Potions.AWKWARD) && !p.is(Potions.MUNDANE) && !p.is(Potions.THICK);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, PotionSprayerBlockEntity be) {
      be.counter++;
      Direction facing = DeviceBlock.getFacing(state);
      if (level.isClientSide) {
         if (be.venting > 0) {
            be.venting--;
            var r = level.random;
            for (int a = 0; a < be.venting / 2; a++) {
               float fx = 0.1F - r.nextFloat() * 0.2F, fz = 0.1F - r.nextFloat() * 0.2F, fy = 0.1F - r.nextFloat() * 0.2F;
               float fx2 = (float) (r.nextGaussian() * 0.06), fz2 = (float) (r.nextGaussian() * 0.06), fy2 = (float) (r.nextGaussian() * 0.06);
               thaumcraft.client.fx.DeviceFX.drawVentParticles2(pos.getX() + 0.5F + fx + facing.getStepX() / 2.0F,
                  pos.getY() + 0.5F + fy + facing.getStepY() / 2.0F, pos.getZ() + 0.5F + fz + facing.getStepZ() / 2.0F,
                  fx2 + facing.getStepX() * 0.25, fy2 + facing.getStepY() * 0.25, fz2 + facing.getStepZ() * 0.25, be.color, 4.0F);
            }
         }
         return;
      }
      if (be.counter % 5 == 0) {
         be.currentSuction = null;
         if (!be.getItem(0).isEmpty() && be.charges < 8) {
            boolean done = true;
            for (Aspect aspect : be.recipe.getAspectsSortedByName()) {
               if (be.recipeProgress.getAmount(aspect) < be.recipe.getAmount(aspect)) {
                  be.currentSuction = aspect;
                  done = false;
                  break;
               }
            }
            if (done) {
               be.recipeProgress = new AspectList();
               be.charges++;
               be.sync();
            } else if (be.currentSuction != null) {
               be.fill();
            }
         }
      }
      if (!DeviceBlock.isEnabled(state)) {
         if (!be.activated && be.charges > 0) {
            be.charges--;
            be.spray(level, pos, facing);
            level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.25F,
               2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
            level.blockEvent(pos, state.getBlock(), 0, 0);
            be.sync();
         }
         be.activated = true;
      } else if (be.activated) {
         be.activated = false;
      }
   }

   private void spray(Level level, BlockPos pos, Direction facing) {
      PotionContents pc = getItem(0).get(DataComponents.POTION_CONTENTS);
      if (pc == null) return;
      BlockPos p = pos.relative(facing, 2);
      int area = 1;
      List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
         new AABB(p.getX() - area, p.getY() - area, p.getZ() - area, p.getX() + 1 + area, p.getY() + 1 + area, p.getZ() + 1 + area));
      for (LivingEntity e : targets) {
         if (!e.isAlive() || !e.isAffectedByPotions()) continue;
         for (MobEffectInstance effect : pc.getAllEffects()) {
            if (effect.getEffect().value().isInstantenous()) {
               effect.getEffect().value().applyInstantenousEffect(null, null, e, effect.getAmplifier(), 1.0);
            } else {
               e.addEffect(new MobEffectInstance(effect.getEffect(), effect.getDuration(), effect.getAmplifier()));
            }
         }
      }
   }

   private void recalcAspects() {
      if (level == null || level.isClientSide) return;
      ItemStack stack = getItem(0);
      color = 0x333333;
      if (!stack.isEmpty()) {
         AspectList al = AspectHelper.getObjectAspects(level, stack);
         recipe = al == null ? new AspectList() : al.copy();
         PotionContents pc = stack.get(DataComponents.POTION_CONTENTS);
         if (pc != null) color = pc.getColor() & 0xFFFFFF;
      } else {
         recipe = new AspectList();
      }
      charges = 0;
      recipe = AspectHelper.cullTags(recipe, 10);
      recipeProgress = new AspectList();
      sync();
   }

   @Override
   public void setItem(int slot, ItemStack stack) {
      super.setItem(slot, stack);
      recalcAspects();
   }

   @Override
   public ItemStack removeItem(int slot, int amount) {
      ItemStack out = super.removeItem(slot, amount);
      recalcAspects();
      return out;
   }

   void fill() {
      Direction facing = facing();
      for (int y = 0; y <= 1; y++) {
         for (Direction dir : Direction.values()) {
            if (dir == facing) continue;
            IEssentiaTransport ic = ThaumcraftApiHelper.getConnectableTile(level, worldPosition.above(y), dir);
            if (ic != null && ic.getEssentiaAmount(dir.getOpposite()) > 0 && ic.getSuctionAmount(dir.getOpposite()) < getSuctionAmount(null)
               && getSuctionAmount(null) >= ic.getMinimumSuction()) {
               int ess = ic.takeEssentia(currentSuction, 1, dir.getOpposite());
               if (ess > 0) {
                  addToContainer(currentSuction, ess);
                  return;
               }
            }
         }
      }
   }

   @Override
   public boolean triggerEvent(int id, int param) {
      if (id >= 0) {
         if (level.isClientSide) venting = 15;
         return true;
      }
      return super.triggerEvent(id, param);
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      return isValidPotion(stack);
   }

   // ---------------------------------------------------------------- aspects

   @Override
   public int addToContainer(Aspect tt, int am) {
      int ce = recipe.getAmount(tt) - recipeProgress.getAmount(tt);
      if (ce <= 0) return am;
      int add = Math.min(ce, am);
      recipeProgress.add(tt, add);
      sync();
      return am - add;
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tt, int am) {
      return recipeProgress.getAmount(tt) >= am;
   }

   @Override
   public int containerContains(Aspect tt) {
      return recipeProgress.getAmount(tt);
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   @Override
   public AspectList getAspects() {
      return recipeProgress;
   }

   @Override
   public void setAspects(AspectList aspects) {
      recipeProgress = aspects;
   }

   @Override
   public boolean isConnectable(Direction face) {
      return face != facing();
   }

   @Override
   public boolean canInputFrom(Direction face) {
      return face != facing();
   }

   @Override
   public boolean canOutputTo(Direction face) {
      return false;
   }

   @Override
   public void setSuction(@Nullable Aspect aspect, int amount) {
      currentSuction = aspect;
   }

   @Override
   public Aspect getSuctionType(@Nullable Direction face) {
      return currentSuction;
   }

   @Override
   public int getSuctionAmount(@Nullable Direction face) {
      return currentSuction != null ? 128 : 0;
   }

   @Override
   public Aspect getEssentiaType(@Nullable Direction face) {
      return null;
   }

   @Override
   public int getEssentiaAmount(@Nullable Direction face) {
      return 0;
   }

   @Override
   public int takeEssentia(Aspect aspect, int amount, @Nullable Direction face) {
      return 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, @Nullable Direction face) {
      return canInputFrom(face) ? amount - addToContainer(aspect, amount) : 0;
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   // ---------------------------------------------------------------- nbt / menu

   private static AspectList readList(CompoundTag tag, String key) {
      return tag.contains(key) ? AspectList.CODEC.parse(NbtOps.INSTANCE, tag.get(key)).result().map(AspectList::copy).orElse(new AspectList())
         : new AspectList();
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.readSync(tag, registries);
      recipe = readList(tag, "recipe");
      recipeProgress = readList(tag, "progress");
      charges = tag.getInt("charges");
      color = tag.getInt("color");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.writeSync(tag, registries);
      AspectList.CODEC.encodeStart(NbtOps.INSTANCE, recipe).result().ifPresent(t -> tag.put("recipe", t));
      AspectList.CODEC.encodeStart(NbtOps.INSTANCE, recipeProgress).result().ifPresent(t -> tag.put("progress", t));
      tag.putInt("charges", charges);
      tag.putInt("color", color);
   }

   @Override
   public Component getDisplayName() {
      return getBlockState().getBlock().getName();
   }

   @Override
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new PotionSprayerMenu(id, inv, this);
   }
}
