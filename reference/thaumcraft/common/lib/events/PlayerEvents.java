package thaumcraft.common.lib.events;

import baubles.api.BaublesApi;
import java.util.HashMap;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.items.curios.ItemThaumonomicon;
import thaumcraft.common.items.resources.ItemCrystalEssence;
import thaumcraft.common.lib.capabilities.PlayerKnowledge;
import thaumcraft.common.lib.capabilities.PlayerWarp;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.potions.PotionDeathGaze;
import thaumcraft.common.lib.potions.PotionUnnaturalHunger;
import thaumcraft.common.lib.potions.PotionWarpWard;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.world.aura.AuraHandler;

@EventBusSubscriber
public class PlayerEvents {
   static HashMap<Integer, Long> nextCycle = new HashMap<>();
   static HashMap<Integer, Integer> lastCharge = new HashMap<>();
   static HashMap<Integer, Integer> lastMaxCharge = new HashMap<>();
   static HashMap<Integer, Integer> runicInfo = new HashMap<>();
   static HashMap<String, Long> upgradeCooldown = new HashMap<>();
   public static HashMap<Integer, Float> prevStep = new HashMap<>();

   @SubscribeEvent
   public static void onFallDamage(LivingHurtEvent event) {
      if (event.getSource() == DamageSource.FALL && event.getEntityLiving() instanceof EntityPlayer) {
         if (((ItemStack)((EntityPlayer)event.getEntityLiving()).inventory.armorInventory.get(0)).getItem() == ItemsTC.travellerBoots) {
            float f = Math.max(0.0F, event.getAmount() / 2.0F - 1.0F);
            if (f < 1.0F) {
               event.setCanceled(true);
               event.setAmount(0.0F);
            } else {
               event.setAmount(f);
            }
         }

         if (BaublesApi.isBaubleEquipped((EntityPlayer)event.getEntityLiving(), ItemsTC.ringCloud) >= 0) {
            float f = Math.max(0.0F, event.getAmount() / 3.0F - 2.0F);
            if (f < 1.0F) {
               event.setCanceled(true);
               event.setAmount(0.0F);
            } else if (f < event.getAmount()) {
               event.setAmount(f);
            }

            if (event.getAmount() < 1.0F) {
               event.setCanceled(true);
               event.setAmount(0.0F);
            }
         }
      }
   }

   @SubscribeEvent
   public static void livingTick(LivingUpdateEvent event) {
      if (event.getEntity() instanceof EntityPlayer) {
         EntityPlayer player = (EntityPlayer)event.getEntity();
         handleMisc(player);
         handleSpeedMods(player);
         if (!player.world.isRemote) {
            handleRunicArmor(player);
            handleWarp(player);
            if (player.ticksExisted % 20 == 0 && ResearchManager.syncList.remove(player.getName()) != null) {
               IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
               knowledge.sync((EntityPlayerMP)player);
            }

            if (player.ticksExisted % 200 == 0) {
               ConfigResearch.checkPeriodicStuff(player);
            }
         }
      }
   }

   @SubscribeEvent
   public static void pickupItem(EntityItemPickupEvent event) {
      if (event.getEntityPlayer() != null
         && !event.getEntityPlayer().world.isRemote
         && event.getItem() != null
         && event.getItem().getItem() != null) {
         IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(event.getEntityPlayer());
         if (event.getItem().getItem().getItem() instanceof ItemCrystalEssence && !knowledge.isResearchKnown("!gotcrystals")) {
            knowledge.addResearch("!gotcrystals");
            knowledge.sync((EntityPlayerMP)event.getEntityPlayer());
            event.getEntityPlayer().sendMessage(new TextComponentString(TextFormatting.DARK_PURPLE + I18n.translateToLocal("got.crystals")));
            if (ModConfig.CONFIG_MISC.noSleep && !knowledge.isResearchKnown("!gotdream")) {
               giveDreamJournal(event.getEntityPlayer());
            }
         }

         if (event.getItem().getItem().getItem() instanceof ItemThaumonomicon && !knowledge.isResearchKnown("!gotthaumonomicon")) {
            knowledge.addResearch("!gotthaumonomicon");
            knowledge.sync((EntityPlayerMP)event.getEntityPlayer());
         }
      }
   }

   @SubscribeEvent
   public static void wakeUp(PlayerWakeUpEvent event) {
      IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(event.getEntityPlayer());
      if (event.getEntityPlayer() != null
         && !event.getEntityPlayer().world.isRemote
         && knowledge.isResearchKnown("!gotcrystals")
         && !knowledge.isResearchKnown("!gotdream")) {
         giveDreamJournal(event.getEntityPlayer());
      }
   }

   private static void giveDreamJournal(EntityPlayer player) {
      IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(player);
      knowledge.addResearch("!gotdream");
      knowledge.sync((EntityPlayerMP)player);
      ItemStack book = ConfigItems.startBook.copy();
      book.getTagCompound().setString("author", player.getName());
      if (!player.inventory.addItemStackToInventory(book)) {
         InventoryUtils.dropItemAtEntity(player.world, book, player);
      }

      try {
         player.sendMessage(new TextComponentString(TextFormatting.DARK_PURPLE + I18n.translateToLocal("got.dream")));
      } catch (Exception var4) {
      }
   }

   private static void handleMisc(EntityPlayer player) {
      if (player.world.provider.getDimension() == ModConfig.CONFIG_WORLD.dimensionOuterId
         && player.ticksExisted % 20 == 0
         && !player.isSpectator()
         && !player.capabilities.isCreativeMode
         && player.capabilities.isFlying) {
         player.capabilities.isFlying = false;
         player.sendStatusMessage(new TextComponentString(TextFormatting.ITALIC + "" + TextFormatting.GRAY + I18n.translateToLocal("tc.break.fly")), true);
      }
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public static void tooltipEvent(ItemTooltipEvent event) {
      try {
         int charge = getRunicCharge(event.getItemStack());
         if (charge > 0) {
            event.getToolTip().add(TextFormatting.GOLD + I18n.translateToLocal("item.runic.charge") + " +" + charge);
         }

         int warp = getFinalWarp(event.getItemStack(), event.getEntityPlayer());
         if (warp > 0) {
            event.getToolTip().add(TextFormatting.DARK_PURPLE + I18n.translateToLocal("item.warping") + " " + warp);
         }

         int al = getFinalDiscount(event.getItemStack(), event.getEntityPlayer());
         if (al > 0) {
            event.getToolTip().add(TextFormatting.DARK_PURPLE + I18n.translateToLocal("tc.visdiscount") + ": " + al + "%");
         }

         if (event.getItemStack() != null) {
            if (event.getItemStack().getItem() instanceof IRechargable) {
               int c = Math.round((float)RechargeHelper.getCharge(event.getItemStack()));
               if (c >= 0) {
                  event.getToolTip().add(TextFormatting.YELLOW + I18n.translateToLocal("tc.charge") + " " + c);
               }
            }

            if (event.getItemStack().getItem() instanceof IEssentiaContainerItem) {
               AspectList aspects = ((IEssentiaContainerItem)event.getItemStack().getItem()).getAspects(event.getItemStack());
               if (aspects != null && aspects.size() > 0) {
                  for (Aspect tag : aspects.getAspectsSortedByName()) {
                     event.getToolTip().add(tag.getName() + " x" + aspects.getAmount(tag));
                  }
               }
            }

            NBTTagList nbttaglist = EnumInfusionEnchantment.getInfusionEnchantmentTagList(event.getItemStack());
            if (nbttaglist != null) {
               for (int j = 0; j < nbttaglist.tagCount(); j++) {
                  int k = nbttaglist.getCompoundTagAt(j).getShort("id");
                  int l = nbttaglist.getCompoundTagAt(j).getShort("lvl");
                  if (k >= 0 && k < EnumInfusionEnchantment.values().length) {
                     String s = TextFormatting.GOLD + I18n.translateToLocal("enchantment.infusion." + EnumInfusionEnchantment.values()[k].toString());
                     if (EnumInfusionEnchantment.values()[k].maxLevel > 1) {
                        s = s + " " + I18n.translateToLocal("enchantment.level." + l);
                     }

                     event.getToolTip().add(1, s);
                  }
               }
            }
         }
      } catch (Exception var9) {
      }
   }

   private static void handleRunicArmor(EntityPlayer player) {
      if (player.ticksExisted % 20 == 0) {
         int max = 0;

         for (int a = 0; a < 4; a++) {
            max += getRunicCharge((ItemStack)player.inventory.armorInventory.get(a));
         }

         IInventory baubles = BaublesApi.getBaubles(player);

         for (int a = 0; a < baubles.getSizeInventory(); a++) {
            max += getRunicCharge(baubles.getStackInSlot(a));
         }

         if (lastMaxCharge.containsKey(player.getEntityId())) {
            int charge = lastMaxCharge.get(player.getEntityId());
            if (charge > max) {
               player.setAbsorptionAmount(player.getAbsorptionAmount() - (charge - max));
            }

            if (max <= 0) {
               lastMaxCharge.remove(player.getEntityId());
            }
         }

         if (max > 0) {
            runicInfo.put(player.getEntityId(), max);
            lastMaxCharge.put(player.getEntityId(), max);
         } else {
            runicInfo.remove(player.getEntityId());
         }
      }

      if (runicInfo.containsKey(player.getEntityId())) {
         if (!nextCycle.containsKey(player.getEntityId())) {
            nextCycle.put(player.getEntityId(), 0L);
         }

         long time = System.currentTimeMillis();
         int chargex = (int)player.getAbsorptionAmount();
         if (chargex == 0 && lastCharge.containsKey(player.getEntityId()) && lastCharge.get(player.getEntityId()) > 0) {
            nextCycle.put(player.getEntityId(), time + ModConfig.CONFIG_MISC.shieldWait);
            lastCharge.put(player.getEntityId(), 0);
         }

         if (chargex < runicInfo.get(player.getEntityId())
            && nextCycle.get(player.getEntityId()) < time
            && !AuraHandler.shouldPreserveAura(player.world, player, player.getPosition())
            && AuraHelper.getVis(player.world, new BlockPos(player)) >= ModConfig.CONFIG_MISC.shieldCost) {
            AuraHandler.drainVis(player.world, new BlockPos(player), ModConfig.CONFIG_MISC.shieldCost, false);
            nextCycle.put(player.getEntityId(), time + ModConfig.CONFIG_MISC.shieldRecharge);
            player.setAbsorptionAmount(chargex + 1);
            lastCharge.put(player.getEntityId(), chargex + 1);
         }
      }
   }

   public static int getRunicCharge(ItemStack stack) {
      int base = 0;
      if (stack.hasTagCompound() && stack.getTagCompound().hasKey("TC.RUNIC")) {
         base += stack.getTagCompound().getByte("TC.RUNIC");
      }

      return base;
   }

   public static int getFinalWarp(ItemStack stack, EntityPlayer player) {
      if (stack != null && !stack.isEmpty()) {
         int warp = 0;
         if (stack.getItem() instanceof IWarpingGear) {
            IWarpingGear armor = (IWarpingGear)stack.getItem();
            warp += armor.getWarp(stack, player);
         }

         if (stack.hasTagCompound() && stack.getTagCompound().hasKey("TC.WARP")) {
            warp += stack.getTagCompound().getByte("TC.WARP");
         }

         return warp;
      } else {
         return 0;
      }
   }

   public static int getFinalDiscount(ItemStack stack, EntityPlayer player) {
      if (stack != null && !stack.isEmpty() && stack.getItem() instanceof IVisDiscountGear) {
         IVisDiscountGear gear = (IVisDiscountGear)stack.getItem();
         return gear.getVisDiscount(stack, player);
      } else {
         return 0;
      }
   }

   private static void handleSpeedMods(EntityPlayer player) {
      if (player.world.isRemote
         && (player.isSneaking() || ((ItemStack)player.inventory.armorInventory.get(0)).getItem() != ItemsTC.travellerBoots)
         && prevStep.containsKey(player.getEntityId())) {
         player.stepHeight = prevStep.get(player.getEntityId());
         prevStep.remove(player.getEntityId());
      }
   }

   @SubscribeEvent
   public static void playerJumps(LivingJumpEvent event) {
      if (event.getEntity() instanceof EntityPlayer
         && ((ItemStack)((EntityPlayer)event.getEntity()).inventory.armorInventory.get(0)).getItem() == ItemsTC.travellerBoots) {
         ItemStack is = (ItemStack)((EntityPlayer)event.getEntity()).inventory.armorInventory.get(0);
         if (RechargeHelper.getCharge(is) > 0) {
            event.getEntityLiving().motionY += 0.275F;
         }
      }
   }

   private static void handleWarp(EntityPlayer player) {
      if (!ModConfig.CONFIG_MISC.wussMode && player.ticksExisted > 0 && player.ticksExisted % 2000 == 0 && !player.isPotionActive(PotionWarpWard.instance)) {
         WarpEvents.checkWarpEvent(player);
      }

      if (player.ticksExisted % 20 == 0 && player.isPotionActive(PotionDeathGaze.instance)) {
         WarpEvents.checkDeathGaze(player);
      }
   }

   @SubscribeEvent
   public static void droppedItem(ItemTossEvent event) {
      NBTTagCompound itemData = event.getEntityItem().getEntityData();
      itemData.setString("thrower", event.getPlayer().getName());
   }

   @SubscribeEvent
   public static void finishedUsingItem(Finish event) {
      if (!event.getEntity().world.isRemote && event.getEntityLiving().isPotionActive(PotionUnnaturalHunger.instance)) {
         if (event.getItem().isItemEqual(new ItemStack(Items.ROTTEN_FLESH)) || event.getItem().isItemEqual(new ItemStack(ItemsTC.brain))) {
            PotionEffect pe = event.getEntityLiving().getActivePotionEffect(PotionUnnaturalHunger.instance);
            int amp = pe.getAmplifier() - 1;
            int duration = pe.getDuration() - 600;
            event.getEntityLiving().removePotionEffect(PotionUnnaturalHunger.instance);
            if (duration > 0 && amp >= 0) {
               pe = new PotionEffect(PotionUnnaturalHunger.instance, duration, amp, true, false);
               pe.getCurativeItems().clear();
               pe.addCurativeItem(new ItemStack(Items.ROTTEN_FLESH));
               event.getEntityLiving().addPotionEffect(pe);
            }

            event.getEntityLiving().sendMessage(new TextComponentString("§2§o" + I18n.translateToLocal("warp.text.hunger.2")));
         } else if (event.getItem().getItem() instanceof ItemFood) {
            event.getEntityLiving().sendMessage(new TextComponentString("§4§o" + I18n.translateToLocal("warp.text.hunger.1")));
         }
      }
   }

   @SubscribeEvent
   public static void attachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
      if (event.getObject() instanceof EntityPlayer) {
         event.addCapability(PlayerKnowledge.Provider.NAME, new PlayerKnowledge.Provider());
         event.addCapability(PlayerWarp.Provider.NAME, new PlayerWarp.Provider());
      }
   }

   @SubscribeEvent
   public static void playerJoin(EntityJoinWorldEvent event) {
      if (!event.getWorld().isRemote && event.getEntity() instanceof EntityPlayerMP) {
         EntityPlayerMP player = (EntityPlayerMP)event.getEntity();
         IPlayerKnowledge pk = ThaumcraftCapabilities.getKnowledge(player);
         IPlayerWarp pw = ThaumcraftCapabilities.getWarp(player);
         if (pk != null) {
            pk.sync(player);
         }

         if (pw != null) {
            pw.sync(player);
         }
      }
   }

   @SubscribeEvent
   public static void cloneCapabilitiesEvent(Clone event) {
      try {
         NBTTagCompound nbtKnowledge = (NBTTagCompound)ThaumcraftCapabilities.getKnowledge(event.getOriginal()).serializeNBT();
         ThaumcraftCapabilities.getKnowledge(event.getEntityPlayer()).deserializeNBT(nbtKnowledge);
         NBTTagCompound nbtWarp = (NBTTagCompound)ThaumcraftCapabilities.getWarp(event.getOriginal()).serializeNBT();
         ThaumcraftCapabilities.getWarp(event.getEntityPlayer()).deserializeNBT(nbtWarp);
      } catch (Exception var3) {
         Thaumcraft.log.error("Could not clone player [" + event.getOriginal().getName() + "] knowledge when changing dimensions");
      }
   }

   @SubscribeEvent
   public static void pickupXP(PlayerPickupXpEvent event) {
      if (event.getEntityPlayer() != null
         && !event.getEntityPlayer().world.isRemote
         && BaublesApi.isBaubleEquipped(event.getEntityPlayer(), ItemsTC.bandCuriosity) >= 0
         && event.getOrb().getXpValue() > 1) {
         int d = event.getOrb().xpValue / 2;
         event.getOrb().xpValue -= d;
         float r = event.getEntityPlayer().getRNG().nextFloat();
         if (r < 0.05 * d) {
            String[] s = ResearchCategories.researchCategories.keySet().toArray(new String[0]);
            String cat = s[event.getEntityPlayer().getRNG().nextInt(s.length)];
            ThaumcraftApi.internalMethods
               .addKnowledge(event.getEntityPlayer(), IPlayerKnowledge.EnumKnowledgeType.THEORY, ResearchCategories.getResearchCategory(cat), 1);
         } else if (r < 0.2 * d) {
            String[] s = ResearchCategories.researchCategories.keySet().toArray(new String[0]);
            String cat = s[event.getEntityPlayer().getRNG().nextInt(s.length)];
            ThaumcraftApi.internalMethods
               .addKnowledge(event.getEntityPlayer(), IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, ResearchCategories.getResearchCategory(cat), 1);
         }
      }
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent event) {
      if (event.getEntityLiving() instanceof EntityPlayer) {
         EntityPlayer player = (EntityPlayer)event.getEntityLiving();
         int slot = BaublesApi.isBaubleEquipped(player, ItemsTC.charmUndying);
         if (slot >= 0) {
            if (player instanceof EntityPlayerMP) {
               EntityPlayerMP entityplayermp = (EntityPlayerMP)player;
               entityplayermp.addStat(StatList.getObjectUseStats(Items.TOTEM_OF_UNDYING));
               CriteriaTriggers.USED_TOTEM.trigger(entityplayermp, BaublesApi.getBaubles(player).getStackInSlot(slot));
            }

            BaublesApi.getBaublesHandler(player).extractItem(slot, 1, false);
            player.setHealth(1.0F);
            player.clearActivePotions();
            player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 900, 1));
            player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 100, 1));
            player.world.setEntityState(player, (byte)35);
            event.setCanceled(true);
         }
      }
   }
}
