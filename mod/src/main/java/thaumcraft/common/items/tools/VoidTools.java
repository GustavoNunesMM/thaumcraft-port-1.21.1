package thaumcraft.common.items.tools;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IWarpingGear;

/**
 * ItemVoidSword/Pickaxe/Axe/Shovel/Hoe: void metal tools. They mend themselves (one point a second while carried), weaken
 * what they hit and give one warp.
 */
public final class VoidTools {
   private VoidTools() {
   }

   /** onUpdate: stack.damageItem(-1) every 20 ticks while damaged. */
   public static void selfRepair(ItemStack stack, Level level, Entity entity) {
      if (!level.isClientSide && stack.isDamaged() && entity instanceof LivingEntity && entity.tickCount % 20 == 0) {
         stack.setDamageValue(stack.getDamageValue() - 1);
      }
   }

   /** PvP is honoured as in 1.12 (FMLCommonHandler...isPVPEnabled). */
   public static boolean canAffect(Entity target, Entity attacker) {
      if (!(target instanceof Player) || !(attacker instanceof Player)) return true;
      return !(target.level() instanceof ServerLevel sl) || sl.getServer().isPvpAllowed();
   }

   /** onLeftClickEntity of the void tools: weakness for 4 seconds. */
   static void weaken(Player player, Entity entity) {
      if (!player.level().isClientSide && entity instanceof LivingEntity le && canAffect(entity, player)) {
         le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80));
      }
   }

   public static class Sword extends SwordItem implements IWarpingGear {
      public Sword(Properties p) {
         super(ThaumcraftMaterials.TOOLMAT_VOID, p.attributes(SwordItem.createAttributes(ThaumcraftMaterials.TOOLMAT_VOID, 3, -2.4F)));
      }

      @Override
      public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
         super.inventoryTick(stack, level, entity, slot, selected);
         selfRepair(stack, level, entity);
      }

      @Override
      public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
         if (!target.level().isClientSide && canAffect(target, attacker)) target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60));
         return super.hurtEnemy(stack, target, attacker);
      }

      @Override
      public int getWarp(ItemStack stack, Player player) {
         return 1;
      }

      @Override
      public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
         tooltip.add(Component.translatable("enchantment.special.sapless").withStyle(ChatFormatting.GOLD));
         super.appendHoverText(stack, ctx, tooltip, flag);
      }
   }

   public static class Pickaxe extends PickaxeItem implements IWarpingGear {
      public Pickaxe(Properties p) {
         super(ThaumcraftMaterials.TOOLMAT_VOID, p.attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_VOID, 1.0F, -2.8F)));
      }

      @Override
      public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
         super.inventoryTick(stack, level, entity, slot, selected);
         selfRepair(stack, level, entity);
      }

      @Override
      public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
         weaken(player, entity);
         return super.onLeftClickEntity(stack, player, entity);
      }

      @Override
      public int getWarp(ItemStack stack, Player player) {
         return 1;
      }
   }

   public static class Axe extends AxeItem implements IWarpingGear {
      public Axe(Properties p) {
         super(ThaumcraftMaterials.TOOLMAT_VOID, p.attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_VOID, 8.0F, -3.0F)));
      }

      @Override
      public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
         super.inventoryTick(stack, level, entity, slot, selected);
         selfRepair(stack, level, entity);
      }

      @Override
      public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
         weaken(player, entity);
         return super.onLeftClickEntity(stack, player, entity);
      }

      @Override
      public int getWarp(ItemStack stack, Player player) {
         return 1;
      }
   }

   public static class Shovel extends ShovelItem implements IWarpingGear {
      public Shovel(Properties p) {
         super(ThaumcraftMaterials.TOOLMAT_VOID, p.attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_VOID, 1.5F, -3.0F)));
      }

      @Override
      public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
         super.inventoryTick(stack, level, entity, slot, selected);
         selfRepair(stack, level, entity);
      }

      @Override
      public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
         weaken(player, entity);
         return super.onLeftClickEntity(stack, player, entity);
      }

      @Override
      public int getWarp(ItemStack stack, Player player) {
         return 1;
      }
   }

   public static class Hoe extends HoeItem implements IWarpingGear {
      public Hoe(Properties p) {
         super(ThaumcraftMaterials.TOOLMAT_VOID, p.attributes(ThaumiumTools.hoeAttributes(ThaumcraftMaterials.TOOLMAT_VOID)));
      }

      @Override
      public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
         super.inventoryTick(stack, level, entity, slot, selected);
         selfRepair(stack, level, entity);
      }

      @Override
      public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
         weaken(player, entity);
         return super.onLeftClickEntity(stack, player, entity);
      }

      @Override
      public int getWarp(ItemStack stack, Player player) {
         return 1;
      }
   }
}
