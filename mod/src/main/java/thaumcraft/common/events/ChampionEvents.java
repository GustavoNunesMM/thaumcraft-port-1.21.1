package thaumcraft.common.events;

import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.construct.OwnedConstructEntity;
import thaumcraft.common.entities.monster.cult.ThaumcraftBossEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchHelper;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.network.EldritchPayload;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.world.eldritch.OuterLands;

/**
 * The champion parts of EntityEvents: the spawn roll (entitySpawns), the every-tick modifiers (livingTick), the
 * modifiers acting when a champion hits or is hit and the warded shield (entityHurt), and creatures dying of flux
 * taint turning into tainted champions. Their loot bags and bonus experience are in MobEvents.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class ChampionEvents {
   private ChampionEvents() {
   }

   @SubscribeEvent
   public static void livingTick(EntityTickEvent.Post event) {
      if (!(event.getEntity() instanceof PathfinderMob mob) || mob.level().isClientSide || !mob.isAlive()) return;
      int t = ChampionModifier.getChampionValue(mob);
      if (t < 0) return;
      if (t >= ChampionModifier.MODS.length) {
         mob.discard();
         return;
      }
      if (ChampionModifier.MODS[t].type == 0) ChampionModifier.MODS[t].effect.perform(mob, null, null, 0.0F);
   }

   /** The shield flash of a warded champion soaking a hit (eldritch creatures are handled by EldritchEvents). */
   @SubscribeEvent
   public static void incomingDamage(LivingIncomingDamageEvent event) {
      LivingEntity mob = event.getEntity();
      if (mob.level().isClientSide || !(mob instanceof Monster) || EldritchHelper.isEldritch(mob) || mob.getAbsorptionAmount() <= 0.0F) return;
      if (ChampionModifier.getChampionValue(mob) != 5) return;
      if (mob.level() instanceof ServerLevel sl) {
         ModNetwork.sendNear(sl, mob.getX(), mob.getY(), mob.getZ(), 32.0, new EldritchPayload(EldritchPayload.SHIELD, mob.getId()));
      }
      mob.playSound(ModSounds.RUNICSHIELDECHARGE.get(), 0.66F, 1.1F + mob.level().random.nextFloat() * 0.1F);
   }

   @SubscribeEvent
   public static void entityHurt(LivingDamageEvent.Pre event) {
      LivingEntity hurt = event.getEntity();
      if (hurt.level().isClientSide) return;
      // a creature about to die of flux taint may turn instead
      if (hurt.getHealth() < 2.0F && !hurt.getType().is(EntityTypeTags.UNDEAD) && hurt.isAlive() && !(hurt instanceof OwnedConstructEntity)
         && !(hurt instanceof ITaintedMob) && hurt instanceof PathfinderMob && hurt.hasEffect(ModFlux.FLUX_TAINT) && hurt.getRandom().nextBoolean()) {
         ChampionModifier.makeTainted(hurt);
         return;
      }
      if (hurt instanceof Monster mob) {
         ChampionModifier mod = ChampionModifier.get(mob);
         boolean shielded = (mod != null && mod.id == 5 || EldritchHelper.isEldritch(mob)) && mob.getAbsorptionAmount() > 0.0F;
         if (mod != null && !shielded && mod.type == 2 && event.getSource().getEntity() instanceof LivingEntity attacker) {
            event.setNewDamage(mod.effect.perform(mob, attacker, event.getSource(), event.getNewDamage()));
         }
      }
      if (event.getNewDamage() > 0.0F && event.getSource().getEntity() instanceof Monster mob) {
         ChampionModifier mod = ChampionModifier.get(mob);
         if (mod != null && mod.type == 1) event.setNewDamage(mod.effect.perform(mob, hurt, event.getSource(), event.getNewDamage()));
      }
   }

   @SubscribeEvent
   public static void entitySpawns(EntityJoinLevelEvent event) {
      if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity living)) return;
      if (ChampionModifier.getChampionValue(living) == ChampionModifier.TAINTED) ChampionModifier.clearTaintedAI(living);
      if (!(living instanceof Monster mob) || ChampionModifier.getChampionValue(mob) >= -1) return;
      if (shouldBeChampion(mob, event.getLevel())) ChampionModifier.makeChampion(mob, false);
      else ChampionModifier.markNone(mob);
   }

   /** The spawn roll: about 1 in 100 for whitelisted creatures, likelier on hard, in spooky places and the Outer Lands. */
   static boolean shouldBeChampion(Monster mob, Level level) {
      boolean allow = ModConfig.allowChampionMobs();
      int c = level.random.nextInt(100);
      if (level.getDifficulty() == Difficulty.EASY || !allow) c += 2;
      if (level.getDifficulty() == Difficulty.HARD) c -= allow ? 2 : 0;
      if (OuterLands.isOuterLands(level)) c -= 3;
      var biome = level.getBiome(mob.blockPosition());
      if (biome.is(Tags.Biomes.IS_SPOOKY) || biome.is(Tags.Biomes.IS_NETHER) || biome.is(Tags.Biomes.IS_END)) c -= allow ? 2 : 1;
      int cc = 0;
      boolean whitelisted = false;
      for (Map.Entry<Class<? extends Mob>, Integer> e : ChampionModifier.WHITELIST.entrySet()) {
         if (e.getKey().isInstance(mob)) {
            whitelisted = true;
            if (allow || mob instanceof ThaumcraftBossEntity) cc = Math.max(cc, e.getValue() - 1);
         }
      }
      c -= cc;
      return whitelisted && c <= 0 && mob.getAttributeBaseValue(Attributes.MAX_HEALTH) >= 10.0;
   }

   /** The loot bag rarity a slain champion drops (MobEvents). */
   public static int bagRarity(LivingEntity dead, int looting) {
      return Math.min(2, Mth.floor((dead.getRandom().nextInt(9) + looting) / 5.0F));
   }
}
