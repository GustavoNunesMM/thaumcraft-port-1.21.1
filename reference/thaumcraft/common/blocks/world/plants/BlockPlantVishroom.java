package thaumcraft.common.blocks.world.plants;

import java.util.Random;
import net.minecraft.block.BlockBush;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.config.ConfigItems;

public class BlockPlantVishroom extends BlockBush {
   public BlockPlantVishroom() {
      super(Material.PLANTS);
      this.setTranslationKey("vishroom");
      this.setRegistryName("thaumcraft", "vishroom");
      this.setCreativeTab(ConfigItems.TABTC);
      this.setSoundType(SoundType.PLANT);
      this.setLightLevel(0.4F);
   }

   public boolean isFullCube(IBlockState state) {
      return state.isFullBlock();
   }

   public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
      if (!worldIn.isRemote && entityIn instanceof EntityLivingBase && worldIn.rand.nextInt(5) == 0) {
         ((EntityLivingBase)entityIn).addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 200, 0));
      }
   }

   public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos) {
      return EnumPlantType.Cave;
   }

   @SideOnly(Side.CLIENT)
   public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
      if (rand.nextInt(3) == 0) {
         float xr = pos.getX() + 0.5F + (rand.nextFloat() - rand.nextFloat()) * 0.4F;
         float yr = pos.getY() + 0.3F;
         float zr = pos.getZ() + 0.5F + (rand.nextFloat() - rand.nextFloat()) * 0.4F;
         FXDispatcher.INSTANCE.drawWispyMotes(xr, yr, zr, 0.0, 0.0, 0.0, 10, 0.5F, 0.3F, 0.8F, 0.001F);
      }
   }
}
