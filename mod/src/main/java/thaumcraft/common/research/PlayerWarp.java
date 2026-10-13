package thaumcraft.common.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Port of IPlayerWarp: permanent, normal (sticky) and temporary warp, plus the counter that drives warp events.
 * The warp events themselves arrive with the eldritch milestone.
 */
public class PlayerWarp {
   public enum Type {
      PERMANENT, NORMAL, TEMPORARY
   }

   public static final Codec<PlayerWarp> CODEC = RecordCodecBuilder.create(i -> i.group(
      Codec.INT.optionalFieldOf("permanent", 0).forGetter(w -> w.warp[0]),
      Codec.INT.optionalFieldOf("normal", 0).forGetter(w -> w.warp[1]),
      Codec.INT.optionalFieldOf("temporary", 0).forGetter(w -> w.warp[2]),
      Codec.INT.optionalFieldOf("counter", 0).forGetter(w -> w.counter)
   ).apply(i, PlayerWarp::new));

   public static final StreamCodec<ByteBuf, PlayerWarp> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, w -> w.warp[0], ByteBufCodecs.VAR_INT, w -> w.warp[1], ByteBufCodecs.VAR_INT, w -> w.warp[2],
      ByteBufCodecs.VAR_INT, w -> w.counter, PlayerWarp::new);

   private final int[] warp = new int[3];
   private int counter;

   public PlayerWarp() {
   }

   private PlayerWarp(int perm, int normal, int temp, int counter) {
      warp[0] = perm;
      warp[1] = normal;
      warp[2] = temp;
      this.counter = counter;
   }

   public void clear() {
      warp[0] = warp[1] = warp[2] = 0;
      counter = 0;
   }

   public int get(Type type) {
      return warp[type.ordinal()];
   }

   public void set(Type type, int amount) {
      warp[type.ordinal()] = Math.max(0, amount);
   }

   public int add(Type type, int amount) {
      set(type, get(type) + amount);
      return get(type);
   }

   public int reduce(Type type, int amount) {
      return add(type, -amount);
   }

   public int total() {
      return warp[0] + warp[1] + warp[2];
   }

   public int getCounter() {
      return counter;
   }

   public void setCounter(int counter) {
      this.counter = counter;
   }

   public PlayerWarp copy() {
      return new PlayerWarp(warp[0], warp[1], warp[2], counter);
   }
}
