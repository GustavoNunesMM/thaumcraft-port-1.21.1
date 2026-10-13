package thaumcraft.common.events;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.aspects.AspectManager;
import thaumcraft.common.items.VisCrystalItem;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.network.ScanSourcePayload;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModSounds;

/** ToolEvents: the infusion enchantments at work, and rare earth from ores. */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ToolEvents {
   private static final Map<Integer, Direction> LAST_FACE = new HashMap<>();
   private static boolean destructiveRecursion;
   private static boolean burrowRecursion;

   private ToolEvents() {
   }

   private static boolean has(ItemStack s, EnumInfusionEnchantment e) {
      return EnumInfusionEnchantment.getInfusionEnchantmentLevel(s, e) > 0;
   }

   @SubscribeEvent
   public static void playerAttack(AttackEntityEvent event) {
      Player player = event.getEntity();
      ItemStack held = player.getMainHandItem();
      if (held.isEmpty() || player.level().isClientSide || !has(held, EnumInfusionEnchantment.ARCING) || !event.getTarget().isAlive()) return;
      int rank = EnumInfusionEnchantment.getInfusionEnchantmentLevel(held, EnumInfusionEnchantment.ARCING);
      Entity main = event.getTarget();
      List<Entity> targets = player.level().getEntities(player, main.getBoundingBox().inflate(1.5 + rank, 1.0F + rank / 2.0F, 1.5 + rank));
      int count = 0;
      for (Entity e : targets) {
         if (e == main || !(e instanceof Mob mob) || !mob.isAlive()) continue;
         float f = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
         if (mob.hurt(player.damageSources().playerAttack(player), f * 0.5F)) {
            mob.push(-Mth.sin(player.getYRot() * (float) Math.PI / 180.0F) * 0.5F, 0.1, Mth.cos(player.getYRot() * (float) Math.PI / 180.0F) * 0.5F);
            count++;
         }
         if (count >= rank) break;
      }
      if (count > 0) player.level().playSound(null, player.blockPosition(), ModSounds.WIND.get(), SoundSource.PLAYERS, 1.0F, 0.9F + player.getRandom().nextFloat() * 0.2F);
   }

   @SubscribeEvent
   public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
      Player player = event.getEntity();
      if (player.level().isClientSide || !player.isShiftKeyDown()) return;
      ItemStack held = event.getItemStack();
      int lvl = EnumInfusionEnchantment.getInfusionEnchantmentLevel(held, EnumInfusionEnchantment.SOUNDING);
      if (lvl > 0 && player instanceof ServerPlayer sp) {
         held.hurtAndBreak(5, sp, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
         player.level().playSound(null, event.getPos(), ModSounds.WANDFAIL.get(), SoundSource.BLOCKS, 0.2F, 0.2F + player.getRandom().nextFloat() * 0.2F);
         ModNetwork.sendTo(sp, new ScanSourcePayload(event.getPos(), lvl));
      }
   }

   @SubscribeEvent
   public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
      LAST_FACE.put(event.getEntity().getId(), event.getFace());
   }

   private static boolean isWoodLog(BlockState s) {
      return s.is(BlockTags.LOGS);
   }

   private static boolean isOre(BlockState s) {
      return s.is(Tags.Blocks.ORES);
   }

   @SubscribeEvent
   public static void breakBlock(BlockEvent.BreakEvent event) {
      Player player = event.getPlayer();
      if (burrowRecursion || !(player instanceof ServerPlayer sp) || player.isShiftKeyDown()) return;
      ItemStack held = player.getMainHandItem();
      BlockState state = event.getState();
      if (has(held, EnumInfusionEnchantment.BURROWING) && held.isCorrectToolForDrops(state) && (isWoodLog(state) || isOre(state))) {
         BlockPos furthest = findFurthest((ServerLevel) event.getLevel(), event.getPos(), state, isWoodLog(state) ? 2 : 1);
         if (!furthest.equals(event.getPos())) {
            event.setCanceled(true);
            burrowRecursion = true;
            try {
               sp.gameMode.destroyBlock(furthest);
            } finally {
               burrowRecursion = false;
            }
         }
      }
   }

   /** BlockUtils.findBlocks: the connected block of the same kind furthest from pos. */
   private static BlockPos findFurthest(ServerLevel level, BlockPos pos, BlockState state, int reach) {
      Set<BlockPos> seen = new HashSet<>();
      ArrayDeque<BlockPos> queue = new ArrayDeque<>();
      queue.add(pos);
      seen.add(pos);
      BlockPos best = pos;
      double bestDist = 0;
      while (!queue.isEmpty() && seen.size() < 256) {
         BlockPos p = queue.poll();
         for (BlockPos q : BlockPos.betweenClosed(p.offset(-reach, -reach, -reach), p.offset(reach, reach, reach))) {
            if (seen.contains(q) || level.getBlockState(q).getBlock() != state.getBlock()) continue;
            BlockPos qi = q.immutable();
            seen.add(qi);
            queue.add(qi);
            double d = qi.distSqr(pos);
            if (d > bestDist) {
               bestDist = d;
               best = qi;
            }
         }
      }
      return best;
   }

   private static final Map<net.minecraft.world.item.Item, java.util.function.Supplier<ItemStack>> SPECIAL_MINING = Map.of(
      Items.RAW_IRON, () -> new ItemStack(ModItems.CLUSTER_IRON.get()), Items.RAW_GOLD, () -> new ItemStack(ModItems.CLUSTER_GOLD.get()),
      Items.RAW_COPPER, () -> new ItemStack(ModItems.CLUSTER_COPPER.get()), Items.QUARTZ, () -> new ItemStack(ModItems.CLUSTER_QUARTZ.get()),
      Items.IRON_ORE, () -> new ItemStack(ModItems.CLUSTER_IRON.get()), Items.GOLD_ORE, () -> new ItemStack(ModItems.CLUSTER_GOLD.get()),
      Items.COPPER_ORE, () -> new ItemStack(ModItems.CLUSTER_COPPER.get()));

   /** Utils.findSpecialMiningResult: ore drops may turn into native clusters. */
   private static ItemStack specialMiningResult(ItemStack is, float chance, net.minecraft.util.RandomSource rand) {
      var out = SPECIAL_MINING.get(is.getItem());
      if (is.is(ModBlocks.ORE_CINNABAR.get().asItem()) || is.is(ModBlocks.DEEPSLATE_ORE_CINNABAR.get().asItem())) {
         out = () -> new ItemStack(ModItems.CLUSTER_CINNABAR.get());
      }
      if (out != null && rand.nextFloat() <= chance) {
         ItemStack r = out.get();
         r.setCount(r.getCount() * is.getCount());
         return r;
      }
      return is;
   }

   @SubscribeEvent
   public static void blockDrops(BlockDropsEvent event) {
      ServerLevel level = event.getLevel();
      BlockState state = event.getState();
      boolean silk = event.getTool().getEnchantmentLevel(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
         .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH)) > 0;
      // Rare earth: a small chance from some ores.
      float re = state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE) ? 0.05F
         : state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE) ? 0.075F
         : state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE) || state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)
            || state.is(Blocks.NETHER_QUARTZ_ORE) ? 0.01F
         : state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE) ? 0.001F
         : state.is(ModBlocks.ORE_AMBER.get()) || state.is(ModBlocks.ORE_QUARTZ.get()) || state.is(ModBlocks.DEEPSLATE_ORE_QUARTZ.get()) ? 0.05F : 0.0F;
      if (!silk && re > 0 && level.random.nextFloat() < re) {
         event.getDrops().add(new ItemEntity(level, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5,
            new ItemStack(ModItems.RARE_EARTH.get())));
      }
      if (!(event.getBreaker() instanceof Player player)) return;
      ItemStack held = event.getTool();
      if (held.isEmpty() || !(silk || held.isCorrectToolForDrops(state) || held.getDestroySpeed(state) > 1.0F)) return;
      int refining = EnumInfusionEnchantment.getInfusionEnchantmentLevel(held, EnumInfusionEnchantment.REFINING);
      if (refining > 0) {
         float chance = (1 + refining) * 0.125F;
         boolean b = false;
         for (ItemEntity ie : event.getDrops()) {
            ItemStack smr = specialMiningResult(ie.getItem(), chance, level.random);
            if (smr != ie.getItem()) {
               ie.setItem(smr);
               b = true;
            }
         }
         if (b) level.playSound(null, event.getPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2F, 0.7F + level.random.nextFloat() * 0.2F);
      }
      if (!destructiveRecursion && has(held, EnumInfusionEnchantment.DESTRUCTIVE) && !player.isShiftKeyDown() && player instanceof ServerPlayer sp) {
         destructiveRecursion = true;
         try {
            Direction face = LAST_FACE.getOrDefault(player.getId(), Direction.orderedByNearest(player)[0]);
            for (int aa = -1; aa <= 1; aa++) {
               for (int bb = -1; bb <= 1; bb++) {
                  if (aa == 0 && bb == 0) continue;
                  BlockPos p = switch (face.getAxis()) {
                     case Y -> event.getPos().offset(aa, 0, bb);
                     case Z -> event.getPos().offset(aa, bb, 0);
                     case X -> event.getPos().offset(0, bb, aa);
                  };
                  BlockState bl = level.getBlockState(p);
                  if (bl.getDestroySpeed(level, p) >= 0.0F && !bl.isAir() && (held.isCorrectToolForDrops(bl) || held.getDestroySpeed(bl) > 1.0F)) {
                     sp.gameMode.destroyBlock(p);
                  }
               }
            }
         } finally {
            destructiveRecursion = false;
         }
      }
      if (has(held, EnumInfusionEnchantment.COLLECTOR) && !player.isShiftKeyDown()) {
         for (ItemEntity ie : event.getDrops()) {
            ie.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            ie.setNoPickUpDelay();
         }
      }
      if (has(held, EnumInfusionEnchantment.LAMPLIGHT) && !player.isShiftKeyDown()) {
         BlockPos pos = event.getPos();
         level.getServer().execute(() -> {
            if (level.isEmptyBlock(pos) && !level.getBlockState(pos).is(ModBlocks.EFFECT_GLIMMER.get()) && level.getMaxLocalRawBrightness(pos) < 10) {
               level.setBlock(pos, ModBlocks.EFFECT_GLIMMER.get().defaultBlockState(), 3);
            }
         });
      }
   }

   @SubscribeEvent
   public static void livingDrops(LivingDropsEvent event) {
      if (!(event.getSource().getEntity() instanceof Player player)) return;
      ItemStack held = player.getMainHandItem();
      if (held.isEmpty()) return;
      LivingEntity dead = event.getEntity();
      boolean collector = has(held, EnumInfusionEnchantment.COLLECTOR);
      if (collector) {
         for (ItemEntity ie : event.getDrops()) {
            ie.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            ie.setNoPickUpDelay();
         }
      }
      int q = EnumInfusionEnchantment.getInfusionEnchantmentLevel(held, EnumInfusionEnchantment.ESSENCE);
      if (q <= 0) return;
      AspectList as = AspectManager.SERVER.getEntityAspects(dead);
      if (as == null || as.size() == 0) return;
      AspectList aspects = as.copy();
      Aspect[] al = aspects.getAspects();
      int b = dead.getRandom().nextInt(5) < q ? 0 : 99;
      while (b < q && al.length > 0) {
         Aspect aspect = al[dead.getRandom().nextInt(al.length)];
         if (aspects.getAmount(aspect) > 0) {
            aspects.remove(aspect, 1);
            ItemEntity ie = new ItemEntity(dead.level(), dead.getX(), dead.getY() + dead.getEyeHeight(), dead.getZ(), VisCrystalItem.of(aspect, 1));
            if (collector) {
               ie.setPos(player.getX(), player.getY() + 0.5, player.getZ());
               ie.setNoPickUpDelay();
            }
            event.getDrops().add(ie);
            b++;
         }
         al = aspects.getAspects();
         if (dead.getRandom().nextInt(q) == 0) b += 1 + dead.getRandom().nextInt(2);
      }
   }
}
