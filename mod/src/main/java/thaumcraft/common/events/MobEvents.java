package thaumcraft.common.events;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.construct.OwnedConstructEntity;
import thaumcraft.common.entities.monster.BrainyZombieEntity;
import thaumcraft.common.entities.monster.FireBatEntity;
import thaumcraft.common.entities.monster.PechEntity;
import thaumcraft.common.entities.monster.ThaumicSlimeEntity;
import thaumcraft.common.entities.monster.WispEntity;
import thaumcraft.common.entities.monster.cult.CultistEntity;
import thaumcraft.common.entities.monster.cult.ThaumcraftBossEntity;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.entities.FluxRiftEntity;
import thaumcraft.common.entities.monster.tainted.TaintCrawlerEntity;
import thaumcraft.common.entities.monster.tainted.TaintSeedEntity;
import thaumcraft.common.entities.monster.tainted.TaintSwarmEntity;
import thaumcraft.common.entities.monster.tainted.TaintacleEntity;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModItems;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.research.ScanningManager;

/**
 * EntityEvents.livingDrops: loot bags and bonus experience from champions (bosses drop their bags themselves), brains
 * from zombies and the Crimson Rites from cultists.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class MobEvents {
   private static final ResourceLocation CURIO_RITES = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "curio_rites");

   private MobEvents() {
   }

   private static int looting(LivingDropsEvent event) {
      if (!(event.getSource().getEntity() instanceof LivingEntity killer)) return 0;
      var reg = killer.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
      return EnchantmentHelper.getEnchantmentLevel(reg.getHolderOrThrow(Enchantments.LOOTING), killer);
   }

   private static void drop(LivingDropsEvent event, ItemStack stack) {
      LivingEntity e = event.getEntity();
      event.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY() + e.getEyeHeight(), e.getZ(), stack));
   }

   @SubscribeEvent
   public static void livingDrops(LivingDropsEvent event) {
      LivingEntity dead = event.getEntity();
      if (dead.level().isClientSide) return;
      boolean fakeplayer = event.getSource().getEntity() instanceof FakePlayer;
      if (event.isRecentlyHit() && !fakeplayer && dead instanceof net.minecraft.world.entity.monster.Monster && !(dead instanceof ThaumcraftBossEntity)
         && ChampionModifier.isChampion(dead) && ChampionModifier.getChampionValue(dead) != ChampionModifier.TAINTED) {
         if (dead.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            net.minecraft.world.entity.ExperienceOrb.award(sl, dead.position(), 5 + dead.getRandom().nextInt(3));
         }
         int lb = ChampionEvents.bagRarity(dead, looting(event));
         drop(event, new ItemStack(lb == 0 ? ModMobs.LOOT_BAG_COMMON.get() : lb == 1 ? ModMobs.LOOT_BAG_UNCOMMON.get() : ModMobs.LOOT_BAG_RARE.get()));
      }
      if (dead instanceof Zombie && !(dead instanceof BrainyZombieEntity) && event.isRecentlyHit()
         && dead.getRandom().nextInt(10) - looting(event) < 1) {
         drop(event, new ItemStack(ModDevices.BRAIN.get()));
      }
      if (dead instanceof CultistEntity && !fakeplayer && event.getSource().getEntity() instanceof Player p
         && BuiltInRegistries.ITEM.containsKey(CURIO_RITES)) {
         Item rites = BuiltInRegistries.ITEM.get(CURIO_RITES);
         int c = !ResearchManager.knowsResearch(p, "!CrimsonCultist@2") ? 4 : 20;
         if (p.getInventory().contains(new ItemStack(rites))) c = 50;
         if (dead.getRandom().nextInt(c) == 0) drop(event, new ItemStack(rites));
      }
   }

   /** ConfigResearch: the creatures that can be scanned for research. */
   @EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.MOD)
   public static final class Setup {
      private Setup() {
      }

      @SubscribeEvent
      public static void setup(FMLCommonSetupEvent e) {
         e.enqueueWork(() -> {
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!Wisp", WispEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!ThaumSlime", ThaumicSlimeEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!Firebat", FireBatEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!Pech", PechEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!BrainyZombie", BrainyZombieEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!CrimsonCultist", CultistEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("f_golem", OwnedConstructEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("f_BAT", FireBatEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("f_FLY", FireBatEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("f_FLY", TaintSwarmEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("f_FLY", WispEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!TaintCrawler", TaintCrawlerEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!Taintacle", TaintacleEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!TaintSeed", TaintSeedEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!TaintSwarm", TaintSwarmEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("f_toomuchflux", FluxRiftEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("!FluxRift", FluxRiftEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanEntity.of("f_BRAIN", BrainyZombieEntity.class));
            ScanningManager.addScannableThing(ScanningManager.ScanItem.of("f_BRAIN", ModDevices.BRAIN));
            ScanningManager.addScannableThing(ScanningManager.ScanItem.of("PRIMPEARL", ModMobs.PRIMORDIAL_PEARL));
            ScanningManager.addScannableThing(ScanningManager.ScanItem.of("!Pechwand", ModMobs.PECH_WAND));
            ScanningManager.addScannableThing(ScanningManager.ScanItem.of("f_VOIDSEED", ModItems.VOID_SEED));
         });
      }
   }
}
