package thaumcraft.common.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.monster.eldritch.EldritchHelper;
import thaumcraft.common.network.EldritchPayload;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.world.eldritch.OuterLands;

/**
 * Milestone 14 game events: the warp events (PlayerEvents.handleWarp: every 2000 ticks unless wuss mode or warp ward),
 * no flying in the Outer Lands (PlayerEvents.handleMisc), and the void shield of eldritch mobs flaring and humming
 * when it soaks up a hit (EntityEvents.entityHurt).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class EldritchEvents {
   private EldritchEvents() {
   }

   @SubscribeEvent
   public static void playerTick(PlayerTickEvent.Post event) {
      Player player = event.getEntity();
      if (!(player instanceof ServerPlayer sp)) return;
      if (player.tickCount > 0 && player.tickCount % 2000 == 0 && warpEventsApply(player)) WarpEvents.checkWarpEvent(sp);
      if (OuterLands.isOuterLands(player.level()) && player.tickCount % 20 == 0 && !player.isSpectator() && !player.isCreative()
         && player.getAbilities().flying) {
         player.getAbilities().flying = false;
         sp.onUpdateAbilities();
         player.displayClientMessage(Component.translatable("tc.break.fly").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY), true);
      }
   }

   /** Warp events are off in wuss mode and while the warp ward potion lasts. */
   public static boolean warpEventsApply(Player player) {
      return !ModConfig.wussMode() && !player.hasEffect(ModFlux.WARP_WARD);
   }

   @SubscribeEvent
   public static void entityHurt(LivingIncomingDamageEvent event) {
      LivingEntity mob = event.getEntity();
      if (mob.level().isClientSide || !EldritchHelper.isEldritch(mob) || mob instanceof Player || mob.getAbsorptionAmount() <= 0.0F) return;
      if (mob.level() instanceof ServerLevel sl) {
         ModNetwork.sendNear(sl, mob.getX(), mob.getY(), mob.getZ(), 32.0, new EldritchPayload(EldritchPayload.SHIELD, mob.getId()));
      }
      mob.playSound(ModSounds.RUNICSHIELDECHARGE.get(), 0.66F, 1.1F + mob.level().random.nextFloat() * 0.1F);
   }
}
