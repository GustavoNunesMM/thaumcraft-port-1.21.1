package thaumcraft.common.blocks.devices;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.devices.TileMirror;
import thaumcraft.common.tiles.devices.TileMirrorEssentia;

public class BlockMirrorItem extends ItemBlock {
   public BlockMirrorItem(Block par1) {
      super(par1);
   }

   public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand) {
      if (world.getBlockState(pos).getBlock() instanceof BlockMirror) {
         if (world.isRemote) {
            player.swingArm(hand);
            return super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand);
         }

         if (this.block == BlocksTC.mirror) {
            TileEntity tm = world.getTileEntity(pos);
            if (tm != null && tm instanceof TileMirror && !((TileMirror)tm).isLinkValid()) {
               ItemStack st = player.getHeldItem(hand).copy();
               st.setCount(1);
               st.setItemDamage(1);
               st.setTagInfo("linkX", new NBTTagInt(tm.getPos().getX()));
               st.setTagInfo("linkY", new NBTTagInt(tm.getPos().getY()));
               st.setTagInfo("linkZ", new NBTTagInt(tm.getPos().getZ()));
               st.setTagInfo("linkDim", new NBTTagInt(world.provider.getDimension()));
               world.playSound(null, pos, SoundsTC.jar, SoundCategory.BLOCKS, 1.0F, 2.0F);
               if (!player.inventory.addItemStackToInventory(st) && !world.isRemote) {
                  world.spawnEntity(new EntityItem(world, player.posX, player.posY, player.posZ, st));
               }

               if (!player.capabilities.isCreativeMode) {
                  player.getHeldItem(hand).shrink(1);
               }

               player.inventoryContainer.detectAndSendChanges();
            } else if (tm != null && tm instanceof TileMirror) {
               player.sendMessage(new TextComponentTranslation("§5§oThat mirror is already linked to a valid destination.", new Object[0]));
            }
         } else {
            TileEntity tm = world.getTileEntity(pos);
            if (tm != null && tm instanceof TileMirrorEssentia && !((TileMirrorEssentia)tm).isLinkValid()) {
               ItemStack stx = player.getHeldItem(hand).copy();
               stx.setCount(1);
               stx.setItemDamage(1);
               stx.setTagInfo("linkX", new NBTTagInt(tm.getPos().getX()));
               stx.setTagInfo("linkY", new NBTTagInt(tm.getPos().getY()));
               stx.setTagInfo("linkZ", new NBTTagInt(tm.getPos().getZ()));
               stx.setTagInfo("linkDim", new NBTTagInt(world.provider.getDimension()));
               world.playSound(null, pos, SoundsTC.jar, SoundCategory.BLOCKS, 1.0F, 2.0F);
               if (!player.inventory.addItemStackToInventory(stx) && !world.isRemote) {
                  world.spawnEntity(new EntityItem(world, player.posX, player.posY, player.posZ, stx));
               }

               if (!player.capabilities.isCreativeMode) {
                  player.getHeldItem(hand).shrink(1);
               }

               player.inventoryContainer.detectAndSendChanges();
            } else if (tm != null && tm instanceof TileMirrorEssentia) {
               player.sendMessage(new TextComponentTranslation("§5§oThat mirror is already linked to a valid destination.", new Object[0]));
            }
         }
      }

      return super.onItemUseFirst(player, world, pos, side, hitX, hitY, hitZ, hand);
   }

   public boolean placeBlockAt(
      ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState
   ) {
      boolean ret = super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
      if (ret && !world.isRemote) {
         if (this.block == BlocksTC.mirror) {
            TileEntity te = world.getTileEntity(pos);
            if (te != null && te instanceof TileMirror && stack.hasTagCompound()) {
               ((TileMirror)te).linkX = stack.getTagCompound().getInteger("linkX");
               ((TileMirror)te).linkY = stack.getTagCompound().getInteger("linkY");
               ((TileMirror)te).linkZ = stack.getTagCompound().getInteger("linkZ");
               ((TileMirror)te).linkDim = stack.getTagCompound().getInteger("linkDim");
               ((TileMirror)te).restoreLink();
            }
         } else {
            TileEntity te = world.getTileEntity(pos);
            if (te != null && te instanceof TileMirrorEssentia && stack.hasTagCompound()) {
               ((TileMirrorEssentia)te).linkX = stack.getTagCompound().getInteger("linkX");
               ((TileMirrorEssentia)te).linkY = stack.getTagCompound().getInteger("linkY");
               ((TileMirrorEssentia)te).linkZ = stack.getTagCompound().getInteger("linkZ");
               ((TileMirrorEssentia)te).linkDim = stack.getTagCompound().getInteger("linkDim");
               ((TileMirrorEssentia)te).restoreLink();
            }
         }
      }

      return ret;
   }

   public EnumRarity getRarity(ItemStack itemstack) {
      return EnumRarity.UNCOMMON;
   }

   @SideOnly(Side.CLIENT)
   public void addInformation(ItemStack item, World worldIn, List<String> list, ITooltipFlag flagIn) {
      if (item.hasTagCompound()) {
         int lx = item.getTagCompound().getInteger("linkX");
         int ly = item.getTagCompound().getInteger("linkY");
         int lz = item.getTagCompound().getInteger("linkZ");
         int ldim = item.getTagCompound().getInteger("linkDim");
         String desc = "" + ldim;
         World world = DimensionManager.getWorld(ldim);
         if (world != null) {
            desc = world.provider.getDimensionType().getName();
         }

         list.add("Linked to " + lx + "," + ly + "," + lz + " in " + desc);
      }
   }
}
