package thaumcraft.common.menu;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.common.entities.construct.ArcaneBoreEntity;
import thaumcraft.common.registers.ModMobs;

/** ContainerArcaneBore: the bore's pickaxe slot. */
public class ArcaneBoreMenu extends ConstructMenu<ArcaneBoreEntity> {
   public ArcaneBoreMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, inv.player.level().getEntity(buf.readVarInt()) instanceof ArcaneBoreEntity b ? b : null);
   }

   public ArcaneBoreMenu(int id, Inventory inv, @Nullable ArcaneBoreEntity bore) {
      super(ModMobs.ARCANE_BORE_MENU.get(), id, inv, bore, 80, 29, ArcaneBoreEntity::isPickaxe);
   }
}
