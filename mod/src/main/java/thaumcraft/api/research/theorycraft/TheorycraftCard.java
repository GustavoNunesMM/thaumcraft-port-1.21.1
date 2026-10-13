package thaumcraft.api.research.theorycraft;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A research table card. Drawing one picks it (initialize may refuse, e.g. when its conditions aren't met);
 * playing it costs inspiration and does something to the theory being written (activate).
 */
public abstract class TheorycraftCard {
   private long seed = -1L;

   public long getSeed() {
      if (seed < 0L) setSeed(System.nanoTime());
      return seed;
   }

   public void setSeed(long seed) {
      this.seed = Math.abs(seed);
   }

   /** Called when drawn (server); false = discard this card and draw another. */
   public boolean initialize(Player player, ResearchTableData data) {
      return true;
   }

   /** Only drawn from research aids, never from the general deck. */
   public boolean isAidOnly() {
      return false;
   }

   /** Inspiration spent to play it; negative cards give inspiration back. */
   public abstract int getInspirationCost();

   @Nullable
   public String getResearchCategory() {
      return null;
   }

   public abstract Component getLocalizedName();

   public abstract Component getLocalizedText();

   /** Items the player must carry to play the card (null entries show as unknown). */
   @Nullable
   public ItemStack[] getRequiredItems() {
      return null;
   }

   /** Which of the required items are used up. */
   @Nullable
   public boolean[] getRequiredItemsConsumed() {
      ItemStack[] r = getRequiredItems();
      return r == null ? null : new boolean[r.length];
   }

   /** Plays the card (server). False = couldn't (nothing is spent). */
   public abstract boolean activate(Player player, ResearchTableData data);

   public CompoundTag serialize() {
      CompoundTag nbt = new CompoundTag();
      nbt.putLong("seed", seed);
      return nbt;
   }

   public void deserialize(CompoundTag nbt) {
      seed = nbt.getLong("seed");
   }
}
