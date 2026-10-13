package thaumcraft.common.entities.construct;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemNameTag;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandler;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftInvHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.lib.network.FakeNetHandlerPlayServer;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXBoreDig;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.lib.utils.Utils;
import thaumcraft.common.world.aura.AuraHandler;

@EventBusSubscriber
public class EntityArcaneBore extends EntityOwnedConstruct {
   BlockPos digTarget = null;
   BlockPos digTargetPrev = null;
   float digCost = 0.25F;
   int paused = 100;
   int maxPause = 100;
   long soundDelay = 0L;
   Object beam1 = null;
   double beamLength = 0.0;
   private static HashMap<Integer, ArrayList<ItemStack>> drops = new HashMap<>();
   int breakCounter = 0;
   int digDelay = 0;
   int digDelayMax = 0;
   float radInc = 0.0F;
   public int spiral = 0;
   public float currentRadius = 0.0F;
   private float charge = 0.0F;
   private static final DataParameter<EnumFacing> FACING = EntityDataManager.createKey(EntityArcaneBore.class, DataSerializers.FACING);
   private static final DataParameter<Boolean> ACTIVE = EntityDataManager.createKey(EntityArcaneBore.class, DataSerializers.BOOLEAN);
   public boolean clientDigging = false;

   public EntityArcaneBore(World worldIn) {
      super(worldIn);
      this.setSize(0.9F, 0.9F);
   }

   public EntityArcaneBore(World worldIn, BlockPos pos, EnumFacing facing) {
      this(worldIn);
      this.setFacing(facing);
      this.setPositionAndRotation(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
   }

   protected void applyEntityAttributes() {
      super.applyEntityAttributes();
      this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(50.0);
      this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0);
   }

   @Override
   public void onUpdate() {
      super.onUpdate();
      if (!this.world.isRemote) {
         this.rotationYaw = this.rotationYawHead;
         if (this.ticksExisted % 50 == 0) {
            this.heal(1.0F);
         }

         if (this.ticksExisted % 10 == 0 && this.getCharge() < 10.0F) {
            this.rechargeVis();
         }

         int k = MathHelper.floor(this.posX);
         int l = MathHelper.floor(this.posY);
         int i1 = MathHelper.floor(this.posZ);
         if (BlockRailBase.isRailBlock(this.world, new BlockPos(k, l - 1, i1))) {
            l--;
         }

         BlockPos blockpos = new BlockPos(k, l, i1);
         IBlockState iblockstate = this.world.getBlockState(blockpos);
         if (BlockRailBase.isRailBlock(iblockstate)) {
            if (iblockstate.getBlock() == BlocksTC.activatorRail) {
               boolean ac = (Boolean)iblockstate.getValue(BlockRailPowered.POWERED);
               this.setActive(!ac);
            }
         } else if (!this.isRiding()) {
            this.setActive(this.world.isBlockPowered(new BlockPos(this).down()));
         }

         if (this.validInventory()) {
            try {
               this.getHeldItemMainhand().updateAnimation(this.world, this, 0, true);
            } catch (Exception var7) {
            }
         }
      }

      if (!this.isActive()) {
         this.digTarget = null;
         this.getLookHelper()
            .setLookPosition(
               this.posX + this.getFacing().getXOffset(), this.posY, this.posZ + this.getFacing().getZOffset(), 10.0F, 33.0F
            );
      }

      if (this.digTarget != null && this.getCharge() >= this.digCost && !this.world.isRemote) {
         this.getLookHelper()
            .setLookPosition(this.digTarget.getX() + 0.5, this.digTarget.getY(), this.digTarget.getZ() + 0.5, 10.0F, 90.0F);
         if (this.digDelay-- <= 0 && this.dig()) {
            this.setCharge((byte)(this.getCharge() - this.digCost));
            if (this.soundDelay < System.currentTimeMillis()) {
               this.soundDelay = System.currentTimeMillis() + 1200L + this.world.rand.nextInt(100);
               this.playSound(SoundsTC.rumble, 0.25F, 0.9F + this.world.rand.nextFloat() * 0.2F);
            }
         }
      }

      if (!this.world.isRemote && this.digTarget == null && this.isActive() && this.validInventory()) {
         this.findNextBlockToDig();
         if (this.digTarget != null) {
            this.world.setEntityState(this, (byte)16);
            PacketHandler.INSTANCE
               .sendToAllAround(
                  new PacketFXBoreDig(this.digTarget, this, this.digDelayMax),
                  new TargetPoint(
                     this.world.provider.getDimension(),
                     this.digTarget.getX(),
                     this.digTarget.getY(),
                     this.digTarget.getZ(),
                     32.0
                  )
               );
         } else {
            this.world.setEntityState(this, (byte)17);
            this.getLookHelper()
               .setLookPosition(
                  this.posX + this.getFacing().getXOffset() * 2,
                  this.posY + this.getFacing().getYOffset() * 2 + this.getEyeHeight(),
                  this.posZ + this.getFacing().getZOffset() * 2,
                  10.0F,
                  33.0F
               );
         }
      }
   }

   public boolean validInventory() {
      boolean b = this.getHeldItemMainhand() != null
         && !this.getHeldItemMainhand().isEmpty()
         && (
            this.getHeldItemMainhand().getItem() instanceof ItemPickaxe
               || this.getHeldItemMainhand().getItem().getToolClasses(this.getHeldItemMainhand()).contains("pickaxe")
         );
      if (b && this.getHeldItemMainhand().getItemDamage() + 1 >= this.getHeldItemMainhand().getMaxDamage()) {
         b = false;
      }

      return b;
   }

   public int getDigRadius() {
      int r = 0;
      if (this.getHeldItemMainhand() != null
         && !this.getHeldItemMainhand().isEmpty()
         && (
            this.getHeldItemMainhand().getItem() instanceof ItemPickaxe
               || this.getHeldItemMainhand().getItem().getToolClasses(this.getHeldItemMainhand()).contains("pickaxe")
         )) {
         r = this.getHeldItemMainhand().getItem().getItemEnchantability() / 3;
         r += EnumInfusionEnchantment.getInfusionEnchantmentLevel(this.getHeldItemMainhand(), EnumInfusionEnchantment.DESTRUCTIVE) * 2;
      }

      return r <= 1 ? 2 : r;
   }

   public int getDigDepth() {
      int r = this.getDigRadius() * 8;
      return r + EnumInfusionEnchantment.getInfusionEnchantmentLevel(this.getHeldItemMainhand(), EnumInfusionEnchantment.BURROWING) * 16;
   }

   public int getFortune() {
      int r = 0;
      if (this.validInventory()) {
         r = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, this.getHeldItemMainhand());
         int r2 = EnumInfusionEnchantment.getInfusionEnchantmentLevel(this.getHeldItemMainhand(), EnumInfusionEnchantment.SOUNDING);
         r = Math.max(r, r2);
      }

      return r;
   }

   public int getDigSpeed(IBlockState blockState) {
      int speed = 0;
      if (this.validInventory()) {
         speed = (int)(speed + this.getHeldItemMainhand().getItem().getDestroySpeed(this.getHeldItemMainhand(), blockState) / 2.0F);
         speed += EnchantmentHelper.getEnchantmentLevel(Enchantments.EFFICIENCY, this.getHeldItemMainhand());
      }

      return speed;
   }

   public int getRefining() {
      int refining = 0;
      if (this.getHeldItemMainhand() != null && !this.getHeldItemMainhand().isEmpty()) {
         refining = EnumInfusionEnchantment.getInfusionEnchantmentLevel(this.getHeldItemMainhand(), EnumInfusionEnchantment.REFINING);
      }

      return refining;
   }

   public boolean hasSilkTouch() {
      return this.getHeldItemMainhand() != null
         && !this.getHeldItemMainhand().isEmpty()
         && EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, this.getHeldItemMainhand()) > 0;
   }

   private boolean canSilkTouch(BlockPos pos, IBlockState state) {
      return this.hasSilkTouch() && state.getBlock().canSilkHarvest(this.world, pos, state, null);
   }

   @SubscribeEvent
   public static void harvestBlockEvent(HarvestDropsEvent event) {
      if (event.getHarvester() != null && event.getHarvester().getName().equals("FakeThaumcraftBore")) {
         ArrayList<ItemStack> droplist = new ArrayList<>();
         if (drops.containsKey(event.getHarvester().arrowHitTimer) && drops.get(event.getHarvester().arrowHitTimer) != null) {
            droplist = drops.get(event.getHarvester().arrowHitTimer);
         }

         for (ItemStack s : event.getDrops()) {
            if (event.getHarvester().world.rand.nextFloat() <= event.getDropChance()) {
               droplist.add(s);
            }
         }

         drops.put(event.getHarvester().arrowHitTimer, droplist);
         event.getDrops().clear();
      }
   }

   private boolean dig() {
      boolean b = false;
      if (this.digTarget != null && !this.world.isAirBlock(this.digTarget)) {
         IBlockState digBs = this.world.getBlockState(this.digTarget);
         if (!digBs.getBlock().isAir(digBs, this.world, this.digTarget)) {
            boolean silktouch = false;
            int fortune = this.getFortune();
            if (this.canSilkTouch(this.digTarget, digBs)) {
               silktouch = true;
               fortune = 0;
            }

            FakePlayer fp = FakePlayerFactory.get((WorldServer)this.world, new GameProfile((UUID)null, "FakeThaumcraftBore"));
            fp.connection = new FakeNetHandlerPlayServer(fp.server, new NetworkManager(EnumPacketDirection.CLIENTBOUND), fp);
            fp.arrowHitTimer = this.getEntityId();
            fp.xpCooldown = 1;
            fp.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
            fp.setHeldItem(EnumHand.MAIN_HAND, this.getHeldItemMainhand());
            if (BlockUtils.harvestBlock(this.getEntityWorld(), fp, this.digTarget, false, false, fortune, false)) {
               ArrayList<ItemStack> items = drops.get(this.getEntityId());
               if (items == null) {
                  items = new ArrayList<>();
               }

               List<EntityItem> targets = this.world
                  .getEntitiesWithinAABB(
                     EntityItem.class,
                     new AxisAlignedBB(
                           this.digTarget.getX(),
                           this.digTarget.getY(),
                           this.digTarget.getZ(),
                           this.digTarget.getX() + 1,
                           this.digTarget.getY() + 1,
                           this.digTarget.getZ() + 1
                        )
                        .grow(1.5, 1.5, 1.5)
                  );
               if (targets.size() > 0) {
                  for (EntityItem e : targets) {
                     items.add(e.getItem().copy());
                     e.setDead();
                  }
               }

               int refining = this.getRefining();
               if (items.size() > 0) {
                  for (ItemStack is : items) {
                     ItemStack dropped = is.copy();
                     if (!silktouch && refining > 0) {
                        dropped = Utils.findSpecialMiningResult(is, (refining + 1) * 0.125F, this.world.rand);
                     }

                     if (dropped != null && !dropped.isEmpty()) {
                        boolean e = false;

                        for (EnumFacing f : EnumFacing.VALUES) {
                           BlockPos p = this.getPosition().offset(f);
                           IItemHandler inventory = ThaumcraftInvHelper.getItemHandlerAt(this.getEntityWorld(), p, f);
                           if (inventory != null) {
                              InventoryUtils.ejectStackAt(this.getEntityWorld(), this.getPosition(), f, dropped);
                              e = true;
                              break;
                           }
                        }

                        if (!e) {
                           InventoryUtils.ejectStackAt(this.getEntityWorld(), this.getPosition(), this.getFacing().getOpposite(), dropped);
                        }
                     }
                  }
               }

               this.breakCounter = this.breakCounter + fp.xpCooldown;
               items.clear();
            }
         }

         if (this.getHeldItemMainhand() != null && !this.getHeldItemMainhand().isEmpty()) {
            if (this.breakCounter >= 50) {
               this.breakCounter -= 50;
               this.getHeldItemMainhand().damageItem(1, this);
            }

            if (this.getHeldItemMainhand().getCount() <= 0) {
               this.setHeldItem(this.getActiveHand(), ItemStack.EMPTY);
            }
         } else {
            this.breakCounter = 0;
         }

         b = this.world.setBlockToAir(this.digTarget);
      }

      this.digTarget = null;
      return b;
   }

   private void findNextBlockToDig() {
      if (this.digTargetPrev == null || this.getDistanceSqToCenter(this.digTargetPrev) > (this.getDigRadius() + 1) * (this.getDigRadius() + 1)) {
         this.digTargetPrev = new BlockPos(this);
      }

      if (this.radInc == 0.0F) {
         this.radInc = 1.0F;
      }

      int digRadius = this.getDigRadius();
      int digDepth = this.getDigDepth();
      int x = this.digTargetPrev.getX();
      int z = this.digTargetPrev.getZ();
      int y = this.digTargetPrev.getY();
      int x2 = x + this.getFacing().getXOffset() * digDepth;
      int y2 = y + this.getFacing().getYOffset() * digDepth;
      int z2 = z + this.getFacing().getZOffset() * digDepth;
      BlockPos end = new BlockPos(x2, y2, z2);
      RayTraceResult mop = this.world
         .rayTraceBlocks(new Vec3d(this.digTargetPrev).add(0.5, 0.5, 0.5), new Vec3d(end).add(0.5, 0.5, 0.5), false, true, false);
      if (mop != null) {
         Vec3d digger = new Vec3d(
            this.posX + this.getFacing().getXOffset(),
            this.posY + this.getEyeHeight() + this.getFacing().getYOffset(),
            this.posZ + this.getFacing().getZOffset()
         );
         mop = this.world.rayTraceBlocks(digger, new Vec3d(mop.getBlockPos()).add(0.5, 0.5, 0.5), false, true, false);
         if (mop != null) {
            IBlockState blockState = this.world.getBlockState(mop.getBlockPos());
            if (blockState.getBlockHardness(this.world, mop.getBlockPos()) > -1.0F
               && blockState.getCollisionBoundingBox(this.world, mop.getBlockPos()) != null) {
               this.digDelay = Math.max(
                  10 - this.getDigSpeed(blockState),
                  (int)(blockState.getBlockHardness(this.world, mop.getBlockPos()) * 2.0F) - this.getDigSpeed(blockState) * 2
               );
               if (this.digDelay < 1) {
                  this.digDelay = 1;
               }

               this.digDelayMax = this.digDelay;
               if (!mop.getBlockPos().equals(this.getPosition()) && !mop.getBlockPos().equals(this.getPosition().down())) {
                  this.digTarget = mop.getBlockPos();
                  return;
               }
            }
         }
      }

      while (x == this.digTargetPrev.getX() && z == this.digTargetPrev.getZ() && y == this.digTargetPrev.getY()) {
         if (Math.abs(this.currentRadius) > digRadius) {
            this.currentRadius = digRadius;
         }

         this.spiral = (int)(this.spiral + (3.0F + Math.max(0.0F, (10.0F - Math.abs(this.currentRadius)) * 2.0F)));
         if (this.spiral >= 360) {
            this.spiral -= 360;
            this.currentRadius = this.currentRadius + this.radInc;
            if (this.currentRadius > digRadius || this.currentRadius < -digRadius) {
               this.currentRadius = 0.0F;
            }
         }

         Vec3d vsource = new Vec3d(
            (int)this.posX + 0.5 + this.getFacing().getXOffset(),
            this.posY + this.getFacing().getYOffset() + this.getEyeHeight(),
            (int)this.posZ + 0.5 + this.getFacing().getZOffset()
         );
         Vec3d vtar = new Vec3d(0.0, this.currentRadius, 0.0);
         vtar = Utils.rotateAroundZ(vtar, this.spiral / 180.0F * (float) Math.PI);
         vtar = Utils.rotateAroundY(vtar, (float) (Math.PI / 2) * this.getFacing().getXOffset());
         vtar = Utils.rotateAroundX(vtar, (float) (Math.PI / 2) * this.getFacing().getYOffset());
         Vec3d vres = vsource.add(vtar.x, vtar.y, vtar.z);
         x = MathHelper.floor(vres.x);
         y = MathHelper.floor(vres.y);
         z = MathHelper.floor(vres.z);
      }

      this.digTargetPrev = new BlockPos(x, y, z);
   }

   public boolean attackEntityFrom(DamageSource source, float amount) {
      try {
         if (source.getTrueSource() != null && this.isOwner((EntityLivingBase)source.getTrueSource())) {
            EnumFacing f = EnumFacing.getDirectionFromEntityLiving(this.getPosition(), (EntityLivingBase)source.getTrueSource());
            if (f != EnumFacing.DOWN) {
               this.setFacing(f);
            }

            return false;
         }
      } catch (Exception var4) {
      }

      this.rotationYaw = (float)(this.rotationYaw + this.getRNG().nextGaussian() * 45.0);
      this.rotationPitch = (float)(this.rotationPitch + this.getRNG().nextGaussian() * 20.0);
      return super.attackEntityFrom(source, amount);
   }

   protected void rechargeVis() {
      this.setCharge(this.getCharge() + AuraHandler.drainVis(this.world, this.getPosition(), 10.0F, false));
   }

   public boolean canBePushed() {
      return true;
   }

   public boolean canBeCollidedWith() {
      return true;
   }

   @Override
   public void onDeath(DamageSource cause) {
      super.onDeath(cause);
      if (!this.world.isRemote) {
         this.dropStuff();
      }
   }

   protected void dropStuff() {
      if (this.getHeldItemMainhand() != null && !this.getHeldItemMainhand().isEmpty()) {
         this.entityDropItem(this.getHeldItemMainhand(), 0.5F);
      }
   }

   @Override
   protected boolean processInteract(EntityPlayer player, EnumHand hand) {
      if (player.getHeldItem(hand).getItem() instanceof ItemNameTag) {
         return false;
      } else if (!this.world.isRemote && this.isOwner(player) && !this.isDead) {
         if (player.isSneaking()) {
            this.playSound(SoundsTC.zap, 1.0F, 1.0F);
            this.dropStuff();
            this.entityDropItem(new ItemStack(ItemsTC.turretPlacer, 1, 2), 0.5F);
            this.setDead();
            player.swingArm(hand);
         } else {
            player.openGui(Thaumcraft.instance, 14, this.world, this.getEntityId(), 0, 0);
         }

         return true;
      } else {
         return super.processInteract(player, hand);
      }
   }

   public void knockBack(Entity entityIn, float strength, double xRatio, double zRatio) {
      super.knockBack(entityIn, strength, xRatio / 10.0, zRatio / 10.0);
      if (this.motionY > 0.1) {
         this.motionY = 0.1;
      }
   }

   @Override
   protected void entityInit() {
      super.entityInit();
      this.getDataManager().register(FACING, EnumFacing.DOWN);
      this.dataManager.register(ACTIVE, false);
   }

   public boolean isActive() {
      return (Boolean)this.dataManager.get(ACTIVE);
   }

   public void setActive(boolean attacking) {
      this.dataManager.set(ACTIVE, attacking);
   }

   @Override
   public void readEntityFromNBT(NBTTagCompound nbt) {
      super.readEntityFromNBT(nbt);
      this.setCharge(nbt.getFloat("charge"));
      this.setFacing(EnumFacing.VALUES[nbt.getByte("faceing")]);
      this.setActive(nbt.getBoolean("active"));
   }

   @Override
   public void writeEntityToNBT(NBTTagCompound nbt) {
      super.writeEntityToNBT(nbt);
      nbt.setFloat("charge", this.getCharge());
      nbt.setByte("faceing", (byte)this.getFacing().getIndex());
      nbt.setBoolean("active", this.isActive());
   }

   public EnumFacing getFacing() {
      return (EnumFacing)this.getDataManager().get(FACING);
   }

   public void setFacing(EnumFacing face) {
      this.getDataManager().set(FACING, face);
   }

   public float getCharge() {
      return this.charge;
   }

   public void setCharge(float c) {
      this.charge = c;
   }

   public void move(MoverType mt, double x, double y, double z) {
      super.move(mt, x / 5.0, y, z / 5.0);
   }

   public void onKillCommand() {
      this.attackEntityFrom(DamageSource.OUT_OF_WORLD, 400.0F);
   }

   protected void dropFewItems(boolean wasRecentlyHit, int treasure) {
      float b = treasure * 0.15F;
      if (this.rand.nextFloat() < 0.2F + b) {
         this.entityDropItem(new ItemStack(ItemsTC.mind), 0.5F);
      }

      if (this.rand.nextFloat() < 0.2F + b) {
         this.entityDropItem(new ItemStack(ItemsTC.morphicResonator), 0.5F);
      }

      if (this.rand.nextFloat() < 0.2F + b) {
         this.entityDropItem(new ItemStack(BlocksTC.crystalAir), 0.5F);
      }

      if (this.rand.nextFloat() < 0.2F + b) {
         this.entityDropItem(new ItemStack(BlocksTC.crystalEarth), 0.5F);
      }

      if (this.rand.nextFloat() < 0.5F + b) {
         this.entityDropItem(new ItemStack(ItemsTC.mechanismSimple), 0.5F);
      }

      if (this.rand.nextFloat() < 0.5F + b) {
         this.entityDropItem(new ItemStack(ItemsTC.plate), 0.5F);
      }

      if (this.rand.nextFloat() < 0.5F + b) {
         this.entityDropItem(new ItemStack(BlocksTC.plankGreatwood), 0.5F);
      }
   }

   public int getVerticalFaceSpeed() {
      return 10;
   }

   @Override
   public Team getTeam() {
      if (this.isOwned()) {
         EntityLivingBase entitylivingbase = this.getOwnerEntity();
         if (entitylivingbase != null) {
            return entitylivingbase.getTeam();
         }
      }

      return super.getTeam();
   }

   public float getEyeHeight() {
      return 0.8125F;
   }

   @SideOnly(Side.CLIENT)
   public void handleStatusUpdate(byte par1) {
      if (par1 == 16) {
         this.clientDigging = true;
      } else if (par1 == 17) {
         this.clientDigging = false;
      } else {
         super.handleStatusUpdate(par1);
      }
   }
}
