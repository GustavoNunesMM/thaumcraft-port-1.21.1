package thaumcraft.common.items.tools;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ScanningManager;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.lib.events.RenderEventHandler;
import thaumcraft.common.items.ItemTCBase;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.misc.PacketAuraToClient;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.utils.EntityUtils;
import thaumcraft.common.world.aura.AuraChunk;
import thaumcraft.common.world.aura.AuraHandler;

public class ItemThaumometer extends ItemTCBase {
   public ItemThaumometer() {
      super("thaumometer");
      this.setMaxStackSize(1);
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.UNCOMMON;
   }

   public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer p, EnumHand hand) {
      if (world.isRemote) {
         this.drawFX(world, p);
         p.world.playSound(p.posX, p.posY, p.posZ, SoundsTC.scan, SoundCategory.PLAYERS, 0.5F, 1.0F, false);
      } else {
         this.doScan(world, p);
      }

      return new ActionResult(EnumActionResult.SUCCESS, p.getHeldItem(hand));
   }

   public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot, boolean isSelected) {
      boolean held = isSelected || itemSlot == 0;
      if (held && !world.isRemote && entity.ticksExisted % 20 == 0 && entity instanceof EntityPlayerMP) {
         this.updateAura(stack, world, (EntityPlayerMP)entity);
      }

      if (held && world.isRemote && entity.ticksExisted % 5 == 0 && entity instanceof EntityPlayer) {
         Entity target = EntityUtils.getPointedEntity(world, entity, 1.0, 16.0, 5.0F, true);
         if (target != null && ScanningManager.isThingStillScannable((EntityPlayer)entity, target)) {
            FXDispatcher.INSTANCE.scanHighlight(target);
         }

         RenderEventHandler.thaumTarget = target;
         RayTraceResult mop = this.getRayTraceResultFromPlayerWild(world, (EntityPlayer)entity, true);
         if (mop != null && mop.getBlockPos() != null && ScanningManager.isThingStillScannable((EntityPlayer)entity, mop.getBlockPos())) {
            FXDispatcher.INSTANCE.scanHighlight(mop.getBlockPos());
         }
      }
   }

   protected RayTraceResult getRayTraceResultFromPlayerWild(World worldIn, EntityPlayer playerIn, boolean useLiquids) {
      float f = playerIn.prevRotationPitch
         + (playerIn.rotationPitch - playerIn.prevRotationPitch)
         + worldIn.rand.nextInt(25)
         - worldIn.rand.nextInt(25);
      float f1 = playerIn.prevRotationYaw
         + (playerIn.rotationYaw - playerIn.prevRotationYaw)
         + worldIn.rand.nextInt(25)
         - worldIn.rand.nextInt(25);
      double d0 = playerIn.prevPosX + (playerIn.posX - playerIn.prevPosX);
      double d1 = playerIn.prevPosY + (playerIn.posY - playerIn.prevPosY) + playerIn.getEyeHeight();
      double d2 = playerIn.prevPosZ + (playerIn.posZ - playerIn.prevPosZ);
      Vec3d vec3 = new Vec3d(d0, d1, d2);
      float f2 = MathHelper.cos(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f3 = MathHelper.sin(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f4 = -MathHelper.cos(-f * (float) (Math.PI / 180.0));
      float f5 = MathHelper.sin(-f * (float) (Math.PI / 180.0));
      float f6 = f3 * f4;
      float f7 = f2 * f4;
      double d3 = 16.0;
      Vec3d vec31 = vec3.add(f6 * d3, f5 * d3, f7 * d3);
      return worldIn.rayTraceBlocks(vec3, vec31, useLiquids, !useLiquids, false);
   }

   private void updateAura(ItemStack stack, World world, EntityPlayerMP player) {
      AuraChunk ac = AuraHandler.getAuraChunk(
         world.provider.getDimension(), player.getPosition().getX() >> 4, player.getPosition().getZ() >> 4
      );
      if (ac != null) {
         if ((ac.getFlux() > ac.getVis() || ac.getFlux() > ac.getBase() / 3) && !ThaumcraftCapabilities.knowsResearch(player, "FLUX")) {
            ResearchManager.startResearchWithPopup(player, "FLUX");
            player.sendStatusMessage(new TextComponentString(TextFormatting.DARK_PURPLE + I18n.translateToLocal("research.FLUX.warn")), true);
         }

         PacketHandler.INSTANCE.sendTo(new PacketAuraToClient(ac), player);
      }
   }

   private void drawFX(World worldIn, EntityPlayer playerIn) {
      Entity target = EntityUtils.getPointedEntity(worldIn, playerIn, 1.0, 9.0, 0.0F, true);
      if (target != null) {
         for (int a = 0; a < 10; a++) {
            FXDispatcher.INSTANCE
               .blockRunes(
                  target.posX - 0.5,
                  target.posY + target.getEyeHeight() / 2.0F,
                  target.posZ - 0.5,
                  0.3F + worldIn.rand.nextFloat() * 0.7F,
                  0.0F,
                  0.3F + worldIn.rand.nextFloat() * 0.7F,
                  (int)(target.height * 15.0F),
                  0.03F
               );
         }
      } else {
         RayTraceResult mop = this.rayTrace(worldIn, playerIn, true);
         if (mop != null && mop.getBlockPos() != null) {
            for (int a = 0; a < 10; a++) {
               FXDispatcher.INSTANCE
                  .blockRunes(
                     mop.getBlockPos().getX(),
                     mop.getBlockPos().getY() + 0.25,
                     mop.getBlockPos().getZ(),
                     0.3F + worldIn.rand.nextFloat() * 0.7F,
                     0.0F,
                     0.3F + worldIn.rand.nextFloat() * 0.7F,
                     15,
                     0.03F
                  );
            }
         }
      }
   }

   public void doScan(World worldIn, EntityPlayer playerIn) {
      if (!worldIn.isRemote) {
         Entity target = EntityUtils.getPointedEntity(worldIn, playerIn, 1.0, 9.0, 0.0F, true);
         if (target != null) {
            ScanningManager.scanTheThing(playerIn, target);
         } else {
            RayTraceResult mop = this.rayTrace(worldIn, playerIn, true);
            if (mop != null && mop.getBlockPos() != null) {
               ScanningManager.scanTheThing(playerIn, mop.getBlockPos());
            } else {
               ScanningManager.scanTheThing(playerIn, null);
            }
         }
      }
   }
}
