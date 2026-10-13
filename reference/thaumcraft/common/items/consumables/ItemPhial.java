package thaumcraft.common.items.consumables;

import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.ItemTCEssentiaContainer;
import thaumcraft.common.tiles.essentia.TileAlembic;
import thaumcraft.common.tiles.essentia.TileJarFillable;

public class ItemPhial extends ItemTCEssentiaContainer {
   public ItemPhial() {
      super("phial", 10, "empty", "filled");
   }

   public static ItemStack makePhial(Aspect aspect, int amt) {
      ItemStack i = new ItemStack(ItemsTC.phial, 1, 1);
      ((IEssentiaContainerItem)i.getItem()).setAspects(i, new AspectList().add(aspect, amt));
      return i;
   }

   public static ItemStack makeFilledPhial(Aspect aspect) {
      return makePhial(aspect, 10);
   }

   @Override
   public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
      if (tab == ConfigItems.TABTC || tab == CreativeTabs.SEARCH) {
         items.add(new ItemStack(this, 1, 0));

         for (Aspect tag : Aspect.aspects.values()) {
            ItemStack i = new ItemStack(this, 1, 1);
            this.setAspects(i, new AspectList().add(tag, this.base));
            items.add(i);
         }
      }
   }

   public String getItemStackDisplayName(ItemStack stack) {
      return this.getAspects(stack) != null && !this.getAspects(stack).aspects.isEmpty()
         ? String.format(super.getItemStackDisplayName(stack), this.getAspects(stack).getAspects()[0].getName())
         : super.getItemStackDisplayName(stack);
   }

   public String getTranslationKey(ItemStack stack) {
      return super.getTranslationKey() + "." + this.getVariantNames()[stack.getItemDamage()];
   }

   @Override
   public void onUpdate(ItemStack stack, World world, Entity entity, int par4, boolean par5) {
      if (!world.isRemote && !stack.hasTagCompound() && stack.getItemDamage() == 1) {
         stack.setItemDamage(0);
      }
   }

   @Override
   public void onCreated(ItemStack stack, World world, EntityPlayer player) {
      if (!world.isRemote && !stack.hasTagCompound() && stack.getItemDamage() == 1) {
         stack.setItemDamage(0);
      }
   }

   public boolean doesSneakBypassUse(ItemStack stack, IBlockAccess world, BlockPos pos, EntityPlayer player) {
      return true;
   }

   public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float f1, float f2, float f3, EnumHand hand) {
      IBlockState bi = world.getBlockState(pos);
      if (player.getHeldItem(hand).getItemDamage() == 0 && bi.getBlock() == BlocksTC.alembic) {
         TileAlembic tile = (TileAlembic)world.getTileEntity(pos);
         if (tile.amount >= this.base) {
            if (world.isRemote) {
               player.swingArm(hand);
               return EnumActionResult.PASS;
            }

            ItemStack phial = new ItemStack(this, 1, 1);
            this.setAspects(phial, new AspectList().add(tile.aspect, this.base));
            if (tile.takeFromContainer(tile.aspect, this.base)) {
               player.getHeldItem(hand).shrink(1);
               if (!player.inventory.addItemStackToInventory(phial)) {
                  world.spawnEntity(new EntityItem(world, player.posX, player.posY, player.posZ, phial));
               }

               player.playSound(SoundEvents.ITEM_BOTTLE_FILL, 0.25F, 1.0F);
               player.inventoryContainer.detectAndSendChanges();
               return EnumActionResult.SUCCESS;
            }
         }
      }

      if (player.getHeldItem(hand).getItemDamage() == 0 && (bi.getBlock() == BlocksTC.jarNormal || bi.getBlock() == BlocksTC.jarVoid)) {
         TileJarFillable tile = (TileJarFillable)world.getTileEntity(pos);
         if (tile.amount >= this.base) {
            if (world.isRemote) {
               player.swingArm(hand);
               return EnumActionResult.PASS;
            }

            Aspect asp = Aspect.getAspect(tile.aspect.getTag());
            if (tile.takeFromContainer(asp, this.base)) {
               player.getHeldItem(hand).shrink(1);
               ItemStack phial = new ItemStack(this, 1, 1);
               this.setAspects(phial, new AspectList().add(asp, this.base));
               if (!player.inventory.addItemStackToInventory(phial)) {
                  world.spawnEntity(new EntityItem(world, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, phial));
               }

               player.playSound(SoundEvents.ITEM_BOTTLE_FILL, 0.25F, 1.0F);
               player.inventoryContainer.detectAndSendChanges();
               return EnumActionResult.SUCCESS;
            }
         }
      }

      AspectList al = this.getAspects(player.getHeldItem(hand));
      if (al != null && al.size() == 1) {
         Aspect aspect = al.getAspects()[0];
         if (player.getHeldItem(hand).getItemDamage() != 0 && (bi.getBlock() == BlocksTC.jarNormal || bi.getBlock() == BlocksTC.jarVoid)) {
            TileJarFillable tile = (TileJarFillable)world.getTileEntity(pos);
            if (tile.amount <= 250 - this.base && tile.doesContainerAccept(aspect)) {
               if (world.isRemote) {
                  player.swingArm(hand);
                  return EnumActionResult.PASS;
               }

               if (tile.addToContainer(aspect, this.base) == 0) {
                  world.markAndNotifyBlock(pos, world.getChunk(pos), bi, bi, 3);
                  tile.markDirty();
                  player.getHeldItem(hand).shrink(1);
                  if (!player.inventory.addItemStackToInventory(new ItemStack(this, 1, 0))) {
                     world.spawnEntity(
                        new EntityItem(world, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F, new ItemStack(this, 1, 0))
                     );
                  }

                  player.playSound(SoundEvents.ITEM_BOTTLE_FILL, 0.25F, 1.0F);
                  player.inventoryContainer.detectAndSendChanges();
                  return EnumActionResult.SUCCESS;
               }
            }
         }
      }

      return EnumActionResult.PASS;
   }
}
