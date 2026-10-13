package thaumcraft.common.blocks.misc;

import java.util.Random;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.blocks.BlockTC;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.events.ServerEvents;

public class BlockEffect extends BlockTC {
   public BlockEffect(String name) {
      super(Material.AIR, name);
      this.setTickRandomly(true);
      this.blockResistance = 999.0F;
      this.setLightLevel(0.5F);
   }

   public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
      if (state.getBlock() != this) {
         return super.getLightValue(state, world, pos);
      } else {
         return state.getBlock() == BlocksTC.effectGlimmer ? 15 : super.getLightValue(state, world, pos);
      }
   }

   public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
      return BlockFaceShape.UNDEFINED;
   }

   public void onEntityCollision(World world, BlockPos pos, IBlockState state, final Entity entity) {
      if (state.getBlock() == BlocksTC.effectShock) {
         if (entity instanceof EntityLivingBase) {
            ServerEvents.addRunnableServer(world, new Runnable() {
               @Override
               public void run() {
                  entity.attackEntityFrom(DamageSource.MAGIC, 1.0F);
                  PotionEffect pe = new PotionEffect(MobEffects.SLOWNESS, 20, 0, true, true);
                  ((EntityLivingBase)entity).addPotionEffect(pe);
               }
            }, 0);
         }

         if (!world.isRemote && world.rand.nextInt(100) == 0) {
            world.setBlockToAir(pos);
         }
      } else if (state.getBlock() == BlocksTC.effectSap
         && !(entity instanceof IEldritchMob)
         && entity instanceof EntityLivingBase
         && !((EntityLivingBase)entity).isPotionActive(MobEffects.WITHER)) {
         ServerEvents.addRunnableServer(world, new Runnable() {
            @Override
            public void run() {
               PotionEffect pe0 = new PotionEffect(MobEffects.WITHER, 40, 0, true, true);
               ((EntityLivingBase)entity).addPotionEffect(pe0);
               PotionEffect pe1 = new PotionEffect(MobEffects.SLOWNESS, 40, 1, true, true);
               ((EntityLivingBase)entity).addPotionEffect(pe1);
               PotionEffect pe2 = new PotionEffect(MobEffects.HUNGER, 40, 1, true, true);
               ((EntityLivingBase)entity).addPotionEffect(pe2);
            }
         }, 0);
      }
   }

   public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
      super.updateTick(worldIn, pos, state, rand);
      if (!worldIn.isRemote && state.getBlock() != BlocksTC.effectGlimmer) {
         worldIn.setBlockToAir(pos);
      }
   }

   @SideOnly(Side.CLIENT)
   public void randomDisplayTick(IBlockState state, World w, BlockPos pos, Random r) {
      if (state.getBlock() != BlocksTC.effectGlimmer) {
         float h = r.nextFloat() * 0.33F;
         if (state.getBlock() == BlocksTC.effectShock) {
            FXDispatcher.INSTANCE
               .spark(
                  pos.getX() + w.rand.nextFloat(),
                  pos.getY() + 0.1515F + h / 2.0F,
                  pos.getZ() + w.rand.nextFloat(),
                  3.0F + h * 6.0F,
                  0.65F + w.rand.nextFloat() * 0.1F,
                  1.0F,
                  1.0F,
                  0.8F
               );
         } else {
            FXDispatcher.INSTANCE
               .spark(
                  pos.getX() + w.rand.nextFloat(),
                  pos.getY() + 0.1515F + h / 2.0F,
                  pos.getZ() + w.rand.nextFloat(),
                  3.0F + h * 6.0F,
                  0.3F - w.rand.nextFloat() * 0.1F,
                  0.0F,
                  0.5F + w.rand.nextFloat() * 0.2F,
                  1.0F
               );
         }

         if (r.nextInt(50) == 0) {
            w.playSound(
               pos.getX(),
               pos.getY(),
               pos.getZ(),
               SoundsTC.jacobs,
               SoundCategory.AMBIENT,
               0.25F,
               1.0F + (r.nextFloat() - r.nextFloat()) * 0.2F,
               false
            );
         }
      }
   }

   public boolean isAir(IBlockState state, IBlockAccess world, BlockPos pos) {
      return true;
   }

   public EnumBlockRenderType getRenderType(IBlockState state) {
      return EnumBlockRenderType.INVISIBLE;
   }

   public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
      return true;
   }

   public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
      return ItemStack.EMPTY;
   }

   public boolean isSideSolid(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing o) {
      return false;
   }

   public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
      return true;
   }

   public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
      return null;
   }

   public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
      return false;
   }

   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      return new AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   public Item getItemDropped(IBlockState state, Random rand, int fortune) {
      return Item.getItemById(0);
   }
}
