package thaumcraft.api.aspects;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;

/**
 * Blocks that move essentia through tubes. Suction pulls essentia towards the strongest sucker; a face may be null
 * when the caller doesn't care about the side (as in the original).
 */
public interface IEssentiaTransport {
   boolean isConnectable(Direction face);

   boolean canInputFrom(Direction face);

   boolean canOutputTo(Direction face);

   void setSuction(@Nullable Aspect aspect, int amount);

   @Nullable
   Aspect getSuctionType(@Nullable Direction face);

   int getSuctionAmount(@Nullable Direction face);

   /** Returns how much was actually taken. */
   int takeEssentia(Aspect aspect, int amount, @Nullable Direction face);

   /** Returns how much was actually added. */
   int addEssentia(Aspect aspect, int amount, @Nullable Direction face);

   @Nullable
   Aspect getEssentiaType(@Nullable Direction face);

   int getEssentiaAmount(@Nullable Direction face);

   int getMinimumSuction();
}
