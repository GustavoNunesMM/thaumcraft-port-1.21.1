package thaumcraft.common.blocks.devices;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.blocks.devices.BellowsBlockEntity;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TileInfernalFurnace: 32 slots fed from above. Smelts one item every 140 ticks (80 with vis, down to 10 with up to
 * four bellows two blocks away), ejecting the result, the bonuses (one roll per bellows + 1) and the experience out of
 * the front. Things that can't be smelted are destroyed.
 */
public class InfernalFurnaceBlockEntity extends DeviceInventoryBlockEntity {
   public int furnaceCookTime = 0;
   public int furnaceMaxCookTime = 0;
   public int speedyTime = 0;
   public int facingX = -5;
   public int facingZ = -5;

   public InfernalFurnaceBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.INFERNAL_FURNACE_BE.get(), pos, state, 32);
   }

   @Override
   protected boolean syncSlots() {
      return false;
   }

   private Direction facing() {
      return DeviceBlock.getFacing(getBlockState());
   }

   public static void tick(Level level, BlockPos pos, BlockState state, InfernalFurnaceBlockEntity be) {
      if (be.facingX == -5) be.setFacing();
      if (level.isClientSide) return;
      boolean cookedflag = false;
      if (be.furnaceCookTime > 0) {
         be.furnaceCookTime--;
         cookedflag = true;
      }
      if (be.furnaceMaxCookTime <= 0) be.furnaceMaxCookTime = be.calcCookTime();
      if (be.furnaceCookTime > be.furnaceMaxCookTime) be.furnaceCookTime = be.furnaceMaxCookTime;
      if (be.furnaceCookTime <= 0 && cookedflag) {
         for (int a = 0; a < be.getContainerSize(); a++) {
            ItemStack in = be.getItem(a);
            if (in.isEmpty()) continue;
            ItemStack result = be.smeltingResult(in);
            if (!result.isEmpty()) {
               if (be.speedyTime > 0) be.speedyTime--;
               be.ejectItem(result.copy(), in);
               level.blockEvent(pos, state.getBlock(), 3, 0);
               if (level.random.nextInt(20) == 0) AuraHandler.addFlux(level, pos.relative(be.facing().getOpposite()), 1.0F);
               be.removeItem(a, 1);
               break;
            }
            be.setItem(a, ItemStack.EMPTY);
         }
      }
      if (be.speedyTime <= 0) be.speedyTime = (int) AuraHandler.drainVis(level, pos, 20.0F, false);
      if (be.furnaceCookTime == 0 && !cookedflag) {
         for (int a = 0; a < be.getContainerSize(); a++) {
            if (be.canSmelt(be.getItem(a))) {
               be.furnaceMaxCookTime = be.calcCookTime();
               be.furnaceCookTime = be.furnaceMaxCookTime;
               break;
            }
         }
      }
   }

   private Optional<RecipeHolder<SmeltingRecipe>> recipe(ItemStack stack) {
      if (stack.isEmpty()) return Optional.empty();
      return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
   }

   private ItemStack smeltingResult(ItemStack stack) {
      return recipe(stack).map(r -> r.value().assemble(new SingleRecipeInput(stack), level.registryAccess())).orElse(ItemStack.EMPTY);
   }

   private boolean canSmelt(ItemStack stack) {
      return !smeltingResult(stack).isEmpty();
   }

   private int getBellows() {
      int bellows = 0;
      for (Direction dir : Direction.values()) {
         if (dir == Direction.UP) continue;
         BlockPos p2 = worldPosition.relative(dir, 2);
         if (level.getBlockEntity(p2) instanceof BellowsBlockEntity && DeviceBlock.getFacing(level.getBlockState(p2)) == dir.getOpposite()
            && level.getBestNeighborSignal(p2) == 0) {
            bellows++;
         }
      }
      return Math.min(4, bellows);
   }

   private int calcCookTime() {
      int b = getBellows();
      if (b > 0) b = (20 - (b - 1)) * b;
      return Math.max(10, (speedyTime > 0 ? 80 : 140) - b);
   }

   /** Takes what it can smelt (the rest is burnt); returns what didn't fit. */
   public ItemStack addItemsToInventory(ItemStack items) {
      if (canSmelt(items)) return DeviceUtils.insertStackAt(level, worldPosition, Direction.UP, items, false);
      destroyItem();
      return ItemStack.EMPTY;
   }

   private void destroyItem() {
      level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3F,
         2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
      if (level instanceof net.minecraft.server.level.ServerLevel sl) {
         sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, worldPosition.getX() + level.random.nextFloat(), worldPosition.getY() + 1,
            worldPosition.getZ() + level.random.nextFloat(), 1, 0, 0, 0, 0);
      }
   }

   public void ejectItem(ItemStack items, ItemStack furnaceItemStack) {
      if (items.isEmpty()) return;
      List<ItemStack> ejecti = new ArrayList<>();
      ejecti.add(items.copy());
      int bellows = getBellows() + 1;
      float lx = 0.5F + facingX * 1.2F;
      float lz = 0.5F + facingZ * 1.2F;
      for (int a = 0; a < bellows; a++) ejecti.addAll(SmeltingBonus.roll(furnaceItemStack, level.random));
      Direction out = facing().getOpposite();
      for (ItemStack outItem : ejecti) {
         if (!outItem.isEmpty()) DeviceUtils.ejectStackAt(level, worldPosition, out, outItem);
      }
      int cnt = items.getCount();
      float xpf = recipe(furnaceItemStack).map(r -> r.value().getExperience()).orElse(0.0F);
      if (xpf == 0.0F) {
         cnt = 0;
      } else if (xpf < 1.0F) {
         int v = Mth.floor(cnt * xpf);
         if (v < Mth.ceil(cnt * xpf) && (float) Math.random() < cnt * xpf - v) v++;
         cnt = v;
      }
      while (cnt > 0) {
         int v = ExperienceOrb.getExperienceValue(cnt);
         cnt -= v;
         ExperienceOrb xp = new ExperienceOrb(level, worldPosition.getX() + lx, worldPosition.getY() + 0.4F, worldPosition.getZ() + lz, v);
         double mx = facingX == 0 ? (level.random.nextFloat() - level.random.nextFloat()) * 0.025F : facingX * 0.13F;
         double mz = facingZ == 0 ? (level.random.nextFloat() - level.random.nextFloat()) * 0.025F : facingZ * 0.13F;
         xp.setDeltaMovement(mx, 0.0, mz);
         level.addFreshEntity(xp);
      }
   }

   private void setFacing() {
      Direction face = facing().getOpposite();
      facingX = face.getStepX();
      facingZ = face.getStepZ();
   }

   @Override
   public boolean triggerEvent(int id, int param) {
      if (id != 3) return super.triggerEvent(id, param);
      if (level.isClientSide) {
         for (int a = 0; a < 5; a++) {
            thaumcraft.client.fx.DeviceFX.furnaceLava(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), facingX, facingZ);
            level.playLocalSound(worldPosition.getX() + 0.5F, worldPosition.getY() + 0.5F, worldPosition.getZ() + 0.5F, SoundEvents.LAVA_POP,
               SoundSource.BLOCKS, 0.1F + level.random.nextFloat() * 0.1F, 0.9F + level.random.nextFloat() * 0.15F, false);
         }
      }
      return true;
   }

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      return true;
   }

   @Override
   public int[] getSlotsForFace(Direction side) {
      return side == Direction.UP ? super.getSlotsForFace(side) : new int[0];
   }

   @Override
   public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
      return false;
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      super.readExtra(tag, registries);
      furnaceCookTime = tag.getShort("CookTime");
      speedyTime = tag.getShort("SpeedyTime");
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      super.writeExtra(tag, registries);
      tag.putShort("CookTime", (short) furnaceCookTime);
      tag.putShort("SpeedyTime", (short) speedyTime);
   }
}
