package thaumcraft.common.blocks.misc;

import java.util.Random;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.BlockTC;
import thaumcraft.common.blocks.crafting.BlockGolemBuilder;
import thaumcraft.common.blocks.devices.BlockInfernalFurnace;

public class BlockPlaceholder extends BlockTC {
   private final Random rand = new Random();

   public BlockPlaceholder(String name) {
      super(Material.ROCK, name);
      this.setHardness(2.5F);
      this.setSoundType(SoundType.STONE);
      this.setCreativeTab(null);
   }

   public EnumPushReaction getPushReaction(IBlockState state) {
      return EnumPushReaction.BLOCK;
   }

   protected boolean canSilkHarvest() {
      return false;
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
      return state.getBlock() == BlocksTC.placeholderCauldron ? 13 : super.getLightValue(state, world, pos);
   }

   public EnumBlockRenderType getRenderType(IBlockState state) {
      return EnumBlockRenderType.INVISIBLE;
   }

   public Item getItemDropped(IBlockState state, Random rand, int fortune) {
      if (state.getBlock() == BlocksTC.placeholderNetherbrick) {
         return Item.getItemFromBlock(Blocks.NETHER_BRICK);
      } else if (state.getBlock() == BlocksTC.placeholderObsidian) {
         return Item.getItemFromBlock(Blocks.OBSIDIAN);
      } else if (state.getBlock() == BlocksTC.placeholderBars) {
         return Item.getItemFromBlock(Blocks.IRON_BARS);
      } else if (state.getBlock() == BlocksTC.placeholderAnvil) {
         return Item.getItemFromBlock(Blocks.ANVIL);
      } else if (state.getBlock() == BlocksTC.placeholderCauldron) {
         return Item.getItemFromBlock(Blocks.CAULDRON);
      } else {
         return state.getBlock() == BlocksTC.placeholderTable ? Item.getItemFromBlock(BlocksTC.tableStone) : Item.getItemById(0);
      }
   }

   @Override
   public int damageDropped(IBlockState state) {
      return 0;
   }

   public boolean onBlockActivated(
      World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ
   ) {
      if (world.isRemote) {
         return true;
      } else {
         if (state.getBlock() != BlocksTC.placeholderNetherbrick && state.getBlock() != BlocksTC.placeholderObsidian) {
            for (int a = -1; a <= 1; a++) {
               for (int b = -1; b <= 1; b++) {
                  for (int c = -1; c <= 1; c++) {
                     IBlockState s = world.getBlockState(pos.add(a, b, c));
                     if (s.getBlock() == BlocksTC.golemBuilder) {
                        player.openGui(
                           Thaumcraft.instance,
                           19,
                           world,
                           pos.add(a, b, c).getX(),
                           pos.add(a, b, c).getY(),
                           pos.add(a, b, c).getZ()
                        );
                        return true;
                     }
                  }
               }
            }
         }

         return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
      }
   }

   public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
      if ((state.getBlock() == BlocksTC.placeholderNetherbrick || state.getBlock() == BlocksTC.placeholderObsidian)
         && !BlockInfernalFurnace.ignore
         && !worldIn.isRemote) {
         label76:
         for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
               for (int c = -1; c <= 1; c++) {
                  IBlockState s = worldIn.getBlockState(pos.add(a, b, c));
                  if (s.getBlock() == BlocksTC.infernalFurnace) {
                     BlockInfernalFurnace.destroyFurnace(worldIn, pos.add(a, b, c), s, pos);
                     break label76;
                  }
               }
            }
         }
      } else if (state.getBlock() != BlocksTC.placeholderNetherbrick
         && state.getBlock() != BlocksTC.placeholderObsidian
         && !BlockGolemBuilder.ignore
         && !worldIn.isRemote) {
         label53:
         for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
               for (int cx = -1; cx <= 1; cx++) {
                  IBlockState s = worldIn.getBlockState(pos.add(a, b, cx));
                  if (s.getBlock() == BlocksTC.golemBuilder) {
                     BlockGolemBuilder.destroy(worldIn, pos.add(a, b, cx), s, pos);
                     break label53;
                  }
               }
            }
         }
      }

      super.breakBlock(worldIn, pos, state);
   }
}
