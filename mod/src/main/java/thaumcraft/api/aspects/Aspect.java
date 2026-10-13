package thaumcraft.api.aspects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.text.WordUtils;

/**
 * Ported from Thaumcraft 6 (1.12.2). Aspects are a fixed set registered statically, as in the original.
 */
public class Aspect {
   private static final HashMap<Integer, Aspect> mixList = new HashMap<>();
   private static final LinkedHashMap<String, Aspect> aspects = new LinkedHashMap<>();
   private static final ArrayList<Aspect> primals = new ArrayList<>();
   private static final ArrayList<Aspect> compounds = new ArrayList<>();

   public static final Aspect AIR = new Aspect("aer", 0xFFFF7E, ChatFormatting.YELLOW, 1);
   public static final Aspect EARTH = new Aspect("terra", 0x56C000, ChatFormatting.DARK_GREEN, 1);
   public static final Aspect FIRE = new Aspect("ignis", 0xFF5A01, ChatFormatting.RED, 1);
   public static final Aspect WATER = new Aspect("aqua", 0x3CD4FC, ChatFormatting.DARK_AQUA, 1);
   public static final Aspect ORDER = new Aspect("ordo", 0xD5D4EC, ChatFormatting.GRAY, 1);
   public static final Aspect ENTROPY = new Aspect("perditio", 0x404040, ChatFormatting.DARK_GRAY, 771);
   public static final Aspect VOID = new Aspect("vacuos", 0x888888, new Aspect[]{AIR, ENTROPY}, 771);
   public static final Aspect LIGHT = new Aspect("lux", 0xFFFFC0, new Aspect[]{AIR, FIRE});
   public static final Aspect MOTION = new Aspect("motus", 0xCDCCF4, new Aspect[]{AIR, ORDER});
   public static final Aspect COLD = new Aspect("gelum", 0xE1FFFF, new Aspect[]{FIRE, ENTROPY});
   public static final Aspect CRYSTAL = new Aspect("vitreus", 0x80FFFF, new Aspect[]{EARTH, AIR});
   public static final Aspect METAL = new Aspect("metallum", 0xB5B5CD, new Aspect[]{EARTH, ORDER});
   public static final Aspect LIFE = new Aspect("victus", 0xDE0005, new Aspect[]{EARTH, WATER});
   public static final Aspect DEATH = new Aspect("mortuus", 0x6A0005, new Aspect[]{WATER, ENTROPY});
   public static final Aspect ENERGY = new Aspect("potentia", 0xC0FFFF, new Aspect[]{ORDER, FIRE});
   public static final Aspect EXCHANGE = new Aspect("permutatio", 0x578357, new Aspect[]{ENTROPY, ORDER});
   public static final Aspect MAGIC = new Aspect("praecantatio", 0xCF00FF, new Aspect[]{ENERGY, AIR});
   public static final Aspect AURA = new Aspect("auram", 0xFFC0FF, new Aspect[]{MAGIC, AIR});
   public static final Aspect ALCHEMY = new Aspect("alkimia", 0x23AC9D, new Aspect[]{MAGIC, WATER});
   public static final Aspect FLUX = new Aspect("vitium", 0x800080, new Aspect[]{ENTROPY, MAGIC});
   public static final Aspect DARKNESS = new Aspect("tenebrae", 0x222222, new Aspect[]{VOID, LIGHT});
   public static final Aspect ELDRITCH = new Aspect("alienis", 0x805080, new Aspect[]{VOID, DARKNESS});
   public static final Aspect FLIGHT = new Aspect("volatus", 0xE7E7D7, new Aspect[]{AIR, MOTION});
   public static final Aspect PLANT = new Aspect("herba", 0x01AC00, new Aspect[]{LIFE, EARTH});
   public static final Aspect TOOL = new Aspect("instrumentum", 0x4040EE, new Aspect[]{METAL, ENERGY});
   public static final Aspect CRAFT = new Aspect("fabrico", 0x809D80, new Aspect[]{EXCHANGE, TOOL});
   public static final Aspect MECHANISM = new Aspect("machina", 0x8080A0, new Aspect[]{MOTION, TOOL});
   public static final Aspect TRAP = new Aspect("vinculum", 0x9A8080, new Aspect[]{MOTION, ENTROPY});
   public static final Aspect SOUL = new Aspect("spiritus", 0xEBEBFB, new Aspect[]{LIFE, DEATH});
   public static final Aspect MIND = new Aspect("cognitio", 0xF9967F, new Aspect[]{FIRE, SOUL});
   public static final Aspect SENSES = new Aspect("sensus", 0xC0FFC0, new Aspect[]{AIR, SOUL});
   public static final Aspect AVERSION = new Aspect("aversio", 0xC05050, new Aspect[]{SOUL, ENTROPY});
   public static final Aspect PROTECT = new Aspect("praemunio", 0x00C0C0, new Aspect[]{SOUL, EARTH});
   public static final Aspect DESIRE = new Aspect("desiderium", 0xE6BE44, new Aspect[]{SOUL, VOID});
   public static final Aspect UNDEAD = new Aspect("exanimis", 0x3A4000, new Aspect[]{MOTION, DEATH});
   public static final Aspect BEAST = new Aspect("bestia", 0x9F6409, new Aspect[]{MOTION, LIFE});
   public static final Aspect MAN = new Aspect("humanus", 0xFFD7C0, new Aspect[]{SOUL, LIFE});

   public static final Codec<Aspect> CODEC = Codec.STRING.comapFlatMap(
      tag -> {
         Aspect a = getAspect(tag);
         return a != null ? DataResult.success(a) : DataResult.error(() -> "Unknown aspect: " + tag);
      },
      Aspect::getTag
   );
   public static final StreamCodec<ByteBuf, Aspect> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Aspect::getAspect, Aspect::getTag);

   private final String tag;
   private final Aspect[] components;
   private final int color;
   private final ChatFormatting chatcolor;
   private final ResourceLocation image;
   private final int blend;

   private Aspect(String tag, int color, Aspect[] components, ChatFormatting chatcolor, int blend) {
      if (aspects.containsKey(tag)) {
         throw new IllegalArgumentException(tag + " already registered!");
      }
      this.tag = tag;
      this.components = components;
      this.color = color;
      this.chatcolor = chatcolor;
      this.image = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/aspects/" + tag + ".png");
      this.blend = blend;
      aspects.put(tag, this);
      if (components != null) {
         mixList.put((components[0].getTag() + components[1].getTag()).hashCode(), this);
      }
   }

   private Aspect(String tag, int color, Aspect[] components) {
      this(tag, color, components, null, 1);
   }

   private Aspect(String tag, int color, Aspect[] components, int blend) {
      this(tag, color, components, null, blend);
   }

   private Aspect(String tag, int color, ChatFormatting chatcolor, int blend) {
      this(tag, color, null, chatcolor, blend);
   }

   // Aspects are singletons; these exist because data components require them.
   @Override
   public boolean equals(Object o) {
      return this == o || o instanceof Aspect a && a.tag.equals(tag);
   }

   @Override
   public int hashCode() {
      return tag.hashCode();
   }

   public int getColor() {
      return color;
   }

   public String getName() {
      return WordUtils.capitalizeFully(tag);
   }

   public Component getLocalizedName() {
      return Component.literal(getName()).withColor(color);
   }

   public Component getLocalizedDescription() {
      return Component.translatable("tc.aspect." + tag);
   }

   public String getTag() {
      return tag;
   }

   public Aspect[] getComponents() {
      return components;
   }

   public ResourceLocation getImage() {
      return image;
   }

   public int getBlend() {
      return blend;
   }

   public ChatFormatting getChatcolor() {
      return chatcolor;
   }

   public boolean isPrimal() {
      return components == null || components.length != 2;
   }

   public static Aspect getAspect(String tag) {
      return aspects.get(tag);
   }

   public static Collection<Aspect> all() {
      return Collections.unmodifiableCollection(aspects.values());
   }

   public static Aspect getCombination(Aspect a, Aspect b) {
      Aspect r = mixList.get((a.getTag() + b.getTag()).hashCode());
      return r != null ? r : mixList.get((b.getTag() + a.getTag()).hashCode());
   }

   public static List<Aspect> getPrimalAspects() {
      if (primals.isEmpty()) {
         for (Aspect aspect : aspects.values()) {
            if (aspect.isPrimal()) primals.add(aspect);
         }
      }
      return primals;
   }

   public static List<Aspect> getCompoundAspects() {
      if (compounds.isEmpty()) {
         for (Aspect aspect : aspects.values()) {
            if (!aspect.isPrimal()) compounds.add(aspect);
         }
      }
      return compounds;
   }

   @Override
   public String toString() {
      return tag;
   }
}
