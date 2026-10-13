package thaumcraft.api.research;

/** IPlayerKnowledge.EnumKnowledgeType: theory comes from the research table, observation from scanning. */
public enum EnumKnowledgeType {
   THEORY(32, true, "T"),
   OBSERVATION(16, true, "O");

   private final int progression;
   private final boolean hasFields;
   private final String abbr;

   EnumKnowledgeType(int progression, boolean hasFields, String abbr) {
      this.progression = progression;
      this.hasFields = hasFields;
      this.abbr = abbr;
   }

   /** Raw points per full knowledge point. */
   public int getProgression() {
      return progression;
   }

   public boolean hasFields() {
      return hasFields;
   }

   public String getAbbreviation() {
      return abbr;
   }
}
