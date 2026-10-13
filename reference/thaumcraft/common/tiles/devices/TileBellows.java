package thaumcraft.common.tiles.devices;

import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.common.lib.utils.BlockStateUtils;
import thaumcraft.common.tiles.TileThaumcraft;

public class TileBellows extends TileThaumcraft implements ITickable {
   public float inflation = 1.0F;
   boolean direction = false;
   boolean firstrun = true;
   public int delay = 0;

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getRenderBoundingBox() {
      return new AxisAlignedBB(
         this.getPos().getX() - 0.3,
         this.getPos().getY() - 0.3,
         this.getPos().getZ() - 0.3,
         this.getPos().getX() + 1.3,
         this.getPos().getY() + 1.3,
         this.getPos().getZ() + 1.3
      );
   }

   public void update() {
      if (this.world.isRemote) {
         if (BlockStateUtils.isEnabled(this.getBlockMetadata())) {
            if (this.firstrun) {
               this.inflation = 0.35F + this.world.rand.nextFloat() * 0.55F;
            }

            this.firstrun = false;
            if (this.inflation > 0.35F && !this.direction) {
               this.inflation -= 0.075F;
            }

            if (this.inflation <= 0.35F && !this.direction) {
               this.direction = true;
            }

            if (this.inflation < 1.0F && this.direction) {
               this.inflation += 0.025F;
            }

            if (this.inflation >= 1.0F && this.direction) {
               this.direction = false;
               this.world
                  .playSound(
                     this.pos.getX(),
                     this.pos.getY(),
                     this.pos.getZ(),
                     SoundEvents.ENTITY_GHAST_SHOOT,
                     SoundCategory.BLOCKS,
                     0.01F,
                     0.5F + (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F,
                     false
                  );
            }
         }
      } else if (BlockStateUtils.isEnabled(this.getBlockMetadata())) {
         this.delay++;
         if (this.delay >= 2) {
            this.delay = 0;
            TileEntity tile = this.world.getTileEntity(this.pos.offset(BlockStateUtils.getFacing(this.getBlockMetadata())));
            if (tile != null && tile instanceof TileEntityFurnace) {
               TileEntityFurnace tf = (TileEntityFurnace)tile;
               int ct = this.getCooktime(tf);
               if (ct > 0 && ct < 199) {
                  this.setCooktime(tf, ct + 1);
               }
            }
         }
      }
   }

   public void setCooktime(TileEntityFurnace ent, int hit) {
      ent.cookTime = hit;
   }

   public int getCooktime(TileEntityFurnace ent) {
      return ent.cookTime;
   }

   public static int getBellows(World world, BlockPos pos, EnumFacing[] directions) {
      int bellows = 0;

      for (EnumFacing dir : directions) {
         TileEntity tile = world.getTileEntity(pos.offset(dir));

         try {
            if (tile != null
               && tile instanceof TileBellows
               && BlockStateUtils.getFacing(tile.getBlockMetadata()) == dir.getOpposite()
               && BlockStateUtils.isEnabled(tile.getBlockMetadata())) {
               bellows++;
            }
         } catch (Exception var10) {
         }
      }

      return bellows;
   }

   public boolean canRenderBreaking() {
      return true;
   }
}
