package thaumcraft.common.lib.research;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.IScanThing;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ScanningManager;

public class ScanGeneric implements IScanThing {
   @Override
   public boolean checkThing(EntityPlayer player, Object obj) {
      if (obj == null) {
         return false;
      } else {
         AspectList al = null;
         if (obj instanceof Entity && !(obj instanceof EntityItem)) {
            al = AspectHelper.getEntityAspects((Entity)obj);
         } else {
            ItemStack is = ScanningManager.getItemFromParms(player, obj);
            if (is != null && !is.isEmpty()) {
               al = AspectHelper.getObjectAspects(is);
            }
         }

         return al != null && al.size() > 0;
      }
   }

   @Override
   public void onSuccess(EntityPlayer player, Object obj) {
      if (obj != null) {
         AspectList al = null;
         if (obj instanceof Entity && !(obj instanceof EntityItem)) {
            al = AspectHelper.getEntityAspects((Entity)obj);
         } else {
            ItemStack is = ScanningManager.getItemFromParms(player, obj);
            if (is != null && !is.isEmpty()) {
               al = AspectHelper.getObjectAspects(is);
            }
         }

         if (al != null) {
            for (ResearchCategory category : ResearchCategories.researchCategories.values()) {
               ThaumcraftApi.internalMethods.addKnowledge(player, IPlayerKnowledge.EnumKnowledgeType.OBSERVATION, category, category.applyFormula(al));
            }
         }
      }
   }

   @Override
   public String getResearchKey(EntityPlayer player, Object obj) {
      if (obj instanceof Entity && !(obj instanceof EntityItem)) {
         String s = EntityList.getEntityString((Entity)obj);
         return "!" + s;
      } else {
         ItemStack is = ScanningManager.getItemFromParms(player, obj);
         if (is != null && !is.isEmpty()) {
            String s = "!" + is.getItem().getRegistryName();
            if (!is.isItemStackDamageable()) {
               s = s + is.getItemDamage();
            }

            return s;
         } else {
            return null;
         }
      }
   }
}
