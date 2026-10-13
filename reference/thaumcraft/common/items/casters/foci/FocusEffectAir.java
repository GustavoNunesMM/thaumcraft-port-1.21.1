package thaumcraft.common.items.casters.foci;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;

public class FocusEffectAir extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSELEMENTAL";
   }

   @Override
   public String getKey() {
      return "thaumcraft.AIR";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.AIR;
   }

   @Override
   public int getComplexity() {
      return this.getSettingValue("power") * 2;
   }

   @Override
   public float getDamageForDisplay(float finalPower) {
      return (1 + this.getSettingValue("power")) * finalPower;
   }

   @Override
   public boolean execute(RayTraceResult target, Trajectory trajectory, float finalPower, int num) {
      PacketHandler.INSTANCE
         .sendToAllAround(
            new PacketFXFocusPartImpact(
               target.hitVec.x, target.hitVec.y, target.hitVec.z, new String[]{this.getKey()}
            ),
            new TargetPoint(
               this.getPackage().world.provider.getDimension(),
               target.hitVec.x,
               target.hitVec.y,
               target.hitVec.z,
               64.0
            )
         );
      this.getPackage()
         .world
         .playSound(
            null,
            target.hitVec.x,
            target.hitVec.y,
            target.hitVec.z,
            SoundEvents.ENTITY_ENDERDRAGON_FLAP,
            SoundCategory.PLAYERS,
            0.5F,
            0.66F
         );
      if (target.typeOfHit == Type.ENTITY && target.entityHit != null) {
         float damage = this.getDamageForDisplay(finalPower);
         target.entityHit
            .attackEntityFrom(
               DamageSource.causeThrownDamage(
                  (Entity)(target.entityHit != null ? target.entityHit : this.getPackage().getCaster()), this.getPackage().getCaster()
               ),
               damage
            );
         if (target.entityHit instanceof EntityLivingBase) {
            if (trajectory != null) {
               ((EntityLivingBase)target.entityHit)
                  .knockBack(this.getPackage().getCaster(), damage * 0.25F, -trajectory.direction.x, -trajectory.direction.z);
            } else {
               ((EntityLivingBase)target.entityHit)
                  .knockBack(
                     this.getPackage().getCaster(),
                     damage * 0.25F,
                     -MathHelper.sin(target.entityHit.rotationYaw * (float) (Math.PI / 180.0)),
                     MathHelper.cos(target.entityHit.rotationYaw * (float) (Math.PI / 180.0))
                  );
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5))};
   }

   @SideOnly(Side.CLIENT)
   @Override
   public void renderParticleFX(World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
      FXDispatcher.GenPart pp = new FXDispatcher.GenPart();
      pp.grav = -0.1F;
      pp.age = 20 + world.rand.nextInt(10);
      pp.alpha = new float[]{0.5F, 0.0F};
      pp.grid = 32;
      pp.partStart = 337;
      pp.partInc = 1;
      pp.partNum = 5;
      pp.slowDown = 0.75;
      pp.rot = (float)world.rand.nextGaussian() / 2.0F;
      float s = (float)(2.0 + world.rand.nextGaussian() * 0.5);
      pp.scale = new float[]{s, s * 2.0F};
      FXDispatcher.INSTANCE.drawGenericParticles(posX, posY, posZ, motionX, motionY, motionZ, pp);
   }

   @Override
   public void onCast(Entity caster) {
      caster.world.playSound(null, caster.getPosition().up(), SoundsTC.wind, SoundCategory.PLAYERS, 0.125F, 2.0F);
   }
}
