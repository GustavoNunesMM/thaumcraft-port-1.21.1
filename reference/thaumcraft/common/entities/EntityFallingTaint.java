package thaumcraft.common.entities;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.blocks.world.taint.BlockTaint;
import thaumcraft.common.lib.SoundsTC;

public class EntityFallingTaint extends Entity implements IEntityAdditionalSpawnData {
   public IBlockState fallTile;
   BlockPos oldPos;
   public int fallTime = 0;
   private int fallHurtMax = 40;
   private float fallHurtAmount = 2.0F;

   public IBlockState getBlock() {
      return this.fallTile;
   }

   public EntityFallingTaint(World par1World) {
      super(par1World);
   }

   public EntityFallingTaint(World par1World, double par2, double par4, double par6, IBlockState par8, BlockPos o) {
      super(par1World);
      this.fallTile = par8;
      this.preventEntitySpawning = true;
      this.setSize(0.98F, 0.98F);
      this.setPosition(par2, par4, par6);
      this.motionX = 0.0;
      this.motionY = 0.0;
      this.motionZ = 0.0;
      this.prevPosX = par2;
      this.prevPosY = par4;
      this.prevPosZ = par6;
      this.oldPos = o;
   }

   protected boolean canTriggerWalking() {
      return false;
   }

   protected void entityInit() {
   }

   public void writeSpawnData(ByteBuf data) {
      data.writeInt(Block.getIdFromBlock(this.fallTile.getBlock()));
      data.writeByte(this.fallTile.getBlock().getMetaFromState(this.fallTile));
   }

   public void readSpawnData(ByteBuf data) {
      try {
         this.fallTile = Block.getBlockById(data.readInt()).getStateFromMeta(data.readByte());
      } catch (Exception var3) {
      }
   }

   public boolean canBeCollidedWith() {
      return !this.isDead;
   }

   public void onUpdate() {
      if (this.fallTile != null && this.fallTile != Blocks.AIR) {
         this.prevPosX = this.posX;
         this.prevPosY = this.posY;
         this.prevPosZ = this.posZ;
         this.fallTime++;
         this.motionY -= 0.04F;
         this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
         this.motionX *= 0.98F;
         this.motionY *= 0.98F;
         this.motionZ *= 0.98F;
         BlockPos bp = new BlockPos(this);
         if (!this.world.isRemote) {
            if (this.fallTime == 1) {
               if (this.world.getBlockState(this.oldPos) != this.fallTile) {
                  this.setDead();
                  return;
               }

               this.world.setBlockToAir(this.oldPos);
            }

            if (!this.onGround && this.world.getBlockState(bp.down()) != BlocksTC.fluxGoo) {
               if (this.fallTime > 100 && !this.world.isRemote && (bp.getY() < 1 || bp.getY() > 256) || this.fallTime > 600) {
                  this.setDead();
               }
            } else {
               this.motionX *= 0.7F;
               this.motionZ *= 0.7F;
               this.motionY *= -0.5;
               if (this.world.getBlockState(bp).getBlock() != Blocks.PISTON
                  && this.world.getBlockState(bp).getBlock() != Blocks.PISTON_EXTENSION
                  && this.world.getBlockState(bp).getBlock() != Blocks.PISTON_HEAD) {
                  this.playSound(SoundsTC.gore, 0.5F, ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F) * 0.8F);
                  this.setDead();
                  if (this.canPlace(bp)
                     && !BlockTaint.canFallBelow(this.world, bp.down())
                     && this.world.setBlockState(bp, this.fallTile)) {
                  }
               }
            }
         } else if (this.onGround || this.fallTime == 1) {
            for (int j = 0; j < 10; j++) {
               FXDispatcher.INSTANCE.taintLandFX(this);
            }
         }
      } else {
         this.setDead();
      }
   }

   private boolean canPlace(BlockPos pos) {
      return this.world.getBlockState(pos).getBlock() == BlocksTC.taintFibre
         || this.world.getBlockState(pos).getBlock() == BlocksTC.fluxGoo
         || this.world.mayPlace(this.fallTile.getBlock(), pos, true, EnumFacing.UP, (Entity)null);
   }

   public void fall(float distance, float damageMultiplier) {
   }

   protected void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      Block block = this.fallTile != null ? this.fallTile.getBlock() : Blocks.AIR;
      ResourceLocation resourcelocation = (ResourceLocation)Block.REGISTRY.getNameForObject(block);
      par1NBTTagCompound.setString("Block", resourcelocation == null ? "" : resourcelocation.toString());
      par1NBTTagCompound.setByte("Data", (byte)block.getMetaFromState(this.fallTile));
      par1NBTTagCompound.setByte("Time", (byte)this.fallTime);
      par1NBTTagCompound.setFloat("FallHurtAmount", this.fallHurtAmount);
      par1NBTTagCompound.setInteger("FallHurtMax", this.fallHurtMax);
      par1NBTTagCompound.setLong("Old", this.oldPos.toLong());
   }

   protected void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      int i = par1NBTTagCompound.getByte("Data") & 255;
      if (par1NBTTagCompound.hasKey("Block", 8)) {
         this.fallTile = Block.getBlockFromName(par1NBTTagCompound.getString("Block")).getStateFromMeta(i);
      } else if (par1NBTTagCompound.hasKey("TileID", 99)) {
         this.fallTile = Block.getBlockById(par1NBTTagCompound.getInteger("TileID")).getStateFromMeta(i);
      } else {
         this.fallTile = Block.getBlockById(par1NBTTagCompound.getByte("Tile") & 255).getStateFromMeta(i);
      }

      this.fallTime = par1NBTTagCompound.getByte("Time") & 255;
      this.oldPos = BlockPos.fromLong(par1NBTTagCompound.getLong("Old"));
      if (par1NBTTagCompound.hasKey("HurtEntities")) {
         this.fallHurtAmount = par1NBTTagCompound.getFloat("FallHurtAmount");
         this.fallHurtMax = par1NBTTagCompound.getInteger("FallHurtMax");
      }

      if (this.fallTile == null) {
         this.fallTile = Blocks.SAND.getDefaultState();
      }
   }

   public void addEntityCrashInfo(CrashReportCategory par1CrashReportCategory) {
      super.addEntityCrashInfo(par1CrashReportCategory);
      par1CrashReportCategory.addCrashSection("Immitating block ID", Block.getIdFromBlock(this.fallTile.getBlock()));
      par1CrashReportCategory.addCrashSection("Immitating block data", this.fallTile.getBlock().getMetaFromState(this.fallTile));
   }

   public SoundCategory getSoundCategory() {
      return SoundCategory.BLOCKS;
   }

   @SideOnly(Side.CLIENT)
   public World getWorld() {
      return this.world;
   }

   @SideOnly(Side.CLIENT)
   public boolean canRenderOnFire() {
      return false;
   }
}
