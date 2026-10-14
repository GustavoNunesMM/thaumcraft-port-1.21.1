package thaumcraft.common.items.mobs;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import thaumcraft.Thaumcraft;
import thaumcraft.common.registers.ModSounds;

/** ItemLootBag: opening one scatters 8-12 rolls of its rarity's loot (Utils.generateLoot = the loot table thaumcraft:loot_bag/<rarity>). */
public class LootBagItem extends Item {
   public static final String[] RARITIES = {"common", "uncommon", "rare"};
   public final int rarity;

   public LootBagItem(int rarity, Properties properties) {
      super(properties);
      this.rarity = rarity;
   }

   public static ResourceKey<LootTable> table(int rarity) {
      return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "loot_bag/" + RARITIES[rarity]));
   }

   /** Utils.generateLoot: one roll of the bag's table (with the gear roll), as rolled for bags, crates and urns. */
   public static List<ItemStack> generateLoot(ServerLevel level, int rarity, Player player) {
      LootTable t = level.getServer().reloadableRegistries().getLootTable(table(rarity));
      LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, player.position())
         .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT);
      return t.getRandomItems(params);
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
      tooltip.add(Component.translatable("tc.lootbag"));
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      if (level instanceof ServerLevel sl) {
         int q = 8 + level.random.nextInt(5);
         for (int a = 0; a < q; a++) {
            for (ItemStack is : generateLoot(sl, rarity, player)) {
               if (!is.isEmpty()) level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), is.copy()));
            }
         }
         player.playSound(ModSounds.COINS.get(), 0.75F, 1.0F);
      }
      stack.shrink(1);
      return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
   }
}
