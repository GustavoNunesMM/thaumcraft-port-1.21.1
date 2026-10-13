package thaumcraft.common.blocks.misc;

import java.util.Random;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.fx.particles.FXSlimyBubble;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;

public class BlockFluidDeath extends BlockFluidClassic {
   public static final Material FLUID_DEATH_MATERIAL = new MaterialLiquid(MapColor.PURPLE);

   public BlockFluidDeath() {
      super(ConfigBlocks.FluidDeath.instance, FLUID_DEATH_MATERIAL);
      this.setRegistryName("liquid_death");
      this.setTranslationKey("liquid_death");
      this.setCreativeTab(ConfigItems.TABTC);
      this.setQuantaPerBlock(4);
   }

   public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
      entity.motionX = entity.motionX * (1.0F - this.getQuantaPercentage(world, pos) / 2.0F);
      entity.motionZ = entity.motionZ * (1.0F - this.getQuantaPercentage(world, pos) / 2.0F);
      if (!world.isRemote && entity instanceof EntityLivingBase) {
         entity.attackEntityFrom(DamageSourceThaumcraft.dissolve, 4 - this.getMetaFromState(state) + 1);
      }
   }

   public int getQuanta() {
      return this.quantaPerBlock;
   }

   @SideOnly(Side.CLIENT)
   public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
      if (rand.nextInt(20) == 0) {
         int meta = this.getMetaFromState(state);
         float h = rand.nextFloat() * 0.075F;
         FXSlimyBubble ef = new FXSlimyBubble(
            world, pos.getX() + rand.nextFloat(), pos.getY() + 0.1F + 0.225F * (4 - meta), pos.getZ() + rand.nextFloat(), 0.075F + h
         );
         ef.setAlphaF(0.8F);
         ef.setRBGColorF(0.3F - rand.nextFloat() * 0.1F, 0.0F, 0.4F + rand.nextFloat() * 0.1F);
         ParticleEngine.addEffect(world, ef);
      }

      if (rand.nextInt(50) == 0) {
         double var21 = pos.getX() + rand.nextFloat();
         double var22 = (double)pos.getY() + this.getMaxRenderHeightMeta() / 4.0F;
         double var23 = pos.getZ() + rand.nextFloat();
         world.playSound(
            var21, var22, var23, SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS, 0.1F + rand.nextFloat() * 0.1F, 0.9F + rand.nextFloat() * 0.15F, false
         );
      }
   }
}
