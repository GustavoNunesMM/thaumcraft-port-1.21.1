package thaumcraft.common.events;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.entities.monster.MindSpiderEntity;
import thaumcraft.common.entities.monster.cult.CultistPortalLesserEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchGuardianEntity;
import thaumcraft.common.items.armor.FortressArmorItem;
import thaumcraft.common.network.EldritchPayload;
import thaumcraft.common.registers.ModEldritch;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;

/**
 * WarpEvents: every 2000 ticks (PlayerEvents.handleWarp, unless wuss mode or warp ward) a warped player may suffer an
 * event. The chance grows with the warp counter (which is set to the total warp whenever warp is gained); the event
 * strength is a roll over the player's warp, worn warping gear added on top. A fortress helm with the grinning devil
 * mask softens it. From mild (a heartbeat, distant explosions, strange messages) to harsh (vis exhaustion, hunger,
 * blindness, death gaze, hallucinated or real mind spiders, a crimson portal, eldritch guardians in the mist). Enough
 * warp also unlocks the bath salts hint and the eldritch minor/major research.
 *
 * <p>The ModConfig "nostress" graphics option (silences the heartbeat and explosions) isn't ported.
 */
public final class WarpEvents {
   private WarpEvents() {
   }

   private static void say(Player player, String key) {
      player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
   }

   private static MobEffectInstance uncurable(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amp,
      boolean ambient) {
      MobEffectInstance pe = new MobEffectInstance(effect, duration, amp, ambient, true);
      pe.getCures().clear();
      return pe;
   }

   public static void checkWarpEvent(ServerPlayer player) {
      PlayerWarp wc = ResearchManager.warp(player);
      ResearchManager.addWarp(player, -1, PlayerWarp.Type.TEMPORARY);
      int tw = wc.get(PlayerWarp.Type.TEMPORARY);
      int nw = wc.get(PlayerWarp.Type.NORMAL);
      int pw = wc.get(PlayerWarp.Type.PERMANENT);
      int warp = tw + nw + pw;
      int actualwarp = pw + nw;
      int gearWarp = GearEvents.getWarpFromGear(player);
      warp += gearWarp;
      int warpCounter = wc.getCounter();
      RandomSource rand = player.level().random;
      int r = rand.nextInt(100);
      if (warpCounter > 0 && warp > 0 && r <= Math.sqrt(warpCounter)) {
         warp = Math.min(100, (warp + warp + warpCounter) / 3);
         warpCounter = (int) (warpCounter - Math.max(5.0, Math.sqrt(warpCounter) * 2.0 - gearWarp * 2));
         wc.setCounter(warpCounter);
         int eff = rand.nextInt(warp) + gearWarp;
         ItemStack helm = player.getItemBySlot(EquipmentSlot.HEAD);
         if (helm.getItem() instanceof FortressArmorItem && FortressArmorItem.getMask(helm) == 0) eff -= 2 + rand.nextInt(4);
         EldritchPayload.send(player, EldritchPayload.HEARTBEAT);
         if (eff > 0) applyEffect(player, eff, warp, nw);
         unlocks(player, actualwarp);
      }
   }

   /** The research warp unlocks: the bath salts hint (over 10), ELDRITCHMINOR (over 25) and ELDRITCHMAJOR (over 50). */
   public static void unlocks(ServerPlayer player, int actualwarp) {
      if (actualwarp > 10 && !ResearchManager.knowsResearch(player, "BATHSALTS") && !ResearchManager.knowsResearch(player, "!BATHSALTS")) {
         say(player, "warp.text.8");
         ResearchManager.completeResearch(player, "!BATHSALTS");
      }
      if (actualwarp > 25 && !ResearchManager.knowsResearch(player, "ELDRITCHMINOR")) ResearchManager.completeResearch(player, "ELDRITCHMINOR");
      if (actualwarp > 50 && !ResearchManager.knowsResearch(player, "ELDRITCHMAJOR")) ResearchManager.completeResearch(player, "ELDRITCHMAJOR");
   }

   /** One warp event of strength {@code eff} (1.12 thresholds, including the odd "== 76"). */
   public static void applyEffect(ServerPlayer player, int eff, int warp, int nw) {
      RandomSource rand = player.level().random;
      int amp = Math.min(3, warp / 15);
      if (eff <= 4) {
         player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.CREEPER_PRIMED), SoundSource.AMBIENT,
            player.getX(), player.getY(), player.getZ(), 1.0F, 0.5F, rand.nextLong()));
      } else if (eff <= 8) {
         player.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT,
            player.getX() + (rand.nextFloat() - rand.nextFloat()) * 10.0F, player.getY() + (rand.nextFloat() - rand.nextFloat()) * 10.0F,
            player.getZ() + (rand.nextFloat() - rand.nextFloat()) * 10.0F, 4.0F, (1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F,
            rand.nextLong()));
      } else if (eff <= 12) {
         say(player, "warp.text.11");
      } else if (eff <= 16) {
         player.addEffect(uncurable(ModFlux.VIS_EXHAUST, 5000, amp, true));
         say(player, "warp.text.1");
      } else if (eff <= 20) {
         player.addEffect(uncurable(ModFlux.THAUMARHIA, Math.min(32000, 10 * warp), 0, true));
         say(player, "warp.text.15");
      } else if (eff <= 24) {
         player.addEffect(uncurable(ModFlux.UNNATURAL_HUNGER, 5000, amp, true));
         say(player, "warp.text.2");
      } else if (eff <= 28) {
         say(player, "warp.text.12");
      } else if (eff <= 32) {
         spawnMist(player, warp, 1);
      } else if (eff <= 36) {
         player.addEffect(new MobEffectInstance(ModFlux.BLURRED_VISION, Math.min(32000, 10 * warp), 0, true, true));
      } else if (eff <= 40) {
         player.addEffect(uncurable(ModFlux.SUN_SCORNED, 5000, amp, true));
         say(player, "warp.text.5");
      } else if (eff <= 44) {
         player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, amp, true, true));
         say(player, "warp.text.9");
      } else if (eff <= 48) {
         player.addEffect(uncurable(ModFlux.INFECTIOUS_VIS_EXHAUST, 6000, amp, false));
         say(player, "warp.text.1");
      } else if (eff <= 52) {
         player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Math.min(40 * warp, 6000), 0, true, true));
         say(player, "warp.text.10");
      } else if (eff <= 56) {
         player.addEffect(uncurable(ModFlux.DEATH_GAZE, 6000, amp, true));
         say(player, "warp.text.4");
      } else if (eff <= 60) {
         suddenlySpiders(player, warp, false);
      } else if (eff <= 64) {
         say(player, "warp.text.13");
      } else if (eff <= 68) {
         spawnMist(player, warp, warp / 30);
      } else if (eff <= 72) {
         player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(32000, 5 * warp), 0, true, true));
      } else if (eff == 76) {
         if (nw > 0) ResearchManager.addWarp(player, -1, PlayerWarp.Type.NORMAL);
         say(player, "warp.text.14");
      } else if (eff <= 80) {
         player.addEffect(uncurable(ModFlux.UNNATURAL_HUNGER, 6000, amp, true));
         say(player, "warp.text.2");
      } else if (eff <= 88) {
         spawnPortal(player);
      } else if (eff <= 92) {
         suddenlySpiders(player, warp, true);
      } else {
         spawnMist(player, warp, warp / 15);
      }
   }

   private static int offset(RandomSource r) {
      return Mth.nextInt(r, 7, 24) * Mth.nextInt(r, -1, 1);
   }

   static void spawnMist(ServerPlayer player, int warp, int guardian) {
      EldritchPayload.send(player, EldritchPayload.MIST);
      for (int a = 0; a < Math.min(8, guardian); a++) spawnGuardian(player);
      say(player, "warp.text.6");
   }

   private static void spawnPortal(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      CultistPortalLesserEntity eg = ModMobs.CULTIST_PORTAL_LESSER.get().create(level);
      if (eg == null) return;
      BlockPos at = player.blockPosition();
      for (int l = 0; l < 50; l++) {
         BlockPos p = at.offset(offset(level.random), offset(level.random), offset(level.random));
         eg.setPos(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5);
         if (level.getBlockState(p.below()).isSolidRender(level, p.below()) && level.noCollision(eg) && !level.containsAnyLiquid(eg.getBoundingBox())) {
            eg.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.EVENT, null);
            level.addFreshEntity(eg);
            say(player, "warp.text.16");
            break;
         }
      }
   }

   private static void spawnGuardian(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      EldritchGuardianEntity eg = ModEldritch.ELDRITCH_GUARDIAN.get().create(level);
      if (eg == null) return;
      BlockPos at = player.blockPosition();
      for (int l = 0; l < 50; l++) {
         BlockPos p = at.offset(offset(level.random), offset(level.random), offset(level.random));
         if (level.getBlockState(p.below()).isCollisionShapeFullBlock(level, p.below())) {
            eg.setPos(p.getX(), p.getY(), p.getZ());
            if (level.noCollision(eg) && !level.containsAnyLiquid(eg.getBoundingBox())) {
               eg.setTarget(player);
               level.addFreshEntity(eg);
               break;
            }
         }
      }
   }

   /** Mind spiders: harmless phantoms only their viewer sees, or (real) a true swarm. */
   static void suddenlySpiders(ServerPlayer player, int warp, boolean real) {
      ServerLevel level = player.serverLevel();
      BlockPos at = player.blockPosition();
      for (int a = 0; a < Math.min(50, warp); a++) {
         MindSpiderEntity spider = ModMobs.MIND_SPIDER.get().create(level);
         if (spider == null) continue;
         boolean success = false;
         for (int l = 0; l < 50; l++) {
            BlockPos p = at.offset(offset(level.random), offset(level.random), offset(level.random));
            if (level.getBlockState(p.below()).isCollisionShapeFullBlock(level, p.below())) {
               spider.setPos(p.getX(), p.getY(), p.getZ());
               if (level.noCollision(spider) && !level.containsAnyLiquid(spider.getBoundingBox())) {
                  success = true;
                  break;
               }
            }
         }
         if (success) {
            spider.setTarget(player);
            if (!real) {
               spider.setViewer(player.getName().getString());
               spider.setHarmless(true);
            }
            level.addFreshEntity(spider);
         }
      }
      say(player, "warp.text.7");
   }
}
