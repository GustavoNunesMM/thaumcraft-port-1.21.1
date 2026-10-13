package thaumcraft.common.golems;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.common.lib.network.FakeNetHandlerPlayServer;
import thaumcraft.common.lib.utils.InventoryUtils;

public class GolemInteractionHelper {
   public static void golemClick(World world, IGolemAPI golem, BlockPos pos, EnumFacing face, ItemStack clickStack, boolean sneaking, boolean rightClick) {
      FakePlayer fp = FakePlayerFactory.get((WorldServer)world, new GameProfile((UUID)null, "FakeThaumcraftGolem"));
      fp.connection = new FakeNetHandlerPlayServer(fp.server, new NetworkManager(EnumPacketDirection.CLIENTBOUND), fp);
      fp.setPositionAndRotation(
         golem.getGolemEntity().posX,
         golem.getGolemEntity().posY,
         golem.getGolemEntity().posZ,
         golem.getGolemEntity().rotationYaw,
         golem.getGolemEntity().rotationPitch
      );
      IBlockState bs = world.getBlockState(pos);
      fp.setHeldItem(EnumHand.MAIN_HAND, clickStack);
      fp.setSneaking(sneaking);
      if (!rightClick) {
         try {
            fp.interactionManager.onBlockClicked(pos, face);
         } catch (Exception var11) {
         }
      } else {
         if (fp.getHeldItemMainhand().getItem() instanceof ItemBlock
            && !mayPlace(world, ((ItemBlock)fp.getHeldItemMainhand().getItem()).getBlock(), pos, face)) {
            golem.getGolemEntity()
               .setPosition(
                  golem.getGolemEntity().posX + face.getXOffset(),
                  golem.getGolemEntity().posY + face.getYOffset(),
                  golem.getGolemEntity().posZ + face.getZOffset()
               );
         }

         try {
            fp.interactionManager.processRightClickBlock(fp, world, fp.getHeldItemMainhand(), EnumHand.MAIN_HAND, pos, face, 0.5F, 0.5F, 0.5F);
         } catch (Exception var10) {
         }
      }

      golem.addRankXp(1);
      if (!fp.getHeldItemMainhand().isEmpty() && fp.getHeldItemMainhand().getCount() <= 0) {
         fp.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
      }

      dropSomeItems(fp, golem);
      golem.swingArm();
   }

   private static boolean mayPlace(World world, Block blockIn, BlockPos pos, EnumFacing side) {
      IBlockState block = world.getBlockState(pos);
      AxisAlignedBB axisalignedbb = blockIn.getBoundingBox(blockIn.getDefaultState(), world, pos);
      return axisalignedbb == null || world.checkNoEntityCollision(axisalignedbb, null);
   }

   private static void dropSomeItems(FakePlayer fp2, IGolemAPI golem) {
      for (int i = 0; i < fp2.inventory.mainInventory.size(); i++) {
         if (!((ItemStack)fp2.inventory.mainInventory.get(i)).isEmpty()) {
            if (golem.canCarry((ItemStack)fp2.inventory.mainInventory.get(i), true)) {
               fp2.inventory.mainInventory.set(i, golem.holdItem((ItemStack)fp2.inventory.mainInventory.get(i)));
            }

            if (!((ItemStack)fp2.inventory.mainInventory.get(i)).isEmpty()
               && ((ItemStack)fp2.inventory.mainInventory.get(i)).getCount() > 0) {
               InventoryUtils.dropItemAtEntity(golem.getGolemWorld(), (ItemStack)fp2.inventory.mainInventory.get(i), golem.getGolemEntity());
            }

            fp2.inventory.mainInventory.set(i, ItemStack.EMPTY);
         }
      }

      for (int var3 = 0; var3 < fp2.inventory.armorInventory.size(); var3++) {
         if (!((ItemStack)fp2.inventory.armorInventory.get(var3)).isEmpty()) {
            if (golem.canCarry((ItemStack)fp2.inventory.armorInventory.get(var3), true)) {
               fp2.inventory.armorInventory.set(var3, golem.holdItem((ItemStack)fp2.inventory.armorInventory.get(var3)));
            }

            if (!((ItemStack)fp2.inventory.mainInventory.get(var3)).isEmpty()
               && ((ItemStack)fp2.inventory.armorInventory.get(var3)).getCount() > 0) {
               InventoryUtils.dropItemAtEntity(golem.getGolemWorld(), (ItemStack)fp2.inventory.armorInventory.get(var3), golem.getGolemEntity());
            }

            fp2.inventory.armorInventory.set(var3, ItemStack.EMPTY);
         }
      }
   }
}
