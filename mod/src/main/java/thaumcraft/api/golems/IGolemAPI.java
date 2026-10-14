package thaumcraft.api.golems;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface IGolemAPI {
   LivingEntity getGolemEntity();

   IGolemProperties getProperties();

   void setProperties(IGolemProperties props);

   Level getGolemWorld();

   /** Tries to hold the stack; returns what could not be held. */
   ItemStack holdItem(ItemStack stack);

   /** Gives away up to the given stack from what it carries; an empty stack gives the first carried stack. */
   ItemStack dropItem(ItemStack stack);

   boolean canCarry(ItemStack stack, boolean partial);

   int canCarryAmount(ItemStack stack);

   boolean isCarrying(ItemStack stack);

   NonNullList<ItemStack> getCarrying();

   void addRankXp(int xp);

   byte getGolemColor();

   void swingArm();

   boolean isInCombat();
}
