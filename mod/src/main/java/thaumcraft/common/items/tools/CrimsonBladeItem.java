package thaumcraft.common.items.tools;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IWarpingGear;

/** ItemCrimsonBlade: the Crimson Cult's void blade. Self-repairing, weakness and hunger on hit, two warp. */
public class CrimsonBladeItem extends SwordItem implements IWarpingGear {
   public CrimsonBladeItem(Properties p) {
      super(ThaumcraftMaterials.TOOLMAT_CRIMSONVOID, p.attributes(SwordItem.createAttributes(ThaumcraftMaterials.TOOLMAT_CRIMSONVOID, 3, -2.4F)));
   }

   @Override
   public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
      super.inventoryTick(stack, level, entity, slot, selected);
      VoidTools.selfRepair(stack, level, entity);
   }

   @Override
   public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
      if (!target.level().isClientSide && VoidTools.canAffect(target, attacker)) {
         target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60));
         target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120));
      }
      return super.hurtEnemy(stack, target, attacker);
   }

   @Override
   public int getWarp(ItemStack stack, Player player) {
      return 2;
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("enchantment.special.sapgreat").withStyle(ChatFormatting.GOLD));
      super.appendHoverText(stack, ctx, tooltip, flag);
   }
}
