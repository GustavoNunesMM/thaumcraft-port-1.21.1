package thaumcraft.api.casters;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** A setting of a focus node (power, radius, options...), edited in the focal manipulator. */
public class NodeSetting {
   int value;
   public String key;
   String description;
   INodeSettingType type;
   String research;

   public NodeSetting(String key, String description, INodeSettingType setting, String research) {
      this.key = key;
      this.type = setting;
      this.value = setting.getDefault();
      this.description = description;
      this.research = research;
   }

   public NodeSetting(String key, String description, INodeSettingType setting) {
      this(key, description, setting, null);
   }

   public int getValue() {
      return type.getValue(value);
   }

   /** Sets the raw index that gives this value. */
   public void setValue(int truevalue) {
      int lv = -1;
      value = 0;
      while (getValue() != truevalue && lv != value) {
         lv = value;
         increment();
      }
   }

   public String getResearch() {
      return research;
   }

   public Component getValueText() {
      return Component.translatable(type.getValueText(value));
   }

   public void increment() {
      value = type.clamp(value + 1);
   }

   public void decrement() {
      value = type.clamp(value - 1);
   }

   public INodeSettingType getType() {
      return type;
   }

   public Component getLocalizedName() {
      return Component.translatable(description);
   }

   public interface INodeSettingType {
      int getDefault();

      int clamp(int old);

      int getValue(int index);

      String getValueText(int index);
   }

   public static class NodeSettingIntList implements INodeSettingType {
      final int[] values;
      final String[] descriptions;

      public NodeSettingIntList(int[] values, String[] descriptions) {
         this.values = values;
         this.descriptions = descriptions;
      }

      @Override
      public int getDefault() {
         return 0;
      }

      @Override
      public int clamp(int old) {
         return Mth.clamp(old, 0, values.length - 1);
      }

      @Override
      public int getValue(int value) {
         return values[clamp(value)];
      }

      @Override
      public String getValueText(int value) {
         return descriptions[clamp(value)];
      }
   }

   public static class NodeSettingIntRange implements INodeSettingType {
      final int min;
      final int max;

      public NodeSettingIntRange(int min, int max) {
         this.min = min;
         this.max = max;
      }

      @Override
      public int getDefault() {
         return min;
      }

      @Override
      public int clamp(int old) {
         return Mth.clamp(old, min, max);
      }

      @Override
      public int getValue(int value) {
         return clamp(value);
      }

      @Override
      public String getValueText(int value) {
         return "" + getValue(value);
      }
   }
}
