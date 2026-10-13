package thaumcraft.common.blocks.world;

import java.util.Random;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.config.ConfigItems;

public class BlockGrassAmbient extends BlockGrass {
   public BlockGrassAmbient() {
      this.setTranslationKey("grass_ambient");
      this.setRegistryName("thaumcraft", "grass_ambient");
      this.setCreativeTab(ConfigItems.TABTC);
      this.setHardness(0.6F);
      this.setSoundType(SoundType.GROUND);
   }

   @SideOnly(Side.CLIENT)
   public void randomDisplayTick(IBlockState state, World worldIn, BlockPos pos, Random rand) {
      Biome biome = worldIn.getBiome(pos);
      int i = worldIn.getLightFor(EnumSkyBlock.SKY, pos.up()) - worldIn.getSkylightSubtracted();
      float f = worldIn.getCelestialAngleRadians(1.0F);
      float f1 = f < (float) Math.PI ? 0.0F : (float) (Math.PI * 2);
      f += (f1 - f) * 0.2F;
      i = Math.round(i * MathHelper.cos(f));
      i = MathHelper.clamp(i, 0, 15);
      if (4 + i * 2 < 1 + rand.nextInt(13)) {
         int x = MathHelper.getInt(rand, -8, 8);
         int z = MathHelper.getInt(rand, -8, 8);
         BlockPos pp = pos.add(x, 5, z);

         for (int q = 0; q < 10 && pp.getY() > 50 && worldIn.getBlockState(pp).getBlock() != Blocks.GRASS; q++) {
            pp = pp.down();
         }

         if (worldIn.getBlockState(pp).getBlock() == Blocks.GRASS) {
            FXDispatcher.INSTANCE.drawWispyMotesOnBlock(pp.up(), 400, -0.01F);
         }
      }
   }
}
