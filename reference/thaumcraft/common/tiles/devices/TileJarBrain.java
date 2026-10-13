package thaumcraft.common.tiles.devices;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.essentia.TileJar;

public class TileJarBrain extends TileJar {
   public float field_40063_b;
   public float field_40061_d;
   public float field_40059_f;
   public float field_40066_q;
   public float rota;
   public float rotb;
   public int xp = 0;
   public int xpMax = 2000;
   public int eatDelay = 0;
   long lastsigh = System.currentTimeMillis() + 1500L;

   @Override
   public void readSyncNBT(NBTTagCompound nbttagcompound) {
      this.xp = nbttagcompound.getInteger("XP");
   }

   @Override
   public NBTTagCompound writeSyncNBT(NBTTagCompound nbttagcompound) {
      nbttagcompound.setInteger("XP", this.xp);
      return nbttagcompound;
   }

   @Override
   public void update() {
      Entity entity = null;
      if (this.xp > this.xpMax) {
         this.xp = this.xpMax;
      }

      if (this.xp < this.xpMax) {
         entity = this.getClosestXPOrb();
         if (entity != null && this.eatDelay == 0) {
            double var3 = (this.pos.getX() + 0.5 - entity.posX) / 25.0;
            double var5 = (this.pos.getY() + 0.5 - entity.posY) / 25.0;
            double var7 = (this.pos.getZ() + 0.5 - entity.posZ) / 25.0;
            double var9 = Math.sqrt(var3 * var3 + var5 * var5 + var7 * var7);
            double var11 = 1.0 - var9;
            if (var11 > 0.0) {
               var11 *= var11;
               entity.motionX += var3 / var9 * var11 * 0.3;
               entity.motionY += var5 / var9 * var11 * 0.5;
               entity.motionZ += var7 / var9 * var11 * 0.3;
            }
         }
      }

      if (this.world.isRemote) {
         this.rotb = this.rota;
         if (entity == null) {
            entity = this.world
               .getClosestPlayer(
                  this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, 6.0, false
               );
            if (entity != null && this.lastsigh < System.currentTimeMillis()) {
               this.world
                  .playSound(
                     this.pos.getX() + 0.5,
                     this.pos.getY() + 0.5,
                     this.pos.getZ() + 0.5,
                     SoundsTC.brain,
                     SoundCategory.AMBIENT,
                     0.15F,
                     0.8F + this.world.rand.nextFloat() * 0.4F,
                     false
                  );
               this.lastsigh = System.currentTimeMillis() + 5000L + this.world.rand.nextInt(25000);
            }
         }

         if (entity != null) {
            double d = entity.posX - (this.pos.getX() + 0.5F);
            double d1 = entity.posZ - (this.pos.getZ() + 0.5F);
            this.field_40066_q = (float)Math.atan2(d1, d);
            this.field_40059_f += 0.1F;
            if (this.field_40059_f < 0.5F || rand.nextInt(40) == 0) {
               float f3 = this.field_40061_d;

               do {
                  this.field_40061_d = this.field_40061_d + (rand.nextInt(4) - rand.nextInt(4));
               } while (f3 == this.field_40061_d);
            }
         } else {
            this.field_40066_q += 0.01F;
         }

         while (this.rota >= 3.141593F) {
            this.rota -= 6.283185F;
         }

         while (this.rota < -3.141593F) {
            this.rota += 6.283185F;
         }

         while (this.field_40066_q >= 3.141593F) {
            this.field_40066_q -= 6.283185F;
         }

         while (this.field_40066_q < -3.141593F) {
            this.field_40066_q += 6.283185F;
         }

         float f = this.field_40066_q - this.rota;

         while (f >= 3.141593F) {
            f -= 6.283185F;
         }

         while (f < -3.141593F) {
            f += 6.283185F;
         }

         this.rota += f * 0.04F;
      }

      if (this.eatDelay > 0) {
         this.eatDelay--;
      } else if (this.xp < this.xpMax) {
         List ents = this.world
            .getEntitiesWithinAABB(
               EntityXPOrb.class,
               new AxisAlignedBB(
                  this.pos.getX() - 0.1,
                  this.pos.getY() - 0.1,
                  this.pos.getZ() - 0.1,
                  this.pos.getX() + 1.1,
                  this.pos.getY() + 1.1,
                  this.pos.getZ() + 1.1
               )
            );
         if (ents.size() > 0) {
            for (Object ent : ents) {
               EntityXPOrb eo = (EntityXPOrb)ent;
               this.xp = this.xp + eo.getXpValue();
               eo.playSound(
                  SoundEvents.ENTITY_GENERIC_EAT,
                  0.1F,
                  (this.world.rand.nextFloat() - this.world.rand.nextFloat()) * 0.2F + 1.0F
               );
               eo.setDead();
            }

            this.syncTile(false);
            this.markDirty();
         }
      }
   }

   public Entity getClosestXPOrb() {
      double cdist = Double.MAX_VALUE;
      Entity orb = null;
      List ents = this.world
         .getEntitiesWithinAABB(
            EntityXPOrb.class,
            new AxisAlignedBB(
                  this.pos.getX(),
                  this.pos.getY(),
                  this.pos.getZ(),
                  this.pos.getX() + 1,
                  this.pos.getY() + 1,
                  this.pos.getZ() + 1
               )
               .grow(8.0, 8.0, 8.0)
         );
      if (ents.size() > 0) {
         for (Object ent : ents) {
            EntityXPOrb eo = (EntityXPOrb)ent;
            double d = this.getDistanceSq(eo.posX, eo.posY, eo.posZ);
            if (d < cdist) {
               orb = eo;
               cdist = d;
            }
         }
      }

      return orb;
   }
}
