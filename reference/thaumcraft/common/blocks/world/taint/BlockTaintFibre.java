package thaumcraft.common.blocks.world.taint;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.potions.PotionFluxTaint;
import thaumcraft.codechicken.lib.raytracer.ExtendedMOP;
import thaumcraft.codechicken.lib.raytracer.IndexedCuboid6;
import thaumcraft.codechicken.lib.raytracer.RayTracer;
import thaumcraft.codechicken.lib.vec.BlockCoord;
import thaumcraft.codechicken.lib.vec.Cuboid6;
import thaumcraft.codechicken.lib.vec.Vector3;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.lib.SoundsTC;

@EventBusSubscriber({Side.CLIENT})
public class BlockTaintFibre extends Block implements ITaintBlock {
   public static final PropertyBool NORTH = PropertyBool.create("north");
   public static final PropertyBool EAST = PropertyBool.create("east");
   public static final PropertyBool SOUTH = PropertyBool.create("south");
   public static final PropertyBool WEST = PropertyBool.create("west");
   public static final PropertyBool UP = PropertyBool.create("up");
   public static final PropertyBool DOWN = PropertyBool.create("down");
   public static final PropertyBool GROWTH1 = PropertyBool.create("growth1");
   public static final PropertyBool GROWTH2 = PropertyBool.create("growth2");
   public static final PropertyBool GROWTH3 = PropertyBool.create("growth3");
   public static final PropertyBool GROWTH4 = PropertyBool.create("growth4");
   private RayTracer rayTracer = new RayTracer();
   protected static final AxisAlignedBB AABB_EMPTY = new AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   protected static final AxisAlignedBB AABB_UP = new AxisAlignedBB(0.0, 0.95F, 0.0, 1.0, 1.0, 1.0);
   protected static final AxisAlignedBB AABB_DOWN = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.05F, 1.0);
   protected static final AxisAlignedBB AABB_EAST = new AxisAlignedBB(0.95F, 0.0, 0.0, 1.0, 1.0, 1.0);
   protected static final AxisAlignedBB AABB_WEST = new AxisAlignedBB(0.0, 0.0, 0.0, 0.05F, 1.0, 1.0);
   protected static final AxisAlignedBB AABB_SOUTH = new AxisAlignedBB(0.0, 0.0, 0.95F, 1.0, 1.0, 1.0);
   protected static final AxisAlignedBB AABB_NORTH = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 0.05F);

   public BlockTaintFibre() {
      super(ThaumcraftMaterials.MATERIAL_TAINT);
      this.setTranslationKey("taint_fibre");
      this.setRegistryName("thaumcraft", "taint_fibre");
      this.setHardness(1.0F);
      this.setSoundType(SoundsTC.GORE);
      this.setTickRandomly(true);
      this.setCreativeTab(ConfigItems.TABTC);
      this.setDefaultState(
         this.blockState
            .getBaseState()
            .withProperty(NORTH, false)
            .withProperty(EAST, false)
            .withProperty(SOUTH, false)
            .withProperty(WEST, false)
            .withProperty(UP, false)
            .withProperty(DOWN, false)
            .withProperty(GROWTH1, false)
            .withProperty(GROWTH2, false)
            .withProperty(GROWTH3, false)
            .withProperty(GROWTH4, false)
      );
   }

   public SoundType getSoundType() {
      return SoundsTC.GORE;
   }

   public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
      return BlockFaceShape.UNDEFINED;
   }

   public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
      return 3;
   }

   public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
      return 3;
   }

   public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
      return MapColor.PURPLE;
   }

   @Override
   public void die(World world, BlockPos pos, IBlockState blockState) {
      world.setBlockToAir(pos);
   }

   protected boolean canSilkHarvest() {
      return false;
   }

   public Item getItemDropped(IBlockState state, Random rand, int fortune) {
      return Item.getItemById(0);
   }

   public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
      state = this.getActualState(state, worldIn, pos);
      if (state instanceof IBlockState && (Boolean)state.getValue(GROWTH3)) {
         if (worldIn.rand.nextInt(5) <= fortune) {
            spawnAsEntity(worldIn, pos, ConfigItems.FLUX_CRYSTAL.copy());
         }

         AuraHelper.polluteAura(worldIn, pos, 1.0F, true);
      }
   }

   public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
      if (!world.isRemote) {
         state = this.getActualState(state, world, pos);
         if (state instanceof IBlockState) {
            if (!(Boolean)state.getValue(GROWTH1)
               && !(Boolean)state.getValue(GROWTH2)
               && !(Boolean)state.getValue(GROWTH3)
               && !(Boolean)state.getValue(GROWTH4)
               && isOnlyAdjacentToTaint(world, pos)) {
               this.die(world, pos, state);
            } else if (!TaintHelper.isNearTaintSeed(world, pos)) {
               this.die(world, pos, state);
            } else {
               TaintHelper.spreadFibres(world, pos);
            }
         }
      }
   }

   public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos pos2) {
      state = this.getActualState(state, worldIn, pos);
      if (state instanceof IBlockState
         && !(Boolean)state.getValue(GROWTH1)
         && !(Boolean)state.getValue(GROWTH2)
         && !(Boolean)state.getValue(GROWTH3)
         && !(Boolean)state.getValue(GROWTH4)
         && isOnlyAdjacentToTaint(worldIn, pos)) {
         worldIn.setBlockToAir(pos);
      }
   }

   public static int getAdjacentTaint(IBlockAccess world, BlockPos pos) {
      int count = 0;

      for (EnumFacing dir : EnumFacing.VALUES) {
         if (world.getBlockState(pos.offset(dir)).getMaterial() != ThaumcraftMaterials.MATERIAL_TAINT) {
            count++;
         }
      }

      return count;
   }

   public static boolean isOnlyAdjacentToTaint(World world, BlockPos pos) {
      for (EnumFacing dir : EnumFacing.VALUES) {
         if (!world.isAirBlock(pos.offset(dir))
            && world.getBlockState(pos.offset(dir)).getMaterial() != ThaumcraftMaterials.MATERIAL_TAINT
            && world.getBlockState(pos.offset(dir))
               .getBlock()
               .isSideSolid(world.getBlockState(pos.offset(dir)), world, pos.offset(dir), dir.getOpposite())) {
            return false;
         }
      }

      return true;
   }

   public static boolean isHemmedByTaint(World world, BlockPos pos) {
      int c = 0;

      for (EnumFacing dir : EnumFacing.VALUES) {
         IBlockState block = world.getBlockState(pos.offset(dir));
         if (block.getMaterial() == ThaumcraftMaterials.MATERIAL_TAINT) {
            c++;
         } else if (world.isAirBlock(pos.offset(dir))) {
            c--;
         } else if (!block.getMaterial().isLiquid() && !block.isSideSolid(world, pos.offset(dir), dir.getOpposite())) {
            c--;
         }
      }

      return c > 0;
   }

   public void onEntityWalk(World world, BlockPos pos, Entity entity) {
      if (!world.isRemote && entity instanceof EntityLivingBase && !((EntityLivingBase)entity).isEntityUndead() && world.rand.nextInt(750) == 0) {
         ((EntityLivingBase)entity).addPotionEffect(new PotionEffect(PotionFluxTaint.instance, 200, 0, false, true));
      }
   }

   public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int eventID, int eventParam) {
      if (eventID == 1) {
         if (worldIn.isRemote) {
            worldIn.playSound(null, pos, SoundEvents.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.BLOCKS, 0.1F, 0.9F + worldIn.rand.nextFloat() * 0.2F);
         }

         return true;
      } else {
         return super.eventReceived(state, worldIn, pos, eventID, eventParam);
      }
   }

   @SideOnly(Side.CLIENT)
   public BlockRenderLayer getRenderLayer() {
      return BlockRenderLayer.CUTOUT;
   }

   public boolean isSideSolid(IBlockState base_state, IBlockAccess world, BlockPos pos, EnumFacing side) {
      return false;
   }

   private boolean drawAt(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
      IBlockState b = worldIn.getBlockState(pos);
      return b.getBlock() != BlocksTC.taintFibre && b.getBlock() != BlocksTC.taintFeature && b.isSideSolid(worldIn, pos, side.getOpposite());
   }

   @SideOnly(Side.CLIENT)
   @SubscribeEvent
   public void onBlockHighlight(DrawBlockHighlightEvent event) {
      if (event.getTarget().typeOfHit == Type.BLOCK
         && event.getPlayer().world.getBlockState(event.getTarget().getBlockPos()).getBlock() == this) {
         RayTracer.retraceBlock(event.getPlayer().world, event.getPlayer(), event.getTarget().getBlockPos());
      }
   }

   public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
      List<IndexedCuboid6> cuboids = new LinkedList<>();
      if (this.drawAt(world, pos.up(), EnumFacing.UP)) {
         cuboids.add(new IndexedCuboid6(0, new Cuboid6(AABB_UP.offset(pos))));
      }

      if (this.drawAt(world, pos.down(), EnumFacing.DOWN)) {
         cuboids.add(new IndexedCuboid6(1, new Cuboid6(AABB_DOWN.offset(pos))));
      }

      if (this.drawAt(world, pos.east(), EnumFacing.EAST)) {
         cuboids.add(new IndexedCuboid6(2, new Cuboid6(AABB_EAST.offset(pos))));
      }

      if (this.drawAt(world, pos.west(), EnumFacing.WEST)) {
         cuboids.add(new IndexedCuboid6(3, new Cuboid6(AABB_WEST.offset(pos))));
      }

      if (this.drawAt(world, pos.south(), EnumFacing.SOUTH)) {
         cuboids.add(new IndexedCuboid6(4, new Cuboid6(AABB_SOUTH.offset(pos))));
      }

      if (this.drawAt(world, pos.north(), EnumFacing.NORTH)) {
         cuboids.add(new IndexedCuboid6(5, new Cuboid6(AABB_NORTH.offset(pos))));
      }

      IBlockState ss = this.getActualState(world.getBlockState(pos), world, pos);
      if (ss.getBlock() == this && ss instanceof IBlockState) {
         if ((Boolean)ss.getValue(GROWTH1)) {
            cuboids.add(new IndexedCuboid6(6, new Cuboid6(new AxisAlignedBB(0.1F, 0.0, 0.1F, 0.9F, 0.4F, 0.9F).offset(pos))));
         } else if ((Boolean)ss.getValue(GROWTH2)) {
            cuboids.add(new IndexedCuboid6(6, new Cuboid6(new AxisAlignedBB(0.2F, 0.0, 0.2F, 0.8F, 1.0, 0.8F).offset(pos))));
         } else if ((Boolean)ss.getValue(GROWTH3)) {
            cuboids.add(new IndexedCuboid6(6, new Cuboid6(new AxisAlignedBB(0.25, 0.0, 0.25, 0.75, 0.3125, 0.75).offset(pos))));
         } else if ((Boolean)ss.getValue(GROWTH4)) {
            cuboids.add(new IndexedCuboid6(6, new Cuboid6(new AxisAlignedBB(0.1F, 0.3F, 0.1F, 0.9F, 1.0, 0.9F).offset(pos))));
         }
      }

      ArrayList<ExtendedMOP> list = new ArrayList<>();
      this.rayTracer.rayTraceCuboids(new Vector3(start), new Vector3(end), cuboids, new BlockCoord(pos), this, list);
      return list.size() > 0 ? list.get(0) : super.collisionRayTrace(state, world, pos, start, end);
   }

   public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess source, BlockPos pos) {
      return AABB_EMPTY;
   }

   @SideOnly(Side.CLIENT)
   public AxisAlignedBB getSelectedBoundingBox(IBlockState s, World world, BlockPos pos) {
      IBlockState state = this.getActualState(world.getBlockState(pos), world, pos);
      if (state.getBlock() == this && state instanceof IBlockState) {
         if ((Boolean)state.getValue(GROWTH1)) {
            return new AxisAlignedBB(0.1F, 0.0, 0.1F, 0.9F, 0.4F, 0.9F).offset(pos);
         }

         if ((Boolean)state.getValue(GROWTH2)) {
            return new AxisAlignedBB(0.2F, 0.0, 0.2F, 0.8F, 1.0, 0.8F).offset(pos);
         }

         if ((Boolean)state.getValue(GROWTH3)) {
            return new AxisAlignedBB(0.25, 0.0, 0.25, 0.75, 0.3125, 0.75).offset(pos);
         }

         if ((Boolean)state.getValue(GROWTH4)) {
            return new AxisAlignedBB(0.1F, 0.3F, 0.1F, 0.9F, 1.0, 0.9F).offset(pos);
         }
      }

      RayTraceResult hit = RayTracer.retraceBlock(world, Minecraft.getMinecraft().player, pos);
      if (hit != null) {
         switch (hit.subHit) {
            case 0:
               return AABB_UP.offset(pos);
            case 1:
               return AABB_DOWN.offset(pos);
            case 2:
               return AABB_EAST.offset(pos);
            case 3:
               return AABB_WEST.offset(pos);
            case 4:
               return AABB_SOUTH.offset(pos);
            case 5:
               return AABB_NORTH.offset(pos);
         }
      }

      return AABB_EMPTY;
   }

   public void addCollisionBoxToList(
      IBlockState state,
      World worldIn,
      BlockPos pos,
      AxisAlignedBB entityBox,
      List<AxisAlignedBB> collidingBoxes,
      @Nullable Entity entityIn,
      boolean isActualState
   ) {
      if (this.drawAt(worldIn, pos.up(), EnumFacing.UP)) {
         addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_UP);
      }

      if (this.drawAt(worldIn, pos.down(), EnumFacing.DOWN)) {
         addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_DOWN);
      }

      if (this.drawAt(worldIn, pos.east(), EnumFacing.EAST)) {
         addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_EAST);
      }

      if (this.drawAt(worldIn, pos.west(), EnumFacing.WEST)) {
         addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_WEST);
      }

      if (this.drawAt(worldIn, pos.south(), EnumFacing.SOUTH)) {
         addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_SOUTH);
      }

      if (this.drawAt(worldIn, pos.north(), EnumFacing.NORTH)) {
         addCollisionBoxToList(pos, entityBox, collidingBoxes, AABB_NORTH);
      }
   }

   public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
      return true;
   }

   public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
      return true;
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   public int getMetaFromState(IBlockState state) {
      return 0;
   }

   public int getLightValue(IBlockState state2, IBlockAccess world, BlockPos pos) {
      IBlockState state = this.getActualState(world.getBlockState(pos), world, pos);
      if (state.getBlock() == this && state instanceof IBlockState) {
         return state.getValue(GROWTH3)
            ? 12
            : (!state.getValue(GROWTH2) && !state.getValue(GROWTH4) ? super.getLightValue(state2, world, pos) : 6);
      } else {
         return super.getLightValue(state2, world, pos);
      }
   }

   private Boolean[] makeConnections(IBlockState state, IBlockAccess world, BlockPos pos) {
      Boolean[] cons = new Boolean[]{false, false, false, false, false, false};
      int a = 0;

      for (EnumFacing face : EnumFacing.VALUES) {
         if (this.drawAt(world, pos.offset(face), face)) {
            cons[a] = true;
         }

         a++;
      }

      return cons;
   }

   public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
      Boolean[] cons = this.makeConnections(state, worldIn, pos);
      boolean d = this.drawAt(worldIn, pos.down(), EnumFacing.DOWN);
      boolean u = this.drawAt(worldIn, pos.up(), EnumFacing.UP);
      int growth = 0;
      Random rand = new Random(pos.toLong());
      int q = rand.nextInt(50);
      if (d) {
         if (q < 4) {
            growth = 1;
         } else if (q == 4 || q == 5) {
            growth = 2;
         } else if (q == 6) {
            growth = 3;
         }
      }

      if (u && q > 47) {
         growth = 4;
      }

      try {
         return state.withProperty(DOWN, cons[0])
            .withProperty(UP, cons[1])
            .withProperty(NORTH, cons[2])
            .withProperty(SOUTH, cons[3])
            .withProperty(WEST, cons[4])
            .withProperty(EAST, cons[5])
            .withProperty(GROWTH1, growth == 1)
            .withProperty(GROWTH2, growth == 2)
            .withProperty(GROWTH3, growth == 3)
            .withProperty(GROWTH4, growth == 4);
      } catch (Exception var11) {
         return state;
      }
   }

   protected BlockStateContainer createBlockState() {
      return new BlockStateContainer(this, new IProperty[]{NORTH, EAST, SOUTH, WEST, UP, DOWN, GROWTH1, GROWTH2, GROWTH3, GROWTH4});
   }
}
