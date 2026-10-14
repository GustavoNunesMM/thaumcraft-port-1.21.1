package thaumcraft.common.golems;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.GolemAddon;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.api.golems.parts.GolemHead;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.api.golems.parts.GolemMaterial;
import thaumcraft.api.golems.parts.PartModel;
import thaumcraft.common.golems.parts.GolemArmDart;
import thaumcraft.common.golems.parts.GolemLegLevitator;
import thaumcraft.common.golems.parts.GolemLegWheels;
import thaumcraft.common.golems.parts.PartModelBreakers;
import thaumcraft.common.golems.parts.PartModelClaws;
import thaumcraft.common.golems.parts.PartModelDarts;
import thaumcraft.common.golems.parts.PartModelHauler;
import thaumcraft.common.golems.parts.PartModelWheel;
import thaumcraft.common.registers.ModBlocks;
import thaumcraft.common.registers.ModItems;

/**
 * A golem's make-up packed in a long, one byte each: material, head, arms, legs, addon, rank (byte 0 being the most
 * significant, as the ByteBuffer of ThaumcraftApiHelper.setByteInLong). The static block registers the parts.
 */
public class GolemProperties implements IGolemProperties {
   private long data = 0L;
   private Set<EnumGolemTrait> traitCache = null;

   static byte getByteInLong(long data, int index) {
      return (byte) (data >>> (56 - index * 8));
   }

   static long setByteInLong(long data, byte b, int index) {
      int shift = 56 - index * 8;
      return data & ~(0xFFL << shift) | (b & 0xFFL) << shift;
   }

   private static <T> T pick(T[] arr, int index) {
      return arr[index >= 0 && index < arr.length ? index : 0];
   }

   @Override
   public Set<EnumGolemTrait> getTraits() {
      if (traitCache == null) {
         traitCache = EnumSet.noneOf(EnumGolemTrait.class);
         for (EnumGolemTrait t : getMaterial().traits) addTraitSmart(t);
         for (EnumGolemTrait t : getHead().traits) addTraitSmart(t);
         for (EnumGolemTrait t : getArms().traits) addTraitSmart(t);
         for (EnumGolemTrait t : getLegs().traits) addTraitSmart(t);
         for (EnumGolemTrait t : getAddon().traits) addTraitSmart(t);
      }
      return traitCache;
   }

   private void addTraitSmart(EnumGolemTrait trait) {
      if (trait.opposite != null && traitCache.contains(trait.opposite)) {
         traitCache.remove(trait.opposite);
      } else {
         traitCache.add(trait);
      }
   }

   @Override
   public boolean hasTrait(EnumGolemTrait trait) {
      return getTraits().contains(trait);
   }

   @Override
   public void setMaterial(GolemMaterial mat) {
      data = setByteInLong(data, mat.id, 0);
      traitCache = null;
   }

   @Override
   public GolemMaterial getMaterial() {
      return pick(GolemMaterial.getMaterials(), getByteInLong(data, 0));
   }

   @Override
   public void setHead(GolemHead mat) {
      data = setByteInLong(data, mat.id, 1);
      traitCache = null;
   }

   @Override
   public GolemHead getHead() {
      return pick(GolemHead.getHeads(), getByteInLong(data, 1));
   }

   @Override
   public void setArms(GolemArm mat) {
      data = setByteInLong(data, mat.id, 2);
      traitCache = null;
   }

   @Override
   public GolemArm getArms() {
      return pick(GolemArm.getArms(), getByteInLong(data, 2));
   }

   @Override
   public void setLegs(GolemLeg mat) {
      data = setByteInLong(data, mat.id, 3);
      traitCache = null;
   }

   @Override
   public GolemLeg getLegs() {
      return pick(GolemLeg.getLegs(), getByteInLong(data, 3));
   }

   @Override
   public void setAddon(GolemAddon mat) {
      data = setByteInLong(data, mat.id, 4);
      traitCache = null;
   }

   @Override
   public GolemAddon getAddon() {
      return pick(GolemAddon.getAddons(), getByteInLong(data, 4));
   }

   @Override
   public void setRank(int rank) {
      data = setByteInLong(data, (byte) rank, 5);
   }

   @Override
   public int getRank() {
      return getByteInLong(data, 5);
   }

   public static IGolemProperties fromLong(long d) {
      GolemProperties out = new GolemProperties();
      out.data = d;
      return out;
   }

   @Override
   public long toLong() {
      return data;
   }

   @Override
   public ItemStack[] generateComponents() {
      ArrayList<ItemStack> comps = new ArrayList<>();
      addToList(comps, getMaterial().componentBase.get(), 2);
      addToList(comps, getMaterial().componentMechanism.get(), 1);
      addToListFromComps(comps, getArms().components, getMaterial());
      addToListFromComps(comps, getLegs().components, getMaterial());
      addToListFromComps(comps, getHead().components, getMaterial());
      addToListFromComps(comps, getAddon().components, getMaterial());
      return comps.toArray(new ItemStack[0]);
   }

   @SuppressWarnings("unchecked")
   private static void addToListFromComps(ArrayList<ItemStack> comps, Object[] objs, GolemMaterial mat) {
      for (Object o : objs) {
         if (o instanceof ItemStack s) {
            addToList(comps, s, 1);
         } else if (o instanceof Supplier<?> s) {
            addToList(comps, ((Supplier<ItemStack>) s).get(), 1);
         } else if (o instanceof String s) {
            if (s.equalsIgnoreCase("base")) addToList(comps, mat.componentBase.get(), 1);
            else if (s.equalsIgnoreCase("mech")) addToList(comps, mat.componentMechanism.get(), 1);
         }
      }
   }

   private static void addToList(ArrayList<ItemStack> comps, ItemStack newItem, int mult) {
      if (newItem.isEmpty()) return;
      for (ItemStack stack : comps) {
         if (ItemStack.isSameItemSameComponents(stack, newItem)) {
            stack.grow(newItem.getCount() * mult);
            return;
         }
      }
      ItemStack s = newItem.copy();
      s.setCount(s.getCount() * mult);
      comps.add(s);
   }

   // ------------------------------------------------------------------ the parts

   private static ResourceLocation rl(String path) {
      return ResourceLocation.fromNamespaceAndPath("thaumcraft", path);
   }

   private static Supplier<ItemStack> stack(Supplier<? extends net.minecraft.world.level.ItemLike> item, int count) {
      return () -> new ItemStack(item.get(), count);
   }

   private static Supplier<ItemStack> stack(Supplier<? extends net.minecraft.world.level.ItemLike> item) {
      return stack(item, 1);
   }

   /** BlocksTC.levitator (registered by the devices milestone), looked up by id; skipped if it is ever missing. */
   private static Supplier<ItemStack> optional(String id) {
      return () -> {
         var item = BuiltInRegistries.ITEM.get(rl(id));
         return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
      };
   }

   private static PartModel model(String obj, String tex, PartModel.EnumAttachPoint p) {
      return new PartModel(rl("models/obj/" + obj + ".obj"), tex == null ? null : rl(tex), p);
   }

   private static boolean registered;

   public static synchronized void registerParts() {
      if (registered) return;
      registered = true;
      Supplier<ItemStack> mech = stack(ModItems.MECHANISM_SIMPLE);
      GolemMaterial.register(new GolemMaterial("WOOD", new String[]{"MATSTUDWOOD"}, rl("textures/entity/golems/mat_wood.png"), 5059370, 6, 2, 1,
         stack(ModBlocks.PLANK_GREATWOOD), mech, new EnumGolemTrait[]{EnumGolemTrait.LIGHT}));
      GolemMaterial.register(new GolemMaterial("IRON", new String[]{"MATSTUDIRON"}, rl("textures/entity/golems/mat_iron.png"), 16777215, 20, 8, 3,
         stack(ModItems.PLATE_IRON), mech, new EnumGolemTrait[]{EnumGolemTrait.HEAVY, EnumGolemTrait.FIREPROOF, EnumGolemTrait.BLASTPROOF}));
      GolemMaterial.register(new GolemMaterial("CLAY", new String[]{"MATSTUDCLAY"}, rl("textures/entity/golems/mat_clay.png"), 13071447, 10, 4, 2,
         () -> new ItemStack(Blocks.TERRACOTTA), mech, new EnumGolemTrait[]{EnumGolemTrait.FIREPROOF}));
      GolemMaterial.register(new GolemMaterial("BRASS", new String[]{"MATSTUDBRASS"}, rl("textures/entity/golems/mat_brass.png"), 15638812, 16, 6, 3,
         stack(ModItems.PLATE_BRASS), mech, new EnumGolemTrait[]{EnumGolemTrait.LIGHT}));
      GolemMaterial.register(new GolemMaterial("THAUMIUM", new String[]{"MATSTUDTHAUMIUM"}, rl("textures/entity/golems/mat_thaumium.png"), 5257074, 24,
         10, 4, stack(ModItems.PLATE_THAUMIUM), mech,
         new EnumGolemTrait[]{EnumGolemTrait.HEAVY, EnumGolemTrait.FIREPROOF, EnumGolemTrait.BLASTPROOF}));
      GolemMaterial.register(new GolemMaterial("VOID", new String[]{"MATSTUDVOID"}, rl("textures/entity/golems/mat_void.png"), 1445161, 20, 6, 4,
         stack(ModItems.PLATE_VOID), mech, new EnumGolemTrait[]{EnumGolemTrait.REPAIR}));

      Supplier<ItemStack> clockwork = stack(ModItems.MIND_CLOCKWORK);
      Supplier<ItemStack> biothaumic = stack(ModItems.MIND_BIOTHAUMIC);
      Supplier<ItemStack> vision = stack(ModItems.MODULE_VISION);
      Supplier<ItemStack> aggression = stack(ModItems.MODULE_AGGRESSION);
      GolemHead.register(new GolemHead("BASIC", new String[]{"MINDCLOCKWORK"}, rl("textures/misc/golem/head_basic.png"),
         model("golem_head_basic", null, PartModel.EnumAttachPoint.HEAD), new Object[]{clockwork}, new EnumGolemTrait[0]));
      GolemHead.register(new GolemHead("SMART", new String[]{"MINDBIOTHAUMIC"}, rl("textures/misc/golem/head_smart.png"),
         model("golem_head_smart", "textures/entity/golems/golem_head_other.png", PartModel.EnumAttachPoint.HEAD), new Object[]{biothaumic},
         new EnumGolemTrait[]{EnumGolemTrait.SMART, EnumGolemTrait.FRAGILE}));
      GolemHead.register(new GolemHead("SMART_ARMORED", new String[]{"MINDBIOTHAUMIC", "GOLEMCOMBATADV"}, rl("textures/misc/golem/head_smartarmor.png"),
         model("golem_head_smart_armor", null, PartModel.EnumAttachPoint.HEAD),
         new Object[]{biothaumic, stack(ModItems.PLATE_BRASS), "base", (Supplier<ItemStack>) () -> new ItemStack(Blocks.WHITE_WOOL)},
         new EnumGolemTrait[]{EnumGolemTrait.SMART}));
      GolemHead.register(new GolemHead("SCOUT", new String[]{"GOLEMVISION"}, rl("textures/misc/golem/head_scout.png"),
         model("golem_head_scout", "textures/entity/golems/golem_head_other.png", PartModel.EnumAttachPoint.HEAD), new Object[]{clockwork, vision},
         new EnumGolemTrait[]{EnumGolemTrait.SCOUT, EnumGolemTrait.FRAGILE}));
      GolemHead.register(new GolemHead("SMART_SCOUT", new String[]{"GOLEMVISION", "MINDBIOTHAUMIC"}, rl("textures/misc/golem/head_smartscout.png"),
         model("golem_head_scout_smart", "textures/entity/golems/golem_head_other.png", PartModel.EnumAttachPoint.HEAD),
         new Object[]{biothaumic, vision}, new EnumGolemTrait[]{EnumGolemTrait.SCOUT, EnumGolemTrait.SMART, EnumGolemTrait.FRAGILE}));

      GolemArm.register(new GolemArm("BASIC", new String[]{"MINDCLOCKWORK"}, rl("textures/misc/golem/arms_basic.png"),
         model("golem_arms_basic", null, PartModel.EnumAttachPoint.ARMS), new Object[0], new EnumGolemTrait[0]));
      GolemArm.register(new GolemArm("FINE", new String[]{"MATSTUDBRASS"}, rl("textures/misc/golem/arms_fine.png"),
         model("golem_arms_fine", null, PartModel.EnumAttachPoint.ARMS), new Object[]{mech, "base"},
         new EnumGolemTrait[]{EnumGolemTrait.DEFT, EnumGolemTrait.FRAGILE}));
      GolemArm.register(new GolemArm("CLAWS", new String[]{"GOLEMCOMBATADV"}, rl("textures/misc/golem/arms_claws.png"),
         new PartModelClaws(rl("models/obj/golem_arms_claws.obj"), rl("textures/entity/golems/golem_arms_claws.png"), PartModel.EnumAttachPoint.ARMS),
         new Object[]{aggression, (Supplier<ItemStack>) () -> new ItemStack(Items.SHEARS, 2), "base"},
         new EnumGolemTrait[]{EnumGolemTrait.FIGHTER, EnumGolemTrait.CLUMSY, EnumGolemTrait.BRUTAL}));
      GolemArm.register(new GolemArm("BREAKERS", new String[]{"GOLEMBREAKER"}, rl("textures/misc/golem/arms_breakers.png"),
         new PartModelBreakers(rl("models/obj/golem_arms_breakers.obj"), rl("textures/entity/golems/golem_arms_breakers.png"),
            PartModel.EnumAttachPoint.ARMS),
         new Object[]{(Supplier<ItemStack>) () -> new ItemStack(Items.DIAMOND, 2), "base", (Supplier<ItemStack>) () -> new ItemStack(Blocks.PISTON, 2)},
         new EnumGolemTrait[]{EnumGolemTrait.BREAKER, EnumGolemTrait.CLUMSY, EnumGolemTrait.BRUTAL}));
      GolemArm.register(new GolemArm("DARTS", new String[]{"GOLEMCOMBATADV"}, rl("textures/misc/golem/arms_darts.png"),
         new PartModelDarts(rl("models/obj/golem_arms_darter.obj"), rl("textures/entity/golems/golem_arms_darter.png"), PartModel.EnumAttachPoint.ARMS),
         new Object[]{aggression, (Supplier<ItemStack>) () -> new ItemStack(Blocks.DISPENSER, 2),
            (Supplier<ItemStack>) () -> new ItemStack(Items.ARROW, 32), "mech"},
         new GolemArmDart(), new EnumGolemTrait[]{EnumGolemTrait.FIGHTER, EnumGolemTrait.CLUMSY, EnumGolemTrait.RANGED, EnumGolemTrait.FRAGILE}));

      GolemLeg.register(new GolemLeg("WALKER", new String[]{"MINDCLOCKWORK"}, rl("textures/misc/golem/legs_walker.png"),
         model("golem_legs_walker", null, PartModel.EnumAttachPoint.LEGS), new Object[]{"base", "mech"}, new EnumGolemTrait[0]));
      GolemLeg.register(new GolemLeg("ROLLER", new String[]{"MINDCLOCKWORK"}, rl("textures/misc/golem/legs_roller.png"),
         new PartModelWheel(rl("models/obj/golem_legs_wheel.obj"), rl("textures/entity/golems/golem_legs_wheel.png"), PartModel.EnumAttachPoint.BODY),
         new Object[]{(Supplier<ItemStack>) () -> new ItemStack(Items.BOWL, 2), (Supplier<ItemStack>) () -> new ItemStack(Items.LEATHER), "mech"},
         new GolemLegWheels(), new EnumGolemTrait[]{EnumGolemTrait.WHEELED}));
      GolemLeg.register(new GolemLeg("CLIMBER", new String[]{"GOLEMCLIMBER"}, rl("textures/misc/golem/legs_climber.png"),
         model("golem_legs_climber", "textures/blocks/base_metal.png", PartModel.EnumAttachPoint.LEGS),
         new Object[]{(Supplier<ItemStack>) () -> new ItemStack(Items.FLINT, 4), "base", "mech", "mech"},
         new EnumGolemTrait[]{EnumGolemTrait.CLIMBER}));
      GolemLeg.register(new GolemLeg("FLYER", new String[]{"GOLEMFLYER"}, rl("textures/misc/golem/legs_flyer.png"),
         model("golem_legs_floater", "textures/entity/golems/golem_legs_floater.png", PartModel.EnumAttachPoint.BODY),
         new Object[]{optional("levitator"), stack(ModItems.PLATE_BRASS, 4), (Supplier<ItemStack>) () -> new ItemStack(Items.SLIME_BALL), "mech"},
         new GolemLegLevitator(), new EnumGolemTrait[]{EnumGolemTrait.FLYER, EnumGolemTrait.FRAGILE}));

      GolemAddon.register(new GolemAddon("NONE", new String[]{"MINDCLOCKWORK"}, rl("textures/blocks/blank.png"), null, new Object[0],
         new EnumGolemTrait[0]));
      GolemAddon.register(new GolemAddon("ARMORED", new String[]{"GOLEMCOMBATADV"}, rl("textures/misc/golem/addon_armored.png"),
         model("golem_armor", null, PartModel.EnumAttachPoint.BODY), new Object[]{"base", "base", "base", "base"},
         new EnumGolemTrait[]{EnumGolemTrait.ARMORED, EnumGolemTrait.HEAVY}));
      GolemAddon.register(new GolemAddon("FIGHTER", new String[]{"SEALGUARD"}, rl("textures/misc/golem/addon_fighter.png"), null,
         new Object[]{aggression, "mech"}, new EnumGolemTrait[]{EnumGolemTrait.FIGHTER}));
      GolemAddon.register(new GolemAddon("HAULER", new String[]{"MINDCLOCKWORK"}, rl("textures/misc/golem/addon_hauler.png"),
         new PartModelHauler(rl("models/obj/golem_hauler.obj"), rl("textures/entity/golems/golem_hauler.png"), PartModel.EnumAttachPoint.BODY),
         new Object[]{(Supplier<ItemStack>) () -> new ItemStack(Items.LEATHER), (Supplier<ItemStack>) () -> new ItemStack(Blocks.CHEST)},
         new EnumGolemTrait[]{EnumGolemTrait.HAULER}));
   }

   static {
      registerParts();
   }
}
