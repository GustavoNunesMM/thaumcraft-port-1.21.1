package thaumcraft.common.blocks.devices;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.blocks.DeviceBlock;
import thaumcraft.common.menu.ThaumatoriumMenu;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModRecipes;
import thaumcraft.common.registers.ModTags;
import thaumcraft.common.research.ResearchManager;

/**
 * TileThaumatorium: an automated crucible. Players pick crucible recipes they know for the catalyst in its slot (one,
 * plus two per mnemonic matrix); while the crucible below is heated and it is not powered, it sucks the recipe's
 * essentia in through tubes (on itself or the top part) and then crafts, ejecting the result out of its front.
 * Recipes are remembered by recipe id (the 1.12 version used recipe hashes).
 */
public class ThaumatoriumBlockEntity extends DeviceInventoryBlockEntity implements IAspectContainer, IEssentiaTransport, MenuProvider {
   public AspectList essentia = new AspectList();
   public final List<ResourceLocation> recipeIds = new ArrayList<>();
   public final List<AspectList> recipeEssentia = new ArrayList<>();
   public final List<String> recipePlayer = new ArrayList<>();
   public int currentCraft = -1;
   public int maxRecipes = 1;
   @Nullable
   public Aspect currentSuction = null;
   public int venting = 0;
   private int counter = 0;
   private boolean heated = false;
   @Nullable
   private RecipeHolder<CrucibleRecipe> currentRecipe = null;
   /** The recipes the last player to look could pick (server side, refreshed by the menu). */
   public List<RecipeHolder<CrucibleRecipe>> recipes = new ArrayList<>();

   public ThaumatoriumBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.THAUMATORIUM_BE.get(), pos, state, 1);
   }

   private Direction facing() {
      return DeviceBlock.getFacing(getBlockState());
   }

   @Nullable
   public RecipeHolder<CrucibleRecipe> recipe(ResourceLocation id) {
      if (level == null) return null;
      Optional<RecipeHolder<?>> h = level.getRecipeManager().byKey(id);
      if (h.isPresent() && h.get().value() instanceof CrucibleRecipe) {
         @SuppressWarnings("unchecked")
         RecipeHolder<CrucibleRecipe> r = (RecipeHolder<CrucibleRecipe>) h.get();
         return r;
      }
      return null;
   }

   private boolean checkHeat() {
      BlockState bs = level.getBlockState(worldPosition.below(2));
      return bs.getFluidState().is(FluidTags.LAVA) || bs.is(BlockTags.FIRE) || bs.is(ModTags.Blocks.NITOR) || bs.is(Blocks.MAGMA_BLOCK);
   }

   public ItemStack getCurrentOutputRecipe() {
      if (currentCraft >= 0 && currentCraft < recipeIds.size()) {
         RecipeHolder<CrucibleRecipe> r = recipe(recipeIds.get(currentCraft));
         if (r != null) return r.value().getResultItem(level.registryAccess()).copy();
      }
      return ItemStack.EMPTY;
   }

   public static void tick(Level level, BlockPos pos, BlockState state, ThaumatoriumBlockEntity be) {
      if (level.isClientSide) {
         if (be.venting > 0) {
            be.venting--;
            var r = level.random;
            float fx = 0.1F - r.nextFloat() * 0.2F, fz = 0.1F - r.nextFloat() * 0.2F, fy = 0.1F - r.nextFloat() * 0.2F;
            float fx2 = 0.1F - r.nextFloat() * 0.2F, fz2 = 0.1F - r.nextFloat() * 0.2F, fy2 = 0.1F - r.nextFloat() * 0.2F;
            Direction f = be.facing();
            thaumcraft.client.fx.DeviceFX.drawVentParticles(pos.getX() + 0.5F + fx + f.getStepX() / 2.0F, pos.getY() + 0.5F + fy,
               pos.getZ() + 0.5F + fz + f.getStepZ() / 2.0F, f.getStepX() / 4.0F + fx2, fy2, f.getStepZ() / 4.0F + fz2, 0xFFFFFF, 2.0F);
         }
         return;
      }
      if (be.counter == 0 || be.counter % 40 == 0) {
         be.heated = be.checkHeat();
         be.getUpgrades();
      }
      be.counter++;
      if (!be.heated || be.gettingPower() || be.counter % 5 != 0 || be.recipeIds.isEmpty()) return;
      ItemStack cat = be.getItem(0);
      if (cat.isEmpty()) {
         be.currentSuction = null;
         return;
      }
      if (be.currentCraft < 0 || be.currentCraft >= be.recipeIds.size() || be.currentRecipe == null || !be.currentRecipe.value().catalystMatches(cat)) {
         for (int a = 0; a < be.recipeIds.size(); a++) {
            RecipeHolder<CrucibleRecipe> r = be.recipe(be.recipeIds.get(a));
            if (r != null && r.value().catalystMatches(cat)) {
               be.currentCraft = a;
               be.currentRecipe = r;
               break;
            }
         }
      }
      if (be.currentCraft < 0 || be.currentCraft >= be.recipeIds.size()) return;
      boolean done = true;
      be.currentSuction = null;
      AspectList need = be.recipeEssentia.get(be.currentCraft);
      for (Aspect aspect : need.getAspectsSortedByName()) {
         if (be.essentia.getAmount(aspect) < need.getAmount(aspect)) {
            be.currentSuction = aspect;
            done = false;
            break;
         }
      }
      if (done) be.completeRecipe();
      else if (be.currentSuction != null) be.fill();
   }

   private void completeRecipe() {
      if (currentRecipe == null || currentCraft >= recipeIds.size() || !currentRecipe.value().matches(level, essentia, getItem(0))) return;
      ItemStack catalyst = getItem(0).copy();
      removeItem(0, 1);
      essentia = new AspectList();
      ItemStack dropped = getCurrentOutputRecipe();
      if (level.getServer() != null) {
         ServerPlayer p = level.getServer().getPlayerList().getPlayerByName(recipePlayer.get(currentCraft));
         if (p != null) EventHooks.firePlayerCraftingEvent(p, dropped.copy(), new SimpleContainer(catalyst));
      }
      DeviceUtils.ejectStackAt(level, worldPosition, facing(), dropped);
      level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.25F,
         2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
      level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
      currentCraft = -1;
      sync();
   }

   /** Pulls one essentia of the current suction from a tube on its sides (or on the top part). */
   void fill() {
      Direction facing = facing();
      for (int y = 0; y <= 1; y++) {
         for (Direction dir : Direction.values()) {
            if (dir == facing || dir == Direction.DOWN || y == 0 && dir == Direction.UP) continue;
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
   public boolean gettingPower() {
      return level.getBestNeighborSignal(worldPosition) > 0 || level.getBestNeighborSignal(worldPosition.below()) > 0
         || level.getBestNeighborSignal(worldPosition.above()) > 0;
   }

   /** Two more recipe slots for every mnemonic matrix facing it (around itself or the top part). */
   public void getUpgrades() {
      Direction facing = facing();
      int mr = 1;
      for (int yy = 0; yy <= 1; yy++) {
         for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN || dir == facing) continue;
            BlockPos bp = new BlockPos(worldPosition.getX() + dir.getStepX(), worldPosition.getY() + yy + dir.getStepY(), worldPosition.getZ() + dir.getStepZ());
            BlockState bs = level.getBlockState(bp);
            if (bs.is(ModDevices.BRAIN_BOX.get()) && bs.getValue(DeviceBlock.FACING) == dir.getOpposite()) mr += 2;
         }
      }
      if (mr != maxRecipes) {
         maxRecipes = mr;
         while (recipeIds.size() > maxRecipes) {
            recipeIds.remove(recipeIds.size() - 1);
            recipeEssentia.remove(recipeEssentia.size() - 1);
            recipePlayer.remove(recipePlayer.size() - 1);
         }
         sync();
      }
   }

   /** updateRecipes: the crucible recipes for the catalyst that the player knows, plus the ones already chosen. */
   public void updateRecipes(Player player) {
      List<RecipeHolder<CrucibleRecipe>> temp = new ArrayList<>();
      ItemStack cat = getItem(0);
      if (!cat.isEmpty() && level != null) {
         for (RecipeHolder<CrucibleRecipe> h : level.getRecipeManager().getAllRecipesFor(ModRecipes.CRUCIBLE.get())) {
            if (ResearchManager.knowsResearchStrict(player, h.value().getResearch()) && h.value().catalystMatches(cat) || recipeIds.contains(h.id())) {
               temp.add(h);
            }
         }
      }
      temp.sort(Comparator.comparing(h -> h.value().getResultItem(level.registryAccess()).getHoverName().getString()));
      recipes = temp;
   }

   /** generateRecipeHashlist: the chosen recipes that are not in the list, then the list. */
   public List<ResourceLocation> generateRecipeList() {
      List<ResourceLocation> out = new ArrayList<>();
      for (ResourceLocation id : recipeIds) {
         if (recipes.stream().noneMatch(h -> h.id().equals(id))) out.add(id);
      }
      for (RecipeHolder<CrucibleRecipe> h : recipes) out.add(h.id());
      return out;
   }

   /** PacketSelectThaumotoriumRecipeToServer: toggles a recipe on or off. */
   public void selectRecipe(Player player, ResourceLocation id) {
      int i = recipeIds.indexOf(id);
      boolean changed = false;
      if (i >= 0) {
         recipeEssentia.remove(i);
         recipePlayer.remove(i);
         recipeIds.remove(i);
         currentCraft = -1;
         changed = true;
      } else if (recipeIds.size() < maxRecipes) {
         updateRecipes(player);
         for (RecipeHolder<CrucibleRecipe> h : recipes) {
            if (h.id().equals(id)) {
               recipeEssentia.add(h.value().getAspects(level).copy());
               recipePlayer.add(player.getGameProfile().getName());
               recipeIds.add(id);
               changed = true;
               break;
            }
         }
      }
      if (changed) sync();
   }

   @Override
   public boolean triggerEvent(int id, int param) {
      if (id >= 0) {
         if (level.isClientSide) venting = 7;
         return true;
      }
      return super.triggerEvent(id, param);
   }

   // ---------------------------------------------------------------- aspects

   @Override
   public int addToContainer(Aspect tt, int am) {
      if (currentRecipe == null) return am;
      int ce = currentRecipe.value().getAspects(level).getAmount(tt) - essentia.getAmount(tt);
      if (ce > 0) {
         int add = Math.min(ce, am);
         essentia.add(tt, add);
         sync();
         return am - add;
      }
      return am;
   }

   @Override
   public boolean takeFromContainer(Aspect tt, int am) {
      if (essentia.getAmount(tt) >= am) {
         essentia.remove(tt, am);
         sync();
         return true;
      }
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tt, int am) {
      return essentia.getAmount(tt) >= am;
   }

   @Override
   public int containerContains(Aspect tt) {
      return essentia.getAmount(tt);
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   @Override
   public AspectList getAspects() {
      return essentia;
   }

   @Override
   public void setAspects(AspectList aspects) {
      essentia = aspects;
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
      return canOutputTo(face) && takeFromContainer(aspect, amount) ? amount : 0;
   }

   @Override
   public int addEssentia(Aspect aspect, int amount, @Nullable Direction face) {
      return canInputFrom(face) ? amount - addToContainer(aspect, amount) : 0;
   }

   @Override
   public int getMinimumSuction() {
      return 0;
   }

   // ---------------------------------------------------------------- inventory / nbt

   @Override
   public boolean canPlaceItem(int slot, ItemStack stack) {
      return true;
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.readSync(tag, registries);
      essentia = tag.contains("essentia") ? AspectList.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("essentia")).result().orElse(new AspectList())
         : new AspectList();
      maxRecipes = Math.max(1, tag.getByte("maxrec"));
      List<String> players = new ArrayList<>(recipePlayer);
      recipeIds.clear();
      recipeEssentia.clear();
      recipePlayer.clear();
      ListTag list = tag.getList("recipes", Tag.TAG_COMPOUND);
      for (int i = 0; i < list.size(); i++) {
         CompoundTag r = list.getCompound(i);
         ResourceLocation id = ResourceLocation.tryParse(r.getString("id"));
         if (id == null) continue;
         recipeIds.add(id);
         recipeEssentia.add(r.contains("aspects")
            ? AspectList.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, r.get("aspects")).result().orElse(new AspectList()) : new AspectList());
         recipePlayer.add(i < players.size() ? players.get(i) : "");
      }
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      super.writeSync(tag, registries);
      tag.putByte("maxrec", (byte) maxRecipes);
      AspectList.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, essentia).result().ifPresent(t -> tag.put("essentia", t));
      ListTag list = new ListTag();
      for (int i = 0; i < recipeIds.size(); i++) {
         CompoundTag r = new CompoundTag();
         r.putString("id", recipeIds.get(i).toString());
         AspectList.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, recipeEssentia.get(i)).result().ifPresent(t -> r.put("aspects", t));
         list.add(r);
      }
      tag.put("recipes", list);
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      super.readExtra(tag, registries);
      ListTag list = tag.getList("OutputPlayer", Tag.TAG_STRING);
      for (int a = 0; a < list.size() && a < recipePlayer.size(); a++) recipePlayer.set(a, list.getString(a));
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      super.writeExtra(tag, registries);
      ListTag list = new ListTag();
      for (String p : recipePlayer) list.add(StringTag.valueOf(p == null ? "" : p));
      tag.put("OutputPlayer", list);
   }

   @Override
   public Component getDisplayName() {
      return Component.translatable("block.thaumcraft.thaumatorium");
   }

   @Override
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return new ThaumatoriumMenu(id, inv, this);
   }
}
