package thaumcraft.common.menu;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ArrowItem;
import thaumcraft.common.entities.construct.TurretCrossbowEntity;
import thaumcraft.common.registers.ModMobs;

/** ContainerTurretBasic: the crossbow's ammo slot. */
public class TurretBasicMenu extends ConstructMenu<TurretCrossbowEntity> {
   public TurretBasicMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, inv.player.level().getEntity(buf.readVarInt()) instanceof TurretCrossbowEntity t ? t : null);
   }

   public TurretBasicMenu(int id, Inventory inv, @Nullable TurretCrossbowEntity turret) {
      super(ModMobs.TURRET_BASIC_MENU.get(), id, inv, turret, 80, 29, s -> s.getItem() instanceof ArrowItem);
   }
}
