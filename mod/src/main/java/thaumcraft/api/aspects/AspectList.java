package thaumcraft.api.aspects;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Ported from Thaumcraft 6. Insertion-ordered map of aspect -> amount.
 */
public class AspectList {
   public static final Codec<AspectList> CODEC = Codec.unboundedMap(Aspect.CODEC, Codec.INT)
      .xmap(AspectList::new, list -> list.aspects);
   public static final StreamCodec<ByteBuf, AspectList> STREAM_CODEC = ByteBufCodecs.<ByteBuf, Aspect, Integer, Map<Aspect, Integer>>map(
         LinkedHashMap::new, Aspect.STREAM_CODEC, ByteBufCodecs.VAR_INT
      )
      .map(AspectList::new, list -> list.aspects);

   public final LinkedHashMap<Aspect, Integer> aspects = new LinkedHashMap<>();

   public AspectList() {
   }

   private AspectList(Map<Aspect, Integer> map) {
      aspects.putAll(map);
   }

   public AspectList copy() {
      return new AspectList(aspects);
   }

   public int size() {
      return aspects.size();
   }

   public boolean isEmpty() {
      return aspects.isEmpty();
   }

   public int visSize() {
      int q = 0;
      for (int v : aspects.values()) q += v;
      return q;
   }

   public Aspect[] getAspects() {
      return aspects.keySet().toArray(new Aspect[0]);
   }

   public Aspect[] getAspectsSortedByName() {
      Aspect[] out = getAspects();
      Arrays.sort(out, Comparator.comparing(Aspect::getTag));
      return out;
   }

   public Aspect[] getAspectsSortedByAmount() {
      Aspect[] out = getAspects();
      Arrays.sort(out, Comparator.comparingInt(this::getAmount).reversed());
      return out;
   }

   public int getAmount(Aspect key) {
      return aspects.getOrDefault(key, 0);
   }

   public boolean reduce(Aspect key, int amount) {
      if (getAmount(key) >= amount) {
         aspects.put(key, getAmount(key) - amount);
         return true;
      }
      return false;
   }

   public AspectList remove(Aspect key, int amount) {
      int am = getAmount(key) - amount;
      if (am <= 0) aspects.remove(key);
      else aspects.put(key, am);
      return this;
   }

   public AspectList remove(Aspect key) {
      aspects.remove(key);
      return this;
   }

   public AspectList add(Aspect aspect, int amount) {
      aspects.merge(aspect, amount, Integer::sum);
      return this;
   }

   /** Keeps the larger of the current and given amount. */
   public AspectList merge(Aspect aspect, int amount) {
      aspects.merge(aspect, amount, Math::max);
      return this;
   }

   public AspectList add(AspectList in) {
      in.aspects.forEach(this::add);
      return this;
   }

   public AspectList remove(AspectList in) {
      in.aspects.forEach(this::remove);
      return this;
   }

   public AspectList merge(AspectList in) {
      in.aspects.forEach(this::merge);
      return this;
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof AspectList other && aspects.equals(other.aspects);
   }

   @Override
   public int hashCode() {
      return aspects.hashCode();
   }

   @Override
   public String toString() {
      return aspects.toString();
   }
}
