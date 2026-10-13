package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Base of the Thaumcraft potions (common/lib/potions + api/potions). The 1.12 icons came from textures/misc/potions.png;
 * in 1.21 each effect has its own sprite at textures/mob_effect/&lt;id&gt;.png, cut from that sheet by tools/gen_marco12.py.
 * Effects without behaviour of their own (vis exhaust, warp ward, death gaze, blurred vision) are plain instances: other
 * code checks for them.
 */
public class TCMobEffect extends MobEffect {
   public TCMobEffect(MobEffectCategory category, int color) {
      super(category, color);
   }
}
