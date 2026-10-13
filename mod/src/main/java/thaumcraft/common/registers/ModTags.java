package thaumcraft.common.registers;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import thaumcraft.Thaumcraft;

public final class ModTags {
   private ModTags() {
   }

   public static final class Blocks {
      public static final TagKey<Block> NITOR = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "nitor"));
   }

   public static final class Items {
      public static final TagKey<Item> NITOR = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "nitor"));
   }
}
