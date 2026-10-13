package thaumcraft.client.fx.particles;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FXBoreParticles extends Particle {
   private IBlockState blockInstance;
   private ItemStack itemInstance;
   private int side;
   private Entity target;
   private double targetX;
   private double targetY;
   private double targetZ;

   public FXBoreParticles(World par1World, double par2, double par4, double par6, double tx, double ty, double tz, IBlockState par14Block, int par15) {
      super(par1World, par2, par4, par6, 0.0, 0.0, 0.0);
      this.blockInstance = par14Block;

      try {
         this.setParticleTexture(Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getParticleIcon(Item.getItemFromBlock(par14Block.getBlock()), par15));
      } catch (Exception var24) {
         this.setParticleTexture(Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getParticleIcon(Item.getItemFromBlock(Blocks.STONE), 0));
         this.particleMaxAge = 0;
      }

      this.particleGravity = par14Block.getBlock().blockParticleGravity;
      this.particleRed = this.particleGreen = this.particleBlue = 0.6F;
      this.particleScale = this.rand.nextFloat() * 0.3F + 0.4F;
      this.side = par15;
      this.targetX = tx;
      this.targetY = ty;
      this.targetZ = tz;
      double dx = tx - this.posX;
      double dy = ty - this.posY;
      double dz = tz - this.posZ;
      int base = (int)(MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 10.0F);
      if (base < 1) {
         base = 1;
      }

      this.particleMaxAge = base / 2 + this.rand.nextInt(base);
      float f3 = 0.01F;
      this.motionX = (float)this.world.rand.nextGaussian() * f3;
      this.motionY = (float)this.world.rand.nextGaussian() * f3;
      this.motionZ = (float)this.world.rand.nextGaussian() * f3;
      this.particleGravity = 0.01F;
   }

   public FXBoreParticles(
      World par1World, double par2, double par4, double par6, double tx, double ty, double tz, double sx, double sy, double sz, ItemStack item
   ) {
      super(par1World, par2, par4, par6, 0.0, 0.0, 0.0);
      this.itemInstance = item;
      this.setParticleTexture(Minecraft.getMinecraft().getRenderItem().getItemModelMesher().getParticleIcon(item.getItem(), item.getItemDamage()));
      this.particleGravity = Blocks.SNOW_LAYER.blockParticleGravity;
      this.particleRed = this.particleGreen = this.particleBlue = 0.6F;
      this.particleScale = this.rand.nextFloat() * 0.3F + 0.4F;
      this.side = 0;
      this.targetX = tx;
      this.targetY = ty;
      this.targetZ = tz;
      double dx = tx - this.posX;
      double dy = ty - this.posY;
      double dz = tz - this.posZ;
      int base = (int)(MathHelper.sqrt(dx * dx + dy * dy + dz * dz) * 10.0F);
      if (base < 1) {
         base = 1;
      }

      this.particleMaxAge = base / 2 + this.rand.nextInt(base);
      float f3 = 0.01F;
      this.motionX = sx + (float)this.world.rand.nextGaussian() * f3;
      this.motionY = sy + (float)this.world.rand.nextGaussian() * f3;
      this.motionZ = sz + (float)this.world.rand.nextGaussian() * f3;
      this.particleGravity = 0.01F;
      Entity renderentity = FMLClientHandler.instance().getClient().getRenderViewEntity();
      int visibleDistance = 64;
      if (!FMLClientHandler.instance().getClient().gameSettings.fancyGraphics) {
         visibleDistance = 32;
      }

      if (renderentity.getDistance(this.posX, this.posY, this.posZ) > visibleDistance) {
         this.particleMaxAge = 0;
      }
   }

   public void setTarget(Entity target) {
      this.target = target;
   }

   public void onUpdate() {
      this.prevPosX = this.posX;
      this.prevPosY = this.posY;
      this.prevPosZ = this.posZ;
      if (this.target != null) {
         this.targetX = this.target.posX;
         this.targetY = this.target.posY + this.target.getEyeHeight();
         this.targetZ = this.target.posZ;
      }

      if (this.particleAge++ < this.particleMaxAge
         && (
            MathHelper.floor(this.posX) != MathHelper.floor(this.targetX)
               || MathHelper.floor(this.posY) != MathHelper.floor(this.targetY)
               || MathHelper.floor(this.posZ) != MathHelper.floor(this.targetZ)
         )) {
         this.move(this.motionX, this.motionY, this.motionZ);
         this.motionX *= 0.985;
         this.motionY *= 0.95;
         this.motionZ *= 0.985;
         double dx = this.targetX - this.posX;
         double dy = this.targetY - this.posY;
         double dz = this.targetZ - this.posZ;
         double d11 = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
         double clamp = Math.min(0.25, d11 / 15.0);
         if (d11 < 2.0) {
            this.particleScale *= 0.9F;
         }

         dx /= d11;
         dy /= d11;
         dz /= d11;
         this.motionX += dx * clamp;
         this.motionY += dy * clamp;
         this.motionZ += dz * clamp;
         this.motionX = MathHelper.clamp((float)this.motionX, -clamp, clamp);
         this.motionY = MathHelper.clamp((float)this.motionY, -clamp, clamp);
         this.motionZ = MathHelper.clamp((float)this.motionZ, -clamp, clamp);
         this.motionX = this.motionX + this.rand.nextGaussian() * 0.005;
         this.motionY = this.motionY + this.rand.nextGaussian() * 0.005;
         this.motionZ = this.motionZ + this.rand.nextGaussian() * 0.005;
      } else {
         this.setExpired();
      }
   }

   public int getFXLayer() {
      return 1;
   }

   public FXBoreParticles getObjectColor(BlockPos pos) {
      if (this.blockInstance == null || this.world.getBlockState(pos) != this.blockInstance) {
         try {
            int var4x = Minecraft.getMinecraft().getItemColors().colorMultiplier(this.itemInstance, 0);
            this.particleRed *= (var4x >> 16 & 0xFF) / 255.0F;
            this.particleGreen *= (var4x >> 8 & 0xFF) / 255.0F;
            this.particleBlue *= (var4x & 0xFF) / 255.0F;
         } catch (Exception var41) {
         }

         return this;
      } else if (this.blockInstance == Blocks.GRASS && this.side != 1) {
         return this;
      } else {
         try {
            int var4 = Minecraft.getMinecraft().getBlockColors().colorMultiplier(this.blockInstance, this.world, pos, 0);
            this.particleRed *= (var4 >> 16 & 0xFF) / 255.0F;
            this.particleGreen *= (var4 >> 8 & 0xFF) / 255.0F;
            this.particleBlue *= (var4 & 0xFF) / 255.0F;
         } catch (Exception var3) {
         }

         return this;
      }
   }

   public void renderParticle(
      BufferBuilder buffer,
      Entity entityIn,
      float partialTicks,
      float rotationX,
      float rotationZ,
      float rotationYZ,
      float rotationXY,
      float rotationXZ
   ) {
      float f6 = (this.particleTextureIndexX + this.particleTextureJitterX / 4.0F) / 16.0F;
      float f7 = f6 + 0.015609375F;
      float f8 = (this.particleTextureIndexY + this.particleTextureJitterY / 4.0F) / 16.0F;
      float f9 = f8 + 0.015609375F;
      float f10 = 0.1F * this.particleScale;
      if (this.particleTexture != null) {
         f6 = this.particleTexture.getInterpolatedU(this.particleTextureJitterX / 4.0F * 16.0F);
         f7 = this.particleTexture.getInterpolatedU((this.particleTextureJitterX + 1.0F) / 4.0F * 16.0F);
         f8 = this.particleTexture.getInterpolatedV(this.particleTextureJitterY / 4.0F * 16.0F);
         f9 = this.particleTexture.getInterpolatedV((this.particleTextureJitterY + 1.0F) / 4.0F * 16.0F);
      }

      int i = this.getBrightnessForRender(partialTicks);
      int j = i >> 16 & 65535;
      int k = i & 65535;
      float f11 = (float)(this.prevPosX + (this.posX - this.prevPosX) * partialTicks - interpPosX);
      float f12 = (float)(this.prevPosY + (this.posY - this.prevPosY) * partialTicks - interpPosY);
      float f13 = (float)(this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks - interpPosZ);
      buffer.pos(f11 - rotationX * f10 - rotationXY * f10, f12 - rotationZ * f10, f13 - rotationYZ * f10 - rotationXZ * f10)
         .tex(f6, f9)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 1.0F)
         .lightmap(j, k)
         .endVertex();
      buffer.pos(f11 - rotationX * f10 + rotationXY * f10, f12 + rotationZ * f10, f13 - rotationYZ * f10 + rotationXZ * f10)
         .tex(f6, f8)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 1.0F)
         .lightmap(j, k)
         .endVertex();
      buffer.pos(f11 + rotationX * f10 + rotationXY * f10, f12 + rotationZ * f10, f13 + rotationYZ * f10 + rotationXZ * f10)
         .tex(f7, f8)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 1.0F)
         .lightmap(j, k)
         .endVertex();
      buffer.pos(f11 + rotationX * f10 - rotationXY * f10, f12 - rotationZ * f10, f13 + rotationYZ * f10 - rotationXZ * f10)
         .tex(f7, f9)
         .color(this.particleRed, this.particleGreen, this.particleBlue, 1.0F)
         .lightmap(j, k)
         .endVertex();
   }
}
