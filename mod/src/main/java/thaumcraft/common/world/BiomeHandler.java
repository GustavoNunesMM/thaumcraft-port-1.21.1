package thaumcraft.common.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.api.aspects.Aspect;

/**
 * Port of BiomeHandler. The 1.12 Forge BiomeDictionary types map onto the NeoForge {@code c:} biome tags,
 * so modded biomes that use the common tags get the same treatment as in the original.
 */
public final class BiomeHandler {
   public record BiomeInfo(float auraLevel, Aspect aspect, boolean greatwood, float greatwoodChance) {}

   private static final Map<TagKey<Biome>, BiomeInfo> INFO = new LinkedHashMap<>();

   static {
      register(Tags.Biomes.IS_AQUATIC, 0.33F, Aspect.WATER, false, 0.0F);
      register(Tags.Biomes.IS_OCEAN, 0.33F, Aspect.WATER, false, 0.0F);
      register(Tags.Biomes.IS_RIVER, 0.4F, Aspect.WATER, false, 0.0F);
      register(Tags.Biomes.IS_WET, 0.4F, Aspect.WATER, false, 0.0F);
      register(Tags.Biomes.IS_LUSH, 0.5F, Aspect.WATER, true, 0.5F);
      register(Tags.Biomes.IS_HOT, 0.33F, Aspect.FIRE, false, 0.0F);
      // The original registered DRY twice; the second entry (0.125, perditio) is the one that stuck.
      register(Tags.Biomes.IS_DRY, 0.125F, Aspect.ENTROPY, false, 0.0F);
      register(Tags.Biomes.IS_NETHER, 0.125F, Aspect.FIRE, false, 0.0F);
      register(Tags.Biomes.IS_BADLANDS, 0.33F, Aspect.FIRE, false, 0.0F);
      register(Tags.Biomes.IS_SPOOKY, 0.5F, Aspect.FIRE, false, 0.0F);
      register(Tags.Biomes.IS_DENSE_VEGETATION, 0.4F, Aspect.ORDER, false, 0.0F);
      register(Tags.Biomes.IS_SNOWY, 0.25F, Aspect.ORDER, false, 0.0F);
      register(Tags.Biomes.IS_COLD, 0.25F, Aspect.ORDER, false, 0.0F);
      register(Tags.Biomes.IS_MUSHROOM, 0.75F, Aspect.ORDER, false, 0.0F);
      register(Tags.Biomes.IS_MAGICAL, 0.75F, Aspect.ORDER, true, 1.0F);
      register(Tags.Biomes.IS_CONIFEROUS_TREE, 0.33F, Aspect.EARTH, true, 0.2F);
      register(Tags.Biomes.IS_FOREST, 0.5F, Aspect.EARTH, true, 1.0F);
      register(Tags.Biomes.IS_SANDY, 0.25F, Aspect.EARTH, false, 0.0F);
      register(Tags.Biomes.IS_BEACH, 0.3F, Aspect.EARTH, false, 0.0F);
      register(Tags.Biomes.IS_JUNGLE, 0.6F, Aspect.EARTH, false, 0.0F);
      register(Tags.Biomes.IS_SAVANNA, 0.25F, Aspect.AIR, true, 0.2F);
      register(Tags.Biomes.IS_MOUNTAIN, 0.3F, Aspect.AIR, false, 0.0F);
      register(Tags.Biomes.IS_HILL, 0.33F, Aspect.AIR, false, 0.0F);
      register(Tags.Biomes.IS_PLAINS, 0.3F, Aspect.AIR, true, 0.2F);
      register(Tags.Biomes.IS_END, 0.125F, Aspect.AIR, false, 0.0F);
      register(Tags.Biomes.IS_SPARSE_VEGETATION, 0.2F, Aspect.ENTROPY, false, 0.0F);
      register(Tags.Biomes.IS_SWAMP, 0.5F, Aspect.ENTROPY, true, 0.2F);
      register(Tags.Biomes.IS_WASTELAND, 0.125F, Aspect.ENTROPY, false, 0.0F);
      register(Tags.Biomes.IS_DEAD, 0.1F, Aspect.ENTROPY, false, 0.0F);
   }

   private static void register(TagKey<Biome> tag, float aura, Aspect aspect, boolean greatwood, float chance) {
      INFO.put(tag, new BiomeInfo(aura, aspect, greatwood, chance));
   }

   private static List<BiomeInfo> infoFor(Holder<Biome> biome) {
      List<BiomeInfo> out = new ArrayList<>();
      INFO.forEach((tag, info) -> {
         if (biome.is(tag)) out.add(info);
      });
      return out;
   }

   /** Average aura level of the biome's types; 0.5 for biomes with none, as in the original. */
   public static float getBiomeAuraModifier(Holder<Biome> biome) {
      List<BiomeInfo> infos = infoFor(biome);
      if (infos.isEmpty()) return 0.5F;
      float sum = 0;
      for (BiomeInfo i : infos) sum += i.auraLevel();
      return sum / infos.size();
   }

   /** The aspect of one of the biome's types, picked at random; null for biomes without any. */
   @Nullable
   public static Aspect getRandomBiomeTag(Holder<Biome> biome, RandomSource random) {
      List<BiomeInfo> infos = infoFor(biome);
      return infos.isEmpty() ? null : infos.get(random.nextInt(infos.size())).aspect();
   }

   /**
    * Chance (0-1) of a greatwood growing here. The original returned the first greatwood type it found in an
    * unordered set; taking the highest makes the result deterministic.
    */
   public static float getBiomeSupportsGreatwood(Holder<Biome> biome) {
      float best = 0;
      for (BiomeInfo i : infoFor(biome)) {
         if (i.greatwood()) best = Math.max(best, i.greatwoodChance());
      }
      return best;
   }

   private BiomeHandler() {
   }
}
