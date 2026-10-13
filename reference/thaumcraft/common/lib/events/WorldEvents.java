package thaumcraft.common.lib.events;

import java.util.ArrayList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent.MultiPlaceEvent;
import net.minecraftforge.event.world.BlockEvent.PlaceEvent;
import net.minecraftforge.event.world.NoteBlockEvent.Play;
import net.minecraftforge.event.world.WorldEvent.Load;
import net.minecraftforge.event.world.WorldEvent.Save;
import net.minecraftforge.event.world.WorldEvent.Unload;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import thaumcraft.common.golems.seals.SealHandler;
import thaumcraft.common.tiles.devices.TileArcaneEar;
import thaumcraft.common.world.aura.AuraHandler;

@EventBusSubscriber
public class WorldEvents {
   public static WorldEvents INSTANCE = new WorldEvents();

   @SubscribeEvent
   public static void worldLoad(Load event) {
      if (!event.getWorld().isRemote) {
         AuraHandler.addAuraWorld(event.getWorld().provider.getDimension());
      }
   }

   @SubscribeEvent
   public static void worldSave(Save event) {
      if (!event.getWorld().isRemote) {
      }
   }

   @SubscribeEvent
   public static void worldUnload(Unload event) {
      if (!event.getWorld().isRemote) {
         SealHandler.sealEntities.remove(event.getWorld().provider.getDimension());
         AuraHandler.removeAuraWorld(event.getWorld().provider.getDimension());
      }
   }

   @SubscribeEvent
   public static void placeBlockEvent(PlaceEvent event) {
      if (isNearActiveBoss(event.getWorld(), event.getPlayer(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ())
         )
       {
         event.setCanceled(true);
      }
   }

   @SubscribeEvent
   public static void placeBlockEvent(MultiPlaceEvent event) {
      if (isNearActiveBoss(event.getWorld(), event.getPlayer(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ())
         )
       {
         event.setCanceled(true);
      }
   }

   private static boolean isNearActiveBoss(World world, EntityPlayer player, int x, int y, int z) {
      return false;
   }

   @SubscribeEvent
   public static void noteEvent(Play event) {
      if (!event.getWorld().isRemote) {
         if (!TileArcaneEar.noteBlockEvents.containsKey(event.getWorld().provider.getDimension())) {
            TileArcaneEar.noteBlockEvents.put(event.getWorld().provider.getDimension(), new ArrayList<>());
         }

         ArrayList<Integer[]> list = TileArcaneEar.noteBlockEvents.get(event.getWorld().provider.getDimension());
         list.add(
            new Integer[]{
               event.getPos().getX(),
               event.getPos().getY(),
               event.getPos().getZ(),
               event.getInstrument().ordinal(),
               event.getVanillaNoteId()
            }
         );
         TileArcaneEar.noteBlockEvents.put(event.getWorld().provider.getDimension(), list);
      }
   }
}
