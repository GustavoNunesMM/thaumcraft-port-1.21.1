package thaumcraft.common.items.baubles;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.Thaumcraft;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.items.ICreativeVariants;
import thaumcraft.common.registers.ModGear;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * ItemVerdantCharm: spends vis charge to cure wither, poison and flux taint; the "type" (thaumcraft:verdant_type,
 * set by infusion) adds healing (1, life) or air and food (2, sustain).
 */
public class VerdantCharmItem extends Item implements ICurioItem, IRechargable, ICreativeVariants {
   public VerdantCharmItem(Properties p) {
      super(p.stacksTo(1));
   }

   public static int getType(ItemStack stack) {
      return stack.getOrDefault(ModGear.VERDANT_TYPE.get(), 0);
   }



   @Override
   public void addCreativeStacks(Consumer<ItemStack> output) {
      for (int t = 0; t < 3; t++) {
         ItemStack s = new ItemStack(this);
         if (t > 0) s.set(ModGear.VERDANT_TYPE.get(), t);
         output.accept(s);
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      if (getType(stack) == 1) tooltip.add(Component.translatable("item.verdant_charm.life.text").withStyle(ChatFormatting.GOLD));
      if (getType(stack) == 2) tooltip.add(Component.translatable("item.verdant_charm.sustain.text").withStyle(ChatFormatting.GOLD));
   }

   @Override
   public void curioTick(SlotContext ctx, ItemStack stack) {
      LivingEntity player = ctx.entity();
      if (player.level().isClientSide || player.tickCount % 20 != 0 || !(player instanceof Player p)) return;
      if (player.hasEffect(MobEffects.WITHER) && RechargeHelper.consumeCharge(stack, player, 20)) {
         player.removeEffect(MobEffects.WITHER);
         return;
      }
      if (player.hasEffect(MobEffects.POISON) && RechargeHelper.consumeCharge(stack, player, 10)) {
         player.removeEffect(MobEffects.POISON);
         return;
      }
      if (player.hasEffect(thaumcraft.common.registers.ModFlux.FLUX_TAINT) && RechargeHelper.consumeCharge(stack, player, 5)) {
         player.removeEffect(thaumcraft.common.registers.ModFlux.FLUX_TAINT);
         return;
      }
      int type = getType(stack);
      if (type == 1 && player.getHealth() < player.getMaxHealth() && RechargeHelper.consumeCharge(stack, player, 5)) {
         player.heal(1.0F);
         return;
      }
      if (type == 2) {
         if (player.getAirSupply() < 100 && RechargeHelper.consumeCharge(stack, player, 1)) {
            player.setAirSupply(300);
            return;
         }
         if (p.canEat(false) && RechargeHelper.consumeCharge(stack, player, 1)) {
            p.getFoodData().eat(1, 0.3F);
         }
      }
   }

   @Override
   public int getMaxCharge(ItemStack stack, LivingEntity player) {
      return 200;
   }

   @Override
   public EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player) {
      return EnumChargeDisplay.NORMAL;
   }
}
