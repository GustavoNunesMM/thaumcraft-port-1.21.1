package thaumcraft.common.research;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import thaumcraft.api.research.ResearchItem;
import thaumcraft.common.items.CelestialNotesItem;
import thaumcraft.common.items.ScribingToolsItem;
import thaumcraft.common.lib.InventoryHelper;

/**
 * ScanSky: with CELESTIALSCANNING, scanning the sun, the moon or (at night) the stars with scribing tools and paper
 * in the inventory writes celestial notes. Each body once per day.
 */
public class ScanSky implements ScanningManager.ScanThing {
   private record Aim(boolean night, boolean onBody) {}

   @Nullable
   private static Aim aim(ServerPlayer player, @Nullable Object obj) {
      Level level = player.level();
      if (obj != null || player.getXRot() > 0.0F || !level.canSeeSky(player.blockPosition().above()) || level.dimension() != Level.OVERWORLD
         || !ResearchManager.knowsResearchStrict(player, "CELESTIALSCANNING")) {
         return null;
      }
      int yaw = (int) (player.getYRot() + 90.0F) % 360;
      int pitch = (int) Math.abs(player.getXRot());
      int ca = (int) ((level.getTimeOfDay(0.0F) + 0.25) * 360.0) % 360;
      boolean night = ca > 180;
      if (night) ca -= 180;
      boolean inYaw, inPitch;
      if (ca > 90) {
         inYaw = Math.abs(Math.abs(yaw) - 180) < 10;
         inPitch = Math.abs(180 - ca - pitch) < 7;
      } else {
         inYaw = Math.abs(yaw) < 10;
         inPitch = Math.abs(ca - pitch) < 7;
      }
      return new Aim(night, inYaw && inPitch);
   }

   @Override
   public boolean checkThing(ServerPlayer player, @Nullable Object obj) {
      Aim a = aim(player, obj);
      return a != null && (a.onBody() || a.night());
   }

   @Override
   public void onSuccess(ServerPlayer player, @Nullable Object obj) {
      Aim a = aim(player, obj);
      if (a == null) return;
      Level level = player.level();
      int worldDay = (int) (level.getGameTime() / 24000L);
      String pk = "CEL_" + worldDay + "_";
      String key;
      int notes;
      if (a.onBody()) {
         key = pk + (a.night() ? "Moon" + level.getMoonPhase() : "Sun");
         notes = a.night() ? 5 + level.getMoonPhase() : 0;
      } else if (a.night()) {
         int num = player.getDirection().get3DDataValue() - 2;
         key = pk + "Star" + num;
         notes = 1 + num;
      } else {
         return;
      }
      PlayerKnowledge k = ResearchManager.knowledge(player);
      if (k.isResearchKnown(key)) {
         player.displayClientMessage(Component.translatable("tc.celestial.fail.1", ""), true);
         return;
      }
      boolean tools = player.getInventory().items.stream().anyMatch(s -> s.getItem() instanceof ScribingToolsItem);
      if (tools && InventoryHelper.consume(player, new ResearchItem(new ItemStack(Items.PAPER), null))) {
         InventoryHelper.giveOrDrop(player, CelestialNotesItem.of(notes));
         ResearchManager.progressResearch(player, key);
      } else {
         player.displayClientMessage(Component.translatable("tc.celestial.fail.2", ""), true);
      }
      List<String> old = new ArrayList<>();
      for (String r : k.getResearchList()) {
         if (r.startsWith("CEL_") && !r.startsWith(pk)) old.add(r);
      }
      old.forEach(k::removeResearch);
      ResearchManager.markDirty(player);
   }

   @Override
   public String getResearchKey(ServerPlayer player, @Nullable Object obj) {
      return "";
   }
}
