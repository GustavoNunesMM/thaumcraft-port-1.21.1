package thaumcraft.common.golems;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.common.golems.menu.LogisticsMenu;
import thaumcraft.common.golems.menu.SealMenu;

/** Opens the golem screens (gui ids 18 and 20 of 1.12). */
public final class GolemMenus {
   private GolemMenus() {
   }

   public static void openSeal(ServerPlayer player, ISealEntity se) {
      player.openMenu(new SimpleMenuProvider((id, inv, p) -> new SealMenu(id, inv, se), Component.translatable("item.thaumcraft.seal_blank")),
         buf -> {
            buf.writeBlockPos(se.getSealPos().pos);
            buf.writeByte(se.getSealPos().face.get3DDataValue());
            buf.writeNbt(se.writeNBT(player.registryAccess()));
         });
   }

   public static void openLogistics(ServerPlayer player, @Nullable BlockPos target, @Nullable Direction side) {
      player.openMenu(new SimpleMenuProvider((id, inv, p) -> new LogisticsMenu(id, inv, target, side), Component.translatable("tc.logistics")),
         buf -> {
            buf.writeBoolean(target != null && side != null);
            if (target != null && side != null) {
               buf.writeBlockPos(target);
               buf.writeByte(side.get3DDataValue());
            }
         });
   }
}
