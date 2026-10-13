package thaumcraft.common.lib;

/** Hooks the client fills in at startup, so common code can open screens without referencing client classes. */
public final class ClientBridge {
   public static Runnable openThaumonomicon = () -> {
   };
   /** Called on the client after its research knowledge was replaced by a sync (shows "research complete" toasts). */
   public static java.util.function.Consumer<thaumcraft.common.research.PlayerKnowledge> knowledgeSynced = k -> {
   };

   /** Client effects sent by the server. */
   public static java.util.function.Consumer<thaumcraft.common.network.BamfPayload> bamf = p -> {
   };

   public static java.util.function.Consumer<thaumcraft.common.network.EssentiaSourcePayload> essentiaSource = p -> {
   };

   public static java.util.function.Consumer<thaumcraft.common.network.InfusionFXPayload> infusionFX = p -> {
   };

   public static java.util.function.Consumer<thaumcraft.common.network.ScanSourcePayload> scanSource = p -> {
   };

   public static java.util.function.Consumer<thaumcraft.common.network.FocusFXPayload> focusFX = p -> {
   };

   private ClientBridge() {
   }
}
