package thaumcraft.common.items.casters.foci;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.FXHelper;
import thaumcraft.common.lib.ServerTasks;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * ServerEvents.addBreaker / addSwapper: blocks broken over several ticks (break and earth foci) and blocks swapped for
 * another (exchange focus), processed once per level tick.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class BlockWorks {
   public static final Predicate<SwapperPredicate> DEFAULT_PREDICATE = p -> true;
   private static final Map<Level, List<BreakData>> BREAK = new WeakHashMap<>();
   private static final Map<Level, List<VirtualSwapper>> SWAP = new WeakHashMap<>();

   private BlockWorks() {
   }

   public record SwapperPredicate(Level world, Player player, BlockPos pos) {}

   static final class BreakData {
      final float strength;
      float durabilityCurrent;
      final float durabilityMax;
      final BlockState source;
      final BlockPos pos;
      final Player player;
      final boolean fx, silk;
      final int fortune;
      final float visCost;

      BreakData(float strength, float durabilityCurrent, float durabilityMax, BlockPos pos, BlockState source, Player player, boolean fx, boolean silk,
                int fortune, float vis) {
         this.strength = strength;
         this.durabilityCurrent = durabilityCurrent;
         this.durabilityMax = durabilityMax;
         this.pos = pos;
         this.source = source;
         this.player = player;
         this.fx = fx;
         this.silk = silk;
         this.fortune = fortune;
         this.visCost = vis;
      }
   }

   record VirtualSwapper(BlockPos pos, @Nullable BlockState source, ItemStack target, boolean consumeTarget, int lifespan, Player player, boolean fx,
                         boolean fancy, int color, boolean pickup, boolean silk, int fortune, Predicate<SwapperPredicate> allowSwap, float visCost) {}

   public static void addBreaker(Level world, BlockPos pos, BlockState source, Player player, boolean fx, boolean silk, int fortune, float str,
                                 float durabilityCurrent, float durabilityMax, int delay, float vis, @Nullable Runnable run) {
      if (world.isClientSide) return;
      if (delay > 0) {
         ServerTasks.schedule(delay, () -> addBreaker(world, pos, source, player, fx, silk, fortune, str, durabilityCurrent, durabilityMax, 0, vis, run));
         return;
      }
      synchronized (BREAK) {
         BREAK.computeIfAbsent(world, w -> new ArrayList<>()).add(new BreakData(str, durabilityCurrent, durabilityMax, pos, source, player, fx, silk,
            fortune, vis));
      }
      if (run != null) run.run();
   }

   public static void addSwapper(Level world, BlockPos pos, @Nullable BlockState source, ItemStack target, boolean consumeTarget, int life,
                                 Player player, boolean fx, boolean fancy, int color, boolean pickup, boolean silk, int fortune,
                                 Predicate<SwapperPredicate> allowSwap, float visCost) {
      if (world.isClientSide) return;
      synchronized (SWAP) {
         SWAP.computeIfAbsent(world, w -> new ArrayList<>()).add(new VirtualSwapper(pos, source, target, consumeTarget, life, player, fx, fancy, color,
            pickup, silk, fortune, allowSwap, visCost));
      }
   }

   @SubscribeEvent
   public static void levelTick(LevelTickEvent.Post event) {
      if (event.getLevel() instanceof ServerLevel level) {
         tickBlockSwap(level);
         tickBlockBreak(level);
      }
   }

   private static void tickBlockBreak(ServerLevel world) {
      List<BreakData> queue;
      synchronized (BREAK) {
         queue = BREAK.remove(world);
      }
      if (queue == null || queue.isEmpty()) return;
      List<BreakData> next = new ArrayList<>();
      for (BreakData vs : queue) {
         BlockState bs = world.getBlockState(vs.pos);
         if (bs == vs.source) {
            if ((vs.visCost <= 0.0F || AuraHandler.getVis(world, vs.pos) >= vs.visCost) && world.mayInteract(vs.player, vs.pos)
               && bs.getDestroySpeed(world, vs.pos) >= 0.0F) {
               if (vs.fx) world.destroyBlockProgress(vs.pos.hashCode(), vs.pos, (int) ((1.0F - vs.durabilityCurrent / vs.durabilityMax) * 10.0F));
               vs.durabilityCurrent -= vs.strength;
               if (vs.durabilityCurrent <= 0.0F) {
                  harvestBlock(world, vs.player, vs.pos, true, vs.silk, vs.fortune, false);
                  if (vs.fx) world.destroyBlockProgress(vs.pos.hashCode(), vs.pos, -1);
                  if (vs.visCost > 0.0F) AuraHandler.drainVis(world, vs.pos, vs.visCost, false);
               } else {
                  next.add(vs);
               }
            }
         } else if (vs.fx) {
            world.destroyBlockProgress(vs.pos.hashCode(), vs.pos, -1);
         }
      }
      if (!next.isEmpty()) {
         synchronized (BREAK) {
            BREAK.computeIfAbsent(world, w -> new ArrayList<>()).addAll(next);
         }
      }
   }

   private static void tickBlockSwap(ServerLevel world) {
      List<VirtualSwapper> queue;
      synchronized (SWAP) {
         queue = SWAP.remove(world);
      }
      if (queue == null || queue.isEmpty()) return;
      List<VirtualSwapper> next = new ArrayList<>();
      for (VirtualSwapper vs : queue) {
         BlockState bs = world.getBlockState(vs.pos);
         boolean allow = bs.getDestroySpeed(world, vs.pos) >= 0.0F;
         if (vs.source != null && vs.source != bs) allow = false;
         if (vs.visCost > 0.0F && AuraHandler.getVis(world, vs.pos) < vs.visCost) allow = false;
         if (!world.mayInteract(vs.player, vs.pos) || !allow) continue;
         if (!vs.target.isEmpty() && vs.target.is(bs.getBlock().asItem())) continue;
         if (EventHooks.onBlockPlace(vs.player, BlockSnapshot.create(world.dimension(), world, vs.pos), Direction.UP)) continue;
         if (!vs.allowSwap.test(new SwapperPredicate(world, vs.player, vs.pos))) continue;
         int slot = vs.consumeTarget && !vs.target.isEmpty() ? playerSlotFor(vs.player, vs.target) : 1;
         if (vs.player.getAbilities().instabuild) slot = 1;
         if (slot < 0) continue;
         if (!vs.player.getAbilities().instabuild) {
            if (vs.consumeTarget) vs.player.getInventory().removeItem(slot, 1);
            if (vs.pickup) {
               List<ItemStack> ret = new ArrayList<>();
               if (vs.silk) {
                  ItemStack itemstack = new ItemStack(bs.getBlock().asItem());
                  if (!itemstack.isEmpty()) ret.add(itemstack);
               } else {
                  ret = Block.getDrops(bs, world, vs.pos, world.getBlockEntity(vs.pos), vs.player, enchantedTool(world, ItemStack.EMPTY, false, vs.fortune));
               }
               for (ItemStack is : ret) {
                  if (!vs.player.getInventory().add(is)) {
                     world.addFreshEntity(new ItemEntity(world, vs.pos.getX() + 0.5, vs.pos.getY() + 0.5, vs.pos.getZ() + 0.5, is));
                  }
               }
            }
            if (vs.visCost > 0.0F) AuraHandler.drainVis(world, vs.pos, vs.visCost, false);
         }
         if (!vs.target.isEmpty()) {
            if (vs.target.getItem() instanceof BlockItem bi && bi.getBlock() != Blocks.AIR) {
               world.setBlock(vs.pos, bi.getBlock().defaultBlockState(), 3);
            } else {
               world.removeBlock(vs.pos, false);
               ItemEntity entityItem = new ItemEntity(world, vs.pos.getX() + 0.5, vs.pos.getY() + 0.1, vs.pos.getZ() + 0.5, vs.target.copyWithCount(1));
               entityItem.setDeltaMovement(0, 0, 0);
               world.addFreshEntity(entityItem);
            }
         } else {
            world.removeBlock(vs.pos, false);
         }
         if (vs.fx) FXHelper.bamf(world, vs.pos.getX() + 0.5, vs.pos.getY() + 0.5, vs.pos.getZ() + 0.5, vs.color, true, vs.fancy, null);
         if (vs.lifespan > 0) {
            for (int xx = -1; xx <= 1; xx++) {
               for (int yy = -1; yy <= 1; yy++) {
                  for (int zz = -1; zz <= 1; zz++) {
                     BlockPos p = vs.pos.offset(xx, yy, zz);
                     boolean matches = vs.source != null && world.getBlockState(p) == vs.source;
                     if ((xx != 0 || yy != 0 || zz != 0) && matches && FocusUtils.isBlockExposed(world, p)) {
                        next.add(new VirtualSwapper(p, vs.source, vs.target, vs.consumeTarget, vs.lifespan - 1, vs.player, vs.fx, vs.fancy, vs.color,
                           vs.pickup, vs.silk, vs.fortune, vs.allowSwap, vs.visCost));
                     }
                  }
               }
            }
         }
      }
      if (!next.isEmpty()) {
         synchronized (SWAP) {
            SWAP.computeIfAbsent(world, w -> new ArrayList<>()).addAll(next);
         }
      }
   }

   /** InventoryUtils.getPlayerSlotFor: a main inventory slot holding the same item. */
   private static int playerSlotFor(Player player, ItemStack stack) {
      for (int i = 0; i < player.getInventory().items.size(); i++) {
         ItemStack s = player.getInventory().items.get(i);
         if (!s.isEmpty() && ItemStack.isSameItem(s, stack)) return i;
      }
      return -1;
   }

   /** A copy of the tool (or a stick) with silk touch and/or fortune added. */
   private static ItemStack enchantedTool(Level world, ItemStack base, boolean silk, int fortune) {
      ItemStack fake = base.isEmpty() ? new ItemStack(Items.STICK) : base.copy();
      var reg = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
      if (silk) fake.enchant(reg.getOrThrow(Enchantments.SILK_TOUCH), 1);
      if (fortune > 0) {
         var f = reg.getOrThrow(Enchantments.FORTUNE);
         if (EnchantmentHelper.getItemEnchantmentLevel(f, fake) < fortune) fake.enchant(f, fortune);
      }
      return fake;
   }

   /** BlockUtils.harvestBlock: breaks a block as the player would, optionally always dropping and with silk/fortune. */
   public static boolean harvestBlock(ServerLevel world, Player p, BlockPos pos, boolean alwaysDrop, boolean silkOverride, int fortuneOverride,
                                      boolean skipEvent) {
      if (!(p instanceof ServerPlayer player)) return false;
      BlockState state = world.getBlockState(pos);
      if (!skipEvent) {
         BlockEvent.BreakEvent ev = CommonHooks.fireBlockBreak(world, player.gameMode.getGameModeForPlayer(), player, pos, state);
         if (ev.isCanceled()) return false;
      }
      BlockEntity be = world.getBlockEntity(pos);
      Block block = state.getBlock();
      if (block instanceof net.minecraft.world.level.block.GameMasterBlock && !player.canUseGameMasterBlocks()) {
         world.sendBlockUpdated(pos, state, state, 3);
         return false;
      }
      world.levelEvent(null, 2001, pos, Block.getId(state));
      FluidState fluid = world.getFluidState(pos);
      if (player.isCreative()) {
         boolean removed = state.onDestroyedByPlayer(world, pos, player, false, fluid);
         if (removed) block.destroy(world, pos, state);
         return removed;
      }
      ItemStack held = player.getMainHandItem();
      boolean canHarvest = alwaysDrop || state.canHarvestBlock(world, pos, player);
      boolean removed = state.onDestroyedByPlayer(world, pos, player, canHarvest, fluid);
      if (removed) block.destroy(world, pos, state);
      if (removed && canHarvest) {
         ItemStack tool = held.copy();
         if (silkOverride || fortuneOverride > 0) tool = enchantedTool(world, alwaysDrop ? ItemStack.EMPTY : held, silkOverride, fortuneOverride);
         block.playerDestroy(world, player, pos, state, be, tool);
         int exp = state.getExpDrop(world, pos, be, player, tool);
         if (exp > 0) block.popExperience(world, pos, exp);
      }
      return removed;
   }

   static void clear() {
      synchronized (BREAK) {
         BREAK.clear();
      }
      synchronized (SWAP) {
         SWAP.clear();
      }
   }
}
