package thaumcraft.common.events;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.blocks.devices.CondenserBlockEntity;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.FluxRiftEntity;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Milestone 12 game events: the aura's rift trigger opens flux rifts (ServerEvents, once a second), bath salts that
 * dissolve in a water source turn it into purifying fluid (EntityEvents.itemExpire), and the resonator reports a
 * condenser's cost and speed (ItemResonator).
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
}
