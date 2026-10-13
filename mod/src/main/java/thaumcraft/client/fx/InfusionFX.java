package thaumcraft.client.fx;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.common.blocks.infusion.InfusionMatrixBlockEntity;
import thaumcraft.common.blocks.infusion.InlayBlock;
import thaumcraft.common.blocks.infusion.PedestalBlockEntity;
import thaumcraft.common.network.InfusionFXPayload;
import thaumcraft.common.network.ScanSourcePayload;
import thaumcraft.common.registers.ModSounds;

/** Client effects of milestone 8: infusion streams and arcs, pedestal runes, inlay sparks, sounding scans. */
public final class InfusionFX {
   private InfusionFX() {
   }

   private static ClientLevel level() {
      return Minecraft.getInstance().level;
   }

   private static void add(FXGeneric fx) {
      Minecraft.getInstance().particleEngine.add(fx);
   }

   /** FXDispatcher.blockRunes: little glowing runes drifting up around a block. */
   public static void blockRunes(double x, double y, double z, float r, float g, float b, int dur) {
      ClientLevel level = level();
      FXGeneric fb = new FXGeneric(level, x + 0.5 + (level.random.nextFloat() - 0.5) * 0.6, y + level.random.nextFloat() * 0.5 + 0.5,
         z + 0.5 + (level.random.nextFloat() - 0.5) * 0.6, 0, 0.02, 0);
      fb.setMaxAge(dur);
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(0.0F, 0.6F, 0.6F, 0.0F);
      fb.setParticles(224, 16, 1);
      fb.setScale(0.6F);
      fb.setLayer(0);
      fb.setGravity(-0.03F);
      add(fb);
   }

   public static void inlaySparkle(Level level, BlockPos pos, BlockState state, RandomSource rand) {
      Direction face = Direction.Plane.HORIZONTAL.getRandomDirection(rand);
      if (state.getValue(InlayBlock.BY_DIR.get(face)) == InlayBlock.Attach.NONE) return;
      double d0 = pos.getX() + 0.5 + rand.nextGaussian() * 0.08;
      double d1 = pos.getY() + 0.025F;
      double d2 = pos.getZ() + 0.5 + rand.nextGaussian() * 0.08;
      FXGeneric fb = new FXGeneric((ClientLevel) level, d0, d1, d2, face.getStepX() / 70.0, 0, face.getStepZ() / 70.0);
      fb.setMaxAge(16);
      float r = Mth.nextInt(rand, 150, 200) / 255.0F, g = Mth.nextInt(rand, 0, 200) / 255.0F;
      fb.setRBGColorF(r, g, g / 2.0F);
      fb.setAlphaF(1.0F, 0.0F);
      fb.setParticle(7);
      fb.setScale(0.33F);
      fb.setLayer(0);
      add(fb);
   }

   public static void pedestalEvent(PedestalBlockEntity ped, int id) {
      BlockPos p = ped.getBlockPos().above();
      switch (id) {
         case 11 -> FXDispatcher.drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0.75F, 0.0F, 0.5F, true, true, null);
         case 12 -> FXDispatcher.drawBamf(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, true, true, null);
         default -> {
            // drawPedestalShield: a ring of sparks around the item
            for (int a = 0; a < 12; a++) {
               double ang = a / 12.0 * Math.PI * 2;
               spark(p.getX() + 0.5 + Math.cos(ang) * 0.4, p.getY() + 0.3, p.getZ() + 0.5 + Math.sin(ang) * 0.4, 0.4F, 0.4F, 1.0F);
            }
         }
      }
   }

   private static void spark(double x, double y, double z, float r, float g, float b) {
      FXGeneric fb = new FXGeneric(level(), x, y, z, 0, 0, 0);
      fb.setMaxAge(8);
      fb.setRBGColorF(r, g, b);
      fb.setAlphaF(1.0F, 0.0F);
      fb.setParticles(310, 16, 1);
      fb.setScale(1.5F);
      fb.setLayer(0);
      add(fb);
   }

   /** TileInfusionMatrix.doEffects. */
   public static void matrixEffects(InfusionMatrixBlockEntity m) {
      ClientLevel level = level();
      BlockPos pos = m.getBlockPos();
      if (m.crafting) {
         if (m.craftCount == 0) level.playLocalSound(pos, ModSounds.INFUSERSTART.get(), SoundSource.BLOCKS, 0.5F, 1.0F, false);
         else if (m.craftCount % 65 == 0) level.playLocalSound(pos, ModSounds.INFUSER.get(), SoundSource.BLOCKS, 0.5F, 1.0F, false);
         m.craftCount++;
         blockRunes(pos.getX(), pos.getY() - 2, pos.getZ(), 0.5F + level.random.nextFloat() * 0.2F, 0.1F, 0.7F + level.random.nextFloat() * 0.3F, 25);
      } else if (m.craftCount > 0) {
         m.craftCount = Math.min(50, Math.max(0, m.craftCount - 2));
      }
      if (m.active && m.startUp != 1.0F) {
         if (m.startUp < 1.0F) m.startUp += Math.max(m.startUp / 10.0F, 0.001F);
         if (m.startUp > 0.999) m.startUp = 1.0F;
      }
      if (!m.active && m.startUp > 0.0F) {
         m.startUp -= m.startUp / 10.0F;
         if (m.startUp < 0.001) m.startUp = 0.0F;
      }
      for (String key : new ArrayList<>(m.sourceFX.keySet())) {
         InfusionMatrixBlockEntity.SourceFX fx = m.sourceFX.get(key);
         if (fx.ticks() <= 0) {
            m.sourceFX.remove(key);
            continue;
         }
         if (fx.loc().equals(pos)) {
            Entity player = level.getEntity(fx.entity());
            if (player != null) {
               for (int a = 0; a < 4; a++) {
                  stream(player.getX() + (level.random.nextFloat() - level.random.nextFloat()) * player.getBbWidth(),
                     player.getBoundingBox().minY + level.random.nextFloat() * player.getBbHeight(),
                     player.getZ() + (level.random.nextFloat() - level.random.nextFloat()) * player.getBbWidth(), pos, 0x7FFF00);
               }
            }
         } else if (level.getBlockEntity(fx.loc()) instanceof PedestalBlockEntity ped && !ped.getItem().isEmpty()) {
            ItemStack is = ped.getItem();
            BlockPos l = fx.loc();
            if (level.random.nextInt(3) == 0) {
               stream(l.getX() + level.random.nextFloat(), l.getY() + level.random.nextFloat() + 1.0F, l.getZ() + level.random.nextFloat(), pos, 0xCC66FF);
            } else {
               for (int a = 0; a < 4; a++) {
                  Vec3 s = new Vec3(l.getX() + 0.4 + level.random.nextFloat() * 0.2, l.getY() + 1.23 + level.random.nextFloat() * 0.2,
                     l.getZ() + 0.4 + level.random.nextFloat() * 0.2);
                  Vec3 v = Vec3.atCenterOf(pos).subtract(s).scale(0.05);
                  if (is.getItem() instanceof BlockItem bi) {
                     level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bi.getBlock().defaultBlockState()), s.x, s.y, s.z, v.x, v.y, v.z);
                  } else {
                     level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, is), s.x, s.y, s.z, v.x, v.y, v.z);
                  }
               }
            }
         } else {
            m.sourceFX.put(key, new InfusionMatrixBlockEntity.SourceFX(fx.loc(), 0, fx.entity()));
            continue;
         }
         m.sourceFX.put(key, new InfusionMatrixBlockEntity.SourceFX(fx.loc(), fx.ticks() - 1, fx.entity()));
      }
      if (m.crafting && m.stability < 0.0F && level.random.nextInt(250) <= Math.abs(m.stability)) {
         spark(pos.getX() + level.random.nextFloat(), pos.getY() + level.random.nextFloat(), pos.getZ() + level.random.nextFloat(), 0.75F, 0.1F, 0.7F);
      }
      if (m.active && !m.getProblemBlocks().isEmpty() && level.random.nextInt(25) == 0) {
         BlockPos p = m.getProblemBlocks().get(level.random.nextInt(m.getProblemBlocks().size()));
         spark(p.getX() + level.random.nextFloat(), p.getY() + level.random.nextFloat(), p.getZ() + level.random.nextFloat(), 0.75F, 0.1F, 0.7F);
      }
   }

   private static void stream(double x, double y, double z, BlockPos to, int color) {
      add(new EssentiaFX.Stream(level(), x, y, z, to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5, color, 0));
   }

   public static void handle(InfusionFXPayload p) {
      ClientLevel level = level();
      if (level == null) return;
      switch (p.kind()) {
         case InfusionFXPayload.SOURCE -> {
            if (level.getBlockEntity(p.pos()) instanceof InfusionMatrixBlockEntity m) {
               String key = p.target().asLong() + ":" + p.entity();
               m.sourceFX.put(key, new InfusionMatrixBlockEntity.SourceFX(p.target().equals(p.pos()) ? p.pos() : p.target(), 40, p.entity()));
            }
         }
         case InfusionFXPayload.ARC_BLOCK -> arc(Vec3.atCenterOf(p.pos()), Vec3.atCenterOf(p.target()));
         default -> {
            Entity e = level.getEntity(p.entity());
            if (e != null) arc(Vec3.atCenterOf(p.pos()), e.position().add(0, e.getBbHeight() / 2, 0));
         }
      }
   }

   /** PacketFXBlockArc, simplified: a jagged line of violet sparks. */
   public static void arc(Vec3 from, Vec3 to) {
      ClientLevel level = level();
      int steps = (int) Math.max(4, from.distanceTo(to) * 6);
      Vec3 prev = from;
      for (int i = 1; i <= steps; i++) {
         Vec3 p = from.lerp(to, i / (double) steps).add(i < steps ? (level.random.nextFloat() - 0.5) * 0.3 : 0,
            i < steps ? (level.random.nextFloat() - 0.5) * 0.3 : 0, i < steps ? (level.random.nextFloat() - 0.5) * 0.3 : 0);
         spark((prev.x + p.x) / 2, (prev.y + p.y) / 2, (prev.z + p.z) / 2, 0.6F, 0.3F, 1.0F);
         prev = p;
      }
      level.playLocalSound(from.x, from.y, from.z, ModSounds.ZAP.get(), SoundSource.BLOCKS, 0.4F, 1.0F, false);
   }

   /** PacketFXScanSource.startScan: a glow over each ore vein in range, nearest first. */
   public static void scan(ScanSourcePayload p) {
      ClientLevel level = level();
      if (level == null) return;
      int range = 4 + p.size() * 4;
      List<BlockPos> positions = new ArrayList<>();
      for (BlockPos q : BlockPos.betweenClosed(p.pos().offset(-range, -range, -range), p.pos().offset(range, range, range))) {
         if (level.getBlockState(q).is(Tags.Blocks.ORES)) positions.add(q.immutable());
      }
      while (!positions.isEmpty()) {
         BlockPos start = positions.remove(0);
         List<BlockPos> group = new ArrayList<>();
         group.add(start);
         BlockState bs = level.getBlockState(start);
         for (int i = 0; i < group.size(); i++) {
            BlockPos g = group.get(i);
            for (BlockPos t : BlockPos.betweenClosed(g.offset(-1, -1, -1), g.offset(1, 1, 1))) {
               if (positions.contains(t) && level.getBlockState(t) == bs) {
                  positions.remove(t);
                  group.add(t.immutable());
               }
            }
         }
         double x = 0, y = 0, z = 0;
         for (BlockPos g : group) {
            x += g.getX() + 0.5;
            y += g.getY() + 0.5;
            z += g.getZ() + 0.5;
         }
         x /= group.size();
         y /= group.size();
         z /= group.size();
         int c = bs.getMapColor(level, start).col;
         FXGeneric fb = new FXGeneric(level, x, y, z, 0, 0, 0);
         fb.setMaxAge(44);
         fb.setRBGColorF((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F);
         fb.setAlphaF(0.0F, 1.0F, 0.8F, 0.0F);
         fb.setParticles(240, 15, 1);
         fb.setGridSize(16);
         fb.setLoop(true);
         fb.setScale(9.0F);
         fb.setLayer(2);
         fb.setRotationSpeed(0.0F);
         add(fb);
      }
   }
}
