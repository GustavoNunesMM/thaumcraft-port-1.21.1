package thaumcraft.common.blocks.misc;

import java.util.Random;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.potions.PotionWarpWard;

public class BlockFluidPure extends BlockFluidClassic {
   public static final Material FLUID_PURE_MATERIAL = new MaterialLiquid(MapColor.SILVER);

   public BlockFluidPure() {
      super(ConfigBlocks.FluidPure.instance, FLUID_PURE_MATERIAL);
      this.setRegistryName("purifying_fluid");
      this.setTranslationKey("purifying_fluid");
      this.setCreativeTab(ConfigItems.TABTC);
   }

   public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
      entity.motionX = entity.motionX * (1.0F - this.getQuantaPercentage(world, pos) / 2.0F);
      entity.motionZ = entity.motionZ * (1.0F - this.getQuantaPercentage(world, pos) / 2.0F);
      if (!world.isRemote
         && this.isSourceBlock(world, pos)
         && entity instanceof EntityPlayer
         && !((EntityPlayer)entity).isPotionActive(PotionWarpWard.instance)) {
         int warp = ThaumcraftCapabilities.getWarp((EntityPlayer)entity).get(IPlayerWarp.EnumWarpType.PERMANENT);
         int div = 1;
         if (warp > 0) {
            div = (int)Math.sqrt(warp);
            if (div < 1) {
               div = 1;
            }
         }

         ((EntityPlayer)entity).addPotionEffect(new PotionEffect(PotionWarpWard.instance, Math.min(32000, 200000 / div), 0, true, true));
         world.setBlockToAir(pos);
      }
   }

   @SideOnly(Side.CLIENT)
   public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
      int meta = this.getMetaFromState(state);
      if (rand.nextInt(10) == 0) {
         FXGeneric fb = new FXGeneric(
            world, pos.getX() + rand.nextFloat(), pos.getY() + 0.125F * (8 - meta), pos.getZ() + rand.nextFloat(), 0.0, 0.0, 0.0
         );
         fb.setMaxAge(10 + world.rand.nextInt(10));
         fb.setScale(world.rand.nextFloat() * 0.3F + 0.3F);
         fb.setRBGColorF(1.0F, 1.0F, 1.0F);
         fb.setRandomMovementScale(0.001F, 0.001F, 0.001F);
         fb.setGravity(-0.01F);
         fb.setAlphaF(0.25F);
         fb.setParticle(64);
         fb.setFinalFrames(65, 66);
         ParticleEngine.addEffect(world, fb);
      }

      if (rand.nextInt(50) == 0) {
         double var21 = pos.getX() + rand.nextFloat();
         double var22 = pos.getY() + 0.5;
         double var23 = pos.getZ() + rand.nextFloat();
         world.playSound(
            var21, var22, var23, SoundEvents.BLOCK_LAVA_POP, SoundCategory.BLOCKS, 0.1F + rand.nextFloat() * 0.1F, 0.9F + rand.nextFloat() * 0.15F, false
         );
      }
   }
}
