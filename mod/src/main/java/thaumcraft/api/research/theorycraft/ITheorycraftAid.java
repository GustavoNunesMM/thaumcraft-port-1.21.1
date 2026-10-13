package thaumcraft.api.research.theorycraft;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * A research aid: a block (or creature) near the research table that, if chosen when starting a theory, mixes
 * its special cards into the draws. The aid object is a Block, an ItemStack (matched against the block's item)
 * or an entity Class; null while the thing it needs isn't ported yet.
 */
public interface ITheorycraftAid {
   @Nullable
   Object getAidObject();

   List<Supplier<TheorycraftCard>> getCards();
}
