package thaumcraft.common.items.casters;

import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.core.component.DataComponents;
import thaumcraft.common.menu.FocusPouchMenu;

/** ItemFocusPouch: 18 slots for foci; worn in the Curios belt slot or carried, the focus selector draws from it. */
public class FocusPouchItem extends Item {
   public static final int SIZE = 18;

   public FocusPouchItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      if (!level.isClientSide && player instanceof ServerPlayer sp) {
         int slot = hand == InteractionHand.MAIN_HAND ? sp.getInventory().selected : -1;
         sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new FocusPouchMenu(id, inv, hand, slot),
            Component.translatable("item.thaumcraft.focus_pouch")), buf -> {
            buf.writeBoolean(hand == InteractionHand.MAIN_HAND);
            buf.writeVarInt(slot);
         });
      }
      return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
   }

   public static NonNullList<ItemStack> getInventory(ItemStack item) {
      NonNullList<ItemStack> stackList = NonNullList.withSize(SIZE, ItemStack.EMPTY);
      ItemContainerContents c = item.get(DataComponents.CONTAINER);
      if (c != null) c.copyInto(stackList);
      return stackList;
   }

   public static void setInventory(ItemStack item, NonNullList<ItemStack> stackList) {
      item.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stackList));
   }
}
