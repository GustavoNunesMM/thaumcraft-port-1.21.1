package thaumcraft.common.entities;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntitySpecialItem extends EntityItem {
   public EntitySpecialItem(World par1World, double par2, double par4, double par6, ItemStack par8ItemStack) {
      super(par1World);
      this.setSize(0.25F, 0.25F);
      this.setPosition(par2, par4, par6);
      this.setItem(par8ItemStack);
      this.rotationYaw = (float)(Math.random() * 360.0);
      this.motionX = (float)(Math.random() * 0.2F - 0.1F);
      this.motionY = 0.2F;
      this.motionZ = (float)(Math.random() * 0.2F - 0.1F);
   }

   public EntitySpecialItem(World par1World) {
      super(par1World);
      this.setSize(0.25F, 0.25F);
   }

   public void onUpdate() {
      if (this.ticksExisted > 1) {
         if (this.motionY > 0.0) {
            this.motionY *= 0.9F;
         }

         this.motionY += 0.04F;
         super.onUpdate();
      }
   }

   public boolean attackEntityFrom(DamageSource source, float damage) {
      return source.isExplosion() ? false : super.attackEntityFrom(source, damage);
   }
}
