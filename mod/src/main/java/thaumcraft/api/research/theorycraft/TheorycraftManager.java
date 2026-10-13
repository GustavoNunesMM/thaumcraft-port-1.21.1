package thaumcraft.api.research.theorycraft;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Registry of research aids and cards, keyed by name (saved in the research table's data). */
public final class TheorycraftManager {
   public static final Map<String, ITheorycraftAid> AIDS = new LinkedHashMap<>();
   public static final Map<String, Supplier<TheorycraftCard>> CARDS = new LinkedHashMap<>();

   private TheorycraftManager() {
   }

   public static void registerAid(String key, ITheorycraftAid aid) {
      AIDS.putIfAbsent(key, aid);
   }

   public static void registerCard(String key, Supplier<TheorycraftCard> card) {
      CARDS.putIfAbsent(key, card);
   }

   /** The registry key of a card instance (its class's simple name, as registered). */
   public static String keyOf(TheorycraftCard card) {
      for (Map.Entry<String, Supplier<TheorycraftCard>> e : CARDS.entrySet()) {
         if (e.getValue().get().getClass() == card.getClass()) return e.getKey();
      }
      return card.getClass().getSimpleName();
   }
}
