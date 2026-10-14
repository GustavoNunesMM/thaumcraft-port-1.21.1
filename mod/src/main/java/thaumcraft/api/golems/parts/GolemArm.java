package thaumcraft.api.golems.parts;

import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;

public class GolemArm {
   protected static GolemArm[] arms = new GolemArm[0];
   public byte id;
   public String key;
   public String[] research;
   public ResourceLocation icon;
   public Object[] components;
   public EnumGolemTrait[] traits;
   public IArmFunction function;
   public PartModel model;

   public GolemArm(String key, String[] research, ResourceLocation icon, PartModel model, Object[] comp, EnumGolemTrait[] tags) {
      this(key, research, icon, model, comp, null, tags);
   }

   public GolemArm(String key, String[] research, ResourceLocation icon, PartModel model, Object[] comp, IArmFunction function, EnumGolemTrait[] tags) {
      this.key = key;
      this.research = research;
      this.icon = icon;
      this.components = comp;
      this.traits = tags;
      this.model = model;
      this.function = function;
   }

   public static void register(GolemArm thing) {
      thing.id = (byte) arms.length;
      GolemArm[] temp = new GolemArm[arms.length + 1];
      System.arraycopy(arms, 0, temp, 0, arms.length);
      temp[thing.id] = thing;
      arms = temp;
   }

   public Component getLocalizedName() {
      return Component.translatable("golem.arm." + key.toLowerCase(Locale.ROOT));
   }

   public Component getLocalizedDescription() {
      return Component.translatable("golem.arm.text." + key.toLowerCase(Locale.ROOT));
   }

   public static GolemArm[] getArms() {
      return arms;
   }

   public interface IArmFunction extends IGenericFunction {
      void onMeleeAttack(IGolemAPI golem, Entity target);

      void onRangedAttack(IGolemAPI golem, LivingEntity target, float range);

      @Nullable
      Goal getRangedAttackAI(RangedAttackMob golem);
   }
}
