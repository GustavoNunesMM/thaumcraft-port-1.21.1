package thaumcraft.common.golems.ai;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.EntityMoveHelper.Action;
import net.minecraft.util.math.MathHelper;

public class FlightMoveHelper extends EntityMoveHelper {
   private static final String __OBFID = "CL_00002209";

   public FlightMoveHelper(EntityLiving entity) {
      super(entity);
   }

   public void onUpdateMoveHelper() {
      if (this.action == Action.MOVE_TO && !this.entity.getNavigator().noPath()) {
         this.action = Action.WAIT;
         double d0 = this.posX - this.entity.posX;
         double d1 = this.posY - this.entity.posY;
         double d2 = this.posZ - this.entity.posZ;
         double d3 = d0 * d0 + d1 * d1 + d2 * d2;
         d3 = MathHelper.sqrt(d3);
         d1 /= d3;
         float f = (float)(Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F;
         this.entity.rotationYaw = this.limitAngle(this.entity.rotationYaw, f, 30.0F);
         this.entity.renderYawOffset = this.entity.rotationYaw;
         float f1 = (float)(this.speed * this.entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
         this.entity.setAIMoveSpeed(this.entity.getAIMoveSpeed() + (f1 - this.entity.getAIMoveSpeed()) * 0.125F);
         double d4 = Math.sin((this.entity.ticksExisted + this.entity.getEntityId()) * 0.5) * 0.05;
         double d5 = Math.cos(this.entity.rotationYaw * (float) Math.PI / 180.0F);
         double d6 = Math.sin(this.entity.rotationYaw * (float) Math.PI / 180.0F);
         this.entity.motionX += d4 * d5;
         this.entity.motionZ += d4 * d6;
         d4 = Math.sin((this.entity.ticksExisted + this.entity.getEntityId()) * 0.75) * 0.05;
         this.entity.motionY += d4 * (d6 + d5) * 0.25;
         this.entity.motionY = this.entity.motionY + this.entity.getAIMoveSpeed() * d1 * 0.1;
         EntityLookHelper entitylookhelper = this.entity.getLookHelper();
         double d7 = this.entity.posX + d0 / d3 * 2.0;
         double d8 = this.entity.getEyeHeight() + this.entity.posY + d1 / d3 * 1.0;
         double d9 = this.entity.posZ + d2 / d3 * 2.0;
         double d10 = entitylookhelper.getLookPosX();
         double d11 = entitylookhelper.getLookPosY();
         double d12 = entitylookhelper.getLookPosZ();
         if (!entitylookhelper.getIsLooking()) {
            d10 = d7;
            d11 = d8;
            d12 = d9;
         }

         this.entity.getLookHelper().setLookPosition(d10 + (d7 - d10) * 0.125, d11 + (d8 - d11) * 0.125, d12 + (d9 - d12) * 0.125, 10.0F, 40.0F);
      } else {
         this.entity.setAIMoveSpeed(0.0F);
      }
   }
}
