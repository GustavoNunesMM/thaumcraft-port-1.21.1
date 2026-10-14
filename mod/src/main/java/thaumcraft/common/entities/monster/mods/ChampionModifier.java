package thaumcraft.common.entities.monster.mods;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.cult.ThaumcraftBossEntity;
import thaumcraft.common.entities.monster.eldritch.EldritchCrabEntity;
import thaumcraft.common.entities.monster.eldritch.InhabitedZombieEntity;
import thaumcraft.common.entities.monster.tainted.TaintCrawlerEntity;
import thaumcraft.common.entities.monster.tainted.TaintacleEntity;
import thaumcraft.common.registers.ModChampions;
import thaumcraft.common.registers.ModFlux;
import thaumcraft.common.registers.ModSounds;
import thaumcraft.common.research.PlayerWarp;
import thaumcraft.common.research.ResearchManager;

/**
 * ChampionModifier and the champion parts of EntityUtils (makeChampion, makeTainted): a champion is a monster with
 * one of 15 modifiers, kept in the synced thaumcraft:champion_mod attribute (-2 = not rolled yet, -1 = rolled, no
 * champion, 0..13 = the modifier). type -1: only stat buffs; 0: acts every tick; 1: acts when it hits; 2: acts when hit.
 */
public final class ChampionModifier {
   public interface Effect {
      /** The champion's effect; returns the (possibly changed) damage. target/source are null for type 0. */
      float perform(LivingEntity mob, @Nullable LivingEntity target, @Nullable DamageSource source, float amount);
   }

   public final int id;
   public final String name;
   public final int type;
   public final Effect effect;
   public final ResourceLocation modifierId;

   private ChampionModifier(int id, String name, int type, Effect effect) {
      this.id = id;
      this.name = name;
      this.type = type;
      this.effect = effect;
      this.modifierId = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "champion_" + name);
   }

   public Component localizedName() {
      return Component.translatable("champion.mod." + name);
   }

   public static final int TAINTED = 13;

   public static final ChampionModifier[] MODS = {
      new ChampionModifier(0, "bold", -1, (m, t, s, a) -> 0.0F),
      new ChampionModifier(1, "spine", 2, ChampionModifier::spined),
      new ChampionModifier(2, "armor", 2, (m, t, s, a) -> s != null && !s.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR) ? a * 19.0F / 25.0F : a),
      new ChampionModifier(3, "mighty", -1, (m, t, s, a) -> 0.0F),
      new ChampionModifier(4, "grim", 1, (m, t, s, a) -> {
         if (m.getRandom().nextFloat() < 0.4F) t.addEffect(new MobEffectInstance(MobEffects.WITHER, 200));
         return a;
      }),
      new ChampionModifier(5, "warded", 0, ChampionModifier::warded),
      new ChampionModifier(6, "warp", 1, (m, t, s, a) -> {
         if (m.getRandom().nextFloat() < 0.33F && t instanceof Player p) ResearchManager.addWarp(p, 1 + m.getRandom().nextInt(3), PlayerWarp.Type.TEMPORARY);
         return a;
      }),
      new ChampionModifier(7, "undying", 0, (m, t, s, a) -> {
         if (m.tickCount % 20 == 0) m.heal(1.0F);
         return a;
      }),
      new ChampionModifier(8, "fiery", 1, (m, t, s, a) -> {
         if (m.getRandom().nextFloat() < 0.4F) t.igniteForSeconds(4);
         return a;
      }),
      new ChampionModifier(9, "sickly", 1, (m, t, s, a) -> {
         if (m.getRandom().nextFloat() < 0.4F) t.addEffect(new MobEffectInstance(MobEffects.HUNGER, 500));
         return a;
      }),
      new ChampionModifier(10, "venomous", 1, (m, t, s, a) -> {
         if (m.getRandom().nextFloat() < 0.4F) t.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
         return a;
      }),
      new ChampionModifier(11, "vampiric", 1, (m, t, s, a) -> {
         m.heal(Math.max(2.0F, a / 2.0F));
         return a;
      }),
      new ChampionModifier(12, "infested", 2, ChampionModifier::infested),
      new ChampionModifier(TAINTED, "tainted", 0, (m, t, s, a) -> {
         if (m instanceof PathfinderMob pm) resetAI(pm);
         return a;
      })
   };

   // ------------------------------------------------------------------ attribute modifiers (EntityUtils)

   private static ResourceLocation tc(String path) {
      return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
   }

   /** ChampionModifier.ATTRIBUTE_MOD_NONE / ATTRIBUTE_MINUS_ONE */
   public static final AttributeModifier MOD_NONE = new AttributeModifier(tc("champion_none"), 1.0, AttributeModifier.Operation.ADD_VALUE);
   public static final AttributeModifier MINUS_ONE = new AttributeModifier(tc("champion_minus_one"), -1.0, AttributeModifier.Operation.ADD_VALUE);
   public static final AttributeModifier CHAMPION_HEALTH = new AttributeModifier(tc("champion_health"), 100.0, AttributeModifier.Operation.ADD_VALUE);
   public static final AttributeModifier CHAMPION_DAMAGE = new AttributeModifier(tc("champion_damage"), 2.0,
      AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
   public static final AttributeModifier BOLD_BUFF = new AttributeModifier(tc("champion_bold_speed"), 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
   public static final AttributeModifier MIGHTY_BUFF = new AttributeModifier(tc("champion_mighty_damage"), 2.0,
      AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
   /** EntityUtils.HPBUFF[5] and DMGBUFF[0], given to the tainted. */
   public static final AttributeModifier TAINTED_HEALTH = new AttributeModifier(tc("tainted_health"), 25.0, AttributeModifier.Operation.ADD_VALUE);
   public static final AttributeModifier TAINTED_DAMAGE = new AttributeModifier(tc("tainted_damage"), 0.5, AttributeModifier.Operation.ADD_VALUE);
   /** The warded shield needs room for absorption (1.21 caps it by max_absorption). */
   private static final ResourceLocation WARDED_ABSORPTION = tc("champion_warded_absorption");
   /** ChampionModTainted's "istainted" flag: the critter's AI has been replaced. */
   private static final ResourceLocation IS_TAINTED = tc("is_tainted");

   private static void set(@Nullable AttributeInstance ai, AttributeModifier mod) {
      if (ai == null) return;
      ai.removeModifier(mod.id());
      ai.addPermanentModifier(mod);
   }

   @Nullable
   private static AttributeInstance attr(LivingEntity e, Holder<Attribute> a) {
      return e.getAttributes().hasAttribute(a) ? e.getAttribute(a) : null;
   }

   /** The champion_mod value (-2 if the entity has no such attribute). */
   public static int getChampionValue(LivingEntity e) {
      AttributeInstance ai = attr(e, ModChampions.CHAMPION_MOD);
      return ai == null ? -2 : (int) ai.getValue();
   }

   /** The entity's modifier, or null if it isn't a champion. */
   @Nullable
   public static ChampionModifier get(LivingEntity e) {
      int t = getChampionValue(e);
      return t >= 0 && t < MODS.length ? MODS[t] : null;
   }

   public static boolean isChampion(LivingEntity e) {
      return get(e) != null;
   }

   /** Marks a monster as rolled and not a champion (-1). */
   public static void markNone(LivingEntity e) {
      set(attr(e, ModChampions.CHAMPION_MOD), MOD_NONE);
   }

   /** EntityUtils.makeChampion: a random modifier (creepers are always bold). */
   public static void makeChampion(Monster entity, boolean persist) {
      makeChampion(entity, persist, entity instanceof Creeper ? 0 : entity.getRandom().nextInt(MODS.length));
   }

   public static void makeChampion(Monster entity, boolean persist, int type) {
      AttributeInstance modai = attr(entity, ModChampions.CHAMPION_MOD);
      if (modai == null || modai.getValue() > -2.0) return;
      set(modai, new AttributeModifier(MODS[type].modifierId, type + 2, AttributeModifier.Operation.ADD_VALUE));
      if (!(entity instanceof ThaumcraftBossEntity boss)) {
         set(attr(entity, Attributes.MAX_HEALTH), CHAMPION_HEALTH);
         set(attr(entity, Attributes.ATTACK_DAMAGE), CHAMPION_DAMAGE);
         entity.heal(25.0F);
         entity.setCustomName(MODS[type].localizedName().copy().append(" ").append(entity.getName()));
      } else {
         boss.generateName();
      }
      if (persist) entity.setPersistenceRequired();
      switch (type) {
         case 0 -> set(attr(entity, Attributes.MOVEMENT_SPEED), BOLD_BUFF);
         case 3 -> set(attr(entity, Attributes.ATTACK_DAMAGE), MIGHTY_BUFF);
         case 5 -> {
            int bh = (int) entity.getAttributeBaseValue(Attributes.MAX_HEALTH) / 2;
            set(attr(entity, Attributes.MAX_ABSORPTION), new AttributeModifier(WARDED_ABSORPTION, bh, AttributeModifier.Operation.ADD_VALUE));
            entity.setAbsorptionAmount(entity.getAbsorptionAmount() + bh);
         }
         default -> {
         }
      }
   }

   /** EntityUtils.makeTainted: a creature dying of flux taint becomes a tainted champion instead. */
   public static void makeTainted(LivingEntity target) {
      AttributeInstance modai = attr(target, ModChampions.CHAMPION_MOD);
      if (modai == null || modai.getValue() > -1.0) return;
      if (modai.getValue() == -1.0) set(modai, MINUS_ONE);
      set(modai, new AttributeModifier(MODS[TAINTED].modifierId, TAINTED + 2, AttributeModifier.Operation.ADD_VALUE));
      if (!(target instanceof ThaumcraftBossEntity boss)) {
         set(attr(target, Attributes.MAX_HEALTH), TAINTED_HEALTH);
         AttributeInstance dmg = attr(target, Attributes.ATTACK_DAMAGE);
         if (dmg != null && dmg.getModifiers().isEmpty() && dmg.getBaseValue() <= 0.0) {
            dmg.setBaseValue(Math.max(2.0F, (target.getBbHeight() + target.getBbWidth()) * 2.0F));
         } else {
            set(dmg, TAINTED_DAMAGE);
         }
         target.heal(25.0F);
      } else {
         boss.generateName();
      }
   }

   /** ChampionModTainted.resetAI: a tainted critter forgets its peaceful ways and hunts players. */
   public static void resetAI(PathfinderMob critter) {
      AttributeInstance modai = attr(critter, ModChampions.TAINTED_MOD);
      if (!(critter instanceof Monster) && modai != null && modai.getValue() == 0.0) {
         critter.goalSelector.removeAllGoals(g -> true);
         critter.targetSelector.removeAllGoals(g -> true);
         critter.goalSelector.addGoal(0, new FloatGoal(critter));
         critter.goalSelector.addGoal(2, new MeleeAttackGoal(critter, 1.2, false));
         critter.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(critter, 1.0));
         critter.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(critter, 1.0));
         critter.goalSelector.addGoal(8, new LookAtPlayerGoal(critter, Player.class, 8.0F));
         critter.goalSelector.addGoal(8, new RandomLookAroundGoal(critter));
         critter.targetSelector.addGoal(1, new HurtByTargetGoal(critter));
         critter.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(critter, Player.class, true));
         set(modai, new AttributeModifier(IS_TAINTED, 1.0, AttributeModifier.Operation.ADD_VALUE));
      }
      AttributeInstance dmg = attr(critter, Attributes.ATTACK_DAMAGE);
      if (dmg != null && dmg.getBaseValue() <= 0.0) dmg.setBaseValue(Math.max(2.0F, (critter.getBbHeight() + critter.getBbWidth()) * 2.0F));
   }

   /** On (re)joining the world the tainted AI must be set up again (EntityEvents.entitySpawns). */
   public static void clearTaintedAI(LivingEntity e) {
      AttributeInstance modai = attr(e, ModChampions.TAINTED_MOD);
      if (modai != null) modai.removeModifier(IS_TAINTED);
   }

   // ------------------------------------------------------------------ effects

   private static float spined(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
      if (target != null && (source == null || !source.is(net.minecraft.world.damagesource.DamageTypes.THORNS))) {
         target.hurt(boss.damageSources().thorns(boss), 1 + boss.getRandom().nextInt(3));
         target.playSound(SoundEvents.THORNS_HIT, 0.5F, 1.0F);
      }
      return amount;
   }

   private static float warded(LivingEntity mob, LivingEntity target, DamageSource source, float amount) {
      if (mob.invulnerableTime <= 0 && mob.tickCount % 25 == 0) {
         int bh = (int) mob.getAttributeBaseValue(Attributes.MAX_HEALTH) / 2;
         if (mob.getAbsorptionAmount() < bh) mob.setAbsorptionAmount(mob.getAbsorptionAmount() + 1.0F);
      }
      return amount;
   }

   private static float infested(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
      if (boss.getRandom().nextFloat() < 0.4F && !boss.level().isClientSide) {
         TaintCrawlerEntity spiderling = new TaintCrawlerEntity(ModFlux.TAINT_CRAWLER.get(), boss.level());
         spiderling.moveTo(boss.getX(), boss.getY() + boss.getBbHeight() / 2.0F, boss.getZ(), boss.getRandom().nextFloat() * 360.0F, 0.0F);
         boss.level().addFreshEntity(spiderling);
         boss.playSound(ModSounds.GORE.get(), 0.5F, 1.0F);
      }
      return amount;
   }

   // ------------------------------------------------------------------ the whitelist (ConfigEntities championWhiteList)

   /** Creature classes that may spawn as champions, with their bonus level (0 = normal odds, higher = likelier). */
   public static final Map<Class<? extends Mob>, Integer> WHITELIST = new LinkedHashMap<>();

   static {
      WHITELIST.put(Zombie.class, 0);
      WHITELIST.put(Spider.class, 0);
      WHITELIST.put(Blaze.class, 0);
      WHITELIST.put(EnderMan.class, 0);
      WHITELIST.put(Skeleton.class, 0);
      WHITELIST.put(Witch.class, 1);
      WHITELIST.put(EldritchCrabEntity.class, 0);
      WHITELIST.put(TaintacleEntity.class, 2);
      WHITELIST.put(InhabitedZombieEntity.class, 3);
   }
}
