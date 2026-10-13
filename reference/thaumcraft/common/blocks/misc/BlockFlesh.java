package thaumcraft.common.blocks.misc;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import thaumcraft.common.blocks.BlockTC;
import thaumcraft.common.lib.SoundsTC;

public class BlockFlesh extends BlockTC {
   public BlockFlesh() {
      super(Material.SPONGE, "flesh_block");
      this.setResistance(2.0F);
      this.setHardness(0.25F);
   }

   public SoundType getSoundType() {
      return SoundsTC.GORE;
   }
}
