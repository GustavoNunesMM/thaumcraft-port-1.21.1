package thaumcraft.common.entities.monster.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.devices.BannerBlockEntity;
import thaumcraft.common.network.InfusionFXPayload;
import thaumcraft.common.network.ModNetwork;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModMobs;
import thaumcraft.common.registers.ModSounds;

/**
 * EntityCultistPortalGreater: the great crimson portal. It plants cult banners and loot crates around itself, then
 * summons waves of cultists (faster the fewer are around) and finally a Crimson Praetor; afterwards every summon wears
 * it down. Drops a primordial pearl. (Its natural home, the Outer Lands, belongs to the eldritch milestone.)
 */
public class CultistPortalGreaterEntity extends Monster {
   protected final ServerBossEvent bossInfo = (ServerBossEvent) new ServerBossEvent(Component.translatable("entity.thaumcraft.cultist_portal_greater"),
      BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6).setDarkenScreen(true);
   int stage = 0;
   int stagecounter = 200;
   public int pulse = 0;

   public CultistPortalGreaterEntity(EntityType<? extends CultistPortalGreaterEntity> type, Level level) {
      super(type, level);
      xpReward = 30;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 500.0).add(Attributes.ATTACK_DAMAGE, 0.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 5.0);
   }

   @Override
   public void addAdditionalSaveData(CompoundTag nbt) {
      super.addAdditionalSaveData(nbt);
      nbt.putInt("stage", stage);
   }

   @Override
   public void readAdditionalSaveData(CompoundTag nbt) {
      super.readAdditionalSaveData(nbt);
      stage = nbt.getInt("stage");
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   public void move(MoverType type, Vec3 pos) {
   }

   @Override
   public void aiStep() {
   }

   @Override
   public boolean removeWhenFarAway(double dist) {
      return false;
   }

   @Override
   public boolean shouldRenderAtSqrDistance(double d) {
      return d < 4096.0;
   }

   @Override
   public float getLightLevelDependentMagicValue() {
      return 1.0F;
   }

   @Override
   public void tick() {
      super.tick();
      if (!level().isClientSide) {
         if (stagecounter <= 0) {
            if (level().getNearestPlayer(this, 48.0) != null) {
               level().broadcastEntityEvent(this, (byte) 16);
               if (stage < 5) {
                  stagecounter = 15 + random.nextInt(10 - stage) - stage;
                  spawnMinions();
               } else if (stage == 12) {
                  stagecounter = 50 + getTiming() * 2 + random.nextInt(50);
                  spawnBoss();
               } else {
                  int t = getTiming();
                  stagecounter = t + random.nextInt(5 + t / 3);
                  spawnMinions();
               }
               stage++;
            } else {
               stagecounter = 30 + random.nextInt(30);
            }
         } else {
            stagecounter--;
            if (stagecounter == 160 && stage == 0) {
               level().broadcastEntityEvent(this, (byte) 16);
               for (Direction dir : Direction.Plane.HORIZONTAL) {
                  BlockPos bp = new BlockPos((int) getX() - dir.getStepX() * 6, (int) getY(), (int) getZ() + dir.getStepZ() * 6);
                  level().setBlock(bp, ModDevices.BANNER_CRIMSON_CULT.get().defaultBlockState(), Block.UPDATE_ALL);
                  if (level().getBlockEntity(bp) instanceof BannerBlockEntity te) {
                     int face = switch (dir) {
                        case NORTH -> 8;
                        case WEST -> 12;
                        case EAST -> 4;
                        default -> 0;
                     };
                     te.setBannerFacing((byte) face);
                     arc(bp);
                     playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);
                  }
               }
            }
            if (stagecounter > 20 && stagecounter < 150 && stage == 0 && stagecounter % 13 == 0) {
               int a = (int) getX() + random.nextInt(5) - random.nextInt(5);
               int b = (int) getZ() + random.nextInt(5) - random.nextInt(5);
               BlockPos bp = new BlockPos(a, (int) getY(), b);
               if (a != (int) getX() && b != (int) getZ() && level().isEmptyBlock(bp)) {
                  level().broadcastEntityEvent(this, (byte) 16);
                  float rr = level().random.nextFloat();
                  Block bb = rr < 0.05F ? ModDevices.LOOT_CRATE_RARE.get()
                     : rr < 0.2F ? ModDevices.LOOT_CRATE_UNCOMMON.get() : ModDevices.LOOT_CRATE_COMMON.get();
                  level().setBlockAndUpdate(bp, bb.defaultBlockState());
                  arc(bp);
                  playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);
               }
            }
         }
         if (stage < 12) heal(1.0F);
         bossInfo.setProgress(getHealth() / getMaxHealth());
      }
      if (pulse > 0) pulse--;
   }

   /** PacketFXBlockArc from the block to the portal. */
   private void arc(BlockPos bp) {
      if (level() instanceof ServerLevel sl) ModNetwork.sendNear(sl, getX(), getY(), getZ(), 32.0, InfusionFXPayload.arc(bp, this));
   }

   int getTiming() {
      return level().getEntitiesOfClass(CultistEntity.class, getBoundingBox().inflate(32.0), e -> e.distanceToSqr(this) <= 1024.0).size() * 20;
   }

   private void summon(CultistEntity cultist) {
      cultist.setPos(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25, getZ() + random.nextFloat() - random.nextFloat());
      if (level() instanceof ServerLevel sl) {
         cultist.finalizeSpawn(sl, sl.getCurrentDifficultyAt(cultist.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
      }
      cultist.restrictTo(blockPosition(), 32);
      level().addFreshEntity(cultist);
      cultist.spawnAnim();
      cultist.playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);
   }

   void spawnMinions() {
      CultistEntity cultist = random.nextFloat() > 0.33F ? ModMobs.CULTIST_KNIGHT.get().create(level()) : ModMobs.CULTIST_CLERIC.get().create(level());
      if (cultist == null) return;
      summon(cultist);
      if (stage > 12) hurt(damageSources().fellOutOfWorld(), 5 + random.nextInt(5));
   }

   void spawnBoss() {
      CultistLeaderEntity leader = ModMobs.CULTIST_LEADER.get().create(level());
      if (leader == null) return;
      leader.setPos(getX() + random.nextFloat() - random.nextFloat(), getY() + 0.25, getZ() + random.nextFloat() - random.nextFloat());
      if (level() instanceof ServerLevel sl) {
         leader.finalizeSpawn(sl, sl.getCurrentDifficultyAt(leader.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
      }
      leader.restrictTo(blockPosition(), 32);
      level().addFreshEntity(leader);
      leader.spawnAnim();
      leader.playSound(ModSounds.WANDFAIL.get(), 1.0F, 1.0F);
   }

   @Override
   public void playerTouch(Player p) {
      if (distanceToSqr(p) < 3.0 && p.hurt(damageSources().indirectMagic(this, this), 8.0F)) {
         playSound(ModSounds.ZAP.get(), 1.0F, (random.nextFloat() - random.nextFloat()) * 0.1F + 1.0F);
      }
   }

   @Override
   protected float getSoundVolume() {
      return 0.75F;
   }

   @Override
   public int getAmbientSoundInterval() {
      return 540;
   }

   @Override
   protected SoundEvent getAmbientSound() {
      return ModSounds.MONOLITH.get();
   }

   @Override
   protected SoundEvent getHurtSound(DamageSource src) {
      return ModSounds.ZAP.get();
   }

   @Override
   protected SoundEvent getDeathSound() {
      return ModSounds.SHOCK.get();
   }

   @Override
   protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
      ThaumcraftBossEntity.dropSpecialItem(this, new ItemStack(ModMobs.PRIMORDIAL_PEARL.get()), getBbHeight() / 2.0F);
   }

   @Override
   public void handleEntityEvent(byte id) {
      if (id == 16) pulse = 10;
      else super.handleEntityEvent(id);
   }

   @Override
   public boolean addEffect(MobEffectInstance effect, Entity source) {
      return false;
   }

   @Override
   public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
      return false;
   }

   @Override
   public void die(DamageSource cause) {
      if (!level().isClientSide) level().explode(this, getX(), getY(), getZ(), 2.0F, Level.ExplosionInteraction.NONE);
      super.die(cause);
   }

   @Override
   public void startSeenByPlayer(ServerPlayer player) {
      super.startSeenByPlayer(player);
      bossInfo.addPlayer(player);
   }

   @Override
   public void stopSeenByPlayer(ServerPlayer player) {
      super.stopSeenByPlayer(player);
      bossInfo.removePlayer(player);
   }
}
