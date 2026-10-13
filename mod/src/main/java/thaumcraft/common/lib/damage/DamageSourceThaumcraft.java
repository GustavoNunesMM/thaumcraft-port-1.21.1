package thaumcraft.common.lib.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import thaumcraft.Thaumcraft;

/**
 * DamageSourceThaumcraft (the part milestone 12 needs): taint (bypasses armor, magic) and dissolve (liquid death,
 * bypasses armor). The damage types are data driven (data/thaumcraft/damage_type, tags from tools/gen_marco12.py).
 */
public final class DamageSourceThaumcraft {
   public static final ResourceKey<DamageType> TAINT = key("taint");
   public static final ResourceKey<DamageType> DISSOLVE = key("dissolve");

   private DamageSourceThaumcraft() {
   }

   private static ResourceKey<DamageType> key(String name) {
      return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, name));
   }

   public static DamageSource taint(Level level) {
      return level.damageSources().source(TAINT);
   }

   public static DamageSource dissolve(Level level) {
      return level.damageSources().source(DISSOLVE);
   }
}
