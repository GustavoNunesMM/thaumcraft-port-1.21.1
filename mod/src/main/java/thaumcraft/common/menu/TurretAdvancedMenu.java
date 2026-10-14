package thaumcraft.common.menu;

import javax.annotation.Nullable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArrowItem;
import thaumcraft.common.entities.construct.TurretCrossbowAdvancedEntity;
import thaumcraft.common.registers.ModMobs;

/** ContainerTurretAdvanced: the ammo slot and the four target toggles (buttons 1-4: animals, mobs, players, friendly). */
public class TurretAdvancedMenu extends ConstructMenu<TurretCrossbowAdvancedEntity> {
   public TurretAdvancedMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
      this(id, inv, inv.player.level().getEntity(buf.readVarInt()) instanceof TurretCrossbowAdvancedEntity t ? t : null);
   }

   public TurretAdvancedMenu(int id, Inventory inv, @Nullable TurretCrossbowAdvancedEntity turret) {
      super(ModMobs.TURRET_ADVANCED_MENU.get(), id, inv, turret, 42, 29, s -> s.getItem() instanceof ArrowItem);
   }

   @Override
   public boolean clickMenuButton(Player player, int id) {
      if (entity == null) return false;
      switch (id) {
         case 1 -> entity.setTargetAnimal(!entity.getTargetAnimal());
         case 2 -> entity.setTargetMob(!entity.getTargetMob());
         case 3 -> entity.setTargetPlayer(!entity.getTargetPlayer());
         case 4 -> entity.setTargetFriendly(!entity.getTargetFriendly());
         default -> {
            return false;
         }
      }
      return true;
   }
}
