package thaumcraft.common.entities.projectile;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class EntityGolemDart extends EntityArrow {
   public EntityGolemDart(World par1World) {
      super(par1World);
      this.setSize(0.2F, 0.2F);
   }

   public EntityGolemDart(World par1World, double par2, double par4, double par6) {
      super(par1World, par2, par4, par6);
      this.setSize(0.2F, 0.2F);
   }

   public EntityGolemDart(World par1World, EntityLivingBase par2EntityLivingBase) {
      super(par1World, par2EntityLivingBase);
      this.setSize(0.2F, 0.2F);
   }

   protected ItemStack getArrowStack() {
      return new ItemStack(Items.ARROW);
   }
}
