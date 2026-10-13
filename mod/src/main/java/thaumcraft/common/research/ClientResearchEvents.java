package thaumcraft.common.research;

import java.util.concurrent.ConcurrentLinkedQueue;
import thaumcraft.api.research.EnumKnowledgeType;

/** Client-bound research notifications waiting for the HUD. Plain data, safe to load on a server. */
public final class ClientResearchEvents {
   public record Gain(EnumKnowledgeType type, String category) {}

   public static final ConcurrentLinkedQueue<Gain> KNOWLEDGE_GAINS = new ConcurrentLinkedQueue<>();

   private ClientResearchEvents() {
   }
}
