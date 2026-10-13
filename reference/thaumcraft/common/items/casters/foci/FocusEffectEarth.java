package thaumcraft.common.items.casters.foci;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
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
import thaumcraft.common.lib.events.ServerEvents;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;

public class FocusEffectEarth extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSELEMENTAL";
   }

   @Override
   public String getKey() {
      return "thaumcraft.EARTH";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.EARTH;
   }

   @Override
   public int getComplexity() {
      return this.getSettingValue("power") * 3;
   }

   @Override
   public float getDamageForDisplay(float finalPower) {
      return 2 * this.getSettingValue("power") * finalPower;
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
      if (target.typeOfHit == Type.ENTITY && target.entityHit != null) {
         float damage = this.getDamageForDisplay(finalPower);
         target.entityHit
            .attackEntityFrom(
               DamageSource.causeThrownDamage(
                  (Entity)(target.entityHit != null ? target.entityHit : this.getPackage().getCaster()), this.getPackage().getCaster()
               ),
               damage
            );
         return true;
      } else {
         if (target.typeOfHit == Type.BLOCK) {
            BlockPos pos = target.getBlockPos();
            if (this.getPackage().getCaster() instanceof EntityPlayer
               && this.getPackage().world.getBlockState(pos).getBlockHardness(this.getPackage().world, pos) <= this.getDamageForDisplay(finalPower) / 25.0F) {
               ServerEvents.addBreaker(
                  this.getPackage().world,
                  pos,
                  this.getPackage().world.getBlockState(pos),
                  (EntityPlayer)this.getPackage().getCaster(),
                  false,
                  false,
                  0,
                  1.0F,
                  0.0F,
                  1.0F,
                  num,
                  0.1F,
                  null
               );
            }
         }

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
      pp.grav = 0.4F;
      pp.layer = 1;
      pp.age = 20 + world.rand.nextInt(10);
      pp.alpha = new float[]{1.0F, 0.0F};
      pp.partStart = 75 + world.rand.nextInt(4);
      pp.partInc = 1;
      pp.partNum = 1;
      pp.slowDown = 0.9;
      pp.rot = (float)world.rand.nextGaussian();
      float s = (float)(1.0 + world.rand.nextGaussian() * 0.2F);
      pp.scale = new float[]{s, s / 2.0F};
      FXDispatcher.INSTANCE.drawGenericParticles(posX, posY, posZ, motionX, motionY, motionZ, pp);
   }

   @Override
   public void onCast(Entity caster) {
      caster.world
         .playSound(
            null,
            caster.getPosition().up(),
            SoundEvents.ENTITY_ENDERDRAGON_FIREBALL_EPLD,
            SoundCategory.PLAYERS,
            0.25F,
            1.0F + (float)(caster.world.rand.nextGaussian() * 0.05F)
         );
   }
}
