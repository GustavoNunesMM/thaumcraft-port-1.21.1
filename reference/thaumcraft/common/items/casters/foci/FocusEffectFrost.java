package thaumcraft.common.items.casters.foci;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;

public class FocusEffectFrost extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSELEMENTAL";
   }

   @Override
   public String getKey() {
      return "thaumcraft.FROST";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.COLD;
   }

   @Override
   public int getComplexity() {
      return this.getSettingValue("duration") + this.getSettingValue("power") * 2;
   }

   @Override
   public float getDamageForDisplay(float finalPower) {
      return (3 + this.getSettingValue("power")) * finalPower;
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
         int duration = 20 * this.getSettingValue("duration");
         int potency = (int)(1.0F + this.getSettingValue("power") * finalPower / 3.0F);
         target.entityHit
            .attackEntityFrom(
               DamageSource.causeThrownDamage(
                  (Entity)(target.entityHit != null ? target.entityHit : this.getPackage().getCaster()), this.getPackage().getCaster()
               ),
               damage
            );
         if (target.entityHit instanceof EntityLivingBase) {
            ((EntityLivingBase)target.entityHit).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, duration, potency));
         }
      } else if (target.typeOfHit == Type.BLOCK) {
         float f = Math.min(16.0F, 2 * this.getSettingValue("power") * finalPower);

         for (MutableBlockPos blockpos$mutableblockpos1 : BlockPos.getAllInBoxMutable(
            target.getBlockPos().add(-f, -f, -f), target.getBlockPos().add(f, f, f)
         )) {
            if (blockpos$mutableblockpos1.distanceSqToCenter(
                  target.hitVec.x, target.hitVec.y, target.hitVec.z
               )
               <= f * f) {
               IBlockState iblockstate1 = this.getPackage().world.getBlockState(blockpos$mutableblockpos1);
               if (iblockstate1.getMaterial() == Material.WATER
                  && (Integer)iblockstate1.getValue(BlockLiquid.LEVEL) == 0
                  && this.getPackage().world.mayPlace(Blocks.FROSTED_ICE, blockpos$mutableblockpos1, false, EnumFacing.DOWN, (Entity)null)) {
                  this.getPackage().world.setBlockState(blockpos$mutableblockpos1, Blocks.FROSTED_ICE.getDefaultState());
                  this.getPackage()
                     .world
                     .scheduleUpdate(
                        blockpos$mutableblockpos1.toImmutable(),
                        Blocks.FROSTED_ICE,
                        MathHelper.getInt(this.getPackage().world.rand, 60, 120)
                     );
               }
            }
         }
      }

      return false;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{
         new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
         new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(2, 10))
      };
   }

   @SideOnly(Side.CLIENT)
   @Override
   public void renderParticleFX(World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
      FXGeneric fb = new FXGeneric(world, posX, posY, posZ, motionX, motionY, motionZ);
      fb.setMaxAge(40 + world.rand.nextInt(40));
      fb.setAlphaF(1.0F, 0.0F);
      fb.setParticles(8, 1, 1);
      fb.setGravity(0.033F);
      fb.setSlowDown(0.8);
      fb.setRandomMovementScale(0.0025F, 1.0E-4F, 0.0025F);
      fb.setScale((float)(0.7F + world.rand.nextGaussian() * 0.3F));
      fb.setRotationSpeed(world.rand.nextFloat() * 3.0F, (float)world.rand.nextGaussian() / 4.0F);
      ParticleEngine.addEffectWithDelay(world, fb, 0);
   }

   @Override
   public void onCast(Entity caster) {
      caster.world
         .playSound(
            null,
            caster.getPosition().up(),
            SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE,
            SoundCategory.PLAYERS,
            0.2F,
            1.0F + (float)(caster.world.rand.nextGaussian() * 0.05F)
         );
   }
}
