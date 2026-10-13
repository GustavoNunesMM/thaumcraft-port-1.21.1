package thaumcraft.common.registers;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;
import thaumcraft.common.world.features.GreatwoodTreeFeature;
import thaumcraft.common.world.features.PlantPatchFeature;
import thaumcraft.common.world.features.SilverwoodTreeFeature;
import thaumcraft.common.world.features.VisCrystalFeature;

/** World generation feature types. Where and how often they generate is data: data/thaumcraft/worldgen + neoforge/biome_modifier. */
public final class ModFeatures {
   public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Thaumcraft.MODID);

   public static final Supplier<Feature<NoneFeatureConfiguration>> GREATWOOD_TREE = FEATURES.register("greatwood_tree", GreatwoodTreeFeature::new);
   public static final Supplier<Feature<NoneFeatureConfiguration>> SILVERWOOD_TREE = FEATURES.register("silverwood_tree", SilverwoodTreeFeature::new);
   public static final Supplier<Feature<NoneFeatureConfiguration>> VIS_CRYSTALS = FEATURES.register("vis_crystals", VisCrystalFeature::new);
   public static final Supplier<Feature<SimpleBlockConfiguration>> PLANT_PATCH = FEATURES.register("plant_patch", PlantPatchFeature::new);

   private ModFeatures() {
   }
}
