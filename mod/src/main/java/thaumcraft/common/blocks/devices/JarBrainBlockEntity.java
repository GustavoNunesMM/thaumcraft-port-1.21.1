package thaumcraft.common.blocks.devices;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.blocks.TCBlockEntity;
import thaumcraft.common.registers.ModDevices;
import thaumcraft.common.registers.ModSounds;

/** TileJarBrain: draws experience orbs within 8 blocks towards itself and eats those that touch it. Looks at players. */
public class JarBrainBlockEntity extends TCBlockEntity {
   public static final int XP_MAX = 2000;
   public int xp = 0;
   public int eatDelay = 0;
   public float rota;
   public float rotb;
   private float targetRot;
   private float blink;
   private float pupil;
   private long lastsigh = System.currentTimeMillis() + 1500L;

   public JarBrainBlockEntity(BlockPos pos, BlockState state) {
      super(ModDevices.JAR_BRAIN_BE.get(), pos, state);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, JarBrainBlockEntity be) {
      Entity entity = null;
      if (be.xp > XP_MAX) be.xp = XP_MAX;
      if (be.xp < XP_MAX) {
         entity = be.getClosestXPOrb();
         if (entity != null && be.eatDelay == 0) {
            double dx = (pos.getX() + 0.5 - entity.getX()) / 25.0;
            double dy = (pos.getY() + 0.5 - entity.getY()) / 25.0;
            double dz = (pos.getZ() + 0.5 - entity.getZ()) / 25.0;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double f = 1.0 - d;
            if (f > 0.0) {
               f *= f;
               Vec3 m = entity.getDeltaMovement();
               entity.setDeltaMovement(m.x + dx / d * f * 0.3, m.y + dy / d * f * 0.5, m.z + dz / d * f * 0.3);
            }
         }
      }
      if (level.isClientSide) {
         be.rotb = be.rota;
         if (entity == null) {
            entity = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6.0, false);
            if (entity != null && be.lastsigh < System.currentTimeMillis()) {
               level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.BRAIN.get(), SoundSource.AMBIENT, 0.15F,
                  0.8F + level.random.nextFloat() * 0.4F, false);
               be.lastsigh = System.currentTimeMillis() + 5000L + level.random.nextInt(25000);
            }
         }
         if (entity != null) {
            be.targetRot = (float) Math.atan2(entity.getZ() - (pos.getZ() + 0.5F), entity.getX() - (pos.getX() + 0.5F));
            be.blink += 0.1F;
            if (be.blink < 0.5F || level.random.nextInt(40) == 0) {
               float f3 = be.pupil;
               do {
                  be.pupil += level.random.nextInt(4) - level.random.nextInt(4);
               } while (f3 == be.pupil);
            }
         } else {
            be.targetRot += 0.01F;
         }
         be.rota = wrap(be.rota);
         be.targetRot = wrap(be.targetRot);
         be.rota += wrap(be.targetRot - be.rota) * 0.04F;
      }
      if (be.eatDelay > 0) {
         be.eatDelay--;
      } else if (be.xp < XP_MAX && !level.isClientSide) {
         List<ExperienceOrb> ents = level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(0.1));
         if (!ents.isEmpty()) {
            for (ExperienceOrb eo : ents) {
               be.xp += eo.getValue();
               eo.playSound(SoundEvents.GENERIC_EAT, 0.1F, (level.random.nextFloat() - level.random.nextFloat()) * 0.2F + 1.0F);
               eo.discard();
            }
            be.sync();
         }
      }
   }

   private static float wrap(float a) {
      while (a >= (float) Math.PI) a -= (float) (Math.PI * 2);
      while (a < -(float) Math.PI) a += (float) (Math.PI * 2);
      return a;
   }

   @Nullable
   private Entity getClosestXPOrb() {
      double cdist = Double.MAX_VALUE;
      Entity orb = null;
      for (ExperienceOrb eo : level.getEntitiesOfClass(ExperienceOrb.class, new AABB(worldPosition).inflate(8.0))) {
         double d = eo.distanceToSqr(worldPosition.getCenter());
         if (d < cdist) {
            orb = eo;
            cdist = d;
         }
      }
      return orb;
   }

   @Override
   protected void applyImplicitComponents(DataComponentInput input) {
      super.applyImplicitComponents(input);
      xp = input.getOrDefault(ModDevices.BRAIN_XP.get(), 0);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder builder) {
      super.collectImplicitComponents(builder);
      if (xp > 0) builder.set(ModDevices.BRAIN_XP.get(), xp);
   }

   @Override
   public void removeComponentsFromTag(CompoundTag tag) {
      tag.remove("XP");
   }

   @Override
   protected void readSync(CompoundTag tag, HolderLookup.Provider registries) {
      xp = tag.getInt("XP");
   }

   @Override
   protected void writeSync(CompoundTag tag, HolderLookup.Provider registries) {
      tag.putInt("XP", xp);
   }
}
