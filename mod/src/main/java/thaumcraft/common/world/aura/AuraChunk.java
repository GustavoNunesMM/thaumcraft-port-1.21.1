package thaumcraft.common.world.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Port of AuraChunk: the aura of one chunk. {@code base} is the level vis regenerates towards, set once from the
 * biomes when the chunk is first loaded; {@code vis} and {@code flux} move around as the world ticks.
 * Stored as a chunk data attachment (saved with the chunk, like the "Thaumcraft" chunk NBT tag in 1.12).
 */
public class AuraChunk {
   public static final Codec<AuraChunk> CODEC = RecordCodecBuilder.create(i -> i.group(
      Codec.SHORT.fieldOf("base").forGetter(AuraChunk::getBase),
      Codec.FLOAT.fieldOf("vis").forGetter(AuraChunk::getVis),
      Codec.FLOAT.fieldOf("flux").forGetter(AuraChunk::getFlux)
   ).apply(i, AuraChunk::new));

   private short base;
   private float vis;
   private float flux;

   public AuraChunk(short base, float vis, float flux) {
      this.base = base;
      setVis(vis);
      setFlux(flux);
   }

   /** Placeholder for chunks whose aura has not been generated yet; replaced on chunk load. */
   public static AuraChunk ungenerated() {
      return new AuraChunk((short) -1, 0, 0);
   }

   public boolean isGenerated() {
      return base >= 0;
   }

   public short getBase() {
      return base;
   }

   public void setBase(short base) {
      this.base = base;
   }

   public float getVis() {
      return vis;
   }

   public void setVis(float vis) {
      this.vis = Math.min(32766.0F, Math.max(0.0F, vis));
   }

   public float getFlux() {
      return flux;
   }

   public void setFlux(float flux) {
      this.flux = Math.min(32766.0F, Math.max(0.0F, flux));
   }

   @Override
   public String toString() {
      return String.format("base %d, vis %.1f, flux %.1f", base, vis, flux);
   }
}
