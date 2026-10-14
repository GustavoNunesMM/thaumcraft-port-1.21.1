package thaumcraft.common.events;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.devices.CondenserBlockEntity;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.FluxRiftEntity;
import thaumcraft.common.items.casters.foci.FocusUtils;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Milestone 12 game events: the aura's rift trigger opens flux rifts (ServerEvents, once a second), bath salts that
 * dissolve in a water source turn it into purifying fluid (EntityEvents.itemExpire), and the resonator reports a
 * condenser's cost and speed (ItemResonator). Also the server side of the warp potions: death gaze withers whatever the
 * player looks at (PlayerEvents.handleWarp / WarpEvents.checkDeathGaze) and unnatural hunger only lets rotten flesh and
 * brains through (PlayerEvents.finishedUsingItem).
 *
 * <p>Not ported (champions milestone): mobs dying of flux taint becoming tainted champions (EntityUtils.makeTainted).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class FluxEvents {
   private FluxEvents() {
   }

   @SubscribeEvent
   public static void levelTick(LevelTickEvent.Post event) {
      if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 20 != 0) return;
      BlockPos trigger = AuraHandler.RIFT_TRIGGER.remove(level);
      if (trigger != null && !ModConfig.wussMode()) FluxRiftEntity.createRift(level, trigger);
   }

   @SubscribeEvent
   public static void itemExpire(ItemExpireEvent event) {
      ItemEntity item = event.getEntity();
      if (!item.getItem().is(ModFlux.BATH_SALTS.get())) return;
      BlockPos bp = item.blockPosition();
      BlockState bs = item.level().getBlockState(bp);
      if (bs.is(Blocks.WATER) && bs.getFluidState().isSource() && bs.getFluidState().is(Fluids.WATER)) {
         item.level().setBlockAndUpdate(bp, ModFlux.PURIFYING_FLUID_BLOCK.get().defaultBlockState());
      }
   }

   @SubscribeEvent
   public static void resonator(PlayerInteractEvent.RightClickBlock event) {
      if (event.getLevel().isClientSide || !event.getItemStack().is(ModGear.RESONATOR.get())) return;
      if (event.getLevel().getBlockEntity(event.getPos()) instanceof CondenserBlockEntity tc) {
         event.getEntity().sendSystemMessage(Component.translatable("tc.condenser1", "" + tc.cost));
         event.getEntity().sendSystemMessage(Component.translatable("tc.condenser2", "" + tc.interval, "" + tc.interval / 20));
      }
   }

   @SubscribeEvent
   public static void playerTick(PlayerTickEvent.Post event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide && player.tickCount % 20 == 0 && player.hasEffect(ModFlux.DEATH_GAZE)) checkDeathGaze(player);
   }

   /** WarpEvents.checkDeathGaze: everything alive in the player's gaze gets withered and turns on the player. */
   public static void checkDeathGaze(Player player) {
      MobEffectInstance pe = player.getEffect(ModFlux.DEATH_GAZE);
      if (pe == null) return;
      int range = Math.min(8 + pe.getAmplifier() * 3, 24);
      for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(range))) {
         if (entity.isPickable() && entity instanceof LivingEntity le && le.isAlive() && FocusUtils.isVisibleTo(0.75F, player, entity, range)
            && player.hasLineOfSight(entity) && (!(entity instanceof Player) || player.getServer() != null && player.getServer().isPvpAllowed())
            && !le.hasEffect(MobEffects.WITHER)) {
            le.setLastHurtByMob(player);
            le.setLastHurtMob(player);
            if (entity instanceof PathfinderMob mob) mob.setTarget(player);
            le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80));
         }
      }
   }

   /** PlayerEvents.finishedUsingItem: unnatural hunger is eased by rotten flesh and zombie brains, and no other food. */
   @SubscribeEvent
   public static void finishedUsingItem(LivingEntityUseItemEvent.Finish event) {
      LivingEntity entity = event.getEntity();
      if (entity.level().isClientSide || !entity.hasEffect(ModFlux.UNNATURAL_HUNGER)) return;
      ItemStack item = event.getItem();
      if (item.is(Items.ROTTEN_FLESH) || item.is(ModDevices.BRAIN.get())) {
         MobEffectInstance pe = entity.getEffect(ModFlux.UNNATURAL_HUNGER);
         int amp = pe.getAmplifier() - 1;
         int duration = pe.getDuration() - 600;
         entity.removeEffect(ModFlux.UNNATURAL_HUNGER);
         if (duration > 0 && amp >= 0) {
            MobEffectInstance next = new MobEffectInstance(ModFlux.UNNATURAL_HUNGER, duration, amp, true, false);
            next.getCures().clear();
            entity.addEffect(next);
         }
         entity.sendSystemMessage(Component.translatable("warp.text.hunger.2").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
      } else if (item.has(DataComponents.FOOD)) {
         entity.sendSystemMessage(Component.translatable("warp.text.hunger.1").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
      }
   }
}
