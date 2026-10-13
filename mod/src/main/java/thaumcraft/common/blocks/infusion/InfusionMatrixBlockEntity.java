package thaumcraft.common.blocks.infusion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.casters.IInteractWithCaster;
import thaumcraft.api.crafting.IInfusionStabiliser;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.blocks.devices.StabilizerBlockEntity;
import thaumcraft.common.lib.EssentiaHandler;
import thaumcraft.common.network.InfusionFXPayload;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.registers.ModBlockEntities;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModRecipes;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * TileInfusionMatrix. A caster activates it (if the altar is complete), a second click starts the infusion: every
 * cycle it pulls one essentia from nearby jars, then the components one by one off the pedestals, while instability
 * eats at the stability that symmetric stabilizers (candles, skulls...) replenish. Too unstable and bad things happen.
 */
public class InfusionMatrixBlockEntity extends TCBlockEntity implements IInteractWithCaster, IAspectContainer {
   private final List<BlockPos> pedestals = new ArrayList<>();
   public boolean active;
   public boolean crafting;
   public boolean checkSurroundings = true;
   public float costMult;
   private int cycleTime = 20;
   public int stabilityCap = 25;
   public float stability;
   public float stabilityReplenish;
   private AspectList recipeEssentia = new AspectList();
   private List<ItemStack> recipeIngredients = new ArrayList<>();
   private ItemStack recipeOutput = ItemStack.EMPTY;
   @Nullable
   private String recipePlayer;
   private ItemStack recipeInput = ItemStack.EMPTY;
   private int recipeInstability;
   public int count;
   public int craftCount;
   public float startUp;
   private int countDelay = cycleTime / 2;
   int itemCount;
   private final List<BlockPos> problemBlocks = new ArrayList<>();
   private final Map<Block, Integer> tempBlockCount = new HashMap<>();
   /** Client: items and players currently streaming into the matrix (SourceFX). */
   public final Map<String, SourceFX> sourceFX = new HashMap<>();

   public record SourceFX(BlockPos loc, int ticks, int entity) {
   }

   public InfusionMatrixBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntities.INFUSION_MATRIX.get(), pos, state);
   }

   // ---------------------------------------------------------------- data

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      active = tag.getBoolean("active");
      crafting = tag.getBoolean("crafting");
      stability = tag.getFloat("stability");
      recipeInstability = tag.getInt("recipeinst");
      recipeEssentia = AspectList.CODEC.parse(NbtOps.INSTANCE, tag.get("Aspects")).result().orElseGet(AspectList::new);
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putBoolean("active", active);
      tag.putBoolean("crafting", crafting);
      tag.putFloat("stability", stability);
      tag.putInt("recipeinst", recipeInstability);
      tag.put("Aspects", AspectList.CODEC.encodeStart(NbtOps.INSTANCE, recipeEssentia).getOrThrow());
   }

   @Override
   protected void readExtra(CompoundTag tag, HolderLookup.Provider registries) {
      recipeIngredients = new ArrayList<>();
      for (Tag t : tag.getList("recipein", Tag.TAG_COMPOUND)) recipeIngredients.add(ItemStack.parseOptional(registries, (CompoundTag) t));
      recipeOutput = tag.contains("recipeout") ? ItemStack.parseOptional(registries, tag.getCompound("recipeout")) : ItemStack.EMPTY;
      recipeInput = tag.contains("recipeinput") ? ItemStack.parseOptional(registries, tag.getCompound("recipeinput")) : ItemStack.EMPTY;
      recipePlayer = tag.contains("recipeplayer") ? tag.getString("recipeplayer") : null;
   }

   @Override
   protected void writeExtra(CompoundTag tag, HolderLookup.Provider registries) {
      ListTag list = new ListTag();
      for (ItemStack s : recipeIngredients) {
         if (!s.isEmpty()) list.add(s.save(registries));
      }
      tag.put("recipein", list);
      if (!recipeOutput.isEmpty()) tag.put("recipeout", recipeOutput.save(registries));
      if (!recipeInput.isEmpty()) tag.put("recipeinput", recipeInput.save(registries));
      if (recipePlayer != null) tag.putString("recipeplayer", recipePlayer);
   }

   // ---------------------------------------------------------------- stability

   public enum EnumStability {
      VERY_STABLE, STABLE, UNSTABLE, VERY_UNSTABLE
   }

   public EnumStability getStability() {
      return stability > stabilityCap / 2.0F ? EnumStability.VERY_STABLE : stability >= 0.0F ? EnumStability.STABLE
         : stability > -25.0F ? EnumStability.UNSTABLE : EnumStability.VERY_UNSTABLE;
   }

   private float getModFromCurrentStability() {
      return switch (getStability()) {
         case VERY_STABLE -> 5.0F;
         case STABLE -> 6.0F;
         case UNSTABLE -> 7.0F;
         case VERY_UNSTABLE -> 8.0F;
      };
   }

   public float getLossPerCycle() {
      return recipeInstability / getModFromCurrentStability();
   }

   // ---------------------------------------------------------------- ticking

   public static void tick(Level level, BlockPos pos, BlockState state, InfusionMatrixBlockEntity be) {
      be.update();
   }

   private void update() {
      count++;
      if (checkSurroundings) {
         checkSurroundings = false;
         getSurroundings();
      }
      if (level.isClientSide) {
         thaumcraft.client.fx.InfusionFX.matrixEffects(this);
         return;
      }
      if (count % (crafting ? 20 : 100) == 0 && !validLocation()) {
         active = false;
         sync();
         return;
      }
      if (active && !crafting && stability < stabilityCap && count % Math.max(5, countDelay) == 0) {
         stability = Math.min(stabilityCap, stability + Math.max(0.1F, stabilityReplenish));
         sync();
      }
      if (active && crafting && count % Math.max(1, countDelay) == 0) {
         craftCycle();
         setChanged();
      }
   }

   /** The pedestal two below and the four pillars at the corners. */
   public boolean validLocation() {
      return level.getBlockState(worldPosition.below(2)).getBlock() instanceof PedestalBlock
         && level.getBlockState(worldPosition.offset(1, -2, 1)).getBlock() instanceof PillarBlock
         && level.getBlockState(worldPosition.offset(-1, -2, 1)).getBlock() instanceof PillarBlock
         && level.getBlockState(worldPosition.offset(1, -2, -1)).getBlock() instanceof PillarBlock
         && level.getBlockState(worldPosition.offset(-1, -2, -1)).getBlock() instanceof PillarBlock;
   }

   @Nullable
   private PedestalBlockEntity centralPedestal() {
      return level.getBlockEntity(worldPosition.below(2)) instanceof PedestalBlockEntity p ? p : null;
   }

   public void craftingStart(Player player) {
      if (!validLocation()) {
         active = false;
         sync();
         return;
      }
      getSurroundings();
      PedestalBlockEntity ped = centralPedestal();
      recipeInput = ped != null ? ped.getItem().copy() : ItemStack.EMPTY;
      if (recipeInput.isEmpty()) return;
      List<ItemStack> components = new ArrayList<>();
      for (BlockPos cc : pedestals) {
         if (level.getBlockEntity(cc) instanceof PedestalBlockEntity p && !p.getItem().isEmpty()) components.add(p.getItem().copy());
      }
      if (components.isEmpty()) return;
      InfusionRecipe recipe = findMatchingInfusionRecipe(level, components, recipeInput, player);
      if (costMult < 0.5F) costMult = 0.5F;
      if (recipe == null) return;
      recipeIngredients = components;
      recipeOutput = recipe.getRecipeOutput(player, recipeInput, components);
      recipeInstability = recipe.getInstability(player, recipeInput, components);
      AspectList al = recipe.getAspects(player, recipeInput, components);
      AspectList al2 = new AspectList();
      for (Aspect as : al.getAspects()) {
         int n = (int) (al.getAmount(as) * costMult);
         if (n > 0) al2.add(as, n);
      }
      recipeEssentia = al2;
      recipePlayer = player.getName().getString();
      crafting = true;
      level.playSound(null, worldPosition, ModSounds.CRAFTSTART.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
      sync();
   }

   @Nullable
   public static InfusionRecipe findMatchingInfusionRecipe(Level level, List<ItemStack> items, ItemStack input, Player player) {
      for (RecipeHolder<InfusionRecipe> h : level.getRecipeManager().getAllRecipesFor(ModRecipes.INFUSION.get())) {
         if (h.value().matches(items, input, level, player)) return h.value();
      }
      return null;
   }

   private void craftCycle() {
      boolean valid = false;
      float ff = level.random.nextFloat() * getLossPerCycle();
      stability -= ff;
      stability += stabilityReplenish;
      stability = Mth.clamp(stability, -100.0F, stabilityCap);
      PedestalBlockEntity ped = centralPedestal();
      if (ped != null && !ped.getItem().isEmpty() && ItemStack.isSameItemSameComponents(ped.getItem(), recipeInput)) valid = true;

      if (!valid || stability < 0.0F && level.random.nextInt(1500) <= Math.abs(stability)) {
         switch (level.random.nextInt(24)) {
            case 0, 1, 2, 3 -> inEvEjectItem(0);
            case 4, 5, 6 -> inEvWarp();
            case 7, 8, 9 -> inEvZap(false);
            case 10, 11 -> inEvZap(true);
            case 12, 13 -> inEvEjectItem(1);
            case 14, 15 -> inEvEjectItem(2);
            case 16 -> inEvEjectItem(3);
            case 17 -> inEvEjectItem(4);
            case 18, 19 -> inEvHarm(false);
            case 20, 21 -> inEvEjectItem(5);
            case 22 -> inEvHarm(true);
            default -> level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
               1.5F + level.random.nextFloat(), Level.ExplosionInteraction.NONE);
         }
         stability += 5.0F + level.random.nextFloat() * 5.0F;
         inResAdd();
         if (valid) return;
      }
      if (!valid) {
         crafting = false;
         recipeEssentia = new AspectList();
         recipeInstability = 0;
         sync();
         level.playSound(null, worldPosition, ModSounds.CRAFTFAIL.get(), SoundSource.BLOCKS, 1.0F, 0.6F);
         return;
      }
      if (countDelay < 1) countDelay = 1;
      if (recipeEssentia.visSize() > 0) {
         for (Aspect aspect : recipeEssentia.getAspects()) {
            int na = recipeEssentia.getAmount(aspect);
            if (na > 0) {
               if (EssentiaHandler.drainEssentia(this, aspect, null, 12, na > 1 ? countDelay : 0)) {
                  recipeEssentia.reduce(aspect, 1);
                  sync();
                  return;
               }
               stability -= 0.25F;
               sync();
            }
         }
         checkSurroundings = true;
      } else if (recipeIngredients.isEmpty()) {
         crafting = false;
         craftingFinish(recipeOutput);
         recipeOutput = ItemStack.EMPTY;
         sync();
      } else {
         for (int a = 0; a < recipeIngredients.size(); a++) {
            for (BlockPos cc : pedestals) {
               if (level.getBlockEntity(cc) instanceof PedestalBlockEntity p && !p.getItem().isEmpty()
                  && sameForCrafting(p.getItem(), recipeIngredients.get(a))) {
                  if (itemCount == 0) {
                     itemCount = 5;
                     fx(new InfusionFXPayload(worldPosition, cc, 0, InfusionFXPayload.SOURCE));
                  } else if (itemCount-- <= 1) {
                     ItemStack is = p.getItem().getCraftingRemainingItem();
                     p.setItem(0, is.isEmpty() ? ItemStack.EMPTY : is.copy());
                     recipeIngredients.remove(a);
                     setChanged();
                  }
                  return;
               }
            }
            Aspect[] ingEss = recipeEssentia.getAspects();
            if (ingEss.length > 0 && level.random.nextInt(1 + a) == 0) {
               recipeEssentia.add(ingEss[level.random.nextInt(ingEss.length)], 1);
               stability -= 0.25F;
               sync();
            }
         }
      }
   }

   private static boolean sameForCrafting(ItemStack a, ItemStack b) {
      return ItemStack.isSameItemSameComponents(a, b);
   }

   private void fx(InfusionFXPayload p) {
      if (level instanceof ServerLevel sl) ModNetwork.sendNear(sl, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 32.0, p);
   }

   private AABB area(double grow) {
      return new AABB(worldPosition).inflate(grow);
   }

   private void inEvZap(boolean all) {
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area(10.0))) {
         fx(InfusionFXPayload.arc(worldPosition, target));
         target.hurt(level.damageSources().magic(), 4 + level.random.nextInt(4));
         if (!all) break;
      }
   }

   private void inEvHarm(boolean all) {
      for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area(10.0))) {
         // Flux taint and vis exhaustion come with the flux milestone; until then the matrix poisons and weakens.
         if (level.random.nextBoolean()) target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 120, 0, false, true));
         else target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 2400, 0, true, true));
         if (!all) break;
      }
   }

   private void inResAdd() {
      for (Player player : level.getEntitiesOfClass(Player.class, area(10.0))) {
         var k = ResearchManager.knowledge(player);
         if (!k.isResearchKnown("!INSTABILITY")) {
            k.addResearch("!INSTABILITY");
            if (player instanceof ServerPlayer sp) ResearchManager.sync(sp);
            player.displayClientMessage(Component.translatable("got.instability").withStyle(ChatFormatting.DARK_PURPLE), true);
         }
      }
   }

   private void inEvWarp() {
      List<Player> targets = level.getEntitiesOfClass(Player.class, area(10.0));
      if (targets.isEmpty()) return;
      Player target = targets.get(level.random.nextInt(targets.size()));
      if (level.random.nextFloat() < 0.25F) ResearchManager.addWarp(target, 1, PlayerWarp.Type.NORMAL);
      else ResearchManager.addWarp(target, 2 + level.random.nextInt(4), PlayerWarp.Type.TEMPORARY);
   }

   private void inEvEjectItem(int type) {
      for (int retries = 0; retries < 25 && !pedestals.isEmpty(); retries++) {
         BlockPos cc = pedestals.get(level.random.nextInt(pedestals.size()));
         if (!(level.getBlockEntity(cc) instanceof PedestalBlockEntity ped) || ped.getItem().isEmpty()) continue;
         BlockPos stabPos = ped.findInstabilityMitigator();
         if (stabPos != null && level.getBlockEntity(stabPos) instanceof StabilizerBlockEntity st && st.mitigate(Mth.nextInt(level.random, 5, 10))) {
            level.blockEvent(cc, level.getBlockState(cc).getBlock(), 5, 0);
            fx(InfusionFXPayload.arcBlocks(worldPosition, cc.above()));
            fx(InfusionFXPayload.arcBlocks(cc.above(), stabPos));
            return;
         }
         if (type > 3 && type != 5) {
            ped.setItem(0, ItemStack.EMPTY);
         } else {
            net.minecraft.world.Containers.dropContents(level, cc.above(), new SimpleContainer(ped.removeItemNoUpdate(0)));
            ped.sync();
         }
         if (type == 1 || type == 3) {
            // Flux goo comes with the flux milestone: a puddle of flux in the aura instead.
            AuraHandler.addFlux(level, cc, 3.0F);
            level.playSound(null, cc, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.3F, 1.0F);
         } else if (type == 2 || type == 4) {
            AuraHandler.addFlux(level, cc, 5 + level.random.nextInt(5));
         } else if (type == 5) {
            level.explode(null, cc.getX() + 0.5, cc.getY() + 0.5, cc.getZ() + 0.5, 1.0F, Level.ExplosionInteraction.NONE);
         }
         level.blockEvent(cc, level.getBlockState(cc).getBlock(), 11, 0);
         fx(InfusionFXPayload.arcBlocks(worldPosition, cc.above()));
         return;
      }
   }

   private void craftingFinish(ItemStack out) {
      PedestalBlockEntity ped = centralPedestal();
      if (ped == null) return;
      ItemStack old = ped.getItem();
      ItemStack qs = out.copy();
      if (old.isDamageableItem() && old.isDamaged() && qs.isDamageableItem() && !qs.isDamaged()) {
         float dmg = (float) old.getDamageValue() / old.getMaxDamage();
         qs.setDamageValue((int) (qs.getMaxDamage() * dmg));
      }
      ped.setItemFromInfusion(qs);
      if (recipePlayer != null && level.getServer() != null) {
         ServerPlayer p = level.getServer().getPlayerList().getPlayerByName(recipePlayer);
         if (p != null) EventHooks.firePlayerCraftingEvent(p, ped.getItem(), new SimpleContainer(1));
      }
      recipeEssentia = new AspectList();
      recipeInstability = 0;
      sync();
      level.blockEvent(worldPosition.below(2), level.getBlockState(worldPosition.below(2)).getBlock(), 12, 0);
      level.playSound(null, worldPosition, ModSounds.WAND.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
   }

   /** Finds the pedestals and stabilizers around the matrix and works out stability, speed and cost. */
   private void getSurroundings() {
      Set<BlockPos> stuff = new HashSet<>();
      pedestals.clear();
      tempBlockCount.clear();
      problemBlocks.clear();
      cycleTime = 10;
      stabilityReplenish = 0.0F;
      costMult = 1.0F;
      for (int xx = -8; xx <= 8; xx++) {
         for (int zz = -8; zz <= 8; zz++) {
            for (int yy = -3; yy <= 7; yy++) {
               if (xx == 0 && zz == 0) continue;
               BlockPos bp = worldPosition.offset(xx, -yy, zz);
               BlockState s = level.getBlockState(bp);
               if (s.getBlock() instanceof PedestalBlock) pedestals.add(bp);
               if (s.getBlock() instanceof AbstractSkullBlock || s.getBlock() instanceof IInfusionStabiliser st && st.canStabaliseInfusion(level, bp)) {
                  stuff.add(bp);
               }
            }
         }
      }
      while (!stuff.isEmpty()) {
         BlockPos c1 = stuff.iterator().next();
         BlockPos c2 = new BlockPos(2 * worldPosition.getX() - c1.getX(), c1.getY(), 2 * worldPosition.getZ() - c1.getZ());
         Block sb1 = level.getBlockState(c1).getBlock();
         Block sb2 = level.getBlockState(c2).getBlock();
         float amt1 = sb1 instanceof IInfusionStabiliser s1 ? s1.getStabilizationAmount(level, c1) : 0.1F;
         float amt2 = sb2 instanceof IInfusionStabiliser s2 ? s2.getStabilizationAmount(level, c2) : 0.1F;
         if (sb1 == sb2 && amt1 == amt2) {
            if (sb1 instanceof IInfusionStabiliser s1 && s1.hasSymmetryPenalty(level, c1, c2)) {
               stabilityReplenish -= s1.getSymmetryPenalty(level, c1);
               problemBlocks.add(c1);
            } else {
               stabilityReplenish += calcDiminishingReturns(sb1, amt1);
            }
         } else {
            stabilityReplenish -= Math.max(amt1, amt2);
            problemBlocks.add(c1);
         }
         stuff.remove(c2);
         stuff.remove(c1);
      }
      Block p1 = level.getBlockState(worldPosition.offset(-1, -2, -1)).getBlock();
      if (p1 instanceof PillarBlock && level.getBlockState(worldPosition.offset(1, -2, -1)).is(p1) && level.getBlockState(worldPosition.offset(1, -2, 1)).is(p1)
         && level.getBlockState(worldPosition.offset(-1, -2, 1)).is(p1)) {
         if (p1 == ModBlocks.PILLAR_ANCIENT.get()) {
            cycleTime--;
            costMult -= 0.1F;
            stabilityReplenish -= 0.1F;
         }
         if (p1 == ModBlocks.PILLAR_ELDRITCH.get()) {
            cycleTime -= 3;
            costMult += 0.05F;
            stabilityReplenish += 0.2F;
         }
      }
      int[] xm = {-1, 1, 1, -1};
      int[] zm = {-1, -1, 1, 1};
      for (int a = 0; a < 4; a++) {
         BlockState b = level.getBlockState(worldPosition.offset(xm[a], -3, zm[a]));
         if (b.is(ModBlocks.MATRIX_SPEED.get())) {
            cycleTime--;
            costMult += 0.01F;
         }
         if (b.is(ModBlocks.MATRIX_COST.get())) {
            cycleTime++;
            costMult -= 0.02F;
         }
      }
      countDelay = cycleTime / 2;
      for (BlockPos cc : pedestals) {
         BlockState bb = level.getBlockState(cc);
         if (bb.is(ModBlocks.PEDESTAL_ELDRITCH.get())) costMult += 0.0025F;
         if (bb.is(ModBlocks.PEDESTAL_ANCIENT.get())) costMult -= 0.01F;
      }
   }

   private float calcDiminishingReturns(Block b, float base) {
      int c = tempBlockCount.getOrDefault(b, 0);
      tempBlockCount.put(b, c + 1);
      return c > 0 ? (float) (base * Math.pow(0.75, c)) : base;
   }

   public List<BlockPos> getProblemBlocks() {
      return problemBlocks;
   }

   @Override
   public boolean onCasterRightClick(Level level, ItemStack caster, Player player, BlockPos pos, Direction side, InteractionHand hand, BlockHitResult hit) {
      if (level.isClientSide) {
         if (active && !crafting) checkSurroundings = true;
         return true;
      }
      if (active && !crafting) {
         craftingStart(player);
      } else if (!active && validLocation()) {
         level.playSound(null, pos, ModSounds.CRAFTSTART.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
         active = true;
         sync();
      }
      return true;
   }

   /** IGogglesDisplayExtended: stability readout. */
   public List<Component> getGogglesText() {
      List<Component> out = new ArrayList<>();
      out.add(Component.translatable("stability." + getStability().name()).withStyle(ChatFormatting.BOLD));
      out.add(Component.literal(String.format("%.2f ", stabilityReplenish)).append(Component.translatable("stability.gain"))
         .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
      float lpc = getLossPerCycle();
      if (lpc != 0.0F) {
         out.add(Component.translatable("stability.range").append(String.format("%.2f ", lpc)).append(Component.translatable("stability.loss"))
            .withStyle(ChatFormatting.RED));
      }
      return out;
   }

   // ---------------------------------------------------------------- IAspectContainer (shows the essentia still needed)

   @Override
   public AspectList getAspects() {
      return recipeEssentia;
   }

   @Override
   public void setAspects(AspectList aspects) {
   }

   @Override
   public boolean doesContainerAccept(Aspect tag) {
      return true;
   }

   @Override
   public int addToContainer(Aspect tag, int amount) {
      return 0;
   }

   @Override
   public boolean takeFromContainer(Aspect tag, int amount) {
      return false;
   }

   @Override
   public boolean doesContainerContainAmount(Aspect tag, int amount) {
      return false;
   }

   @Override
   public int containerContains(Aspect tag) {
      return 0;
   }
}
