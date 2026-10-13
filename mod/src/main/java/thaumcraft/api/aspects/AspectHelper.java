package thaumcraft.api.aspects;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.common.aspects.AspectManager;

public class AspectHelper {
   public static AspectList cullTags(AspectList temp) {
      return cullTags(temp, 7);
   }

   /** Drops the "least valuable" aspects until at most {@code cap} remain. Primals are cheapest, deep compounds dearest. */
   public static AspectList cullTags(AspectList temp, int cap) {
      AspectList temp2 = temp.copy();
      while (temp2.size() > cap) {
         Aspect lowest = null;
         float low = 32767.0F;
         for (Aspect tag : temp2.getAspects()) {
            float ta = temp2.getAmount(tag);
            if (tag.isPrimal()) {
               ta *= 0.9F;
            } else {
               for (Aspect c : tag.getComponents()) {
                  if (!c.isPrimal()) {
                     ta *= 1.1F;
                     if (!c.getComponents()[0].isPrimal()) ta *= 1.05F;
                     if (!c.getComponents()[1].isPrimal()) ta *= 1.05F;
                  }
               }
            }
            if (ta < low) {
               low = ta;
               lowest = tag;
            }
         }
         temp2.aspects.remove(lowest);
      }
      return temp2;
   }

   public static AspectList getObjectAspects(Level level, ItemStack stack) {
      return AspectManager.get(level).getObjectAspects(level.registryAccess(), stack);
   }

   public static AspectList getEntityAspects(Entity entity) {
      return AspectManager.get(entity.level()).getEntityAspects(entity);
   }
}
