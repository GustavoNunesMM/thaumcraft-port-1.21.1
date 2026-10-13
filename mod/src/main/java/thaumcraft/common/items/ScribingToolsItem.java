package thaumcraft.common.items;

import net.minecraft.world.item.Item;

/** ItemScribingTools (IScribeTools): ink and quill for the research table; 100 uses. */
public class ScribingToolsItem extends Item {
   public ScribingToolsItem(Properties properties) {
      super(properties.stacksTo(1).durability(100));
   }
}
