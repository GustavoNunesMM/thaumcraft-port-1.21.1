package thaumcraft.common.items.casters.foci;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXBlockBamf;

public class FocusEffectCurse extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSCURSE";
   }

   @Override
   public String getKey() {
      return "thaumcraft.CURSE";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.DEATH;
   }

   @Override
   public int getComplexity() {
      return this.getSettingValue("duration") + this.getSettingValue("power") * 3;
   }

   @Override
   public float getDamageForDisplay(float finalPower) {
      return (1.0F + this.getSettingValue("power")) * finalPower;
   }

   @Override
   public boolean execute(RayTraceResult target, Trajectory trajectory, float finalPower, int num) {
      PacketHandler.INSTANCE
         .sendToAllAround(
            new PacketFXBlockBamf(
               target.hitVec.x, target.hitVec.y, target.hitVec.z, 6946821, true, true, null
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
         int eff = (int)(this.getSettingValue("power") * finalPower / 2.0F);
         if (eff < 0) {
            eff = 0;
         }

         target.entityHit
            .attackEntityFrom(
               DamageSource.causeIndirectMagicDamage(
                  (Entity)(target.entityHit != null ? target.entityHit : this.getPackage().getCaster()), this.getPackage().getCaster()
               ),
               damage
            );
         if (target.entityHit instanceof EntityLivingBase) {
            ((EntityLivingBase)target.entityHit).addPotionEffect(new PotionEffect(MobEffects.POISON, duration, Math.round((float)eff)));
            float c = 0.85F;
            if (this.getPackage().world.rand.nextFloat() < c) {
               ((EntityLivingBase)target.entityHit).addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, duration, Math.round((float)eff)));
               c -= 0.15F;
            }

            if (this.getPackage().world.rand.nextFloat() < c) {
               ((EntityLivingBase)target.entityHit).addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, duration, Math.round((float)eff)));
               c -= 0.15F;
            }

            if (this.getPackage().world.rand.nextFloat() < c) {
               ((EntityLivingBase)target.entityHit).addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, duration * 2, Math.round((float)eff)));
               c -= 0.15F;
            }

            if (this.getPackage().world.rand.nextFloat() < c) {
               ((EntityLivingBase)target.entityHit).addPotionEffect(new PotionEffect(MobEffects.HUNGER, duration * 3, Math.round((float)eff)));
               c -= 0.15F;
            }

            if (this.getPackage().world.rand.nextFloat() < c) {
               ((EntityLivingBase)target.entityHit).addPotionEffect(new PotionEffect(MobEffects.UNLUCK, duration * 3, Math.round((float)eff)));
            }
         }
      } else if (target.typeOfHit == Type.BLOCK) {
         float f = (float)Math.min(8.0, 1.5 * this.getSettingValue("power") * finalPower);

         for (MutableBlockPos blockpos$mutableblockpos1 : BlockPos.getAllInBoxMutable(
            target.getBlockPos().add(-f, -f, -f), target.getBlockPos().add(f, f, f)
         )) {
            if (blockpos$mutableblockpos1.distanceSqToCenter(
                     target.hitVec.x, target.hitVec.y, target.hitVec.z
                  )
                  <= f * f
               && this.getPackage().world.isAirBlock(blockpos$mutableblockpos1.up())
               && this.getPackage().world.isBlockFullCube(blockpos$mutableblockpos1)) {
               this.getPackage().world.setBlockState(blockpos$mutableblockpos1.up(), BlocksTC.effectSap.getDefaultState());
            }
         }
      }

      return false;
   }

   @Override
   public NodeSetting[] createSettings() {
      return new NodeSetting[]{
         new NodeSetting("power", "focus.common.power", new NodeSetting.NodeSettingIntRange(1, 5)),
         new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(1, 10))
      };
   }

   @SideOnly(Side.CLIENT)
   @Override
   public void renderParticleFX(World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
      FXGeneric fb = new FXGeneric(world, posX, posY, posZ, motionX, motionY, motionZ);
      fb.setMaxAge(8);
      fb.setRBGColorF(0.41F + world.rand.nextFloat() * 0.2F, 0.0F, 0.019F + world.rand.nextFloat() * 0.2F);
      fb.setAlphaF(0.0F, world.rand.nextFloat(), world.rand.nextFloat(), world.rand.nextFloat(), 0.0F);
      fb.setGridSize(16);
      fb.setParticles(72 + world.rand.nextInt(4), 1, 1);
      fb.setScale(2.0F + world.rand.nextFloat() * 4.0F);
      fb.setLoop(false);
      fb.setSlowDown(0.9);
      fb.setGravity(0.0F);
      fb.setRotationSpeed(world.rand.nextFloat(), 0.0F);
      ParticleEngine.addEffectWithDelay(world, fb, world.rand.nextInt(4));
   }

   @Override
   public void onCast(Entity caster) {
      caster.world
         .playSound(
            null,
            caster.getPosition().up(),
            SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
            SoundCategory.PLAYERS,
            0.15F,
            1.0F + caster.getEntityWorld().rand.nextFloat() / 2.0F
         );
   }
}
