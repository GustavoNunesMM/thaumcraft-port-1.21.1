package thaumcraft.common.blocks.crafting;

import java.util.List;
import java.util.Random;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.casters.ICaster;
import thaumcraft.common.blocks.BlockTCTile;
import thaumcraft.common.entities.EntitySpecialItem;
import thaumcraft.common.tiles.crafting.TileCrucible;

public class BlockCrucible extends BlockTCTile {
   private int delay = 0;
   protected static final AxisAlignedBB AABB_LEGS = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.3125, 1.0);
   protected static final AxisAlignedBB AABB_WALL_NORTH = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 0.125);
   protected static final AxisAlignedBB AABB_WALL_SOUTH = new AxisAlignedBB(0.0, 0.0, 0.875, 1.0, 1.0, 1.0);
   protected static final AxisAlignedBB AABB_WALL_EAST = new AxisAlignedBB(0.875, 0.0, 0.0, 1.0, 1.0, 1.0);
   protected static final AxisAlignedBB AABB_WALL_WEST = new AxisAlignedBB(0.0, 0.0, 0.0, 0.125, 1.0, 1.0);

   public BlockCrucible() {
      super(Material.IRON, TileCrucible.class, "crucible");
      this.setSoundType(SoundType.METAL);
   }

   public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
      if (!world.isRemote) {
         TileCrucible tile = (TileCrucible)world.getTileEntity(pos);
         if (tile != null && entity instanceof EntityItem && !(entity instanceof EntitySpecialItem) && tile.heat > 150 && tile.tank.getFluidAmount() > 0) {
            tile.attemptSmelt((EntityItem)entity);
         } else {
            this.delay++;
            if (this.delay < 10) {
               return;
            }

            this.delay = 0;
            if (entity instanceof EntityLivingBase && tile != null && tile.heat > 150 && tile.tank.getFluidAmount() > 0) {
               entity.attackEntityFrom(DamageSource.IN_FIRE, 1.0F);
               world.playSound(
                  null,
                  pos.getX() + 0.5,
                  pos.getY() + 0.5,
                  pos.getZ() + 0.5,
                  SoundEvents.BLOCK_LAVA_EXTINGUISH,
                  SoundCategory.BLOCKS,
                  0.4F,
                  2.0F + world.rand.nextFloat() * 0.4F
               );
            }
         }
      }

      super.onEntityCollision(world, pos, state, entity);
   }

   public void addCollisionBoxToList(
      IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB AABB, List<AxisAlignedBB> list, Entity entityIn, boolean isActualState
   ) {
      addCollisionBoxToList(pos, AABB, list, AABB_LEGS);
      addCollisionBoxToList(pos, AABB, list, AABB_WALL_WEST);
      addCollisionBoxToList(pos, AABB, list, AABB_WALL_NORTH);
      addCollisionBoxToList(pos, AABB, list, AABB_WALL_EAST);
      addCollisionBoxToList(pos, AABB, list, AABB_WALL_SOUTH);
   }

   public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
      return FULL_BLOCK_AABB;
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   @Override
   public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
      TileEntity te = worldIn.getTileEntity(pos);
      if (te != null && te instanceof TileCrucible) {
         ((TileCrucible)te).spillRemnants();
      }

      super.breakBlock(worldIn, pos, state);
   }

   public boolean onBlockActivated(
      World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ
   ) {
      if (world.isRemote) {
         return true;
      } else {
         FluidStack fs = FluidUtil.getFluidContained(player.getHeldItem(hand));
         if (fs != null && fs.containsFluid(new FluidStack(FluidRegistry.WATER, 1000))) {
            TileEntity te = world.getTileEntity(pos);
            if (te != null && te instanceof TileCrucible) {
               TileCrucible tile = (TileCrucible)te;
               if (tile.tank.getFluidAmount() < tile.tank.getCapacity()) {
                  if (FluidUtil.interactWithFluidHandler(player, hand, tile.tank)) {
                     player.inventoryContainer.detectAndSendChanges();
                     te.markDirty();
                     world.markAndNotifyBlock(pos, world.getChunk(pos), state, state, 3);
                     world.playSound(
                        null,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        SoundEvents.ITEM_BOTTLE_FILL,
                        SoundCategory.BLOCKS,
                        0.33F,
                        1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.3F
                     );
                  }

                  return true;
               }
            }
         } else if (!player.isSneaking() && !(player.getHeldItem(hand).getItem() instanceof ICaster) && side == EnumFacing.UP) {
            TileEntity te = world.getTileEntity(pos);
            if (te != null && te instanceof TileCrucible) {
               TileCrucible tile = (TileCrucible)te;
               ItemStack ti = player.getHeldItem(hand).copy();
               ti.setCount(1);
               if (tile.heat > 150 && tile.tank.getFluidAmount() > 0 && tile.attemptSmelt(ti, player.getName()) == null) {
                  player.inventory.decrStackSize(player.inventory.currentItem, 1);
                  return true;
               }
            }
         } else if (player.getHeldItem(hand).isEmpty() && player.isSneaking()) {
            TileEntity te = world.getTileEntity(pos);
            if (te != null && te instanceof TileCrucible) {
               TileCrucible tile = (TileCrucible)te;
               tile.spillRemnants();
               return true;
            }
         }

         return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
      }
   }

   public boolean hasComparatorInputOverride(IBlockState state) {
      return true;
   }

   public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
      TileEntity te = world.getTileEntity(pos);
      if (te != null && te instanceof TileCrucible) {
         float r = ((TileCrucible)te).aspects.visSize() / 500.0F;
         return MathHelper.floor(r * 14.0F) + (((TileCrucible)te).aspects.visSize() > 0 ? 1 : 0);
      } else {
         return 0;
      }
   }

   @SideOnly(Side.CLIENT)
   public void randomDisplayTick(IBlockState state, World w, BlockPos pos, Random r) {
      if (r.nextInt(10) == 0) {
         TileEntity te = w.getTileEntity(pos);
         if (te != null && te instanceof TileCrucible && ((TileCrucible)te).tank.getFluidAmount() > 0 && ((TileCrucible)te).heat > 150) {
            w.playSound(
               pos.getX(),
               pos.getY(),
               pos.getZ(),
               SoundEvents.BLOCK_LAVA_POP,
               SoundCategory.BLOCKS,
               0.1F + r.nextFloat() * 0.1F,
               1.2F + r.nextFloat() * 0.2F,
               false
            );
         }
      }
   }
}
