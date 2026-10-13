package thaumcraft.common.items.casters.foci;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundCategory;
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
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.fx.particles.FXGeneric;
import thaumcraft.common.lib.events.ServerEvents;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXFocusPartImpact;

public class FocusEffectBreak extends FocusEffect {
   @Override
   public String getResearch() {
      return "FOCUSBREAK";
   }

   @Override
   public String getKey() {
      return "thaumcraft.BREAK";
   }

   @Override
   public Aspect getAspect() {
      return Aspect.ENTROPY;
   }

   @Override
   public int getComplexity() {
      return this.getSettingValue("power") * 3
         + this.getSettingValue("silk") * 4
         + (this.getSettingValue("fortune") == 0 ? 0 : (this.getSettingValue("fortune") + 1) * 3);
   }

   @Override
   public boolean execute(RayTraceResult target, Trajectory trajectory, float finalPower, int num) {
      if (target.typeOfHit == Type.BLOCK) {
         PacketHandler.INSTANCE
            .sendToAllAround(
               new PacketFXFocusPartImpact(
                  target.getBlockPos().getX() + 0.5,
                  target.getBlockPos().getY() + 0.5,
                  target.getBlockPos().getZ() + 0.5,
                  new String[]{this.getKey()}
               ),
               new TargetPoint(
                  this.getPackage().world.provider.getDimension(),
                  target.hitVec.x,
                  target.hitVec.y,
                  target.hitVec.z,
                  64.0
               )
            );
         boolean silk = this.getSettingValue("silk") > 0;
         int fortune = this.getSettingValue("fortune");
         float strength = this.getSettingValue("power") * finalPower;
         float dur = this.getPackage().world.getBlockState(target.getBlockPos()).getBlockHardness(this.getPackage().world, target.getBlockPos()) * 100.0F;
         dur = (float)Math.sqrt(dur);
         if (this.getPackage().getCaster() instanceof EntityPlayer) {
            ServerEvents.addBreaker(
               this.getPackage().world,
               target.getBlockPos(),
               this.getPackage().world.getBlockState(target.getBlockPos()),
               (EntityPlayer)this.getPackage().getCaster(),
               true,
               silk,
               fortune,
               strength,
               dur,
               dur,
               (int)(dur / strength / 3.0F * num),
               0.25F + (silk ? 0.25F : 0.0F) + fortune * 0.1F,
               null
            );
         }

         return true;
      } else {
         return true;
      }
   }

   @Override
   public NodeSetting[] createSettings() {
      int[] silk = new int[]{0, 1};
      String[] silkDesc = new String[]{"focus.common.no", "focus.common.yes"};
      int[] fortune = new int[]{0, 1, 2, 3, 4};
      String[] fortuneDesc = new String[]{"focus.common.no", "I", "II", "III", "IV"};
      return new NodeSetting[]{
         new NodeSetting("power", "focus.break.power", new NodeSetting.NodeSettingIntRange(1, 5)),
         new NodeSetting("fortune", "focus.common.fortune", new NodeSetting.NodeSettingIntList(fortune, fortuneDesc)),
         new NodeSetting("silk", "focus.common.silk", new NodeSetting.NodeSettingIntList(silk, silkDesc))
      };
   }

   @SideOnly(Side.CLIENT)
   @Override
   public void renderParticleFX(World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
      FXGeneric fb = new FXGeneric(world, posX, posY, posZ, motionX, motionY, motionZ);
      fb.setMaxAge(6 + world.rand.nextInt(6));
      int q = world.rand.nextInt(4);
      fb.setParticles(704 + q * 3, 3, 1);
      fb.setSlowDown(0.8);
      fb.setScale((float)(1.7F + world.rand.nextGaussian() * 0.3F));
      ParticleEngine.addEffect(world, fb);
   }

   @Override
   public void onCast(Entity caster) {
      caster.world
         .playSound(
            null,
            caster.getPosition().up(),
            SoundEvents.BLOCK_END_GATEWAY_SPAWN,
            SoundCategory.PLAYERS,
            0.1F,
            2.0F + (float)(caster.world.rand.nextGaussian() * 0.05F)
         );
   }
}
