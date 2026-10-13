package thaumcraft.api.research;

import javax.annotation.Nullable;

/** ResearchStage.Knowledge: an amount of one knowledge type, in a category ("THEORY;BASICS;2"). */
public record Knowledge(EnumKnowledgeType type, @Nullable ResearchCategory category, int amount) {
   /** Parses "TYPE;amount" or "TYPE;CATEGORY;amount"; null if invalid, like the original. */
   @Nullable
   public static Knowledge parse(String text) {
      String[] s = text.split(";");
      try {
         if (s.length == 2) {
            EnumKnowledgeType t = EnumKnowledgeType.valueOf(s[0].toUpperCase());
            int num = Integer.parseInt(s[1]);
            return !t.hasFields() && num > 0 ? new Knowledge(t, null, num) : null;
         } else if (s.length == 3) {
            EnumKnowledgeType t = EnumKnowledgeType.valueOf(s[0].toUpperCase());
            ResearchCategory f = ResearchCategories.getResearchCategory(s[1].toUpperCase());
            int num = Integer.parseInt(s[2]);
            return f != null && num > 0 ? new Knowledge(t, f, num) : null;
         }
      } catch (IllegalArgumentException ignored) {
      }
      return null;
   }
}
