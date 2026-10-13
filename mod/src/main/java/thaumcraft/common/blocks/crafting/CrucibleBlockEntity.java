package thaumcraft.common.blocks.crafting;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.entities.SpecialItemEntity;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModRecipes;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraHandler;

/** TileCrucible: boils water over a heat source, dissolves items into essentia and turns catalysts into recipe results. */
public class CrucibleBlockEntity extends BlockEntity {
   public static final int MAX_TAGS = 500;
   public short heat;
   public AspectList aspects = new AspectList();
   public final FluidTank tank = new FluidTank(1000, fs -> fs.getFluid() == Fluids.WATER) {
      @Override
      protected void onContentsChanged() {
         setChanged();
         sync();
      }
   };
   private long counter = -100L;

   public CrucibleBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.CRUCIBLE.get(), pos, state);
   }

   public IFluidHandler getTank() {
      return tank;
   }

   public boolean isBoiling() {
      return heat > 150 && tank.getFluidAmount() > 0;
   }

   /** Lava, fire, magma, nitor (and, new in the port, a lit campfire). */
   public static boolean isHeatSource(BlockState block) {
      return block.getFluidState().is(FluidTags.LAVA) || block.is(BlockTags.FIRE) || block.is(Blocks.MAGMA_BLOCK)
         || block.is(thaumcraft.common.registers.ModTags.Blocks.NITOR)
         || block.is(BlockTags.CAMPFIRES) && block.getValue(CampfireBlock.LIT);
   }

   public static void serverTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity be) {
      be.counter++;
      int prevheat = be.heat;
      if (be.tank.getFluidAmount() > 0) {
         if (!isHeatSource(level.getBlockState(pos.below()))) {
            if (be.heat > 0) {
               be.heat--;
               if (be.heat == 149) {
                  be.setChanged();
                  be.sync();
               }
            }
         } else if (be.heat < 200) {
            be.heat++;
            if (prevheat < 151 && be.heat >= 151) {
               be.setChanged();
               be.sync();
            }
         }
      } else if (be.heat > 0) {
         be.heat--;
      }
      if (be.aspects.visSize() > MAX_TAGS) be.spillRandom();
      if (be.counter >= 100L) {
         be.spillRandom();
         be.counter = 0L;
      }
   }

   public static void clientTick(Level level, BlockPos pos, BlockState state, CrucibleBlockEntity be) {
      int prevheat = be.heat;
      if (be.tank.getFluidAmount() > 0) ClientEffects.draw(be);
      if (prevheat < 151 && be.heat >= 151) be.heat++;
   }

   /** Client-only effects, kept apart so the server never loads the particle classes. */
   static final class ClientEffects {
      static void draw(CrucibleBlockEntity be) {
         thaumcraft.client.renderers.CrucibleEffects.draw(be);
      }
   }

   public void ejectItem(ItemStack items) {
      boolean first = true;
      do {
         ItemStack spitout = items.copy();
         if (spitout.getCount() > spitout.getMaxStackSize()) spitout.setCount(spitout.getMaxStackSize());
         items.shrink(spitout.getCount());
         SpecialItemEntity e = new SpecialItemEntity(level, worldPosition.getX() + 0.5F, worldPosition.getY() + 0.71F,
            worldPosition.getZ() + 0.5F, spitout);
         e.setDeltaMovement(first ? 0.0 : (level.random.nextFloat() - level.random.nextFloat()) * 0.01F, 0.075F,
            first ? 0.0 : (level.random.nextFloat() - level.random.nextFloat()) * 0.01F);
         level.addFreshEntity(e);
         first = false;
      } while (items.getCount() > 0);
   }

   /** ThaumcraftCraftingManager.findMatchingCrucibleRecipe: the known recipe with the most essentia. */
   @Nullable
   public static CrucibleRecipe findMatchingCrucibleRecipe(Level level, @Nullable Player player, AspectList aspects, ItemStack lastDrop) {
      if (player == null) return null;
      int highest = 0;
      CrucibleRecipe out = null;
      ItemStack temp = lastDrop.copyWithCount(1);
      for (RecipeHolder<CrucibleRecipe> h : level.getRecipeManager().getAllRecipesFor(ModRecipes.CRUCIBLE.get())) {
         CrucibleRecipe recipe = h.value();
         if (ResearchManager.knowsResearchStrict(player, recipe.getResearch()) && recipe.matches(level, aspects, temp)) {
            int result = recipe.getAspects(level).visSize();
            if (result > highest) {
               highest = result;
               out = recipe;
            }
         }
      }
      return out;
   }

   /** Returns what is left of the stack (empty if all of it was used). */
   public ItemStack attemptSmelt(ItemStack item, @Nullable Player player) {
      boolean bubble = false;
      boolean craftDone = false;
      int stacksize = item.getCount();
      for (int a = 0; a < stacksize; a++) { // (as in the original, only about half a thrown stack goes in per tick)
         CrucibleRecipe rc = findMatchingCrucibleRecipe(level, player, aspects, item);
         if (rc != null && tank.getFluidAmount() > 0) {
            ItemStack out = rc.assemble(null, level.registryAccess());
            if (player != null) EventHooks.firePlayerCraftingEvent(player, out.copy(), new SimpleContainer(item.copyWithCount(1)));
            aspects = rc.removeMatching(level, aspects);
            tank.drain(50, IFluidHandler.FluidAction.EXECUTE);
            ejectItem(out);
            craftDone = true;
            stacksize--;
            counter = -250L;
         } else {
            AspectList ot = AspectHelper.getObjectAspects(level, item.copyWithCount(1));
            if (ot != null && !ot.isEmpty()) {
               for (Aspect tag : ot.getAspects()) aspects.add(tag, ot.getAmount(tag));
               bubble = true;
               stacksize--;
               counter = -150L;
            }
         }
      }
      if (bubble) {
         level.playSound(null, worldPosition, ModSounds.BUBBLE.get(), SoundSource.BLOCKS, 0.2F, 1.0F + level.random.nextFloat() * 0.4F);
         sync();
         level.blockEvent(worldPosition, getBlockState().getBlock(), 2, 1);
      }
      if (craftDone) {
         sync();
         level.blockEvent(worldPosition, getBlockState().getBlock(), 99, 0);
      }
      setChanged();
      return stacksize <= 0 ? ItemStack.EMPTY : item.copyWithCount(stacksize);
   }

   public void attemptSmelt(ItemEntity entity) {
      Player player = entity.getOwner() instanceof Player p ? p : null;
      ItemStack res = attemptSmelt(entity.getItem(), player);
      if (res.isEmpty()) entity.discard();
      else entity.setItem(res);
   }

   public float getFluidHeight() {
      float base = 0.3F + 0.5F * ((float) tank.getFluidAmount() / tank.getCapacity());
      float out = base + aspects.visSize() / 500.0F * (1.0F - base);
      if (out > 1.0F) out = 1.001F;
      if (out == 1.0F) out = 0.9999F;
      return out;
   }

   public void spillRandom() {
      if (!aspects.isEmpty()) {
         Aspect tag = aspects.getAspects()[level.random.nextInt(aspects.size())];
         aspects.remove(tag, 1);
         AuraHandler.addFlux(level, worldPosition, tag == Aspect.FLUX ? 1.0F : 0.25F);
      }
      setChanged();
      sync();
   }

   public void spillRemnants() {
      int vs = aspects.visSize();
      if (tank.getFluidAmount() > 0 || vs > 0) {
         tank.setFluid(FluidStack.EMPTY);
         AuraHandler.addFlux(level, worldPosition, vs * 0.25F);
         int f = aspects.getAmount(Aspect.FLUX);
         if (f > 0) AuraHandler.addFlux(level, worldPosition, f * 0.75F);
         aspects = new AspectList();
         level.blockEvent(worldPosition, getBlockState().getBlock(), 2, 5);
         setChanged();
         sync();
      }
   }

   public int comparatorSignal() {
      float r = aspects.visSize() / 500.0F;
      return (int) Math.floor(r * 14.0F) + (aspects.visSize() > 0 ? 1 : 0);
   }

   @Override
   public boolean triggerEvent(int id, int type) {
      if (level == null) return false;
      if (id == 99 || id == 1 || id == 2) {
         if (level.isClientSide) thaumcraft.client.renderers.CrucibleEffects.event(this, id, type);
         return true;
      }
      return super.triggerEvent(id, type);
   }

   public void sync() {
      if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
   }

   private void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putShort("Heat", heat);
      tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
      tag.put("Aspects", AspectList.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, aspects).getOrThrow());
   }

   private void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      heat = tag.getShort("Heat");
      tank.readFromNBT(registries, tag.getCompound("Tank"));
      aspects = AspectList.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("Aspects")).result().orElseGet(AspectList::new);
   }

   @Override
   protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      readSync(tag, registries);
   }

   @Override
   protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      writeSync(tag, registries);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      CompoundTag tag = new CompoundTag();
      writeSync(tag, registries);
      return tag;
   }

   @Override
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }
}
