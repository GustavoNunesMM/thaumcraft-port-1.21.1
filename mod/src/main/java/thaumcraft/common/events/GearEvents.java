package thaumcraft.common.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.api.research.EnumKnowledgeType;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.items.armor.FortressArmorItem;
import thaumcraft.common.items.armor.TravellerBootsItem;
import thaumcraft.common.items.armor.VoidRobeArmorItem;
import thaumcraft.common.lib.CuriosHelper;
import thaumcraft.common.registers.ModDataComponents;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.research.ResearchManager;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * The gear parts of PlayerEvents (runic shielding, traveller boots, cloud ring, curiosity band, charm of undying, fall
 * damage) plus what ISpecialArmor did for the fortress armor and void robes, and WarpEvents.getWarpFromGear.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public final class GearEvents {
   /** ModConfig.CONFIG_MISC shield settings (milliseconds; game time * 50 here) and vis cost. */
   public static final int SHIELD_RECHARGE = 2000;
   public static final int SHIELD_WAIT = 4000;
   public static final int SHIELD_COST = 1;

   static final Map<Integer, Long> NEXT_CYCLE = new HashMap<>();
   static final Map<Integer, Integer> LAST_CHARGE = new HashMap<>();
   static final Map<Integer, Integer> LAST_MAX_CHARGE = new HashMap<>();
   static final Map<Integer, Integer> RUNIC_INFO = new HashMap<>();

   static final ResourceLocation RUNIC_ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "runic_shield");
   static final ResourceLocation STEP_ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "traveller_step");
   static final ResourceLocation FORTRESS_ID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "fortress_set");

   private GearEvents() {
   }

   // ------------------------------------------------------------------ helpers (PlayerEvents / WarpEvents statics)

   public static int getRunicCharge(ItemStack stack) {
      return stack.isEmpty() ? 0 : stack.getOrDefault(ModGear.RUNIC.get(), 0);
   }

   /** IWarpingGear warp plus the warp infused into the stack (thaumcraft:warp, "TC.WARP"). */
   public static int getFinalWarp(ItemStack stack, Player player) {
      if (stack == null || stack.isEmpty()) return 0;
      int warp = 0;
      if (stack.getItem() instanceof IWarpingGear g) warp += g.getWarp(stack, player);
      warp += stack.getOrDefault(ModDataComponents.WARP.get(), 0);
      return warp;
   }

   public static int getFinalDiscount(ItemStack stack, Player player) {
      if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IVisDiscountGear gear)) return 0;
      return gear.getVisDiscount(stack, player);
   }

   /** WarpEvents.getWarpFromGear: main hand, armor and curios. Added to the player's warp when warp events roll. */
   public static int getWarpFromGear(Player player) {
      int w = getFinalWarp(player.getMainHandItem(), player);
      for (ItemStack s : player.getInventory().armor) w += getFinalWarp(s, player);
      for (ItemStack s : CuriosHelper.getCurios(player)) w += getFinalWarp(s, player);
      return w;
   }

   /** EntityUtils.hasGoggles: IGoggles in the main hand, the armor or the curios that shows popups. */
   public static boolean hasGoggles(LivingEntity e) {
      if (!(e instanceof Player viewer)) return false;
      ItemStack held = viewer.getMainHandItem();
      if (held.getItem() instanceof IGoggles g && g.showIngamePopups(held, viewer)) return true;
      for (ItemStack s : viewer.getInventory().armor) {
         if (s.getItem() instanceof IGoggles g && g.showIngamePopups(s, viewer)) return true;
      }
      for (ItemStack s : CuriosHelper.getCurios(viewer)) {
         if (s.getItem() instanceof IGoggles g && g.showIngamePopups(s, viewer)) return true;
      }
      return false;
   }

   /** EntityUtils.hasRevealer: an IRevealer in either hand, the armor or the curios. */
   public static boolean hasRevealer(LivingEntity e) {
      if (!(e instanceof Player viewer)) return false;
      for (ItemStack s : new ItemStack[]{viewer.getMainHandItem(), viewer.getOffhandItem()}) {
         if (s.getItem() instanceof IRevealer r && r.showNodes(s, viewer)) return true;
      }
      for (ItemStack s : viewer.getInventory().armor) {
         if (s.getItem() instanceof IRevealer r && r.showNodes(s, viewer)) return true;
      }
      for (ItemStack s : CuriosHelper.getCurios(viewer)) {
         if (s.getItem() instanceof IRevealer r && r.showNodes(s, viewer)) return true;
      }
      return false;
   }

   /** Runic shielding of the armor and curios worn. */
   public static int getTotalRunicCharge(Player player) {
      int max = 0;
      for (ItemStack s : player.getInventory().armor) max += getRunicCharge(s);
      for (ItemStack s : CuriosHelper.getCurios(player)) max += getRunicCharge(s);
      return max;
   }

   private static void setModifier(LivingEntity e, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr,
                                   ResourceLocation id, double amount) {
      AttributeInstance inst = e.getAttribute(attr);
      if (inst == null) return;
      AttributeModifier cur = inst.getModifier(id);
      if (amount == 0) {
         if (cur != null) inst.removeModifier(id);
      } else if (cur == null || cur.amount() != amount) {
         inst.addOrUpdateTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
      }
   }

   // ------------------------------------------------------------------ ticking

   @SubscribeEvent
   public static void playerTick(PlayerTickEvent.Post event) {
      Player player = event.getEntity();
      handleSpeedMods(player);
      handleFortressSet(player);
      if (!player.level().isClientSide) handleRunicArmor(player);
   }

   /**
    * handleRunicArmor: every second the shield size is summed from the armor and curios; while below it, one point of
    * absorption comes back per recharge interval for SHIELD_COST vis from the aura, after a pause once it broke. In 1.21
    * absorption is capped by the max_absorption attribute, so the shield size is added to it.
    */
   public static void handleRunicArmor(Player player) {
      int id = player.getId();
      if (player.tickCount % 20 == 0) {
         int max = getTotalRunicCharge(player);
         setModifier(player, Attributes.MAX_ABSORPTION, RUNIC_ID, max);
         if (LAST_MAX_CHARGE.containsKey(id)) {
            int charge = LAST_MAX_CHARGE.get(id);
            if (charge > max) player.setAbsorptionAmount(player.getAbsorptionAmount() - (charge - max));
            if (max <= 0) LAST_MAX_CHARGE.remove(id);
         }
         if (max > 0) {
            RUNIC_INFO.put(id, max);
            LAST_MAX_CHARGE.put(id, max);
         } else {
            RUNIC_INFO.remove(id);
         }
      }
      if (RUNIC_INFO.containsKey(id)) {
         NEXT_CYCLE.putIfAbsent(id, 0L);
         long time = player.level().getGameTime() * 50L;
         int charge = (int) player.getAbsorptionAmount();
         if (charge == 0 && LAST_CHARGE.getOrDefault(id, 0) > 0) {
            NEXT_CYCLE.put(id, time + SHIELD_WAIT);
            LAST_CHARGE.put(id, 0);
         }
         BlockPos pos = player.blockPosition();
         if (charge < RUNIC_INFO.get(id) && NEXT_CYCLE.get(id) < time && !AuraHandler.shouldPreserveAura(player.level(), player, pos)
            && AuraHandler.getVis(player.level(), pos) >= SHIELD_COST) {
            AuraHandler.drainVis(player.level(), pos, SHIELD_COST, false);
            NEXT_CYCLE.put(id, time + SHIELD_RECHARGE);
            player.setAbsorptionAmount(charge + 1);
            LAST_CHARGE.put(id, charge + 1);
         }
      }
   }

   /** Traveller boots step height (prevStep/stepHeight = 1 in 1.12), both sides so the server agrees with the client. */
   static void handleSpeedMods(Player player) {
      ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
      boolean active = TravellerBootsItem.isActive(boots) && !player.isShiftKeyDown() && !player.getAbilities().flying;
      setModifier(player, Attributes.STEP_HEIGHT, STEP_ID, active ? 0.4 : 0.0);
   }

   /**
    * ItemFortressArmor.getProperties: each fortress piece worn adds one armor and one toughness, plus one armor per masked
    * piece; applied as attribute modifiers on the wearer.
    */
   static void handleFortressSet(Player player) {
      int set = FortressArmorItem.countSet(player);
      int masks = 0;
      for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
         ItemStack piece = player.getItemBySlot(s);
         if (piece.getItem() instanceof FortressArmorItem && FortressArmorItem.getMask(piece) >= 0) masks++;
      }
      setModifier(player, Attributes.ARMOR, FORTRESS_ID, set * (1 + masks));
      setModifier(player, Attributes.ARMOR_TOUGHNESS, FORTRESS_ID, set);
   }

   // ------------------------------------------------------------------ damage

   /**
    * onFallDamage (traveller boots, cloud ring) and the ISpecialArmor ratios of the fortress armor (magic /35, fire and
    * explosions /20) and void robes (magic /35), taken off before vanilla armor.
    */
   @SubscribeEvent
   public static void incomingDamage(LivingIncomingDamageEvent event) {
      LivingEntity entity = event.getEntity();
      DamageSource source = event.getSource();
      if (source.is(DamageTypeTags.IS_FALL) && entity instanceof Player player) {
         if (player.getItemBySlot(EquipmentSlot.FEET).is(ModGear.TRAVELLER_BOOTS.get())) {
            float f = Math.max(0.0F, event.getAmount() / 2.0F - 1.0F);
            if (f < 1.0F) {
               event.setCanceled(true);
               return;
            }
            event.setAmount(f);
         }
         if (CuriosHelper.isEquipped(player, ModGear.CLOUD_RING.get())) {
            float f = Math.max(0.0F, event.getAmount() / 3.0F - 2.0F);
            if (f < 1.0F) {
               event.setCanceled(true);
               return;
            }
            if (f < event.getAmount()) event.setAmount(f);
         }
      }
      boolean magic = source.is(Tags.DamageTypes.IS_MAGIC);
      boolean fireOrBoom = source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION);
      if (!magic && !fireOrBoom) return;
      double ratio = 0.0;
      for (ItemStack armor : entity.getArmorSlots()) {
         if (!(armor.getItem() instanceof ArmorItem ai)) continue;
         int def = ai.getDefense();
         if (armor.getItem() instanceof FortressArmorItem) ratio += magic ? def / 35.0 : def / 20.0;
         else if (armor.getItem() instanceof VoidRobeArmorItem && magic) ratio += def / 35.0;
      }
      if (ratio > 0) event.setAmount((float) (event.getAmount() * Math.max(0.0, 1.0 - Math.min(1.0, ratio))));
   }

   @SubscribeEvent
   public static void playerJumps(LivingEvent.LivingJumpEvent event) {
      if (event.getEntity() instanceof Player player && TravellerBootsItem.isActive(player.getItemBySlot(EquipmentSlot.FEET))) {
         player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.275F, 0.0));
      }
   }

   /** Curiosity band: half of every orb's experience is held back; now and then it turns into knowledge instead. */
   @SubscribeEvent
   public static void pickupXP(PlayerXpEvent.PickupXp event) {
      Player player = event.getEntity();
      if (player.level().isClientSide || !CuriosHelper.isEquipped(player, ModGear.CURIOSITY_BAND.get()) || event.getOrb().value <= 1) return;
      int d = event.getOrb().value / 2;
      event.getOrb().value -= d;
      float r = player.getRandom().nextFloat();
      String[] s = ResearchCategories.CATEGORIES.keySet().toArray(new String[0]);
      if (s.length == 0) return;
      if (r < 0.05 * d) {
         String cat = s[player.getRandom().nextInt(s.length)];
         ResearchManager.addKnowledge(player, EnumKnowledgeType.THEORY, ResearchCategories.getResearchCategory(cat), 1);
      } else if (r < 0.2 * d) {
         String cat = s[player.getRandom().nextInt(s.length)];
         ResearchManager.addKnowledge(player, EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory(cat), 1);
      }
   }

   /** Charm of undying: works like a totem from the charm slot. */
   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;
      var slot = CuriosHelper.findFirst(player, ModGear.CHARM_UNDYING.get());
      if (slot.isEmpty()) return;
      ItemStack charm = slot.get().stack();
      if (player instanceof ServerPlayer sp) {
         sp.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
         CriteriaTriggers.USED_TOTEM.trigger(sp, charm.copy());
      }
      charm.shrink(1);
      player.setHealth(1.0F);
      player.removeAllEffects();
      player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
      player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
      player.level().broadcastEntityEvent(player, (byte) 35);
      event.setCanceled(true);
   }

   // ------------------------------------------------------------------ setup

   @EventBusSubscriber(modid = Thaumcraft.MODID, bus = EventBusSubscriber.Bus.MOD)
   public static final class Setup {
      private Setup() {
      }

      /** onItemUseFirst on a cauldron in 1.12: water washes the dye off the robes. */
      @SubscribeEvent
      public static void commonSetup(FMLCommonSetupEvent event) {
         event.enqueueWork(() -> {
            for (var item : List.of(ModGear.CLOTH_CHEST, ModGear.CLOTH_LEGS, ModGear.CLOTH_BOOTS, ModGear.VOID_ROBE_HELM, ModGear.VOID_ROBE_CHEST,
               ModGear.VOID_ROBE_LEGS)) {
               CauldronInteraction.WATER.map().put(item.get(), CauldronInteraction.DYED_ITEM);
            }
         });
      }
   }
}
