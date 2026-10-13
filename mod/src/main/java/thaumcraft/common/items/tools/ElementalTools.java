package thaumcraft.common.items.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftMaterials;
import thaumcraft.api.items.IArchitect;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.common.items.ICreativeVariants;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;
import thaumcraft.common.registers.ModGear;
import thaumcraft.common.registers.ModSounds;

/**
 * The elemental tools (ItemElementalAxe/Hoe/Pickaxe/Shovel/Sword). They come out of the infusion altar with infusion
 * enchantments already on them (the recipes carry the result stacks; the creative tab lists them the same way).
 */
public final class ElementalTools {
   private ElementalTools() {
   }

   /** ItemElementalAxe: hold right-click to pull nearby items in. Burrowing + Collector. */
   public static class Axe extends AxeItem implements ICreativeVariants {
      public Axe(Properties p) {
         super(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, p.attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, 8.0F, -3.0F)));
      }

      @Override
      public UseAnim getUseAnimation(ItemStack stack) {
         return UseAnim.BOW;
      }

      @Override
      public int getUseDuration(ItemStack stack, LivingEntity entity) {
         return 72000;
      }

      @Override
      public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
         player.startUsingItem(hand);
         return InteractionResultHolder.success(player.getItemInHand(hand));
      }

      @Override
      public void onUseTick(Level level, LivingEntity player, ItemStack stack, int count) {
         List<ItemEntity> stuff = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(10.0), e -> e.distanceTo(player) <= 10.0);
         for (ItemEntity e : stuff) {
            if (!e.isAlive()) continue;
            double d6 = e.getX() - player.getX();
            double d8 = e.getY() - player.getY() + player.getBbHeight() / 2.0F;
            double d10 = e.getZ() - player.getZ();
            double d11 = Math.sqrt(d6 * d6 + d8 * d8 + d10 * d10);
            d6 /= d11;
            d8 /= d11;
            d10 /= d11;
            double d13 = 0.3;
            Vec3 m = e.getDeltaMovement();
            double mx = Mth.clamp(m.x - d6 * d13, -0.25, 0.25);
            double my = Mth.clamp(m.y - (d8 * d13 - 0.1), -0.25, 0.25);
            double mz = Mth.clamp(m.z - d10 * d13, -0.25, 0.25);
            e.setDeltaMovement(mx, my, mz);
            if (level.isClientSide) {
               var r = level.random;
               FXDispatcher.crucibleBubble((float) e.getX() + (r.nextFloat() - r.nextFloat()) * 0.2F,
                  (float) e.getY() + e.getBbHeight() + (r.nextFloat() - r.nextFloat()) * 0.2F,
                  (float) e.getZ() + (r.nextFloat() - r.nextFloat()) * 0.2F, 0.33F, 0.33F, 1.0F);
            }
         }
      }

      @Override
      public void addCreativeStacks(Consumer<ItemStack> output) {
         ItemStack w1 = new ItemStack(this);
         EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.BURROWING, 1);
         EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.COLLECTOR, 1);
         output.accept(w1);
      }
   }

   /** ItemElementalHoe: tills 3x3, or bonemeals what can't be tilled. */
   public static class Hoe extends HoeItem {
      public Hoe(Properties p) {
         super(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, p.attributes(ThaumiumTools.hoeAttributes(ThaumcraftMaterials.TOOLMAT_ELEMENTAL)));
      }

      @Override
      public int getEnchantmentValue() {
         return 5;
      }

      @Override
      public InteractionResult useOn(UseOnContext ctx) {
         Player player = ctx.getPlayer();
         if (player == null || player.isShiftKeyDown()) return super.useOn(ctx);
         Level world = ctx.getLevel();
         BlockPos pos = ctx.getClickedPos();
         boolean did = false;
         for (int xx = -1; xx <= 1; xx++) {
            for (int zz = -1; zz <= 1; zz++) {
               BlockPos pp = pos.offset(xx, 0, zz);
               BlockHitResult hit = new BlockHitResult(ctx.getClickLocation().add(xx, 0, zz), ctx.getClickedFace(), pp, false);
               if (super.useOn(new UseOnContext(player, ctx.getHand(), hit)).consumesAction()) {
                  if (world.isClientSide) {
                     FXDispatcher.drawBamf(pp.getX() + 0.5, pp.getY() + 1.01, pp.getZ() + 0.5, 0.3F, 0.12F, 0.1F, xx == 0 && zz == 0, false, Direction.UP);
                  }
                  did = true;
               }
            }
         }
         if (!did) {
            // Utils.useBonemealAtLoc
            did = BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL), world, pos, player);
            if (did) {
               ctx.getItemInHand().hurtAndBreak(3, player, LivingEntity.getSlotForHand(ctx.getHand()));
               if (!world.isClientSide) world.levelEvent(1505, pos, 15);
            }
         }
         return InteractionResult.sidedSuccess(world.isClientSide);
      }
   }

   /** ItemElementalPickaxe: sets what it hits on fire. Refining + Sounding II. */
   public static class Pickaxe extends PickaxeItem implements ICreativeVariants {
      public Pickaxe(Properties p) {
         super(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, p.attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, 1.0F, -2.8F)));
      }

      @Override
      public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
         if (!player.level().isClientSide && VoidTools.canAffect(entity, player)) entity.igniteForSeconds(2);
         return super.onLeftClickEntity(stack, player, entity);
      }

      @Override
      public void addCreativeStacks(Consumer<ItemStack> output) {
         ItemStack w1 = new ItemStack(this);
         EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.REFINING, 1);
         EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.SOUNDING, 2);
         output.accept(w1);
      }
   }

   /**
    * ItemElementalShovel: right-click places a 3x3 plane of the clicked block's kind (taken from the inventory) against the
    * face; the "or" orientation (thaumcraft:orientation) chooses how the plane is laid. Destructive.
    */
   public static class Shovel extends ShovelItem implements IArchitect, ICreativeVariants {
      public Shovel(Properties p) {
         super(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, p.attributes(DiggerItem.createAttributes(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, 1.5F, -3.0F)));
      }

      public static int getOrientation(ItemStack stack) {
         return stack.getOrDefault(ModGear.ORIENTATION.get(), 0);
      }

      /** Set by the item key in 1.12 (PacketItemKeyToServer); the key binding comes with the casters' key handler. */
      public static void setOrientation(ItemStack stack, int o) {
         stack.set(ModGear.ORIENTATION.get(), o % 3);
      }

      /** The offsets (xx, yy, zz) of the 3x3 plane, as computed in onItemUse/getArchitectBlocks. */
      static List<BlockPos> plane(ItemStack stack, BlockPos pos, Direction side, Player player) {
         List<BlockPos> out = new ArrayList<>();
         int o = getOrientation(stack);
         int s = side.get3DDataValue();
         for (int aa = -1; aa <= 1; aa++) {
            for (int bb = -1; bb <= 1; bb++) {
               int xx = 0, yy = 0, zz = 0;
               if (o == 1) {
                  yy = bb;
                  if (s <= 1) {
                     int l = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5) & 3;
                     if (l != 0 && l != 2) zz = aa;
                     else xx = aa;
                  } else if (s <= 3) {
                     zz = aa;
                  } else {
                     xx = aa;
                  }
               } else if (o == 2) {
                  if (s <= 1) {
                     int l = Mth.floor(player.getYRot() * 4.0F / 360.0F + 0.5) & 3;
                     yy = bb;
                     if (l != 0 && l != 2) zz = aa;
                     else xx = aa;
                  } else {
                     zz = bb;
                     xx = aa;
                  }
               } else if (s <= 1) {
                  xx = aa;
                  zz = bb;
               } else if (s <= 3) {
                  xx = aa;
                  yy = bb;
               } else {
                  zz = aa;
                  yy = bb;
               }
               out.add(pos.relative(side).offset(xx, yy, zz));
            }
         }
         return out;
      }

      private static boolean canPlace(Level level, BlockState bs, BlockPos p2) {
         return level.getBlockState(p2).canBeReplaced() && bs.canSurvive(level, p2);
      }

      private static boolean consume(Player player, net.minecraft.world.item.Item item) {
         var inv = player.getInventory();
         for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && !s.isEmpty()) {
               s.shrink(1);
               return true;
            }
         }
         return false;
      }

      @Override
      public InteractionResult useOn(UseOnContext ctx) {
         Level world = ctx.getLevel();
         Player player = ctx.getPlayer();
         BlockPos pos = ctx.getClickedPos();
         Direction side = ctx.getClickedFace();
         BlockState bs = world.getBlockState(pos);
         if (player == null || world.getBlockEntity(pos) != null) return InteractionResult.FAIL;
         ItemStack held = ctx.getItemInHand();
         for (BlockPos p2 : plane(held, pos, side, player)) {
            if (!canPlace(world, bs, p2)) continue;
            boolean creative = player.getAbilities().instabuild;
            if (creative || consume(player, bs.getBlock().asItem())) {
               world.playSound(player, p2, bs.getSoundType(world, p2, player).getBreakSound(), SoundSource.BLOCKS, 0.6F, 0.9F + world.random.nextFloat() * 0.2F);
               world.setBlockAndUpdate(p2, bs);
               held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(ctx.getHand()));
               if (world.isClientSide) FXDispatcher.drawBamf(p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, 8401408, false, false, side);
               player.swing(ctx.getHand());
            } else if (bs.is(Blocks.GRASS_BLOCK) && consume(player, Items.DIRT)) {
               world.playSound(player, p2, bs.getSoundType(world, p2, player).getBreakSound(), SoundSource.BLOCKS, 0.6F, 0.9F + world.random.nextFloat() * 0.2F);
               world.setBlockAndUpdate(p2, Blocks.DIRT.defaultBlockState());
               held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(ctx.getHand()));
               if (world.isClientSide) FXDispatcher.drawBamf(p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, 8401408, false, false, side);
               player.swing(ctx.getHand());
               if (held.isEmpty()) break;
            }
         }
         return InteractionResult.FAIL;
      }

      @Override
      public HitResult getArchitectMOP(ItemStack stack, Level level, LivingEntity player) {
         return player.pick(player instanceof Player p ? p.blockInteractionRange() : 4.5, 1.0F, false);
      }

      @Override
      public boolean useBlockHighlight(ItemStack stack) {
         return true;
      }

      @Override
      public List<BlockPos> getArchitectBlocks(ItemStack stack, Level level, BlockPos pos, Direction side, Player player) {
         List<BlockPos> b = new ArrayList<>();
         if (!player.isShiftKeyDown()) return b;
         BlockState bs = level.getBlockState(pos);
         for (BlockPos p2 : plane(stack, pos, side, player)) {
            if (canPlace(level, bs, p2)) b.add(p2);
         }
         return b;
      }

      @Override
      public boolean showAxis(ItemStack stack, Level level, Player player, Direction side, EnumAxis axis) {
         return false;
      }

      @Override
      public void addCreativeStacks(Consumer<ItemStack> output) {
         ItemStack w1 = new ItemStack(this);
         EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.DESTRUCTIVE, 1);
         output.accept(w1);
      }
   }

   /** ItemElementalSword: hold right-click to rise on a whirlwind that flings mobs away. Arcing II. */
   public static class Sword extends SwordItem implements ICreativeVariants {
      private static java.lang.reflect.Field floatCounter;

      public Sword(Properties p) {
         super(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, p.attributes(SwordItem.createAttributes(ThaumcraftMaterials.TOOLMAT_ELEMENTAL, 3, -2.4F)));
      }

      @Override
      public UseAnim getUseAnimation(ItemStack stack) {
         return UseAnim.NONE;
      }

      @Override
      public int getUseDuration(ItemStack stack, LivingEntity entity) {
         return 72000;
      }

      @Override
      public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
         player.startUsingItem(hand);
         return InteractionResultHolder.success(player.getItemInHand(hand));
      }

      /** EntityUtils.resetFloatCounter: keeps the server from kicking the floating player for flying. */
      static void resetFloatCounter(ServerPlayer player) {
         try {
            if (floatCounter == null) {
               floatCounter = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
               floatCounter.setAccessible(true);
            }
            floatCounter.setInt(player.connection, 0);
         } catch (ReflectiveOperationException | RuntimeException ignored) {
         }
      }

      @Override
      public void onUseTick(Level level, LivingEntity player, ItemStack stack, int count) {
         super.onUseTick(level, player, stack, count);
         int ticks = getUseDuration(stack, player) - count;
         Vec3 m = player.getDeltaMovement();
         double my = m.y;
         if (my < 0.0) {
            my /= 1.2F;
            player.fallDistance /= 1.2F;
         }
         my += 0.08F;
         if (my > 0.5) my = 0.2F;
         player.setDeltaMovement(m.x, my, m.z);
         if (player instanceof ServerPlayer sp && sp.connection != null) resetFloatCounter(sp);
         AABB box = player.getBoundingBox().inflate(2.5);
         for (Entity entity : level.getEntities(player, box)) {
            if (!(entity instanceof Player) && entity instanceof LivingEntity && entity.isAlive() && player.getVehicle() != entity) {
               Vec3 p = player.position();
               Vec3 t = entity.position();
               double distance = p.distanceTo(t) + 0.1;
               Vec3 r = t.subtract(p);
               entity.setDeltaMovement(entity.getDeltaMovement().add(r.x / 2.5 / distance, r.y / 2.5 / distance, r.z / 2.5 / distance));
            }
         }
         if (level.isClientSide) {
            int miny = (int) (player.getBoundingBox().minY - 2.0);
            if (player.onGround()) miny = Mth.floor(player.getBoundingBox().minY);
            // FXDispatcher.smokeSpiral comes with the rest of the FX library; plain smoke stands in for it.
            for (int a = 0; a < 5; a++) {
               float ang = level.random.nextInt(360) / 180.0F * (float) Math.PI;
               level.addParticle(ParticleTypes.CLOUD, player.getX() + Mth.cos(ang) * 1.5F,
                  miny + level.random.nextFloat() * (player.getBoundingBox().minY + player.getBbHeight() / 2.0F - miny), player.getZ() + Mth.sin(ang) * 1.5F,
                  0.0, 0.02, 0.0);
            }
            if (player.onGround()) {
               float r1 = level.random.nextFloat() * 360.0F;
               float mx = -Mth.sin(r1 / 180.0F * (float) Math.PI) / 5.0F;
               float mz = Mth.cos(r1 / 180.0F * (float) Math.PI) / 5.0F;
               level.addParticle(ParticleTypes.SMOKE, player.getX(), player.getBoundingBox().minY + 0.1F, player.getZ(), mx, 0.0, mz);
            }
         } else if (ticks == 0 || ticks % 20 == 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.WIND.get(), player.getSoundSource(), 0.5F,
               0.9F + level.random.nextFloat() * 0.2F);
         }
         if (ticks % 20 == 0 && !level.isClientSide) {
            stack.hurtAndBreak(1, player, player.getUsedItemHand() == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND);
         }
      }

      @Override
      public void addCreativeStacks(Consumer<ItemStack> output) {
         ItemStack w1 = new ItemStack(this);
         EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.ARCING, 2);
         output.accept(w1);
      }
   }
}
