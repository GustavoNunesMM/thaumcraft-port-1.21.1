package thaumcraft.common.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Port of ModConfig (the options the port uses so far), as a NeoForge common config: config/thaumcraft-common.toml. */
public final class ModConfig {
   public static final ModConfigSpec SPEC;
   private static final ModConfigSpec.BooleanValue WUSS_MODE;
   private static final ModConfigSpec.BooleanValue NO_SLEEP;
   private static final ModConfigSpec.BooleanValue ALLOW_CHEAT_SHEET;
   private static final ModConfigSpec.BooleanValue ALLOW_CHAMPION_MOBS;

   static {
      ModConfigSpec.Builder b = new ModConfigSpec.Builder();
      b.push("misc");
      WUSS_MODE = b.comment("Disables warp and flux rifts (research still asks for them, but they never hurt you).")
         .define("wussMode", false);
      NO_SLEEP = b.comment("Get the dream journal when picking up your first vis crystal instead of after sleeping.")
         .define("noSleep", false);
      ALLOW_CHEAT_SHEET = b.comment("Allows the creative-only cheating Thaumonomicon that grants all research.")
         .define("allowCheatSheet", false);
      b.pop();
      b.push("world");
      ALLOW_CHAMPION_MOBS = b.comment("Setting this to false will disable spawning champion mobs. Even when false they will still",
         "have a greatly reduced chance of spawning in certain dangerous places.").define("allowChampionMobs", true);
      b.pop();
      SPEC = b.build();
   }

   private ModConfig() {
   }

   private static boolean get(ModConfigSpec.BooleanValue v, boolean def) {
      return SPEC.isLoaded() ? v.get() : def;
   }

   public static boolean wussMode() {
      return get(WUSS_MODE, false);
   }

   public static boolean noSleep() {
      return get(NO_SLEEP, false);
   }

   public static boolean allowCheatSheet() {
      return get(ALLOW_CHEAT_SHEET, false);
   }

   public static boolean allowChampionMobs() {
      return get(ALLOW_CHAMPION_MOBS, true);
   }
}
