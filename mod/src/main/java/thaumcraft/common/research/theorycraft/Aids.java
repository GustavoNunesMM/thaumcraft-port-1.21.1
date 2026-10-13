package thaumcraft.common.research.theorycraft;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.TheorycraftCard;
import thaumcraft.api.research.theorycraft.TheorycraftManager;

/** The research aids of ConfigResearch.initTheorycraft. Aids whose block isn't ported yet simply never match. */
public final class Aids {
   private Aids() {
   }

   private record Aid(Supplier<Object> object, List<String> cards) implements ITheorycraftAid {
      @Override
      public Object getAidObject() {
         return object.get();
      }

      @Override
      public List<Supplier<TheorycraftCard>> getCards() {
         List<Supplier<TheorycraftCard>> out = new ArrayList<>();
         for (String c : cards) {
            Supplier<TheorycraftCard> s = TheorycraftManager.CARDS.get(c);
            if (s != null) out.add(s);
         }
         return out;
      }
   }

   private static Supplier<Object> block(String id) {
      return () -> {
         Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
         return b == Blocks.AIR ? null : b;
      };
   }

   private static void aid(String key, Supplier<Object> object, String... cards) {
      TheorycraftManager.registerAid(key, new Aid(object, List.of(cards)));
   }

   static void register() {
      aid("Bookshelf", () -> Blocks.BOOKSHELF, "Balance", "Notation", "Notation", "Study", "Study", "Study");
      aid("BrainInAJar", block("thaumcraft:jar_brain"), "DarkWhispers");
      aid("GlyphedStone", () -> {
         Object b = block("thaumcraft:stone_ancient_glyphed").get();
         return b == null ? null : new ItemStack((Block) b);
      }, "Glyphs");
      aid("PortalEnd", () -> Blocks.END_PORTAL, "Portal");
      aid("PortalNether", () -> Blocks.NETHER_PORTAL, "Portal");
      // AidPortalCrimson (the lesser cultist portal) comes with the cultists.
      aid("BasicAlchemy", block("thaumcraft:crucible"), "Concentrate", "Reactions", "Synthesis");
      aid("BasicArtifice", block("thaumcraft:arcane_workbench"), "Calibrate", "Tinker", "MindOverMatter");
      aid("BasicInfusion", block("thaumcraft:infusion_matrix"), "Measure", "Channel", "Infuse");
      aid("BasicAuromancy", block("thaumcraft:wand_workbench"), "Focus", "Awareness", "Spellbinding");
      aid("BasicGolemancy", block("thaumcraft:golem_builder"), "Sculpting", "Scripting", "Synergy");
      aid("BasicEldritch", block("thaumcraft:eldritch"), "Realization", "Revelation", "Truth");
      aid("EnchantmentTable", () -> new ItemStack(Blocks.ENCHANTING_TABLE), "Enchantment");
      aid("Beacon", () -> new ItemStack(Blocks.BEACON), "Beacon");
      aid("DragonEgg", () -> new ItemStack(Blocks.DRAGON_EGG), "DragonEgg");
   }
}
