package thaumcraft.common.lib.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.registers.ModMobs;

/**
 * Utils.generateLoot's gear roll, as a loot function on the loot bag tables: with a 2.5% chance per rarity level the
 * rolled item is replaced by a random, slightly worn piece of armor or weapon (genGear), possibly enchanted.
 */
public class GearRollFunction extends LootItemConditionalFunction {
   public static final MapCodec<GearRollFunction> CODEC = RecordCodecBuilder.mapCodec(i -> commonFields(i)
      .and(com.mojang.serialization.Codec.INT.fieldOf("rarity").forGetter(f -> f.rarity)).apply(i, GearRollFunction::new));
   private final int rarity;

   public GearRollFunction(List<LootItemCondition> conditions, int rarity) {
      super(conditions);
      this.rarity = rarity;
   }

   @Override
   public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
      return ModMobs.GEAR_ROLL.get();
   }

   @Override
   protected ItemStack run(ItemStack stack, LootContext ctx) {
      RandomSource rand = ctx.getRandom();
      if (rarity > 0 && rand.nextFloat() < 0.025F * rarity) {
         ItemStack gear = genGear(rarity, rand, ctx);
         if (!gear.isEmpty()) return gear;
      }
      return stack;
   }

   public static ItemStack genGear(int rarity, RandomSource rand, LootContext ctx) {
      int quality = rand.nextInt(2);
      if (rand.nextFloat() < 0.2F) quality++;
      if (rand.nextFloat() < 0.15F) quality++;
      if (rand.nextFloat() < 0.1F) quality++;
      if (rand.nextFloat() < 0.095F) quality++;
      if (rand.nextFloat() < 0.095F) quality++;
      Item item = getGearItemForSlot(rand.nextInt(5), quality);
      if (item == null) return ItemStack.EMPTY;
      ItemStack is = new ItemStack(item);
      if (is.isDamageableItem()) is.setDamageValue(rand.nextInt(1 + is.getMaxDamage() / 6));
      if (rand.nextInt(4) < rarity) {
         var reg = ctx.getLevel().registryAccess();
         is = EnchantmentHelper.enchantItem(rand, is, (int) (5.0F + rarity * 0.75F * rand.nextInt(18)), reg,
            reg.registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.IN_ENCHANTING_TABLE));
      }
      return is;
   }

   /** Utils.getGearItemForSlot: 4 helmet, 3 chest, 2 legs, 1 boots, 0 weapon; quality 0 (leather) .. 6 (void). */
   public static Item getGearItemForSlot(int slot, int quality) {
      Item[][] table = {
         {Items.IRON_AXE, Items.IRON_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_SWORD, ModGear.THAUMIUM_SWORD.get(), Items.DIAMOND_SWORD, ModGear.VOID_SWORD.get()},
         {Items.LEATHER_BOOTS, Items.GOLDEN_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, ModGear.THAUMIUM_BOOTS.get(), Items.DIAMOND_BOOTS, ModGear.VOID_BOOTS.get()},
         {Items.LEATHER_LEGGINGS, Items.GOLDEN_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, ModGear.THAUMIUM_LEGS.get(), Items.DIAMOND_LEGGINGS,
            ModGear.VOID_LEGS.get()},
         {Items.LEATHER_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, ModGear.THAUMIUM_CHEST.get(),
            Items.DIAMOND_CHESTPLATE, ModGear.VOID_CHEST.get()},
         {Items.LEATHER_HELMET, Items.GOLDEN_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, ModGear.THAUMIUM_HELM.get(), Items.DIAMOND_HELMET,
            ModGear.VOID_HELM.get()}};
      if (slot < 0 || slot >= table.length || quality < 0 || quality > 6) return null;
      return table[slot][quality];
   }
}
