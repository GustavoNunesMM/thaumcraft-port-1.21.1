package thaumcraft.api.items;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/** Block entities (or blocks) that show extra text over themselves to a player wearing goggles. */
public interface IGogglesDisplayExtended {
   List<Component> getIGogglesText();

   default Vec3 getIGogglesTextOffset() {
      return Vec3.ZERO;
   }
}
