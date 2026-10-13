package thaumcraft.common.entities;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import thaumcraft.client.fx.FXDispatcher;

public class EntityFollowingItem extends EntitySpecialItem implements IEntityAdditionalSpawnData {
   double targetX = 0.0;
   double targetY = 0.0;
   double targetZ = 0.0;
   int type = 3;
   public Entity target = null;
   int age = 20;
   public double gravity = 0.04F;

   public EntityFollowingItem(World par1World, double par2, double par4, double par6, ItemStack par8ItemStack) {
      super(par1World);
      this.setSize(0.25F, 0.25F);
      this.setPosition(par2, par4, par6);
      this.setItem(par8ItemStack);
      this.rotationYaw = (float)(Math.random() * 360.0);
   }

   public EntityFollowingItem(World par1World, double par2, double par4, double par6, ItemStack par8ItemStack, Entity target, int t) {
      this(par1World, par2, par4, par6, par8ItemStack);
      this.target = target;
      this.targetX = target.posX;
      this.targetY = target.getEntityBoundingBox().minY + target.height / 2.0F;
      this.targetZ = target.posZ;
      this.type = t;
      this.noClip = true;
   }

   public EntityFollowingItem(World par1World, double par2, double par4, double par6, ItemStack par8ItemStack, double tx, double ty, double tz) {
      this(par1World, par2, par4, par6, par8ItemStack);
      this.targetX = tx;
      this.targetY = ty;
      this.targetZ = tz;
   }

   public EntityFollowingItem(World par1World) {
      super(par1World);
      this.setSize(0.25F, 0.25F);
   }

   @Override
   public void onUpdate() {
      if (this.target != null) {
         this.targetX = this.target.posX;
         this.targetY = this.target.getEntityBoundingBox().minY + this.target.height / 2.0F;
         this.targetZ = this.target.posZ;
      }

      if (this.targetX == 0.0 && this.targetY == 0.0 && this.targetZ == 0.0) {
         this.motionY = this.motionY - this.gravity;
      } else {
         float xd = (float)(this.targetX - this.posX);
         float yd = (float)(this.targetY - this.posY);
         float zd = (float)(this.targetZ - this.posZ);
         if (this.age > 1) {
            this.age--;
         }

         double distance = MathHelper.sqrt(xd * xd + yd * yd + zd * zd);
         if (distance > 0.5) {
            distance *= this.age;
            this.motionX = xd / distance;
            this.motionY = yd / distance;
            this.motionZ = zd / distance;
         } else {
            this.motionX *= 0.1F;
            this.motionY *= 0.1F;
            this.motionZ *= 0.1F;
            this.targetX = 0.0;
            this.targetY = 0.0;
            this.targetZ = 0.0;
            this.target = null;
            this.noClip = false;
         }

         if (this.world.isRemote) {
            float h = (float)((this.getEntityBoundingBox().maxY - this.getEntityBoundingBox().minY) / 2.0)
               + MathHelper.sin(this.getAge() / 10.0F + this.hoverStart) * 0.1F
               + 0.1F;
            if (this.type != 10) {
               FXDispatcher.INSTANCE
                  .drawNitorCore(
                     (float)this.prevPosX + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.125F,
                     (float)this.prevPosY + h + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.125F,
                     (float)this.prevPosZ + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.125F,
                     this.rand.nextGaussian() * 0.01F,
                     this.rand.nextGaussian() * 0.01F,
                     this.rand.nextGaussian() * 0.01F
                  );
            } else {
               FXDispatcher.INSTANCE
                  .crucibleBubble(
                     (float)this.prevPosX + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.125F,
                     (float)this.prevPosY + h + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.125F,
                     (float)this.prevPosZ + (this.rand.nextFloat() - this.rand.nextFloat()) * 0.125F,
                     0.33F,
                     0.33F,
                     1.0F
                  );
            }
         }
      }

      super.onUpdate();
   }

   public void writeEntityToNBT(NBTTagCompound par1NBTTagCompound) {
      super.writeEntityToNBT(par1NBTTagCompound);
      par1NBTTagCompound.setShort("type", (short)this.type);
   }

   public void readEntityFromNBT(NBTTagCompound par1NBTTagCompound) {
      super.readEntityFromNBT(par1NBTTagCompound);
      this.type = par1NBTTagCompound.getShort("type");
   }

   public void writeSpawnData(ByteBuf data) {
      if (this.target != null) {
         data.writeInt(this.target == null ? -1 : this.target.getEntityId());
         data.writeDouble(this.targetX);
         data.writeDouble(this.targetY);
         data.writeDouble(this.targetZ);
         data.writeByte(this.type);
      }
   }

   public void readSpawnData(ByteBuf data) {
      try {
         int ent = data.readInt();
         if (ent > -1) {
            this.target = this.world.getEntityByID(ent);
         }

         this.targetX = data.readDouble();
         this.targetY = data.readDouble();
         this.targetZ = data.readDouble();
         this.type = data.readByte();
      } catch (Exception var3) {
      }
   }
}
